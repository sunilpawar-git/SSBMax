package com.ssbmax.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ArchivalWorker - Phase 6 Testing
 * Tests archival logic and error handling
 */
@RunWith(RobolectricTestRunner::class)
class ArchivalWorkerTest {

    private lateinit var context: Context
    
    @Mock
    private lateinit var mockRepository: SubmissionRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `archival worker succeeds when archival completes`() = runTest {
        // Given: Repository returns successful archival
        `when`(mockRepository.archiveOldSubmissions(any()))
            .thenReturn(Result.success(25)) // 25 items archived
        
        // When: Worker executes
        val worker = TestListenableWorkerBuilder<ArchivalWorker>(context).build()
        val result = worker.doWork()
        
        // Then: Worker should succeed
        assertEquals(ListenableWorker.Result.success(), result)
    }
    
    @Test
    fun `archival worker retries on failure`() = runTest {
        // Given: Repository returns failure
        `when`(mockRepository.archiveOldSubmissions(any()))
            .thenReturn(Result.failure(Exception("Network error")))
        
        // When: Worker executes (first attempt)
        val worker = TestListenableWorkerBuilder<ArchivalWorker>(context)
            .setRunAttemptCount(1) // First retry
            .build()
        val result = worker.doWork()
        
        // Then: Worker should retry
        assertTrue(result is ListenableWorker.Result.Retry)
    }
    
    @Test
    fun `archival worker fails after max retries`() = runTest {
        // Given: Repository continues to fail
        `when`(mockRepository.archiveOldSubmissions(any()))
            .thenReturn(Result.failure(Exception("Persistent error")))
        
        // When: Worker executes (max retries exceeded)
        val worker = TestListenableWorkerBuilder<ArchivalWorker>(context)
            .setRunAttemptCount(3) // Max retries
            .build()
        val result = worker.doWork()
        
        // Then: Worker should fail permanently
        assertTrue(result is ListenableWorker.Result.Failure)
    }
    
    private fun <T> any(): T {
        return null as T
    }
}
