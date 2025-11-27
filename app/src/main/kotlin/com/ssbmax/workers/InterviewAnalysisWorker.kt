package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Background worker for analyzing interview responses using Gemini AI
 *
 * Triggered after user completes an interview and submits all responses.
 * This worker:
 * 1. Fetches all responses for the session from Firestore
 * 2. Analyzes each response with Gemini AI for OLQ scores
 * 3. Updates responses in Firestore with OLQ scores
 * 4. Generates final interview result
 * 5. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class InterviewAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val interviewRepository: InterviewRepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "InterviewAnalysisWorker"
        const val KEY_SESSION_ID = "session_id"
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
        if (sessionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No session ID provided")
            return Result.failure()
        }

        Log.d(TAG, "🔄 Starting background analysis for session: $sessionId")

        return try {
            // 1. Get session
            val sessionResult = interviewRepository.getSession(sessionId)
            val session = sessionResult.getOrNull()
            if (session == null) {
                Log.e(TAG, "❌ Session not found: $sessionId")
                return Result.failure()
            }

            Log.d(TAG, "   Step 1: Session found with ${session.questionIds.size} questions")

            // Verify session is in PENDING_ANALYSIS state
            if (session.status != InterviewStatus.PENDING_ANALYSIS) {
                Log.w(TAG, "⚠️ Session not in PENDING_ANALYSIS state: ${session.status}")
                // Don't fail, just skip - might be already processed
                return Result.success()
            }

            // 2. Get all responses
            val responsesResult = interviewRepository.getResponses(sessionId)
            val responses = responsesResult.getOrNull()
            if (responses.isNullOrEmpty()) {
                Log.e(TAG, "❌ No responses found for session: $sessionId")
                updateSessionFailed(sessionId)
                return Result.failure()
            }

            Log.d(TAG, "   Step 2: Found ${responses.size} responses to analyze")

            // 3. Analyze each response with AI
            var successCount = 0
            var failCount = 0

            for ((index, response) in responses.withIndex()) {
                Log.d(TAG, "   Analyzing response ${index + 1}/${responses.size}...")

                val analysisResult = analyzeResponseWithRetry(response)
                if (analysisResult != null) {
                    // Update response with OLQ scores
                    val updatedResponse = response.copy(
                        olqScores = analysisResult.olqScores,
                        confidenceScore = analysisResult.confidence
                    )

                    val updateResult = interviewRepository.updateResponse(updatedResponse)
                    if (updateResult.isSuccess) {
                        successCount++
                        Log.d(TAG, "   ✅ Response ${index + 1} analyzed and saved")
                    } else {
                        failCount++
                        Log.e(TAG, "   ❌ Failed to save response ${index + 1}")
                    }
                } else {
                    failCount++
                    Log.e(TAG, "   ❌ AI analysis failed for response ${index + 1}")

                    // Save with default scores so interview can still complete
                    val fallbackResponse = response.copy(
                        olqScores = generateFallbackOLQScores(),
                        confidenceScore = InterviewConstants.FALLBACK_CONFIDENCE
                    )
                    interviewRepository.updateResponse(fallbackResponse)
                }

                // Small delay between API calls to avoid rate limiting
                if (index < responses.size - 1) {
                    delay(InterviewConstants.API_CALL_DELAY_MS)
                }
            }

            Log.d(TAG, "   Step 3: Analysis complete - $successCount success, $failCount failed")

            // 4. Generate final result
            Log.d(TAG, "   Step 4: Generating final interview result...")
            val resultResult = interviewRepository.completeInterview(sessionId)
            Log.d(TAG, "   Step 4a: completeInterview returned, isSuccess=${resultResult.isSuccess}")

            if (resultResult.isFailure) {
                Log.e(TAG, "❌ Failed to complete interview", resultResult.exceptionOrNull())
                updateSessionFailed(sessionId)
                notificationHelper.showInterviewAnalysisFailedNotification(sessionId)
                return Result.failure()
            }

            Log.d(TAG, "   Step 4b: Getting interview result...")
            val interviewResult = resultResult.getOrNull()
            Log.d(TAG, "   Step 4c: Got interviewResult, id=${interviewResult?.id}")
            Log.d(TAG, "✅ Interview analysis complete! Result ID: ${interviewResult?.id}")
            Log.d(TAG, "   interviewResult is null: ${interviewResult == null}")

            // 5. Send notification
            if (interviewResult != null) {
                try {
                    Log.d(TAG, "📱 About to send notification for result: ${interviewResult.id}")
                    notificationHelper.showInterviewResultsReadyNotification(
                        sessionId = sessionId,
                        resultId = interviewResult.id
                    )
                    Log.d(TAG, "✅ Push notification sent successfully!")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to send notification", e)
                    ErrorLogger.log(e, "Failed to send interview result notification")
                }
            } else {
                Log.w(TAG, "⚠️ interviewResult is null, skipping notification")
            }

            Log.d(TAG, "🎉 InterviewAnalysisWorker completed successfully for session: $sessionId")
            Result.success()

        } catch (e: Exception) {
            ErrorLogger.log(e, "Background interview analysis failed for session: $sessionId")

            if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) {
                Log.w(TAG, "⚠️ Retry attempt ${runAttemptCount + 1}/${InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS}")
                Result.retry()
            } else {
                Log.e(TAG, "❌ Max retries reached, marking session as failed")
                updateSessionFailed(sessionId)
                notificationHelper.showInterviewAnalysisFailedNotification(sessionId)
                Result.failure()
            }
        }
    }

    /**
     * Analyze a single response with retry logic
     */
    private suspend fun analyzeResponseWithRetry(response: InterviewResponse): AnalysisResult? {
        repeat(InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) { attempt ->
            try {
                // Get the question for context
                val questionResult = interviewRepository.getQuestion(response.questionId)
                val question = questionResult.getOrNull() ?: return null

                // Call AI service
                val analysisResult = aiService.analyzeResponse(
                    question = question,
                    response = response.responseText,
                    responseMode = response.responseMode.name
                )

                if (analysisResult.isSuccess) {
                    val analysis = analysisResult.getOrNull()!!

                    // Convert to OLQScore map
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(1, 10),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }

                    return AnalysisResult(
                        olqScores = olqScores,
                        confidence = analysis.overallConfidence
                    )
                }

            } catch (e: Exception) {
                Log.w(TAG, "Analysis attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS - 1) {
                    delay(InterviewConstants.RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        return null
    }

    /**
     * Generate fallback OLQ scores when AI analysis fails
     *
     * Uses neutral scores (6-7 = Good/Average on SSB scale)
     */
    private fun generateFallbackOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.entries.take(5).associate { olq ->
            olq to OLQScore(
                score = InterviewConstants.FALLBACK_OLQ_SCORE,
                confidence = InterviewConstants.FALLBACK_CONFIDENCE,
                reasoning = "AI analysis unavailable - neutral score assigned"
            )
        }
    }

    /**
     * Update session status to FAILED
     */
    private suspend fun updateSessionFailed(sessionId: String) {
        try {
            val sessionResult = interviewRepository.getSession(sessionId)
            val session = sessionResult.getOrNull() ?: return

            val updatedSession = session.copy(status = InterviewStatus.FAILED)
            interviewRepository.updateSession(updatedSession)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to update session status to FAILED")
        }
    }

    /**
     * Internal result holder for AI analysis
     */
    private data class AnalysisResult(
        val olqScores: Map<OLQ, OLQScore>,
        val confidence: Int
    )
}

