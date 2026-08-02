package com.ssbmax.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.shared.data.repository.InterviewQuestionGenerator
import com.ssbmax.shared.domain.constants.InterviewConstants
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionSource
import com.ssbmax.shared.platform.worker.BackgroundTaskScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Phase 8 (KMP-convergence plan): rewritten for the worker's shell conversion (it no longer
 * builds its own PIQ context or calls `AIService` directly -- both now come from
 * [InterviewQuestionGenerator], `shared`'s own cache-then-AI-fill question source, covered by
 * its own commonTest suite). Also un-ignores this file: it was previously
 * `@Ignore`d for a Robolectric/SDK-35 mismatch, which no longer applies now that the
 * worker needs no Robolectric context (same plain-mockk shape as `PPDTAnalysisWorkerTest`).
 */
class InterviewQuestionGenerationWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var questionGenerator: InterviewQuestionGenerator

    private val piqSubmissionId = "test-piq-123"

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        questionGenerator = mockk(relaxed = true)

        every { workerParams.inputData } returns workDataOf(
            BackgroundTaskScheduler.KEY_PIQ_SUBMISSION_ID to piqSubmissionId
        )
        every { workerParams.runAttemptCount } returns 0

        startKoin {
            modules(module {
                single { questionGenerator }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun mockQuestions() = List(18) { index ->
        InterviewQuestion(
            id = "q-$index",
            questionText = "Question $index",
            expectedOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
            context = null,
            source = QuestionSource.PIQ_BASED
        )
    }

    private fun createWorker() = InterviewQuestionGenerationWorker(context, workerParams)

    @Test
    fun `doWork delegates to InterviewQuestionGenerator and succeeds`() = runTest {
        coEvery {
            questionGenerator.generateQuestions(piqSubmissionId, InterviewConstants.TARGET_PIQ_QUESTION_COUNT)
        } returns Result.success(mockQuestions())

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            questionGenerator.generateQuestions(piqSubmissionId, InterviewConstants.TARGET_PIQ_QUESTION_COUNT)
        }
    }

    @Test
    fun `doWork returns failure when PIQ submission ID is missing`() = runTest {
        every { workerParams.inputData } returns workDataOf()

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 0) { questionGenerator.generateQuestions(any(), any()) }
    }

    @Test
    fun `doWork retries when generation fails and attempts remain`() = runTest {
        coEvery {
            questionGenerator.generateQuestions(any(), any())
        } returns Result.failure(Exception("Gemini unavailable"))
        every { workerParams.runAttemptCount } returns 0

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork fails after max retries`() = runTest {
        coEvery {
            questionGenerator.generateQuestions(any(), any())
        } returns Result.failure(Exception("Persistent failure"))
        every { workerParams.runAttemptCount } returns InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
