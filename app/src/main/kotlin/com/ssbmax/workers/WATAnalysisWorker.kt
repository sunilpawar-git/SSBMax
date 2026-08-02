package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.WATAnalysisOrchestrator
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for analyzing WAT test submissions using Gemini AI.
 *
 * Phase 8 (KMP-convergence plan): the AI-analysis flow itself moved to
 * [WATAnalysisOrchestrator], `shared`'s single-sourced implementation of the exact
 * fetch -> ANALYZING -> Gemini call with retry -> SSB-validate -> write OLQ result flow
 * this class used to re-implement. This worker is now a thin WorkManager shell: it
 * checks PENDING_ANALYSIS itself (so a "skip, already processed" run fires no
 * notification, matching the original), delegates the flow to the orchestrator, then
 * re-reads the submission's persisted status to decide the WorkManager [Result] and
 * notification -- that persisted state can't drift from what the orchestrator did.
 */
class WATAnalysisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val submissionRepository: SubmissionRepository by inject()
    private val orchestrator: WATAnalysisOrchestrator by inject()
    private val notificationHelper: NotificationHelper by inject()

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val MAX_WORKER_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID) ?: return Result.failure()

        return try {
            val before = submissionRepository.getWATSubmission(submissionId).getOrNull() ?: return Result.failure()
            if (before.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) return Result.success()

            orchestrator.analyze(submissionId)

            val after = submissionRepository.getWATSubmission(submissionId).getOrNull()
            if (after?.analysisStatus == AnalysisStatus.COMPLETED) {
                notificationHelper.showWATResultsReadyNotification(submissionId)
                Result.success()
            } else {
                notificationHelper.showWATAnalysisFailedNotification(submissionId)
                Result.failure()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "WAT analysis worker failed")
            if (runAttemptCount < MAX_WORKER_RETRIES) {
                Result.retry()
            } else {
                try {
                    submissionRepository.updateWATAnalysisStatus(submissionId, AnalysisStatus.FAILED)
                    notificationHelper.showWATAnalysisFailedNotification(submissionId)
                } catch (updateError: Exception) {
                    ErrorLogger.log(updateError, "Failed to update WAT submission status to FAILED")
                }
                Result.failure()
            }
        }
    }
}
