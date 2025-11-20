package com.ssbmax.ui.instructor

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.GradingQueueRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class InstructorGradingViewModelTest : BaseViewModelTest() {
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads pending submissions for instructor`() = runTest {
        // Given
        val mockGradingQueueRepo = mockk<GradingQueueRepository>()
        val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
        val testUser = SSBMaxUser("inst-1", "inst@test.com", "Instructor", role = UserRole.INSTRUCTOR)
        
        coEvery { mockObserveCurrentUser() } returns flowOf(testUser)
        coEvery { mockGradingQueueRepo.observePendingSubmissions("inst-1") } returns flowOf(emptyList())
        
        // When
        val viewModel = InstructorGradingViewModel(mockGradingQueueRepo, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `no authenticated user shows error`() = runTest {
        // Given
        val mockGradingQueueRepo = mockk<GradingQueueRepository>()
        val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
        
        coEvery { mockObserveCurrentUser() } returns flowOf(null)
        
        // When
        val viewModel = InstructorGradingViewModel(mockGradingQueueRepo, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("login", ignoreCase = true) == true)
    }
}

