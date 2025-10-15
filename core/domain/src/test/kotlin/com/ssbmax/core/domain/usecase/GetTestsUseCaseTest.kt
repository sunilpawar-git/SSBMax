package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SSBCategory
import com.ssbmax.core.domain.model.SSBTest
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for GetTestsUseCase
 */
class GetTestsUseCaseTest {
    
    private val mockRepository = mockk<TestRepository>()
    private val useCase = GetTestsUseCase(mockRepository)
    
    @Test
    fun useCase_calls_repository_with_category() = runTest {
        // Given
        val tests = listOf(
            SSBTest(
                id = "tat_001",
                type = TestType.TAT,
                category = SSBCategory.PSYCHOLOGY,
                title = "TAT Test",
                description = "Test description",
                timeLimit = 60.minutes,
                questionCount = 12,
                instructions = "Instructions"
            )
        )
        every { mockRepository.getTests(SSBCategory.PSYCHOLOGY) } returns flowOf(Result.success(tests))
        
        // When
        val result = useCase(SSBCategory.PSYCHOLOGY).first()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        verify { mockRepository.getTests(SSBCategory.PSYCHOLOGY) }
    }
    
    @Test
    fun useCase_handles_empty_results() = runTest {
        // Given
        every { mockRepository.getTests(any()) } returns flowOf(Result.success(emptyList()))
        
        // When
        val result = useCase(SSBCategory.GTO).first()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }
    
    @Test
    fun useCase_propagates_errors() = runTest {
        // Given
        val exception = Exception("Network error")
        every { mockRepository.getTests(any()) } returns flowOf(Result.failure(exception))
        
        // When
        val result = useCase(SSBCategory.PSYCHOLOGY).first()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun useCase_no_args_defaults_to_psychology() = runTest {
        // Given
        every { mockRepository.getTests(SSBCategory.PSYCHOLOGY) } returns flowOf(Result.success(emptyList()))
        
        // When
        useCase().first()
        
        // Then
        verify { mockRepository.getTests(SSBCategory.PSYCHOLOGY) }
    }
}

