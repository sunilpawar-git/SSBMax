package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.GTOAnalysisPrompts
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Background worker for analyzing GTO test submissions using Gemini AI
 *
 * This worker:
 * 1. Fetches submission from Firestore
 * 2. Analyzes response with Gemini AI for OLQ scores
 * 3. Updates submission with OLQ scores
 * 4. Marks submission as completed
 * 5. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class GTOAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gtoRepository: GTORepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GTOAnalysisWorker"
        const val KEY_SUBMISSION_ID = "submission_id"
        const val KEY_TEST_TYPE = "test_type"
        
        // Retry configuration
        private const val MAX_AI_RETRIES = 3
        
        // API call delays
        private const val API_CALL_DELAY_MS = 1000L
        private const val FALLBACK_CONFIDENCE = 30 // Low confidence for fallback scores
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No submission ID provided")
            ErrorLogger.log(
                "GTO analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting GTO background analysis for submission: $submissionId")
        ErrorLogger.log(
            "GTO analysis worker started",
            mapOf("submissionId" to submissionId, "attempt" to runAttemptCount.toString()),
            ErrorLogger.Severity.INFO
        )

        return try {
            // 1. Get submission
            val submissionResult = gtoRepository.getSubmission(submissionId)
            val submission = submissionResult.getOrNull()
            if (submission == null) {
                Log.e(TAG, "❌ Submission not found: $submissionId")
                return Result.failure()
            }

            Log.d(TAG, "   Step 1: Submission found - ${submission.testType}")

            // Verify submission is in PENDING_ANALYSIS state
            if (submission.status != GTOSubmissionStatus.PENDING_ANALYSIS) {
                Log.w(TAG, "⚠️ Submission not in PENDING_ANALYSIS state: ${submission.status}")
                // Don't fail, just skip - might be already processed
                return Result.success()
            }

            // 2. Update status to ANALYZING
            gtoRepository.updateSubmissionStatus(submissionId, GTOSubmissionStatus.ANALYZING)
            Log.d(TAG, "   Step 2: Status updated to ANALYZING")

            // 3. Analyze with AI
            Log.d(TAG, "   Step 3: Analyzing with AI...")
            
            // Analyze with retries
            val olqScores = analyzeSubmissionWithRetry(
                submission = submission,
                testType = submission.testType
            )

            if (olqScores != null) {
                // Update submission with OLQ scores
                val updateResult = gtoRepository.updateSubmissionOLQScores(submissionId, olqScores)
                
                if (updateResult.isFailure) {
                    // Firestore update failed - log error and retry
                    val error = updateResult.exceptionOrNull()
                    Log.e(TAG, "❌ Failed to save OLQ scores to Firestore", error)
                    ErrorLogger.log(
                        error ?: Exception("Unknown Firestore error"),
                        "Failed to update submission OLQ scores in Firestore"
                    )
                    
                    // Mark as failed so WorkManager can retry
                    gtoRepository.updateSubmissionStatus(submissionId, GTOSubmissionStatus.FAILED)
                    return Result.retry()
                }
                
                Log.d(TAG, "   ✅ Analysis complete and saved to Firestore")

                // Send notification
                val duration = (System.currentTimeMillis() - startTime) / 1000
                Log.d(TAG, "✅ GTO analysis completed in ${duration}s")
                
                notificationHelper.showGTOAnalysisCompleteNotification(
                    submissionId,
                    submission.testType.displayName,
                    submission.testType
                )

                ErrorLogger.log(
                    "GTO analysis worker completed successfully",
                    mapOf(
                        "submissionId" to submissionId,
                        "testType" to submission.testType.name,
                        "duration" to duration.toString()
                    ),
                    ErrorLogger.Severity.INFO
                )

                Result.success()
            } else {
                Log.e(TAG, "   ❌ AI analysis failed after retries")

                // Use fallback scores
                val fallbackScores = generateFallbackOLQScores()
                gtoRepository.updateSubmissionOLQScores(submissionId, fallbackScores)

                Log.w(TAG, "⚠️ Using fallback scores due to AI failure")
                notificationHelper.showGTOAnalysisCompleteNotification(
                    submissionId,
                    submission.testType.displayName,
                    submission.testType
                )

                ErrorLogger.log(
                    "GTO analysis worker completed with fallback scores",
                    mapOf(
                        "submissionId" to submissionId,
                        "testType" to submission.testType.name
                    ),
                    ErrorLogger.Severity.WARNING
                )

                Result.success()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "GTO analysis worker failed")
            Log.e(TAG, "❌ Worker failed", e)

            // Mark as failed
            try {
                gtoRepository.updateSubmissionStatus(submissionId!!, GTOSubmissionStatus.FAILED)
            } catch (updateError: Exception) {
                ErrorLogger.log(updateError, "Failed to update submission status to FAILED")
            }

            // Retry up to 3 times
            return if (runAttemptCount < 3) {
                Log.d(TAG, "⚠️ Retrying (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                Log.e(TAG, "❌ Max retries reached, failing")
                Result.failure()
            }
        }
    }

    /**
     * Analyze submission with retry logic
     * Enhanced validation: accepts 14-15 OLQs, fills missing ones with neutral score
     */
    private suspend fun analyzeSubmissionWithRetry(
        submission: GTOSubmission,
        testType: GTOTestType
    ): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            Log.d(TAG, "   AI attempt ${attempt + 1}/$MAX_AI_RETRIES...")

            // Analyze with Gemini (same pattern as Interview tests)
            val olqScores = analyzeWithGemini(
                submission = submission,
                testType = testType
            )

            // Detailed logging
            Log.d(TAG, "   Attempt ${attempt + 1}: Received ${olqScores?.size ?: 0}/15 OLQs")

            if (olqScores != null) {
                val missingOLQs = OLQ.entries.filter { it !in olqScores.keys }
                if (missingOLQs.isNotEmpty()) {
                    Log.w(TAG, "   Missing OLQs: ${missingOLQs.joinToString { it.name }}")
                }
            }

            // Accept if we have 14-15 OLQs (allow 1 missing)
            if (olqScores != null && olqScores.size >= 14) {
                return if (olqScores.size == 15) {
                    Log.d(TAG, "   ✅ All 15 OLQs received")
                    olqScores
                } else {
                    Log.w(TAG, "   ⚠️ Filling ${15 - olqScores.size} missing OLQ(s) with neutral scores")
                    fillMissingOLQs(olqScores)
                }
            }

            Log.w(TAG, "   ⚠️ Insufficient OLQs (${olqScores?.size ?: 0}/15 - need at least 14)")

            // Exponential backoff before retry
            if (attempt < MAX_AI_RETRIES - 1) {
                val delayMs = 1000L * (attempt + 1) * 2 // Exponential backoff
                Log.d(TAG, "   Waiting ${delayMs}ms before retry...")
                delay(delayMs)
            }
        }

        return null
    }

    /**
     * Fill missing OLQs with neutral scores (6/10)
     * This ensures we always have all 15 OLQs for consistency
     */
    private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
        val mutable = scores.toMutableMap()
        OLQ.entries.forEach { olq ->
            if (olq !in mutable) {
                mutable[olq] = OLQScore(
                    score = 6, // Neutral "Good" score
                    confidence = 30,
                    reasoning = "AI did not assess this OLQ - neutral score assigned"
                )
                Log.d(TAG, "      Filled missing OLQ: ${olq.name} with score 6")
            }
        }
        return mutable
    }

    private fun generateAnalysisPrompt(submission: GTOSubmission): String {
        return GTOAnalysisPrompts.generateAnalysisPrompt(submission)
    }

    /**
     * Analyze with Gemini using clean AIService pattern
     * 
     * Uses the same clean approach as Interview tests:
     * aiService.analyzeResponse() for interviews
     * aiService.analyzeGTOResponse() for GTO tests
     * 
     * This eliminates custom JSON parsing and workarounds.
     */
    private suspend fun analyzeWithGemini(
        submission: GTOSubmission,
        testType: GTOTestType
    ): Map<OLQ, OLQScore>? {
        return try {
            Log.d(TAG, "🤖 Calling Gemini AI for $testType analysis")
            
            // Generate GTO-specific prompt (worker has access to GTOAnalysisPrompts)
            val prompt = GTOAnalysisPrompts.generateAnalysisPrompt(submission)
            
            // Call AI service with prompt
            val analysisResult = aiService.analyzeGTOResponse(
                prompt = prompt,
                testType = testType
            )
            
            if (analysisResult.isFailure) {
                ErrorLogger.log(
                    analysisResult.exceptionOrNull() ?: Exception("Unknown error"),
                    "Gemini analysis failed for $testType"
                )
                return null
            }
            
            val analysis = analysisResult.getOrNull() ?: return null
            
            // Convert ResponseAnalysis to OLQScore map (same as Interview pattern)
            val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                OLQScore(
                    score = scoreWithReasoning.score.toInt(),
                    confidence = analysis.overallConfidence,
                    reasoning = scoreWithReasoning.reasoning
                )
            }
            
            Log.d(TAG, "✅ Gemini analysis complete: ${olqScores.size} OLQ scores (confidence: ${analysis.overallConfidence}%)")
            olqScores
            
        } catch (e: Exception) {
            ErrorLogger.log(e, "Gemini API call failed for $testType")
            null
        }
    }


    /**
     * Generate fallback OLQ scores (neutral 6/10 for all)
     */
    private fun generateFallbackOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.entries.associateWith { olq ->
            OLQScore(
                score = 6, // Neutral "Good" score
                confidence = FALLBACK_CONFIDENCE,
                reasoning = "AI analysis unavailable - neutral score assigned. Please retake test for accurate assessment."
            )
        }
    }
}
