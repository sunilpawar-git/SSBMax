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
 * Background worker for analyzing PPDT submissions using Gemini AI
 *
 * Triggered after user completes PPDT test and submits their story.
 * This worker:
 * 1. Fetches PPDT submission from Firestore
 * 2. Generates analysis prompt using PsychologyTestPrompts
 * 3. Analyzes with Gemini AI for OLQ scores (all 15 OLQs)
 * 4. Updates submission in Firestore with OLQ result
 * 5. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class PPDTAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: SubmissionRepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper
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
                return Result.success()  // Already processed, skip
            }

            // 3. Update status to ANALYZING
            submissionRepository.updatePPDTAnalysisStatus(submissionId, AnalysisStatus.ANALYZING)
            Log.d(TAG, "   Step 2: Status updated to ANALYZING")

            // 4. Generate PPDT analysis prompt
            val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission)
            Log.d(TAG, "   Step 3: Generated PPDT analysis prompt")

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
                aiConfidence = 85  // Default confidence
            )

            // 7. Update submission with OLQ result
            // Note: updatePPDTOLQResult atomically sets BOTH olqResult AND analysisStatus=COMPLETED
            // Do NOT call updatePPDTAnalysisStatus separately - it causes redundant writes and potential sync issues
            submissionRepository.updatePPDTOLQResult(submissionId, olqResult)
            Log.d(TAG, "   Step 5: Submission updated with OLQ result")

            // 8. Send notification
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
            Log.e(TAG, "❌ PPDT analysis failed", e)
            ErrorLogger.log(e, "PPDT analysis failed for submission: $submissionId")
            handleAnalysisFailure(submissionId)
            Result.failure()
        }
    }

    private suspend fun analyzeSubmissionWithRetry(prompt: String): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                Log.d(TAG, "   Attempt ${attempt + 1}/$MAX_AI_RETRIES: Calling Gemini AI...")
                val analysisResult = aiService.analyzePPDTResponse(prompt)
                
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
                    
                    // Validate 15 OLQs
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
                delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
            }
        }
        return null
    }

    /**
     * Handle analysis failure by marking as FAILED and sending notification
     */
    private suspend fun handleAnalysisFailure(submissionId: String) {
        try {
            submissionRepository.updatePPDTAnalysisStatus(submissionId, AnalysisStatus.FAILED)
            Log.d(TAG, "   Marked submission as FAILED")
            
            // Send failure notification to user
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
