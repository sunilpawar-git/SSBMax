package com.ssbmax.ui.results

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.results.HistoricResult
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricResultsViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: HistoricResultsViewModel
    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val mockGetHistoricResults = mockk<GetHistoricResultsUseCase>(relaxed = true)

    private val mockUser = SSBMaxUser(
        id = "user-123",
        email = "test@ssbmax.com",
        displayName = "Test User",
        role = UserRole.STUDENT
    )

    private val mockResults = listOf(
        HistoricResult(
            submissionId = "tat-sub-1",
            testType = TestType.TAT,
            submittedAt = System.currentTimeMillis() - 86400000, // 1 day ago
            overallScore = 85.5f,
            rating = "Good"
        ),
        HistoricResult(
            submissionId = "oir-sub-1",
            testType = TestType.OIR,
            submittedAt = System.currentTimeMillis() - 172800000, // 2 days ago
            overallScore = 92.0f,
            rating = "Excellent"
        ),
        HistoricResult(
            submissionId = "wat-sub-1",
            testType = TestType.WAT,
            submittedAt = System.currentTimeMillis() - 259200000, // 3 days ago
            overallScore = 78.0f,
            rating = "Average"
        )
    )

    @Before
    fun setup() {
        val userFlow = MutableStateFlow(mockUser)
        every { mockAuthRepository.currentUser } returns userFlow

        coEvery { mockGetHistoricResults(any(), null) } returns Result.success(mockResults)

        viewModel = HistoricResultsViewModel(mockAuthRepository, mockGetHistoricResults)
    }

    @Test
    fun `initial state loads results automatically`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(3, state.results.size)
        assertNull(state.selectedFilter)
        assertNull(state.error)

        coVerify { mockGetHistoricResults(mockUser.id, null) }
    }

    @Test
    fun `loadResults success updates state with results`() = runTest {
        viewModel.loadResults()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(3, state.results.size)
        assertEquals("tat-sub-1", state.results[0].submissionId)
        assertEquals("oir-sub-1", state.results[1].submissionId)
        assertEquals("wat-sub-1", state.results[2].submissionId)
        assertNull(state.error)
    }

    @Test
    fun `loadResults shows error when user not authenticated`() = runTest {
        val userFlow = MutableStateFlow<SSBMaxUser?>(null)
        every { mockAuthRepository.currentUser } returns userFlow

        val newViewModel = HistoricResultsViewModel(mockAuthRepository, mockGetHistoricResults)
        advanceUntilIdle()

        val state = newViewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.results.isEmpty())
        assertNotNull(state.error)
        assertEquals("Please login to view results", state.error)
    }

    @Test
    fun `filterByTestType filters results by test type`() = runTest {
        advanceUntilIdle() // Wait for initial load

        val tatResults = listOf(mockResults[0]) // Only TAT result
        coEvery { mockGetHistoricResults(any(), TestType.TAT) } returns Result.success(tatResults)

        viewModel.filterByTestType(TestType.TAT)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(1, state.results.size)
        assertEquals(TestType.TAT, state.results[0].testType)
        assertEquals(TestType.TAT, state.selectedFilter)

        coVerify { mockGetHistoricResults(mockUser.id, TestType.TAT) }
    }

    @Test
    fun `filterByTestType with null clears filter`() = runTest {
        advanceUntilIdle() // Wait for initial load

        // First apply a filter
        viewModel.filterByTestType(TestType.OIR)
        advanceUntilIdle()

        // Then clear it
        viewModel.filterByTestType(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.selectedFilter)
        assertEquals(3, state.results.size) // All results returned

        coVerify(exactly = 2) { mockGetHistoricResults(mockUser.id, null) } // Initial + clear filter
    }

    @Test
    fun `refresh reloads results with current filter`() = runTest {
        advanceUntilIdle() // Wait for initial load

        // Mock filtered results
        val watResults = listOf(mockResults[2]) // Only WAT result
        coEvery { mockGetHistoricResults(any(), TestType.WAT) } returns Result.success(watResults)

        // Apply filter
        viewModel.filterByTestType(TestType.WAT)
        advanceUntilIdle()

        // Refresh
        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { mockGetHistoricResults(mockUser.id, TestType.WAT) }
    }

    @Test
    fun `loadResults handles use case failure gracefully`() = runTest {
        coEvery { mockGetHistoricResults(any(), any()) } returns 
            Result.failure(Exception("Network error"))

        // Create new ViewModel with failure mock
        val failingViewModel = HistoricResultsViewModel(mockAuthRepository, mockGetHistoricResults)
        advanceUntilIdle()

        val state = failingViewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.results.isEmpty())
        assertNotNull(state.error)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `loadResults handles unexpected exception gracefully`() = runTest {
        coEvery { mockGetHistoricResults(any(), any()) } throws RuntimeException("Unexpected error")

        // Create new ViewModel with exception mock
        val failingViewModel = HistoricResultsViewModel(mockAuthRepository, mockGetHistoricResults)
        advanceUntilIdle()

        val state = failingViewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.results.isEmpty())
        assertNotNull(state.error)
        assertEquals("An unexpected error occurred", state.error)
    }

    @Test
    fun `loadResults sets loading state correctly`() = runTest {
        // Create a delayed response to observe loading state
        coEvery { mockGetHistoricResults(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockResults)
        }

        viewModel.loadResults()

        // Check loading state before results arrive
        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isLoading)

        advanceUntilIdle()

        // Check state after results arrive
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(3, finalState.results.size)
    }

    @Test
    fun `multiple filters can be applied sequentially`() = runTest {
        advanceUntilIdle() // Initial load

        // Filter by TAT
        val tatResults = listOf(mockResults[0])
        coEvery { mockGetHistoricResults(any(), TestType.TAT) } returns Result.success(tatResults)
        viewModel.filterByTestType(TestType.TAT)
        advanceUntilIdle()

        assertEquals(TestType.TAT, viewModel.uiState.value.selectedFilter)

        // Filter by OIR
        val oirResults = listOf(mockResults[1])
        coEvery { mockGetHistoricResults(any(), TestType.OIR) } returns Result.success(oirResults)
        viewModel.filterByTestType(TestType.OIR)
        advanceUntilIdle()

        assertEquals(TestType.OIR, viewModel.uiState.value.selectedFilter)
        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals(TestType.OIR, viewModel.uiState.value.results[0].testType)
    }
}

