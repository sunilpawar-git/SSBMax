package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ssbmax.core.data.ai.prompts.TATSynthesisPrompts
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.scoring.ScoringUtils
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.validation.ValidationIntegration
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.TATStoryAnalysisWorker.Companion.FAILED_MARKER
import com.ssbmax.workers.retry.RetryBackoffPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Holistic TAT synthesis worker.
 *
 * Runs after all per-story TATStoryAnalysisWorker instances complete (WorkManager chain).
 * Reads their Room assessments, sends a cross-story prompt to Gemini, and writes the
 * final OLQAnalysisResult to Firestore.
 */
class TATSynthesisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val tatStoryAssessmentDao: TATStoryAssessmentDao by inject()
    private val submissionRepository: SubmissionRepository by inject()
    private val userProfileRepository: UserProfileRepository by inject()
    private val aiService: AIService by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val getOLQDashboard: GetOLQDashboardUseCase by inject()

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val TAG = "TATSynthesisWorker"
        private const val MAX_AI_RETRIES = 3
        private const val MIN_STORY_THRESHOLD = 6
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No submission ID provided")
            return Result.failure()
        }
        return try {
            synthesize(submissionId)
        } catch (e: Exception) {
            ErrorLogger.log(e, "TAT synthesis failed: $submissionId")
            if (runAttemptCount < MAX_AI_RETRIES) Result.retry()
            else {
                handleFailure(submissionId)
                Result.failure()
            }
        }
    }

    private suspend fun synthesize(submissionId: String): Result {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting TAT synthesis: $submissionId")

        val assessments = tatStoryAssessmentDao.getBySubmissionId(submissionId)
        val validAssessments = assessments.filter { it.overallRating != FAILED_MARKER }
        Log.d(
            TAG,
            "   ${assessments.size} total assessments, ${validAssessments.size} valid " +
                "(${assessments.size - validAssessments.size} failed placeholders)"
        )
        if (validAssessments.size < MIN_STORY_THRESHOLD) {
            Log.e(TAG, "❌ Too few valid story assessments: ${validAssessments.size} < $MIN_STORY_THRESHOLD")
            handleFailure(submissionId)
            return Result.failure()
        }
        Log.d(TAG, "   ${validAssessments.size} story assessments ready for synthesis")

        // ANALYZING status is now set by TATAnalysisPipelineOrchestrator before workers start.
        val prompt = TATSynthesisPrompts.buildPrompt(validAssessments)
        Log.d(TAG, "   Synthesis prompt length: ${prompt.length} chars")
        val olqScores = analyzeWithRetry(prompt) ?: run {
            Log.e(TAG, "❌ AI synthesis failed after $MAX_AI_RETRIES retries")
            handleFailure(submissionId)
            return Result.failure()
        }

        val submission = submissionRepository.getTATSubmission(submissionId).getOrNull()

        val userProfile = try {
            submission?.userId?.let { uid ->
                userProfileRepository.getUserProfile(uid).first().getOrNull()
            }
        } catch (e: Exception) {
            null
        }
        val entryType = ScoringUtils.toScoringEntryType(userProfile?.entryType)
        val validationResult = ValidationIntegration.validateScores(olqScores, entryType)
        Log.d(TAG, "   SSB validation — ${validationResult.recommendation}, limitations: ${validationResult.limitationCount}")

        val overallScore = olqScores.values.map { it.score }.average().toFloat()
        val failedCount = assessments.size - validAssessments.size
        val olqResult = buildOlqResult(
            submissionId = submissionId,
            olqScores = olqScores,
            overallScore = overallScore,
            validStoriesCount = validAssessments.size,
            failedStoriesCount = failedCount
        )

        // Finalize result atomically: psych_results first, then COMPLETED status.
        // Side effects only happen after confirmed durable persistence.
        val finalizeResult = submissionRepository.finalizeTATAnalysisResult(submissionId, olqResult)
        if (finalizeResult.isFailure) {
            Log.e(TAG, "❌ Finalization failed: ${finalizeResult.exceptionOrNull()?.message}")
            return if (runAttemptCount < MAX_AI_RETRIES) Result.retry()
            else {
                handleFailure(submissionId)
                Result.failure()
            }
        }
        Log.d(TAG, "   OLQ result finalized in Firestore")

        submission?.userId?.let { userId ->
            try {
                getOLQDashboard.invalidateCache(userId)
                Log.d(TAG, "   Dashboard cache invalidated for user: $userId")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to invalidate cache: ${e.message}")
            }
        }

        try {
            notificationHelper.showTATResultsReadyNotification(submissionId)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to send TAT synthesis notification")
        }

        val durationMs = System.currentTimeMillis() - startTime
        Log.d(TAG, "🎉 TAT synthesis complete in ${durationMs}ms for $submissionId")
        return Result.success()
    }

    private suspend fun analyzeWithRetry(prompt: String): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                Log.d(TAG, "   Synthesis AI attempt ${attempt + 1}/$MAX_AI_RETRIES")
                val result = aiService.analyzeTATResponse(prompt)
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    val olqScores = analysis.olqScores.mapValues { (_, s) ->
                        OLQScore(
                            score = s.score.toInt().coerceIn(5, 9),
                            confidence = analysis.overallConfidence,
                            reasoning = s.reasoning
                        )
                    }
                    if (olqScores.size >= 14) {
                        Log.d(TAG, "   ✅ AI returned ${olqScores.size}/15 OLQs")
                        return if (olqScores.size == 15) olqScores else fillMissingOLQs(olqScores)
                    } else {
                        Log.w(TAG, "   ⚠️ AI returned only ${olqScores.size}/15 OLQs — retrying")
                    }
                } else {
                    Log.e(TAG, "   ❌ AI returned failure: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "   ❌ AI call threw exception: ${e.message}")
            }
            if (attempt < MAX_AI_RETRIES - 1) delay(RetryBackoffPolicy.nextDelayMillis(attempt))
        }
        return null
    }

    private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
        val mutable = scores.toMutableMap()
        OLQ.entries.forEach { olq ->
            if (olq !in mutable) {
                mutable[olq] = OLQScore(score = 6, confidence = 30, reasoning = "Not assessed — neutral score assigned")
            }
        }
        return mutable
    }

    private fun buildOlqResult(
        submissionId: String,
        olqScores: Map<OLQ, OLQScore>,
        overallScore: Float,
        validStoriesCount: Int,
        failedStoriesCount: Int
    ): OLQAnalysisResult {
        val strengths = olqScores.entries.sortedBy { it.value.score }.take(3)
            .map { "${it.key.displayName} (${it.value.score})" }
        val weaknesses = olqScores.entries.sortedByDescending { it.value.score }.take(3)
            .map { "${it.key.displayName} (${it.value.score})" }
        return OLQAnalysisResult(
            submissionId = submissionId,
            testType = TestType.TAT,
            olqScores = olqScores,
            overallScore = overallScore,
            overallRating = ratingFromScore(overallScore),
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = listOf(
                "Review your narrative arcs across all 12 stories",
                "Focus on strengthening: ${weaknesses.joinToString(", ")}",
                "Maintain proactive and optimistic storytelling"
            ),
            analyzedAt = System.currentTimeMillis(),
            aiConfidence = olqScores.values.map { it.confidence }.average().toInt(),
            validStoriesCount = validStoriesCount,
            failedStoriesCount = failedStoriesCount,
            usedPartialAssessment = failedStoriesCount > 0
        )
    }

    private fun ratingFromScore(score: Float): String = when {
        score <= 5.5f -> "Exceptional"
        score <= 6.5f -> "Good"
        score <= 7.5f -> "Average"
        else -> "Needs Improvement"
    }

    private suspend fun handleFailure(submissionId: String) {
        submissionRepository.updateTATAnalysisStatus(submissionId, AnalysisStatus.FAILED)
            .onFailure { e -> ErrorLogger.log(e, "Failed to mark TAT synthesis as FAILED") }
        try {
            notificationHelper.showTATAnalysisFailedNotification(submissionId)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to send TAT analysis failure notification")
        }
    }
}
