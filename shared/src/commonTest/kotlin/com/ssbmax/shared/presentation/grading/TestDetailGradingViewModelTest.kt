@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.grading

import com.ssbmax.shared.domain.model.GradingStatus
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestSubmission
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeNotificationRepository
import com.ssbmax.shared.presentation.testing.FakeTestSubmissionRepository
import com.ssbmax.shared.presentation.testing.FakeUserProfileRepository
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
 * Characterization test, written before converting [TestDetailGradingViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the assessor grading flow:
 * load submission + student name, validate the 0-100 grade range, submit
 * grading, and send a persisted (not pushed) student notification.
 */
class TestDetailGradingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var testSubmissionRepository: FakeTestSubmissionRepository
    private lateinit var notificationRepository: FakeNotificationRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser(id = "instructor-1"))
        testSubmissionRepository = FakeTestSubmissionRepository()
        notificationRepository = FakeNotificationRepository()
        userProfileRepository = FakeUserProfileRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission() = TestSubmission(
        id = "sub-1", testId = "tat_standard", userId = "student-1", testType = TestType.TAT,
        phase = TestPhase.PHASE_2, submittedAt = 0L, responses = emptyList()
    )

    private fun buildViewModel() = TestDetailGradingViewModel(
        testSubmissionRepository = testSubmissionRepository,
        notificationRepository = notificationRepository,
        userProfileRepository = userProfileRepository,
        authRepository = authRepository
    )

    @Test
    fun `loadSubmission populates submission and default grade`() = runTest(testDispatcher) {
        testSubmissionRepository.getSubmissionByIdResult = Result.success(submission())
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("sub-1", state.submission?.id)
        assertEquals(0f, state.grade)
    }

    @Test
    fun `grade outside 0-100 is rejected before submitting`() = runTest(testDispatcher) {
        testSubmissionRepository.getSubmissionByIdResult = Result.success(submission())
        val viewModel = buildViewModel()
        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGrade(150f)
        viewModel.submitGrading()

        val state = viewModel.uiState.value
        assertEquals("Grade must be between 0 and 100", state.error)
        assertEquals(0, testSubmissionRepository.updatedSubmissions.size)
    }

    @Test
    fun `submitGrading updates the submission and notifies the student`() = runTest(testDispatcher) {
        testSubmissionRepository.getSubmissionByIdResult = Result.success(submission())
        val viewModel = buildViewModel()
        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGrade(85f)
        viewModel.updateRemarks("Well structured")
        viewModel.submitGrading()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.gradingSubmitted)
        assertEquals(1, testSubmissionRepository.updatedSubmissions.size)
        assertEquals(GradingStatus.GRADED, testSubmissionRepository.updatedSubmissions.first().gradingStatus)
        assertEquals(1, notificationRepository.savedNotifications.size)
    }

    @Test
    fun `unauthenticated instructor cannot submit grading`() = runTest(testDispatcher) {
        testSubmissionRepository.getSubmissionByIdResult = Result.success(submission())
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGrade(85f)
        viewModel.submitGrading()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("You must be logged in to submit grading", viewModel.uiState.value.error)
    }
}
