package com.ssbmax.workers

import androidx.work.WorkManager
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATStoryResponse
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger

/**
 * Orchestrates the TAT analysis pipeline: sets ANALYZING status before enqueuing
 * the per-story + synthesis WorkManager chain (Phase 3 fix), and enqueues the
 * per-story work in bounded batches rather than a single 12-way fan-out so Gemini
 * isn't hit with every multimodal call at once (Phase 4 fix).
 *
 * Moved here from `app/ui/tests/tat/` (KMP-convergence Phase 6a): despite the
 * old package name, this is not a UI class -- [WorkManagerSubmissionAnalysisTrigger]
 * (`app/workers/`) is its only real caller, so it stayed in the tree that
 * deletion phase deleted wholesale otherwise. Package now matches location.
 */
class TATAnalysisPipelineOrchestrator(
    private val submissionRepository: SubmissionRepository,
    private val workManager: WorkManager,
    private val workPlanner: TATAnalysisWorkPlanner
) {

    suspend fun startPipeline(
        submissionId: String,
        stories: List<TATStoryResponse>,
        questions: List<TATQuestion>
    ): Result<Unit> {
        if (submissionId.isBlank()) {
            return Result.failure(IllegalArgumentException("submissionId must not be blank"))
        }
        if (stories.isEmpty()) {
            return Result.failure(IllegalArgumentException("stories must not be empty"))
        }

        // Set ANALYZING before any workers are enqueued — this is the Phase 3 invariant.
        val statusResult = submissionRepository.updateTATAnalysisStatus(submissionId, AnalysisStatus.ANALYZING)
        if (statusResult.isFailure) {
            val cause = statusResult.exceptionOrNull()
                ?: Exception("Unknown status update failure")
            ErrorLogger.log(cause, "Pipeline orchestrator: failed to set ANALYZING for $submissionId")
            return Result.failure(cause)
        }

        return try {
            // stories is non-empty (checked above), so the planner always yields at least one batch.
            val plan = workPlanner.plan(submissionId, stories, questions)
            var continuation = workManager.beginWith(plan.storyBatches.first())
            plan.storyBatches.drop(1).forEach { batch ->
                continuation = continuation.then(batch)
            }
            continuation.then(plan.synthesisRequest).enqueue()

            Result.success(Unit)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Pipeline orchestrator: failed to enqueue worker chain for $submissionId")
            Result.failure(e)
        }
    }
}
