package com.ssbmax.ui.tests.tat

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATStoryResponse
import com.ssbmax.workers.TATStoryAnalysisWorker
import com.ssbmax.workers.TATSynthesisWorker

/**
 * Bounded-concurrency work plan for the TAT analysis pipeline: story-analysis
 * requests grouped into batches of [TATAnalysisWorkPlanner.BATCH_SIZE], plus the
 * single synthesis request that depends on every batch (Phase 4 fix).
 */
data class TATAnalysisWorkPlan(
    val storyBatches: List<List<OneTimeWorkRequest>>,
    val synthesisRequest: OneTimeWorkRequest
)

class TATAnalysisWorkPlanner {

    companion object {
        const val BATCH_SIZE = 3
    }

    fun plan(
        submissionId: String,
        stories: List<TATStoryResponse>,
        questions: List<TATQuestion>
    ): TATAnalysisWorkPlan {
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

        return TATAnalysisWorkPlan(storyRequests.chunked(BATCH_SIZE), synthesisRequest)
    }
}
