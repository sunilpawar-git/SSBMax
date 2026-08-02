package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.GTOAnalysisOrchestrator
import com.ssbmax.shared.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for analyzing GTO test submissions using Gemini AI.
 *
 * Phase 8 (KMP-convergence plan): the AI-analysis flow itself (fetch -> ANALYZING ->
 * Gemini call with retry -> SSB-validate -> write OLQ scores, with neutral fallback
 * scores on AI failure) moved to [GTOAnalysisOrchestrator], `shared`'s single-sourced
 * implementation of the exact logic this class used to re-implement. This worker is now
 * a thin WorkManager shell: it delegates the flow to the orchestrator, then re-reads the
 * submission's persisted status to decide the WorkManager [Result] and notification --
 * that persisted state is the same signal a concurrently-running reader would see, so it
 * can't drift from what the orchestrator actually did.
 */
class GTOAnalysisWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val gtoRepository: GTORepository by inject()
    private val orchestrator: GTOAnalysisOrchestrator by inject()
    private val notificationHelper: NotificationHelper by inject()

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val MAX_WORKER_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            ErrorLogger.log(
                "GTO analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        return try {
            val before = gtoRepository.getSubmission(submissionId).getOrNull() ?: return Result.failure()
            if (before.status != GTOSubmissionStatus.PENDING_ANALYSIS) return Result.success()

            orchestrator.analyze(submissionId)

            val after = gtoRepository.getSubmission(submissionId).getOrNull()
            if (after?.status == GTOSubmissionStatus.FAILED) {
                // Orchestrator marks FAILED only when the Firestore write itself failed
                // (AI failure writes neutral fallback scores instead) -- worth a WorkManager retry.
                Result.retry()
            } else {
                notificationHelper.showGTOAnalysisCompleteNotification(
                    submissionId,
                    before.testType.displayName,
                    before.testType
                )
                Result.success()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "GTO analysis worker failed")
            try {
                gtoRepository.updateSubmissionStatus(submissionId, GTOSubmissionStatus.FAILED)
            } catch (updateError: Exception) {
                ErrorLogger.log(updateError, "Failed to update submission status to FAILED")
            }
            if (runAttemptCount < MAX_WORKER_RETRIES) Result.retry() else Result.failure()
        }
    }
}
