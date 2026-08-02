package com.ssbmax.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.shared.data.repository.InterviewQuestionGenerator
import com.ssbmax.shared.domain.constants.InterviewConstants
import com.ssbmax.shared.platform.worker.BackgroundTaskScheduler
import com.ssbmax.utils.ErrorLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker for pre-generating Interview-module questions after a PIQ submission.
 *
 * Enqueued by [BackgroundTaskScheduler.scheduleInterviewQuestionGeneration] (Phase 8,
 * KMP-convergence plan) from [com.ssbmax.shared.presentation.piq.PIQTestViewModel]'s
 * `submitTest()` -- previously nothing enqueued this worker at all on this KMP build
 * ("gap #14"). Caching the 18 questions ahead of time means the interview starts
 * instantly instead of waiting on a Gemini call.
 *
 * A thin shell over [InterviewQuestionGenerator] (`shared`'s own cache-then-AI-fill
 * question source, already used at interview-start time for any shortfall) --
 * previously this worker built its own PIQ context via `core:data`'s
 * `PIQDataMapper` and called `AIService.generatePIQBasedQuestions` directly, a second,
 * Android-only implementation of the same job `InterviewQuestionGenerator` already
 * does on both platforms.
 */
class InterviewQuestionGenerationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val questionGenerator: InterviewQuestionGenerator by inject()

    override suspend fun doWork(): Result {
        val piqSubmissionId = inputData.getString(BackgroundTaskScheduler.KEY_PIQ_SUBMISSION_ID)
            ?: return Result.failure()

        val questionsResult = questionGenerator.generateQuestions(
            piqSnapshotId = piqSubmissionId,
            count = InterviewConstants.TARGET_PIQ_QUESTION_COUNT
        )

        return questionsResult.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                ErrorLogger.log(error, "Interview question pre-generation failed for PIQ: $piqSubmissionId")
                if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) Result.retry() else Result.failure()
            }
        )
    }
}
