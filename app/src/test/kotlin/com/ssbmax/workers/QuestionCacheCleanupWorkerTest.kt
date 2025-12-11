package com.ssbmax.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
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
 * Unit tests for QuestionCacheCleanupWorker
 *
 * Tests:
 * - Successful cleanup with expired entries
 * - Successful cleanup with no expired entries
 * - Retry on cleanup failure
 * - Failure after max retries
 * - Unexpected exception handling
 */
@RunWith(RobolectricTestRunner::class)
class QuestionCacheCleanupWorkerTest {

    private lateinit var context: Context
    private lateinit var questionCacheRepository: QuestionCacheRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        questionCacheRepository = mockk()
    }

    @Test
    fun `worker cleans up expired cache entries successfully`() = runTest {
        // Given
        val deletedCount = 15
        coEvery { questionCacheRepository.cleanupExpired() } returns Result.success(deletedCount)

        // When
        val worker = createWorker()
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { questionCacheRepository.cleanupExpired() }
    }

    @Test
    fun `worker succeeds when no expired entries exist`() = runTest {
        // Given
        coEvery { questionCacheRepository.cleanupExpired() } returns Result.success(0)

        // When
        val worker = createWorker()
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { questionCacheRepository.cleanupExpired() }
    }

    @Test
    fun `worker retries on cleanup failure`() = runTest {
        // Given
        coEvery {
            questionCacheRepository.cleanupExpired()
        } returns Result.failure(Exception("Firestore timeout"))

        // When
        val worker = createWorker(runAttemptCount = 0)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { questionCacheRepository.cleanupExpired() }
    }

    @Test
    fun `worker fails after max retries`() = runTest {
        // Given
        coEvery {
            questionCacheRepository.cleanupExpired()
        } returns Result.failure(Exception("Persistent failure"))

        // When
        val worker = createWorker(runAttemptCount = 2)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 1) { questionCacheRepository.cleanupExpired() }
    }

    @Test
    fun `worker handles unexpected exceptions and retries`() = runTest {
        // Given
        coEvery {
            questionCacheRepository.cleanupExpired()
        } throws RuntimeException("Unexpected error")

        // When
        val worker = createWorker(runAttemptCount = 0)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker fails on unexpected exception after max retries`() = runTest {
        // Given
        coEvery {
            questionCacheRepository.cleanupExpired()
        } throws RuntimeException("Persistent unexpected error")

        // When
        val worker = createWorker(runAttemptCount = 2)
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `worker logs appropriate count when cleaning large number of entries`() = runTest {
        // Given
        val largeDeleteCount = 500
        coEvery { questionCacheRepository.cleanupExpired() } returns Result.success(largeDeleteCount)

        // When
        val worker = createWorker()
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { questionCacheRepository.cleanupExpired() }
    }

    /**
     * Helper to create worker instance with mocked dependencies
     */
    private fun createWorker(runAttemptCount: Int = 0): QuestionCacheCleanupWorker {
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return QuestionCacheCleanupWorker(
                    context = appContext,
                    params = workerParameters,
                    questionCacheRepository = questionCacheRepository
                )
            }
        }

        return TestListenableWorkerBuilder<QuestionCacheCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }
}
