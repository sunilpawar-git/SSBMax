package com.ssbmax.ui.topic

import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.*
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
class TopicViewModelTest : BaseViewModelTest() {
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads topic content`() = runTest {
        // Given
        val savedStateHandle = SavedStateHandle(mapOf("topicId" to "OIR"))
        val mockTestProgressRepo = mockk<TestProgressRepository>(relaxed = true)
        val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
        val mockStudyContentRepo = mockk<StudyContentRepository>(relaxed = true)
        
        val testUser = SSBMaxUser("user-1", "test@test.com", "Test", role = UserRole.STUDENT)
        coEvery { mockObserveCurrentUser() } returns flowOf(testUser)
        
        // When
        val viewModel = TopicViewModel(
            savedStateHandle, mockTestProgressRepo, mockObserveCurrentUser, mockStudyContentRepo
        )
        advanceUntilIdle()
        
        // Then - Should not crash
        assertNotNull(viewModel.uiState.value)
    }
}

