package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.core.data.local.entity.TATStoryAssessmentEntity
import com.ssbmax.core.domain.model.TATImageContext
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.UUID

/**
 * Per-story multimodal TAT analysis worker.
 *
 * Enqueued after a single TAT story is written (or at submission time per story).
 * Downloads the TAT image bytes and calls Gemini multimodal with the per-picture
 * rubric from imageContextJson — mirrors PPDTAnalysisWorker but scoped to one story.
 *
 * Results are cached in tat_story_assessments (Room) for immediate display.
 */
@HiltWorker
class TATStoryAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: SubmissionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val aiService: AIService,
    private val tatStoryAssessmentDao: TATStoryAssessmentDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        const val KEY_QUESTION_ID = "question_id"
        const val KEY_STORY_INDEX = "story_index"
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_IMAGE_CONTEXT_JSON = "image_context_json"
        const val FAILED_MARKER = "FAILED"
        private const val TAG = "TATStoryAnalysisWorker"
        private const val MAX_AI_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        val questionId = inputData.getString(KEY_QUESTION_ID)
        val storyIndex = inputData.getInt(KEY_STORY_INDEX, -1)

        if (submissionId.isNullOrBlank() || questionId.isNullOrBlank() || storyIndex < 0) {
            Log.e(TAG, "❌ Missing required input: submissionId=$submissionId, story=$storyIndex")
            return Result.failure()
        }

        return try {
            analyzeStory(submissionId, questionId, storyIndex)
        } catch (e: Exception) {
            ErrorLogger.log(e, "TAT story analysis failed: submission=$submissionId, story=$storyIndex")
            if (runAttemptCount < MAX_AI_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun analyzeStory(submissionId: String, questionId: String, storyIndex: Int): Result {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting per-story TAT analysis: submission=$submissionId, story=$storyIndex")

        val submission = submissionRepository.getTATSubmission(submissionId).getOrNull()
            ?: run {
                // Transient Firestore read failure — retry so WorkManager retries the worker
                Log.e(TAG, "❌ TAT submission not found: $submissionId (will retry)")
                error("TAT submission not found: $submissionId")
            }

        val storyResponse = submission.stories.find { it.questionId == questionId }
        if (storyResponse == null) {
            // Story ID mismatch — data issue, retry won't help. Save placeholder so chain continues.
            Log.e(TAG, "❌ Story not found: questionId=$questionId — saving placeholder, not blocking chain")
            saveFailedPlaceholder(submissionId, questionId, storyIndex, "", "")
            return Result.success()
        }

        val imageUrl = inputData.getString(KEY_IMAGE_URL) ?: ""
        val imageContextJson = inputData.getString(KEY_IMAGE_CONTEXT_JSON)

        val imageBytes = downloadImageBytes(imageUrl)
        val imageContext = parseImageContext(imageContextJson)
        val candidateGender = fetchCandidateGender(submission.userId)
        Log.d(TAG, "   Step 1: Image bytes prepared (${imageBytes.size} bytes)")

        val olqScores = analyzeStoryWithRetry(
            imageBytes = imageBytes,
            story = storyResponse.story,
            imageContext = imageContext,
            candidateGender = candidateGender,
            storyIndex = storyIndex,
            totalStories = submission.stories.size
        )
        if (olqScores == null) {
            // AI analysis exhausted retries. Save placeholder so synthesis can still run.
            Log.e(TAG, "❌ Per-story AI analysis failed after $MAX_AI_RETRIES retries — saving placeholder, not blocking chain")
            saveFailedPlaceholder(submissionId, questionId, storyIndex, storyResponse.story, imageUrl)
            return Result.success()
        }

        Log.d(TAG, "   Step 2: AI analysis complete — ${olqScores.size}/15 OLQs")
        saveAssessment(submissionId, questionId, storyIndex, storyResponse.story, imageUrl, olqScores)

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "✅ Per-story TAT analysis complete in ${duration}ms (story $storyIndex)")
        return Result.success()
    }

    private suspend fun downloadImageBytes(imageUrl: String): ByteArray {
        if (imageUrl.isBlank()) return ByteArray(0)
        return try {
            withContext(Dispatchers.IO) {
                val conn = URL(imageUrl).openConnection().apply {
                    connectTimeout = 10_000
                    readTimeout = 20_000
                }
                conn.getInputStream().readBytes()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "TAT story image download failed — proceeding with empty bytes")
            ByteArray(0)
        }
    }

    private fun parseImageContext(imageContextJson: String?): TATImageContext {
        if (imageContextJson.isNullOrBlank()) return TATImageContext()
        return try {
            val obj = JSONObject(imageContextJson)
            TATImageContext(
                sceneDescription = obj.optString("sceneDescription"),
                coreElements = obj.optJSONArray("coreElements").toStringList(),
                ambiguousElements = obj.optJSONArray("ambiguousElements").toStringList(),
                expectedThemes = obj.optJSONArray("expectedThemes").toStringList(),
                penalizedThemes = obj.optJSONArray("penalizedThemes").toStringList(),
                primaryOLQs = obj.optJSONArray("primaryOLQs").toStringList(),
                exemplarGoodHints = obj.optJSONArray("exemplarGoodHints").toStringList(),
                exemplarBadHints = obj.optJSONArray("exemplarBadHints").toStringList()
            )
        } catch (e: Exception) {
            TATImageContext()
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        this ?: return emptyList()
        return (0 until length()).map { getString(it) }
    }

    private suspend fun fetchCandidateGender(userId: String): String {
        return try {
            userProfileRepository.getUserProfile(userId).first().getOrNull()
                ?.gender?.displayName ?: "Unknown"
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to fetch user profile for TAT story analysis — defaulting")
            "Unknown"
        }
    }

    private suspend fun saveFailedPlaceholder(
        submissionId: String,
        questionId: String,
        storyIndex: Int,
        story: String,
        imageUrl: String
    ) {
        try {
            tatStoryAssessmentDao.insert(
                TATStoryAssessmentEntity(
                    id = UUID.randomUUID().toString(),
                    submissionId = submissionId,
                    questionId = questionId,
                    storyIndex = storyIndex,
                    story = story,
                    imageUrl = imageUrl,
                    olqScoresJson = "[]",
                    overallScore = 0f,
                    overallRating = FAILED_MARKER,
                    aiConfidence = 0,
                    analyzedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to save placeholder for story $storyIndex: ${e.message}")
        }
    }

    private suspend fun saveAssessment(
        submissionId: String,
        questionId: String,
        storyIndex: Int,
        story: String,
        imageUrl: String,
        olqScores: Map<OLQ, OLQScore>
    ) {
        val overallScore = olqScores.values.map { it.score }.average().toFloat()
        val overallRating = ratingFromScore(overallScore)
        val aiConfidence = olqScores.values.firstOrNull()?.confidence ?: 50
        val olqScoresJson = JSONArray().also { arr ->
            olqScores.forEach { (olq, score) ->
                arr.put(JSONObject().apply {
                    put("olq", olq.name)
                    put("score", score.score)
                    put("confidence", score.confidence)
                    put("reasoning", score.reasoning)
                })
            }
        }.toString()
        tatStoryAssessmentDao.insert(
            TATStoryAssessmentEntity(
                id = UUID.randomUUID().toString(),
                submissionId = submissionId,
                questionId = questionId,
                storyIndex = storyIndex,
                story = story,
                imageUrl = imageUrl,
                olqScoresJson = olqScoresJson,
                overallScore = overallScore,
                overallRating = overallRating,
                aiConfidence = aiConfidence,
                analyzedAt = System.currentTimeMillis()
            )
        )
    }

    private fun ratingFromScore(score: Float): String = when {
        score <= 5.5f -> "Exceptional"
        score <= 6.5f -> "Good"
        score <= 7.5f -> "Average"
        else -> "Needs Improvement"
    }

    private suspend fun analyzeStoryWithRetry(
        imageBytes: ByteArray,
        story: String,
        imageContext: TATImageContext,
        candidateGender: String,
        storyIndex: Int,
        totalStories: Int
    ): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                Log.d(TAG, "   Attempt ${attempt + 1}/$MAX_AI_RETRIES: Calling Gemini AI (multimodal)...")
                val result = aiService.analyzeTATStoryMultimodal(
                    imageBytes = imageBytes,
                    story = story,
                    imageContext = imageContext,
                    candidateGender = candidateGender,
                    storyIndex = storyIndex,
                    totalStories = totalStories
                )

                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(5, 9),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }
                    if (olqScores.size >= 14) {
                        Log.d(TAG, "   ✅ AI returned ${olqScores.size}/15 OLQs")
                        return olqScores
                    } else {
                        Log.w(TAG, "   ⚠️ AI returned ${olqScores.size}/15 OLQs, retrying...")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "   ❌ AI call failed: ${e.message}")
            }

            if (attempt < MAX_AI_RETRIES - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return null
    }
}
