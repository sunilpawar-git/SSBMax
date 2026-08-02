package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.PPDTAnalysisOrchestrator
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for analyzing PPDT submissions using Gemini AI (multimodal path).
 *
 * Phase 8 (KMP-convergence plan): the AI-analysis flow itself moved to
 * [PPDTAnalysisOrchestrator], `shared`'s single-sourced implementation of the exact
 * fetch -> ANALYZING -> resolve question/image -> Gemini multimodal call with retry ->
 * SSB-validate -> write OLQ result flow this class used to re-implement. This worker is
 * now a thin WorkManager shell: it checks PENDING_ANALYSIS itself, delegates the flow to
 * the orchestrator, then re-reads the submission's persisted status to decide the
 * WorkManager [Result] and notification.
 */
class PPDTAnalysisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val submissionRepository: SubmissionRepository by inject()
    private val orchestrator: PPDTAnalysisOrchestrator by inject()
    private val notificationHelper: NotificationHelper by inject()

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val MAX_WORKER_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            ErrorLogger.log(
                "PPDT analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        return try {
            val before = submissionRepository.getPPDTSubmission(submissionId).getOrNull() ?: return Result.failure()
            if (before.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) return Result.success()

            orchestrator.analyze(submissionId)

            val after = submissionRepository.getPPDTSubmission(submissionId).getOrNull()
            if (after?.analysisStatus == AnalysisStatus.COMPLETED) {
                try {
                    notificationHelper.showPPDTResultsReadyNotification(submissionId)
                } catch (e: Exception) {
                    ErrorLogger.log(e, "Failed to send PPDT results notification")
                }
                Result.success()
            } else {
                notificationHelper.showPPDTAnalysisFailedNotification(submissionId)
                Result.failure()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "PPDT analysis failed for submission: $submissionId")
            if (runAttemptCount < MAX_WORKER_RETRIES) {
                Result.retry()
            } else {
                try {
                    submissionRepository.updatePPDTAnalysisStatus(submissionId, AnalysisStatus.FAILED)
                    notificationHelper.showPPDTAnalysisFailedNotification(submissionId)
                } catch (updateError: Exception) {
                    ErrorLogger.log(updateError, "Failed to update PPDT submission status to FAILED")
                }
                Result.failure()
            }
        }
    }
}
