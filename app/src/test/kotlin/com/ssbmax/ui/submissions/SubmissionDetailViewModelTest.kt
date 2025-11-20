package com.ssbmax.ui.submissions

import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.usecase.submission.ObserveSubmissionUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SubmissionDetailViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: SubmissionDetailViewModel
    private lateinit var mockObserveSubmission: ObserveSubmissionUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    
    private val submissionData = mapOf(
        "id" to "sub-1",
        "testType" to "TAT",
        "status" to "GRADED",
        "submittedAt" to 1000L,
        "data" to mapOf("aiPreliminaryScore" to mapOf("overallScore" to 85.5f))
    )
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads submission details`() = runTest {
        // Given
        mockObserveSubmission = mockk()
        savedStateHandle = SavedStateHandle(mapOf("submissionId" to "sub-1"))
        coEvery { mockObserveSubmission("sub-1") } returns flowOf(submissionData)
        
        // When
        viewModel = SubmissionDetailViewModel(mockObserveSubmission, savedStateHandle)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("sub-1", state.submissionId)
    }
    
    @Test
    fun `null submission shows error`() = runTest {
        // Given
        mockObserveSubmission = mockk()
        savedStateHandle = SavedStateHandle(mapOf("submissionId" to "sub-1"))
        coEvery { mockObserveSubmission("sub-1") } returns flowOf(null)
        
        // When
        viewModel = SubmissionDetailViewModel(mockObserveSubmission, savedStateHandle)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("not found", ignoreCase = true) == true)
    }
}

