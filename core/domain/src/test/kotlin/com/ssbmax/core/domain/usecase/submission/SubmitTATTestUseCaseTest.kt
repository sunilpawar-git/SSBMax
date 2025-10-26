package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.TATResponse
import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.SubmissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SubmitTATTestUseCase
 * Tests TAT submission logic with repository interaction
 */
class SubmitTATTestUseCaseTest {
    
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var useCase: SubmitTATTestUseCase
    
    @Before
    fun setUp() {
        submissionRepository = mockk()
        useCase = SubmitTATTestUseCase(submissionRepository)
    }
    
    @Test
    fun `invoke successfully submits TAT test`() = runTest {
        // Given
        val submission = createMockTATSubmission()
        val expectedSubmissionId = "submission-123"
        coEvery { 
            submissionRepository.submitTAT(submission, null) 
        } returns Result.success(expectedSubmissionId)
        
        // When
        val result = useCase(submission)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSubmissionId, result.getOrNull())
        coVerify(exactly = 1) { submissionRepository.submitTAT(submission, null) }
    }
    
    @Test
    fun `invoke submits TAT test with batch ID`() = runTest {
        // Given
        val submission = createMockTATSubmission()
        val batchId = "batch-456"
        val expectedSubmissionId = "submission-789"
        coEvery { 
            submissionRepository.submitTAT(submission, batchId) 
        } returns Result.success(expectedSubmissionId)
        
        // When
        val result = useCase(submission, batchId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSubmissionId, result.getOrNull())
        coVerify(exactly = 1) { submissionRepository.submitTAT(submission, batchId) }
    }
    
    @Test
    fun `invoke handles repository failure`() = runTest {
        // Given
        val submission = createMockTATSubmission()
        val error = Exception("Network error")
        coEvery { 
            submissionRepository.submitTAT(submission, null) 
        } returns Result.failure(error)
        
        // When
        val result = useCase(submission)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `invoke handles submission with multiple responses`() = runTest {
        // Given
        val responses = (1..12).map { i ->
            TATResponse(
                questionId = "tat-$i",
                story = "Story $i",
                timeSpent = 230
            )
        }
        val submission = TATSubmission(
            userId = "user123",
            testId = "tat-test-1",
            testType = TestType.TAT,
            responses = responses,
            startTime = Instant.now().minusSeconds(3000),
            endTime = Instant.now(),
            totalTimeSpent = 3000
        )
        coEvery { 
            submissionRepository.submitTAT(submission, null) 
        } returns Result.success("submission-multi")
        
        // When
        val result = useCase(submission)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { submissionRepository.submitTAT(submission, null) }
    }
    
    @Test
    fun `invoke passes correct submission to repository`() = runTest {
        // Given
        val submission = createMockTATSubmission()
        coEvery { 
            submissionRepository.submitTAT(any(), any()) 
        } returns Result.success("submission-123")
        
        // When
        useCase(submission, "batch-1")
        
        // Then
        coVerify { submissionRepository.submitTAT(submission, "batch-1") }
    }
    
    private fun createMockTATSubmission() = TATSubmission(
        userId = "user123",
        testId = "tat-test-1",
        testType = TestType.TAT,
        responses = listOf(
            TATResponse(
                questionId = "tat-1",
                story = "A story about leadership and courage",
                timeSpent = 240
            )
        ),
        startTime = Instant.now().minusSeconds(300),
        endTime = Instant.now(),
        totalTimeSpent = 300
    )
}

