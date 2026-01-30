package com.ssbmax.ui.interview.session

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.workers.InterviewAnalysisWorker
import java.time.Instant
import java.util.UUID

/**
 * Handles interview completion workflow.
 *
 * Responsibilities:
 * - Save all pending responses to Firestore
 * - Update session status to PENDING_ANALYSIS
 * - Schedule background AI analysis via WorkManager
 *
 * Follows best-effort pattern: partial failures don't block completion.
 */
class InterviewCompleter(
    private val interviewRepository: InterviewRepository,
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "InterviewCompleter"
    }

    /**
     * Complete the interview by saving responses and scheduling analysis.
     *
     * @param sessionId ID of the session to complete
     * @param session Current session data
     * @param pendingResponses All collected responses
     * @param mode Interview mode (used for response metadata)
     * @return Success with sessionId, or Failure with exception
     */
    suspend fun complete(
        sessionId: String,
        session: InterviewSession,
        pendingResponses: List<PendingResponse>,
        mode: InterviewMode
    ): Result<String> {
        Log.d(TAG, "🏁 Completing interview with ${pendingResponses.size} responses")

        return try {
            // Step 1: Save all responses (best-effort)
            saveResponses(sessionId, pendingResponses, mode)

            // Step 2: Update session status
            val updatedSession = session.copy(status = InterviewStatus.PENDING_ANALYSIS)
            interviewRepository.updateSession(updatedSession)
            Log.d(TAG, "✅ Session marked as PENDING_ANALYSIS")

            // Step 3: Schedule background analysis
            enqueueAnalysisWorker(sessionId)

            Log.d(TAG, "✅ Interview completed - background analysis scheduled")
            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception completing interview", e)
            Result.failure(e)
        }
    }

    /**
     * Save responses to repository (best-effort - partial failures logged but don't block).
     */
    private suspend fun saveResponses(
        sessionId: String,
        pendingResponses: List<PendingResponse>,
        mode: InterviewMode
    ) {
        Log.d(TAG, "💾 Saving ${pendingResponses.size} responses to Firestore...")

        for ((index, pending) in pendingResponses.withIndex()) {
            val response = InterviewResponse(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                questionId = pending.questionId,
                responseText = pending.responseText,
                responseMode = mode,
                respondedAt = Instant.ofEpochMilli(pending.respondedAt),
                thinkingTimeSec = pending.thinkingTimeSec,
                audioUrl = null,
                olqScores = emptyMap(),
                confidenceScore = 0
            )

            val submitResult = interviewRepository.submitResponse(response)
            if (submitResult.isFailure) {
                Log.w(TAG, "Failed to save response ${index + 1}: ${submitResult.exceptionOrNull()?.message}")
            }
        }

        Log.d(TAG, "✅ Saved ${pendingResponses.size} responses")
    }

    /**
     * Enqueue background AI analysis worker.
     */
    private fun enqueueAnalysisWorker(sessionId: String) {
        Log.d(TAG, "🔄 Enqueuing InterviewAnalysisWorker...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InterviewAnalysisWorker>()
            .setInputData(workDataOf(InterviewAnalysisWorker.KEY_SESSION_ID to sessionId))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "interview_analysis_$sessionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        Log.d(TAG, "📥 InterviewAnalysisWorker enqueued for session: $sessionId")
    }
}
