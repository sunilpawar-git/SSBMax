package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.InterviewAnalysisOrchestrator
import com.ssbmax.shared.domain.constants.InterviewConstants
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for analyzing interview responses using Gemini AI.
 *
 * Phase 8 (KMP-convergence plan): the AI-analysis flow itself moved to
 * [InterviewAnalysisOrchestrator], `shared`'s single-sourced implementation of the exact
 * per-response analyze -> aggregate -> complete-interview flow this class used to
 * re-implement. This worker is now a thin WorkManager shell: it checks
 * PENDING_ANALYSIS itself, delegates the flow to the orchestrator, then re-reads the
 * session's persisted status to decide the WorkManager [Result] and notification.
 */
class InterviewAnalysisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val interviewRepository: InterviewRepository by inject()
    private val orchestrator: InterviewAnalysisOrchestrator by inject()
    private val notificationHelper: NotificationHelper by inject()

    companion object {
        const val KEY_SESSION_ID = "session_id"
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
        if (sessionId.isNullOrBlank()) {
            ErrorLogger.log(
                "Interview analysis worker started without session ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        return try {
            val before = interviewRepository.getSession(sessionId).getOrNull() ?: return Result.failure()
            if (before.status != InterviewStatus.PENDING_ANALYSIS) return Result.success()

            orchestrator.analyze(sessionId)

            val after = interviewRepository.getSession(sessionId).getOrNull()
            if (after?.status == InterviewStatus.COMPLETED) {
                val resultId = interviewRepository.getLatestResult(after.userId).getOrNull()?.id
                if (resultId != null) {
                    try {
                        notificationHelper.showInterviewResultsReadyNotification(sessionId, resultId)
                    } catch (e: Exception) {
                        ErrorLogger.log(e, "Failed to send interview result notification")
                    }
                }
                Result.success()
            } else {
                notificationHelper.showInterviewAnalysisFailedNotification(sessionId)
                Result.failure()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Background interview analysis failed for session: $sessionId")
            if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                try {
                    val session = interviewRepository.getSession(sessionId).getOrNull()
                    if (session != null) {
                        interviewRepository.updateSession(session.copy(status = InterviewStatus.FAILED))
                    }
                    notificationHelper.showInterviewAnalysisFailedNotification(sessionId)
                } catch (updateError: Exception) {
                    ErrorLogger.log(updateError, "Failed to update session status to FAILED")
                }
                Result.failure()
            }
        }
    }
}
