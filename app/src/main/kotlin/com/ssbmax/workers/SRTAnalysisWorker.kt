package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.data.ai.prompts.PsychologyTestPrompts
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Background worker for analyzing SRT submissions using Gemini AI
 *
 * Triggered after user completes SRT test and submits all reactions.
 * This worker:
 * 1. Fetches SRT submission from Firestore
 * 2. Generates analysis prompt using PsychologyTestPrompts
 * 3. Analyzes with Gemini AI for OLQ scores (all 15 OLQs)
 * 4. Updates submission in Firestore with OLQ result
 * 5. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class SRTAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: SubmissionRepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val TAG = "SRTAnalysisWorker"
        private const val MAX_AI_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No submission ID provided")
            ErrorLogger.log(
                "SRT analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting SRT analysis for submission: $submissionId")

        return try {
            // 1. Get submission from repository
            val submissionResult = submissionRepository.getSRTSubmission(submissionId)
            val submission = submissionResult.getOrNull()
            if (submission == null) {
                Log.e(TAG, "❌ SRT submission not found: $submissionId")
                return Result.failure()
            }

            Log.d(TAG, "   Step 1: SRT submission found with ${submission.responses.size} responses")

            // 2. Verify PENDING_ANALYSIS status (don't process if already done)
            if (submission.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) {
                Log.w(TAG, "⚠️ SRT submission not in PENDING_ANALYSIS state: ${submission.analysisStatus}")
                return Result.success()  // Already processed, skip
            }

            // 3. Update status to ANALYZING
            submissionRepository.updateSRTAnalysisStatus(submissionId, AnalysisStatus.ANALYZING)
            Log.d(TAG, "   Step 2: Status updated to ANALYZING")

            // 4. Generate SRT analysis prompt
            val prompt = PsychologyTestPrompts.generateSRTAnalysisPrompt(submission)
            Log.d(TAG, "   Step 3: Generated SRT analysis prompt")

            // 5. Analyze with Gemini AI (with retry logic)
            val olqScores = analyzeSubmissionWithRetry(prompt)
            if (olqScores == null) {
                Log.e(TAG, "❌ AI analysis failed after $MAX_AI_RETRIES retries")
                handleAnalysisFailure(submissionId)
                return Result.failure()
            }

            Log.d(TAG, "   Step 4: AI analysis complete - received ${olqScores.size}/15 OLQ scores")

            // 6. Create OLQAnalysisResult
            val overallScore = olqScores.values.map { it.score }.average().toFloat()
            val overallRating = when {
                overallScore <= 3.0f -> "Exceptional"
                overallScore <= 5.0f -> "Good"
                overallScore <= 7.0f -> "Average"
                else -> "Needs Improvement"
            }

            // Extract top 3 strengths (lowest scores)
            val strengths = olqScores.entries
                .sortedBy { it.value.score }
                .take(3)
                .map { "${it.key.displayName} (${it.value.score})" }

            // Extract top 3 weaknesses (highest scores)
            val weaknesses = olqScores.entries
                .sortedByDescending { it.value.score }
                .take(3)
                .map { "${it.key.displayName} (${it.value.score})" }

            val recommendations = listOf(
                "Continue practicing SRT with realistic scenarios",
                "Focus on strengthening: ${weaknesses.joinToString(", ")}",
                "Develop balanced and mature reactions to challenges"
            )

            val olqResult = OLQAnalysisResult(
                submissionId = submissionId,
                testType = TestType.SRT,
                olqScores = olqScores,
                overallScore = overallScore,
                overallRating = overallRating,
                strengths = strengths,
                weaknesses = weaknesses,
                recommendations = recommendations,
                analyzedAt = System.currentTimeMillis(),
                aiConfidence = 85  // Default confidence
            )

            // 7. Update submission with OLQ result
            submissionRepository.updateSRTOLQResult(submissionId, olqResult)
            submissionRepository.updateSRTAnalysisStatus(submissionId, AnalysisStatus.COMPLETED)
            Log.d(TAG, "   Step 5: Submission updated with OLQ result")

            // 8. Send notification
            try {
                notificationHelper.showSRTResultsReadyNotification(submissionId)
                Log.d(TAG, "✅ Push notification sent successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send notification", e)
                ErrorLogger.log(e, "Failed to send SRT result notification")
            }

            val durationMs = System.currentTimeMillis() - startTime
            Log.d(TAG, "🎉 SRT analysis completed successfully in ${durationMs}ms")
            ErrorLogger.log(
                "SRT analysis worker completed successfully",
                mapOf(
                    "submissionId" to submissionId,
                    "overallScore" to overallScore.toString(),
                    "durationMs" to durationMs.toString()
                ),
                ErrorLogger.Severity.INFO
            )
            Result.success()

        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            ErrorLogger.log(e, "SRT analysis worker failed for submission: $submissionId")

            if (runAttemptCount < MAX_AI_RETRIES) {
                Log.w(TAG, "⚠️ Retry attempt ${runAttemptCount + 1}/$MAX_AI_RETRIES")
                Result.retry()
            } else {
                Log.e(TAG, "❌ Max retries reached, marking submission as failed")
                handleAnalysisFailure(submissionId)
                Result.failure()
            }
        }
    }

    /**
     * Analyze SRT submission with retry logic
     *
     * Retries up to MAX_AI_RETRIES times with exponential backoff.
     * Accepts if 14-15 OLQs are present (fills missing with neutral score).
     */
    private suspend fun analyzeSubmissionWithRetry(prompt: String): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                Log.d(TAG, "   AI analysis attempt ${attempt + 1}/$MAX_AI_RETRIES")

                val analysisResult = aiService.analyzeSRTResponse(prompt)

                if (analysisResult.isSuccess) {
                    val analysis = analysisResult.getOrNull()!!

                    // Convert ResponseAnalysis to OLQScore map
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(1, 10),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }

                    Log.d(TAG, "   Received ${olqScores.size}/15 OLQs")

                    // Accept if we have 14-15 OLQs (allow 1 missing)
                    if (olqScores.size >= 14) {
                        return if (olqScores.size == 15) {
                            olqScores
                        } else {
                            fillMissingOLQs(olqScores)
                        }
                    } else {
                        Log.w(TAG, "   Only ${olqScores.size}/15 OLQs - retrying")
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "   Analysis attempt ${attempt + 1} failed: ${e.message}")
            }

            // Exponential backoff before retry
            if (attempt < MAX_AI_RETRIES - 1) {
                val delayMs = RETRY_DELAY_MS * (attempt + 1) * 2
                delay(delayMs)
            }
        }
        return null
    }

    /**
     * Fill missing OLQs with neutral scores
     */
    private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
        val mutable = scores.toMutableMap()
        OLQ.entries.forEach { olq ->
            if (olq !in mutable) {
                mutable[olq] = OLQScore(
                    score = 6,  // Neutral score
                    confidence = 30,
                    reasoning = "AI did not assess this OLQ - neutral score assigned"
                )
            }
        }
        return mutable
    }

    /**
     * Handle analysis failure - update status to FAILED
     */
    private suspend fun handleAnalysisFailure(submissionId: String) {
        try {
            submissionRepository.updateSRTAnalysisStatus(submissionId, AnalysisStatus.FAILED)
            notificationHelper.showSRTAnalysisFailedNotification(submissionId)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to update SRT submission status to FAILED")
        }
    }
}
