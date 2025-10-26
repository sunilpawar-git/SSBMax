package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.SubmissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetUserSubmissionsUseCase
 * Tests fetching user submissions with filtering and pagination
 */
class GetUserSubmissionsUseCaseTest {
    
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var useCase: GetUserSubmissionsUseCase
    
    @Before
    fun setUp() {
        submissionRepository = mockk()
        useCase = GetUserSubmissionsUseCase(submissionRepository)
    }
    
    @Test
    fun `invoke successfully fetches user submissions`() = runTest {
        // Given
        val userId = "user123"
        val mockSubmissions = listOf(
            mapOf("id" to "sub1", "testType" to "TAT"),
            mapOf("id" to "sub2", "testType" to "WAT")
        )
        coEvery { 
            submissionRepository.getUserSubmissions(userId, 50) 
        } returns Result.success(mockSubmissions)
        
        // When
        val result = useCase(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify(exactly = 1) { submissionRepository.getUserSubmissions(userId, 50) }
    }
    
    @Test
    fun `invoke uses custom limit`() = runTest {
        // Given
        val userId = "user456"
        val customLimit = 100
        val mockSubmissions = (1..100).map { 
            mapOf("id" to "sub$it", "testType" to "TAT") 
        }
        coEvery { 
            submissionRepository.getUserSubmissions(userId, customLimit) 
        } returns Result.success(mockSubmissions)
        
        // When
        val result = useCase(userId, customLimit)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull()?.size)
        coVerify(exactly = 1) { submissionRepository.getUserSubmissions(userId, customLimit) }
    }
    
    @Test
    fun `invoke handles empty submissions list`() = runTest {
        // Given
        val userId = "user-new"
        coEvery { 
            submissionRepository.getUserSubmissions(userId, 50) 
        } returns Result.success(emptyList())
        
        // When
        val result = useCase(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `invoke handles repository failure`() = runTest {
        // Given
        val userId = "user789"
        val error = Exception("Database error")
        coEvery { 
            submissionRepository.getUserSubmissions(userId, 50) 
        } returns Result.failure(error)
        
        // When
        val result = useCase(userId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `byTestType successfully filters submissions`() = runTest {
        // Given
        val userId = "user123"
        val testType = TestType.TAT
        val mockSubmissions = listOf(
            mapOf("id" to "tat1", "testType" to "TAT"),
            mapOf("id" to "tat2", "testType" to "TAT")
        )
        coEvery { 
            submissionRepository.getUserSubmissionsByTestType(userId, testType, 20) 
        } returns Result.success(mockSubmissions)
        
        // When
        val result = useCase.byTestType(userId, testType)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify(exactly = 1) { 
            submissionRepository.getUserSubmissionsByTestType(userId, testType, 20) 
        }
    }
    
    @Test
    fun `byTestType uses custom limit`() = runTest {
        // Given
        val userId = "user456"
        val testType = TestType.WAT
        val customLimit = 50
        val mockSubmissions = (1..50).map { 
            mapOf("id" to "wat$it", "testType" to "WAT") 
        }
        coEvery { 
            submissionRepository.getUserSubmissionsByTestType(userId, testType, customLimit) 
        } returns Result.success(mockSubmissions)
        
        // When
        val result = useCase.byTestType(userId, testType, customLimit)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(50, result.getOrNull()?.size)
    }
    
    @Test
    fun `byTestType handles empty filtered results`() = runTest {
        // Given
        val userId = "user789"
        val testType = TestType.SRT
        coEvery { 
            submissionRepository.getUserSubmissionsByTestType(userId, testType, 20) 
        } returns Result.success(emptyList())
        
        // When
        val result = useCase.byTestType(userId, testType)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `byTestType handles different test types`() = runTest {
        // Given
        val userId = "user123"
        val testTypes = listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.OIR, TestType.PPDT)
        
        testTypes.forEach { testType ->
            val mockSubmissions = listOf(mapOf("id" to "sub1", "testType" to testType.name))
            coEvery { 
                submissionRepository.getUserSubmissionsByTestType(userId, testType, 20) 
            } returns Result.success(mockSubmissions)
            
            // When
            val result = useCase.byTestType(userId, testType)
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }
    }
}

