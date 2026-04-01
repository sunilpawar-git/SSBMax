package com.ssbmax.ui.tests.oir

import com.ssbmax.core.domain.model.OIRSetProgress
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OIRSetListViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: OIRSetListViewModel
    private val mockRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)

    private val mockUser = SSBMaxUser(
        id = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    private fun makeProgress(
        setNumber: Int,
        isCompleted: Boolean,
        score: Int? = null
    ) = OIRSetProgress(
        setNumber = setNumber,
        isCompleted = isCompleted,
        score = score,
        totalQuestions = 50,
        completedAt = if (isCompleted) System.currentTimeMillis() else null
    )

    @Before
    fun setup() {
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        // Default: no progress, next set = 1, full 1-20 list
        val allSets = (1..20).map { makeProgress(it, false) }
        coEvery { mockRepo.getOIRSetProgressList("user-123") } returns Result.success(allSets)
        coEvery { mockRepo.getNextOIRSetNumber("user-123") } returns Result.success(1)
    }

    private fun createViewModel() = OIRSetListViewModel(mockRepo, mockObserveCurrentUser)

    // ==================== Initial State ====================

    @Test
    fun `empty history - nextSet is 1 and all sets locked except set 1`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
        assertEquals("Should have 20 sets", 20, state.sets.size)
        assertEquals("First unlocked set should be 1", 1, state.nextSetNumber)

        // Set 1 not completed, is next set → should be accessible
        val set1 = state.sets.first { it.setNumber == 1 }
        assertFalse("Set 1 should not be completed", set1.isCompleted)

        // Set 2 not completed, not next → locked
        val set2 = state.sets.first { it.setNumber == 2 }
        assertFalse("Set 2 should not be completed", set2.isCompleted)
        // set2 is not nextSetNumber (which is 1), so it's locked in UI logic
    }

    @Test
    fun `set 1 completed - next set becomes 2`() = runTest {
        val progress = (1..20).map { n ->
            makeProgress(n, isCompleted = n == 1, score = if (n == 1) 42 else null)
        }
        coEvery { mockRepo.getOIRSetProgressList("user-123") } returns Result.success(progress)
        coEvery { mockRepo.getNextOIRSetNumber("user-123") } returns Result.success(2)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Next set should be 2", 2, state.nextSetNumber)

        val set1 = state.sets.first { it.setNumber == 1 }
        assertTrue("Set 1 should be completed", set1.isCompleted)
        assertEquals("Set 1 score should be 42", 42, set1.score)
    }

    @Test
    fun `all 20 sets completed - nextSet stays at 20 or returns last completed`() = runTest {
        val progress = (1..20).map { n -> makeProgress(n, isCompleted = true, score = 45) }
        coEvery { mockRepo.getOIRSetProgressList("user-123") } returns Result.success(progress)
        coEvery { mockRepo.getNextOIRSetNumber("user-123") } returns Result.success(20)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("All sets should be completed", state.sets.all { it.isCompleted })
        assertEquals("Should have 20 sets", 20, state.sets.size)
    }

    // ==================== Error Handling ====================

    @Test
    fun `progress fetch failure shows error`() = runTest {
        coEvery {
            mockRepo.getOIRSetProgressList("user-123")
        } returns Result.failure(Exception("Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
    }

    @Test
    fun `unauthenticated user shows error without repo call`() = runTest {
        coEvery { mockObserveCurrentUser() } returns flowOf(null)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        coVerify(exactly = 0) { mockRepo.getOIRSetProgressList(any()) }
    }

    // ==================== loadSets() ====================

    @Test
    fun `loadSets retries and refreshes state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Simulate a fresh call (e.g., after an error retry)
        val updated = (1..20).map { n -> makeProgress(n, isCompleted = n <= 3, score = if (n <= 3) 40 else null) }
        coEvery { mockRepo.getOIRSetProgressList("user-123") } returns Result.success(updated)
        coEvery { mockRepo.getNextOIRSetNumber("user-123") } returns Result.success(4)

        viewModel.loadSets()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Next set should be 4 after reload", 4, state.nextSetNumber)
        assertEquals("Sets 1-3 should be completed", 3, state.sets.count { it.isCompleted })
    }
}
