package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.SDAnalysisOrchestrator
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for analyzing SD (Self Description) test submissions using Gemini AI.
 *
 * Phase 8 (KMP-convergence plan): see [WATAnalysisWorker]'s doc -- same shell shape,
 * delegating the AI-analysis flow to [SDAnalysisOrchestrator].
 */
class SDTAnalysisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val submissionRepository: SubmissionRepository by inject()
    private val orchestrator: SDAnalysisOrchestrator by inject()
    private val notificationHelper: NotificationHelper by inject()

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val MAX_WORKER_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID) ?: return Result.failure()

        return try {
            val before = submissionRepository.getSDTSubmission(submissionId).getOrNull() ?: return Result.failure()
            if (before.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) return Result.success()

            orchestrator.analyze(submissionId)

            val after = submissionRepository.getSDTSubmission(submissionId).getOrNull()
            if (after?.analysisStatus == AnalysisStatus.COMPLETED) {
                notificationHelper.showSDTResultsReadyNotification(submissionId)
                Result.success()
            } else {
                notificationHelper.showSDTAnalysisFailedNotification(submissionId)
                Result.failure()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "SD analysis worker failed")
            if (runAttemptCount < MAX_WORKER_RETRIES) {
                Result.retry()
            } else {
                try {
                    submissionRepository.updateSDTAnalysisStatus(submissionId, AnalysisStatus.FAILED)
                    notificationHelper.showSDTAnalysisFailedNotification(submissionId)
                } catch (updateError: Exception) {
                    ErrorLogger.log(updateError, "Failed to update SD submission status to FAILED")
                }
                Result.failure()
            }
        }
    }
}
