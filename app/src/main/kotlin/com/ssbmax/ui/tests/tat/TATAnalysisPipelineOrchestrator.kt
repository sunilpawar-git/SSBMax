package com.ssbmax.ui.tests.tat

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TATStoryResponse
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.TATStoryAnalysisWorker
import com.ssbmax.workers.TATSynthesisWorker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the TAT analysis pipeline: sets ANALYZING status before enqueuing
 * the per-story + synthesis WorkManager chain, ensuring the status transition
 * happens before any background work begins (Phase 3 fix).
 */
@Singleton
class TATAnalysisPipelineOrchestrator @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val workManager: WorkManager
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
            val questionById = questions.associateBy { it.id }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val storyRequests = stories.mapIndexed { index, story ->
                val question = questionById[story.questionId]
                OneTimeWorkRequestBuilder<TATStoryAnalysisWorker>()
                    .setInputData(
                        workDataOf(
                            TATStoryAnalysisWorker.KEY_SUBMISSION_ID to submissionId,
                            TATStoryAnalysisWorker.KEY_QUESTION_ID to story.questionId,
                            TATStoryAnalysisWorker.KEY_STORY_INDEX to index,
                            TATStoryAnalysisWorker.KEY_IMAGE_URL to (question?.imageUrl ?: ""),
                            TATStoryAnalysisWorker.KEY_IMAGE_CONTEXT_JSON to (question?.imageContextJson ?: "{}"),
                            TATStoryAnalysisWorker.KEY_IMAGE_GENDER_TAG to (question?.genderTag ?: "MIXED")
                        )
                    )
                    .setConstraints(constraints)
                    .build()
            }

            val synthesisRequest = OneTimeWorkRequestBuilder<TATSynthesisWorker>()
                .setInputData(workDataOf(TATSynthesisWorker.KEY_SUBMISSION_ID to submissionId))
                .setConstraints(constraints)
                .build()

            workManager.beginWith(storyRequests)
                .then(synthesisRequest)
                .enqueue()

            Result.success(Unit)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Pipeline orchestrator: failed to enqueue worker chain for $submissionId")
            Result.failure(e)
        }
    }
}
