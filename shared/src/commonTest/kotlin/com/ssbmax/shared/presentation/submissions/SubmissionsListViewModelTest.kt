@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.submissions

import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.submission.GetUserSubmissionsUseCase
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

/**
 * Characterization test, written before converting [SubmissionsListViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the `init { loadSubmissions() }`
 * auto-load, filter-by-type/status, and index-error-becomes-empty-list
 * behaviour.
 */
class SubmissionsListViewModelTest {

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

    private fun buildViewModel() = SubmissionsListViewModel(
        getUserSubmissions = GetUserSubmissionsUseCase(submissionRepository),
        authRepository = authRepository
    )

    @Test
    fun `unauthenticated user surfaces a login prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please login to view submissions", viewModel.uiState.value.error)
    }

    @Test
    fun `loads and maps submissions on construction`() = runTest(testDispatcher) {
        submissionRepository.getUserSubmissionsResult = Result.success(
            listOf(
                mapOf(
                    "id" to "sub-1", "testType" to "TAT", "testId" to "tat_standard",
                    "status" to "GRADED", "submittedAt" to 1000L
                )
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.submissions.size)
        assertEquals(TestType.TAT, state.submissions.first().testType)
        assertEquals(1, state.gradedCount)
    }

    @Test
    fun `an index-related failure degrades to an empty list without an error`() = runTest(testDispatcher) {
        submissionRepository.getUserSubmissionsResult = Result.failure(Exception("FAILED_PRECONDITION: index required"))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.submissions.isEmpty())
        assertEquals(null, state.error)
    }

    @Test
    fun `filterByStatus narrows the currently loaded submissions`() = runTest(testDispatcher) {
        submissionRepository.getUserSubmissionsResult = Result.success(
            listOf(
                mapOf("id" to "1", "testType" to "TAT", "testId" to "t", "status" to "GRADED", "submittedAt" to 1L),
                mapOf("id" to "2", "testType" to "WAT", "testId" to "t", "status" to "DRAFT", "submittedAt" to 2L)
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filterByStatus(SubmissionStatus.GRADED)

        assertEquals(1, viewModel.uiState.value.submissions.size)
    }
}
