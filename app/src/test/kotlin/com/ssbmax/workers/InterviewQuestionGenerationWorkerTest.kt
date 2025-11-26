package com.ssbmax.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for InterviewQuestionGenerationWorker
 *
 * Tests:
 * - Successful question generation and caching
 * - Retry on AI service failure
 * - Retry on cache failure
 * - Failure after max retries
 * - Invalid PIQ submission handling
 */
@RunWith(RobolectricTestRunner::class)
class InterviewQuestionGenerationWorkerTest {

    private lateinit var context: Context
    private lateinit var aiService: AIService
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var questionCacheRepository: QuestionCacheRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        aiService = mockk()
        submissionRepository = mockk()
        questionCacheRepository = mockk()
    }

    @Test
    fun `worker generates 18 questions and caches them successfully`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-123"
        val mockPIQData = mapOf(
            "id" to piqSubmissionId,
            "data" to mapOf(
                "fullName" to "Test Candidate",
                "hobbies" to "Reading, Sports",
                "whyDefenseForces" to "Serve the nation"
            )
        )

        val mockQuestions = List(18) { index ->
            InterviewQuestion(
                id = "q-$index",
                questionText = "Test question $index",
                expectedOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
                context = null,
                source = QuestionSource.PIQ_BASED
            )
        }

        coEvery { submissionRepository.getSubmission(piqSubmissionId) } returns Result.success(mockPIQData)
        coEvery {
            aiService.generatePIQBasedQuestions(
                piqData = any(),
                targetOLQs = null,
                count = 18,
                difficulty = 3
            )
        } returns Result.success(mockQuestions)
        coEvery {
            questionCacheRepository.cachePIQQuestions(
                piqSnapshotId = piqSubmissionId,
                questions = mockQuestions,
                expirationDays = 30
            )
        } returns Result.success(Unit)

        // When
        val worker = createWorker(piqSubmissionId)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { submissionRepository.getSubmission(piqSubmissionId) }
        coVerify(exactly = 1) { aiService.generatePIQBasedQuestions(any(), null, 18, 3) }
        coVerify(exactly = 1) { questionCacheRepository.cachePIQQuestions(piqSubmissionId, mockQuestions, 30) }
    }

    @Test
    fun `worker retries on AI service failure`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-456"
        val mockPIQData = mapOf(
            "id" to piqSubmissionId,
            "data" to mapOf("fullName" to "Test")
        )

        coEvery { submissionRepository.getSubmission(piqSubmissionId) } returns Result.success(mockPIQData)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("AI service unavailable"))

        // When
        val worker = createWorker(piqSubmissionId, runAttemptCount = 0)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) }
    }

    @Test
    fun `worker retries on cache failure`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-789"
        val mockPIQData = mapOf(
            "id" to piqSubmissionId,
            "data" to mapOf("fullName" to "Test")
        )
        val mockQuestions = List(18) { index ->
            InterviewQuestion(
                id = "q-$index",
                questionText = "Question $index",
                expectedOLQs = listOf(OLQ.INITIATIVE),
                context = null,
                source = QuestionSource.PIQ_BASED
            )
        }

        coEvery { submissionRepository.getSubmission(piqSubmissionId) } returns Result.success(mockPIQData)
        coEvery { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) } returns Result.success(mockQuestions)
        coEvery {
            questionCacheRepository.cachePIQQuestions(any(), any(), any())
        } returns Result.failure(Exception("Cache write failed"))

        // When
        val worker = createWorker(piqSubmissionId, runAttemptCount = 1)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { questionCacheRepository.cachePIQQuestions(any(), any(), any()) }
    }

    @Test
    fun `worker fails after max retries`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-max-retry"
        val mockPIQData = mapOf(
            "id" to piqSubmissionId,
            "data" to mapOf("fullName" to "Test")
        )

        coEvery { submissionRepository.getSubmission(piqSubmissionId) } returns Result.success(mockPIQData)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("Persistent failure"))

        // When
        val worker = createWorker(piqSubmissionId, runAttemptCount = 3)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `worker fails when PIQ submission not found`() = runTest {
        // Given
        val piqSubmissionId = "non-existent-piq"

        coEvery {
            submissionRepository.getSubmission(piqSubmissionId)
        } returns Result.failure(Exception("Not found"))

        // When
        val worker = createWorker(piqSubmissionId)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 1) { submissionRepository.getSubmission(piqSubmissionId) }
        coVerify(exactly = 0) { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) }
    }

    @Test
    fun `worker handles unexpected exceptions and retries`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-exception"

        coEvery {
            submissionRepository.getSubmission(piqSubmissionId)
        } throws RuntimeException("Unexpected error")

        // When
        val worker = createWorker(piqSubmissionId, runAttemptCount = 0)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker fails on unexpected exception after max retries`() = runTest {
        // Given
        val piqSubmissionId = "test-piq-exception-max"

        coEvery {
            submissionRepository.getSubmission(piqSubmissionId)
        } throws RuntimeException("Persistent unexpected error")

        // When
        val worker = createWorker(piqSubmissionId, runAttemptCount = 3)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    /**
     * Helper to create worker instance with mocked dependencies
     */
    private fun createWorker(
        piqSubmissionId: String,
        runAttemptCount: Int = 0
    ): InterviewQuestionGenerationWorker {
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return InterviewQuestionGenerationWorker(
                    context = appContext,
                    params = workerParameters,
                    aiService = aiService,
                    submissionRepository = submissionRepository,
                    questionCacheRepository = questionCacheRepository
                )
            }
        }

        return TestListenableWorkerBuilder<InterviewQuestionGenerationWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(
                androidx.work.workDataOf(
                    InterviewQuestionGenerationWorker.KEY_PIQ_SUBMISSION_ID to piqSubmissionId,
                    InterviewQuestionGenerationWorker.KEY_NOTIFY_ON_COMPLETE to false
                )
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }
}
