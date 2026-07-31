package com.ssbmax.shared.presentation.home.instructor

import com.ssbmax.shared.domain.model.InstructorGradingStats
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeGradingQueueRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization test, written before converting [InstructorHomeViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1 of the KMP-convergence plan).
 * Must pass unmodified against both the pre- and post-conversion class.
 */
class InstructorHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var gradingQueueRepository: FakeGradingQueueRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        gradingQueueRepository = FakeGradingQueueRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = InstructorHomeViewModel(
        gradingQueueRepository = gradingQueueRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository)
    )

    @Test
    fun `unauthenticated instructor surfaces login-required error`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("You must be logged in to view instructor dashboard", state.error)
    }

    @Test
    fun `loads grading stats from repository`() = runTest(testDispatcher) {
        val statsFlow = MutableStateFlow(
            InstructorGradingStats(
                totalPending = 5,
                totalGraded = 20,
                averageGradingTimeMinutes = 120,
                todayGraded = 3,
                weekGraded = 10,
                pendingByTestType = emptyMap(),
                averageScoreGiven = 75f
            )
        )
        gradingQueueRepository.gradingStatsFlow = statsFlow
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.pendingGradingCount)
        assertEquals(3, state.testsGradedToday)
        assertEquals(2, state.avgResponseTime) // 120 minutes / 60 = 2 hours
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `mock student and batch data is populated`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(24, state.totalStudents)
        assertEquals(3, state.activeBatches)
        assertEquals(4, state.students.size)
        assertEquals(3, state.batches.size)
    }

    @Test
    fun `refreshData reloads instructor data`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.students.isNotEmpty())
    }
}
