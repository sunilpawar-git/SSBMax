package com.ssbmax.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for QuestionCacheCleanupWorker
 * 
 * @Ignore - Temporarily ignored due to Robolectric SDK version mismatch
 * TODO: Re-enable when Robolectric supports SDK 35
 *
 * Tests:
 * - Successful cleanup with expired entries
 * - Successful cleanup with no expired entries
 * - Retry on cleanup failure
 * - Failure after max retries
 * - Unexpected exception handling
 */
@Ignore("Robolectric SDK version mismatch - SDK 35 not yet supported")
@RunWith(RobolectricTestRunner::class)
class QuestionCacheCleanupWorkerTest {

    private lateinit var context: Context
    private lateinit var questionCacheRepository: QuestionCacheRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        questionCacheRepository = mockk()

        // QuestionCacheCleanupWorker resolves its dependencies via
        // KoinComponent/inject() (converted from Hilt's @HiltWorker/@AssistedInject).
        startKoin {
            modules(module {
                single { questionCacheRepository }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
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
     * Helper to create worker instance. QuestionCacheCleanupWorker resolves
     * its dependency via KoinComponent/inject() now (mocked/bound in
     * [setup]'s startKoin call), so the default reflection-based
     * TestListenableWorkerBuilder factory (matching the worker's now-plain
     * (Context, WorkerParameters) constructor) is enough.
     */
    private fun createWorker(runAttemptCount: Int = 0): QuestionCacheCleanupWorker {
        return TestListenableWorkerBuilder<QuestionCacheCleanupWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }
}
