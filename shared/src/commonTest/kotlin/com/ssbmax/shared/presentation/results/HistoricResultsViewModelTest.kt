@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.results

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
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
 * Characterization test, written before converting [HistoricResultsViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). The `init { loadResults() }`
 * auto-load-on-construction behaviour must survive the conversion unchanged
 * -- `init` blocks run identically whether the class extends
 * `androidx.lifecycle.ViewModel` or not.
 */
class HistoricResultsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var submissionRepository: FakeSubmissionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        submissionRepository = FakeSubmissionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = HistoricResultsViewModel(
        authRepository = authRepository,
        getHistoricResults = GetHistoricResultsUseCase(submissionRepository),
        logger = NoOpLogger()
    )

    @Test
    fun `unauthenticated user surfaces a login prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Please login to view results", state.error)
    }

    @Test
    fun `loads completed TAT results on construction`() = runTest(testDispatcher) {
        submissionRepository.getUserSubmissionsResult = Result.success(
            listOf(
                mapOf(
                    "id" to "sub-1",
                    "submittedAt" to kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                    "analysisStatus" to "COMPLETED",
                    "olqResult" to mapOf("overallScore" to 7.5, "overallRating" to "Above Average")
                )
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.results.size)
        assertEquals(TestType.TAT, state.results.first().testType)
        assertEquals(null, state.error)
    }

    @Test
    fun `refresh reloads with the previously selected filter`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filterByTestType(TestType.TAT)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TestType.TAT, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `repository failure surfaces an error message`() = runTest(testDispatcher) {
        submissionRepository.getUserSubmissionsResult = Result.failure(Exception("offline"))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertTrue(state.results.isEmpty())
    }
}
