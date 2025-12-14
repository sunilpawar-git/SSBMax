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
        private const val API_CALL_DELAY_MS = 500L // Delay between API calls
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
            val prompt = generateAnalysisPrompt(submission)
            val analysisResult = analyzeSubmissionWithRetry(prompt, submission.testType)

            if (analysisResult != null) {
                // Update submission with OLQ scores
                gtoRepository.updateSubmissionOLQScores(submissionId, analysisResult)
                Log.d(TAG, "   ✅ Analysis complete and saved")

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
     */
    private suspend fun analyzeSubmissionWithRetry(
        prompt: String,
        testType: GTOTestType,
        maxRetries: Int = 3
    ): Map<OLQ, OLQScore>? {
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "      AI attempt ${attempt + 1}/$maxRetries...")
                val olqScores = analyzeWithGemini(prompt, testType)

                if (olqScores != null && olqScores.size == 15) {
                    Log.d(TAG, "      ✅ AI analysis successful")
                    return olqScores
                }

                Log.w(TAG, "      ⚠️ AI returned incomplete scores (${olqScores?.size ?: 0}/15)")
            } catch (e: Exception) {
                ErrorLogger.log(e, "AI analysis attempt ${attempt + 1} failed")
                Log.e(TAG, "      ❌ AI attempt ${attempt + 1} failed", e)
            }

            if (attempt < maxRetries - 1) {
                val backoffDelay = API_CALL_DELAY_MS * (attempt + 1) * 2
                Log.d(TAG, "      Waiting ${backoffDelay}ms before retry...")
                delay(backoffDelay)
            }
        }

        return null
    }

    private fun generateAnalysisPrompt(submission: GTOSubmission): String {
        return GTOAnalysisPrompts.generateAnalysisPrompt(submission)
    }

    /**
     * Analyze with Gemini using direct API call
     * 
     * Calls Gemini API directly with GTO-specific prompt and parses the JSON response.
     * The GTO prompts return a different JSON format than interview analysis:
     * {
     *   "olqScores": {
     *     "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "..."},
     *     ...
     *   }
     * }
     */
    private suspend fun analyzeWithGemini(
        prompt: String,
        testType: GTOTestType
    ): Map<OLQ, OLQScore>? {
        return try {
            Log.d(TAG, "🤖 Calling Gemini AI for $testType analysis")
            
            // Call Gemini API directly
            val response = aiService.callGeminiDirect(prompt)
            
            if (response.isFailure) {
                ErrorLogger.log(
                    response.exceptionOrNull() ?: Exception("Unknown error"),
                    "Gemini analysis failed for $testType"
                )
                return null
            }
            
            // Parse GTO-specific JSON format
            val jsonText = response.getOrNull() ?: return null
            val olqScores = parseGTOAnalysisResponse(jsonText)
            
            if (olqScores != null) {
                Log.d(TAG, "✅ Gemini analysis complete: ${olqScores.size} OLQ scores")
            }
            
            olqScores
            
        } catch (e: Exception) {
            ErrorLogger.log(e, "Gemini API call failed for $testType")
            null
        }
    }
    
    /**
     * Parse GTO analysis JSON response
     * 
     * Expected format:
     * {
     *   "olqScores": {
     *     "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "..."},
     *     "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
     *     ...
     *   }
     * }
     */
    private fun parseGTOAnalysisResponse(jsonText: String): Map<OLQ, OLQScore>? {
        return try {
            // Extract JSON from markdown code blocks if present
            val cleanJson = when {
                "```json" in jsonText -> jsonText
                    .substringAfter("```json")
                    .substringBefore("```")
                    .trim()
                "```" in jsonText -> jsonText
                    .substringAfter("```")
                    .substringBefore("```")
                    .trim()
                else -> jsonText.trim()
            }
            
            val json = org.json.JSONObject(cleanJson)
            val olqScoresJson = json.getJSONObject("olqScores")
            val olqScores = mutableMapOf<OLQ, OLQScore>()
            
            // Iterate through all OLQ keys
            olqScoresJson.keys().forEach { olqKey ->
                // Match by enum name
                val olq = OLQ.entries.find { it.name == olqKey }
                
                if (olq != null) {
                    val scoreObj = olqScoresJson.getJSONObject(olqKey)
                    val score = scoreObj.getInt("score")
                    val confidence = scoreObj.optInt("confidence", 50)
                    val reasoning = scoreObj.optString("reasoning", "")
                    
                    olqScores[olq] = OLQScore(
                        score = score,
                        confidence = confidence,
                        reasoning = reasoning
                    )
                }
            }
            
            Log.d(TAG, "✅ Parsed ${olqScores.size} OLQ scores from GTO analysis")
            olqScores.takeIf { it.isNotEmpty() }
            
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to parse GTO analysis JSON")
            Log.e(TAG, "JSON parsing failed. Response: ${jsonText.take(500)}", e)
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
