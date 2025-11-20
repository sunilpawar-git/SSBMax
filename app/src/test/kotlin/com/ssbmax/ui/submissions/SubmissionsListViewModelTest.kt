package com.ssbmax.ui.submissions

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.GetUserSubmissionsUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SubmissionsListViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: SubmissionsListViewModel
    private lateinit var mockGetUserSubmissions: GetUserSubmissionsUseCase
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    
    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@student.com",
        displayName = "Test Student",
        role = UserRole.STUDENT
    )
    
    private val submissionData1 = mapOf(
        "id" to "sub-1",
        "testType" to "TAT",
        "testId" to "test-1",
        "status" to "GRADED",
        "submittedAt" to 1000L,
        "data" to mapOf("aiPreliminaryScore" to mapOf("overallScore" to 85.5f))
    )
    
    private val submissionData2 = mapOf(
        "id" to "sub-2",
        "testType" to "WAT",
        "testId" to "test-2",
        "status" to "SUBMITTED_PENDING_REVIEW",
        "submittedAt" to 2000L
    )
    
    @Before
    fun setup() {
        mockGetUserSubmissions = mockk()
        mockObserveCurrentUser = mockk()
        
        coEvery { mockObserveCurrentUser() } returns flowOf(testUser)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads submissions for authenticated user`() = runTest {
        // Given
        coEvery { mockGetUserSubmissions(testUser.id) } returns Result.success(listOf(submissionData1))
        
        // When
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.submissions.size)
    }
    
    @Test
    fun `no authenticated user shows error`() = runTest {
        // Given
        coEvery { mockObserveCurrentUser() } returns flowOf(null)
        
        // When
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("login", ignoreCase = true) == true)
    }
    
    @Test
    fun `submissions sorted by submittedAt descending`() = runTest {
        // Given
        coEvery { mockGetUserSubmissions(testUser.id) } returns 
            Result.success(listOf(submissionData1, submissionData2))
        
        // When
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("sub-2", state.submissions[0].id) // More recent
        assertEquals("sub-1", state.submissions[1].id)
    }
    
    @Test
    fun `filterByType loads filtered submissions`() = runTest {
        // Given
        coEvery { mockGetUserSubmissions(testUser.id) } returns Result.success(listOf(submissionData1))
        coEvery { mockGetUserSubmissions.byTestType(testUser.id, TestType.TAT) } returns 
            Result.success(listOf(submissionData1))
        
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.filterByType(TestType.TAT)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(TestType.TAT, state.filteredType)
    }
    
    @Test
    fun `groupedByStatus groups submissions correctly`() = runTest {
        // Given
        coEvery { mockGetUserSubmissions(testUser.id) } returns 
            Result.success(listOf(submissionData1, submissionData2))
        
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val grouped = state.groupedByStatus
        
        assertEquals(1, grouped[SubmissionStatus.GRADED]?.size)
        assertEquals(1, grouped[SubmissionStatus.SUBMITTED_PENDING_REVIEW]?.size)
    }
    
    @Test
    fun `status counts calculated correctly`() = runTest {
        // Given
        coEvery { mockGetUserSubmissions(testUser.id) } returns 
            Result.success(listOf(submissionData1, submissionData2))
        
        viewModel = SubmissionsListViewModel(mockGetUserSubmissions, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.gradedCount)
        assertEquals(1, state.pendingCount)
        assertEquals(0, state.underReviewCount)
    }
}

