@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.instructorgrading

import com.ssbmax.shared.domain.model.GradingPriority
import com.ssbmax.shared.domain.model.GradingQueueItem
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeGradingQueueRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization test, written before converting [InstructorGradingViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the `init { loadPendingSubmissions() }`
 * auto-load, `stateIn`/`launchIn`-based queue observation, and filter-by-type
 * behaviour.
 */
class InstructorGradingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var gradingQueueRepository: FakeGradingQueueRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser(id = "instructor-1"))
        gradingQueueRepository = FakeGradingQueueRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun item(status: SubmissionStatus, testType: TestType = TestType.TAT) = GradingQueueItem(
        submissionId = "sub-1", studentId = "student-1", studentName = "Cadet Singh",
        testType = testType, testName = "TAT", submittedAt = 0L, status = status,
        priority = GradingPriority.NORMAL
    )

    private fun buildViewModel() = InstructorGradingViewModel(gradingQueueRepository, authRepository)

    @Test
    fun `unauthenticated instructor surfaces a login prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please login to view grading queue", viewModel.uiState.value.error)
    }

    @Test
    fun `loads the pending queue on construction`() = runTest(testDispatcher) {
        gradingQueueRepository.pendingSubmissionsFlow = flowOf(
            listOf(item(SubmissionStatus.SUBMITTED_PENDING_REVIEW), item(SubmissionStatus.UNDER_REVIEW))
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.submissions.size)
        assertEquals(1, state.pendingCount)
        assertEquals(1, state.underReviewCount)
    }

    @Test
    fun `filterByType switches to the per-test-type queue`() = runTest(testDispatcher) {
        gradingQueueRepository.submissionsByTestTypeFlow = flowOf(listOf(item(SubmissionStatus.SUBMITTED_PENDING_REVIEW, TestType.WAT)))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filterByType(TestType.WAT)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestType.WAT, state.filteredType)
        assertEquals(1, state.submissions.size)
    }
}
