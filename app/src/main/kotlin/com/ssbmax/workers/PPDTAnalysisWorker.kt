package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.PPDTQuestion
import com.ssbmax.shared.domain.model.PPDTRating
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.scoring.ScoringUtils
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.validation.ValidationIntegration
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Background worker for analyzing PPDT submissions using Gemini AI (multimodal path).
 *
 * Triggered after user completes PPDT test and submits their story.
 * This worker:
 * 1. Fetches PPDT submission from Firestore
 * 2. Resolves candidate gender from UserProfile
 * 3. Fetches PPDT question (for imageUrl + imageContext rubric)
 * 4. Downloads image bytes (best-effort — analysis continues with empty bytes if download fails)
 * 5. Analyzes with Gemini AI multimodal for OLQ scores (all 15 OLQs)
 * 6. Updates submission in Firestore with OLQ result
 * 7. Invalidates dashboard cache so fresh results are shown
 * 8. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class PPDTAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: SubmissionRepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper,
    private val testContentRepository: TestContentRepository,
    private val getOLQDashboard: GetOLQDashboardUseCase,
    private val userProfileRepository: UserProfileRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val TAG = "PPDTAnalysisWorker"
        private const val MAX_AI_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No submission ID provided")
            ErrorLogger.log(
                "PPDT analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting PPDT analysis for submission: $submissionId")

        return try {
            // 1. Get submission from repository
            val submissionResult = submissionRepository.getPPDTSubmission(submissionId)
            val submission = submissionResult.getOrNull()
            if (submission == null) {
                Log.e(TAG, "❌ PPDT submission not found: $submissionId")
                return Result.failure()
            }

            Log.d(TAG, "   Step 1: PPDT submission found (${submission.charactersCount} characters)")

            // 2. Verify PENDING_ANALYSIS status (don't process if already done)
            if (submission.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) {
                Log.w(TAG, "⚠️ PPDT submission not in PENDING_ANALYSIS state: ${submission.analysisStatus}")
                return Result.success()
            }

            // 3. Update status to ANALYZING
            submissionRepository.updatePPDTAnalysisStatus(submissionId, AnalysisStatus.ANALYZING)
            Log.d(TAG, "   Step 2: Status updated to ANALYZING")

            // 4. Resolve candidate profile (gender + entryType); falls back on any error
            val userProfile = try {
                userProfileRepository.getUserProfile(submission.userId).first().getOrNull()
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to fetch user profile for PPDT analysis")
                null
            }
            val candidateGender = userProfile?.gender?.displayName ?: "Unknown"
            val entryType = ScoringUtils.toScoringEntryType(userProfile?.entryType)

            // 5. Fetch full question (imageUrl + imageContext rubric)
            var ppdtQuestion: PPDTQuestion? = null
            try {
                val questionResult = testContentRepository.getPPDTQuestion(submission.questionId)
                ppdtQuestion = questionResult.getOrNull()
                if (ppdtQuestion != null) {
                    Log.d(TAG, "   Step 3: Retrieved PPDT question for ID: ${submission.questionId}")
                } else {
                    Log.w(TAG, "   ⚠️ PPDT question not found for ID: ${submission.questionId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "   ❌ Error fetching PPDT question", e)
            }

            // 6. Download image bytes (best-effort — analysis continues with empty bytes if download fails)
            val imageBytes: ByteArray = ppdtQuestion?.imageUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { imageUrl ->
                    try {
                        withContext(Dispatchers.IO) {
                            val conn = URL(imageUrl).openConnection().apply {
                                connectTimeout = 10_000
                                readTimeout = 20_000
                            }
                            conn.getInputStream().readBytes()
                        }
                    } catch (e: Exception) {
                        ErrorLogger.log(e, "PPDT image download failed — proceeding with empty bytes")
                        null
                    }
                } ?: ByteArray(0)
            Log.d(TAG, "   Step 4: Image bytes prepared (${imageBytes.size} bytes)")

            // 7. Analyze with Gemini AI multimodal (with retry logic)
            val imageContext = ppdtQuestion?.imageContext ?: PPDTImageContext()
            val olqScores = analyzeSubmissionMultimodal(
                imageBytes = imageBytes,
                story = submission.story,
                imageContext = imageContext,
                candidateGender = candidateGender
            )
            if (olqScores == null) {
                Log.e(TAG, "❌ AI analysis failed after $MAX_AI_RETRIES retries")
                handleAnalysisFailure(submissionId)
                return Result.failure()
            }

            Log.d(TAG, "   Step 5: AI analysis complete - received ${olqScores.size}/15 OLQ scores")

            // 7.5. Validate OLQ scores against SSB rules using actual candidate entry type
            val validationResult = ValidationIntegration.validateScores(olqScores, entryType)
            Log.d(TAG, "   Step 6: SSB Validation - ${validationResult.recommendation}, limitations: ${validationResult.limitationCount}")
            if (!validationResult.isValid || validationResult.hasCriticalWeakness) {
                Log.w(TAG, "⚠️ SSB Validation alert: ${validationResult.summary}")
            }

            // 8. Create OLQAnalysisResult
            val overallScore = olqScores.values.map { it.score }.average().toFloat()
            val overallRating = PPDTRating.fromScore(overallScore).displayKey

            val strengths = olqScores.entries
                .sortedBy { it.value.score }
                .take(3)
                .map { "${it.key.displayName} (${it.value.score})" }

            val weaknesses = olqScores.entries
                .sortedByDescending { it.value.score }
                .take(3)
                .map { "${it.key.displayName} (${it.value.score})" }

            val recommendations = listOf(
                "Continue practicing PPDT with diverse scenarios",
                "Focus on strengthening: ${weaknesses.joinToString(", ")}",
                "Maintain clear and positive storytelling"
            )

            val olqResult = OLQAnalysisResult(
                submissionId = submissionId,
                testType = TestType.PPDT,
                olqScores = olqScores,
                overallScore = overallScore,
                overallRating = overallRating,
                strengths = strengths,
                weaknesses = weaknesses,
                recommendations = recommendations,
                analyzedAt = System.currentTimeMillis(),
                aiConfidence = olqScores.values.firstOrNull()?.confidence ?: 50
            )

            // 9. Update submission with OLQ result (atomically sets COMPLETED status)
            submissionRepository.updatePPDTOLQResult(submissionId, olqResult)
            Log.d(TAG, "   Step 7: Submission updated with OLQ result")

            // 10. Invalidate dashboard cache AFTER result is in Firestore
            try {
                getOLQDashboard.invalidateCache(submission.userId)
                Log.d(TAG, "   Step 8: Dashboard cache invalidated")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to invalidate cache: ${e.message}")
            }

            // 11. Send notification
            try {
                notificationHelper.showPPDTResultsReadyNotification(submissionId)
                Log.d(TAG, "✅ Push notification sent successfully!")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to send notification: ${e.message}")
            }

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ PPDT analysis complete in ${duration}ms")
            Result.success()

        } catch (e: Exception) {
            ErrorLogger.log(e, "PPDT analysis failed for submission: $submissionId")
            handleAnalysisFailure(submissionId)
            Result.failure()
        }
    }

    private suspend fun analyzeSubmissionMultimodal(
        imageBytes: ByteArray,
        story: String,
        imageContext: PPDTImageContext,
        candidateGender: String
    ): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                Log.d(TAG, "   Attempt ${attempt + 1}/$MAX_AI_RETRIES: Calling Gemini AI (multimodal)...")
                val analysisResult = aiService.analyzePPDTMultimodal(
                    imageBytes = imageBytes,
                    story = story,
                    imageContext = imageContext,
                    candidateGender = candidateGender
                )

                if (analysisResult.isSuccess) {
                    val analysis = analysisResult.getOrNull()!!
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(5, 9),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }

                    if (olqScores.size == 15) {
                        Log.d(TAG, "   ✅ AI returned all 15 OLQs")
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

    private suspend fun handleAnalysisFailure(submissionId: String) {
        try {
            submissionRepository.updatePPDTAnalysisStatus(submissionId, AnalysisStatus.FAILED)
            Log.d(TAG, "   Marked submission as FAILED")

            try {
                notificationHelper.showPPDTAnalysisFailedNotification(submissionId)
                Log.d(TAG, "   Sent failure notification to user")
            } catch (e: Exception) {
                Log.w(TAG, "   Failed to send failure notification: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "   Failed to update status to FAILED", e)
        }
    }
}
