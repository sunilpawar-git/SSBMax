package com.ssbmax.ui.grading

import com.ssbmax.core.domain.model.GradingStatus
import com.ssbmax.core.domain.model.NotificationPriority
import com.ssbmax.core.domain.model.NotificationType
import com.ssbmax.core.domain.model.SSBMaxNotification
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.TestSubmission
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.repository.TestSubmissionRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for TestDetailGradingViewModel
 * Tests grading submission, notification sending, and authentication
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDetailGradingViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: TestDetailGradingViewModel
    private val mockTestSubmissionRepository = mockk<TestSubmissionRepository>()
    private val mockNotificationRepository = mockk<NotificationRepository>()
    private val mockUserProfileRepository = mockk<com.ssbmax.core.domain.repository.UserProfileRepository>()
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>()
    private val mockCurrentUserFlow = MutableStateFlow<SSBMaxUser?>(null)

    private val mockInstructor = SSBMaxUser(
        id = "instructor-123",
        email = "instructor@example.com",
        displayName = "Test Instructor",
        photoUrl = null,
        role = UserRole.INSTRUCTOR,
        subscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
        subscription = null,
        studentProfile = null,
        instructorProfile = null,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    private val mockSubmission = TestSubmission(
        id = "submission-123",
        testId = "test-789",
        userId = "student-456",
        testType = TestType.TAT,
        phase = com.ssbmax.core.domain.model.TestPhase.PHASE_1,
        submittedAt = System.currentTimeMillis(),
        responses = emptyList(),
        aiPreliminaryScore = 75f,
        instructorScore = null,
        finalScore = null,
        gradingStatus = GradingStatus.PENDING,
        instructorId = null,
        instructorFeedback = null,
        gradedAt = null
    )

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        // Mock current user flow
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow

        // Mock user profile repository to return a test user profile
        every { mockUserProfileRepository.getUserProfile(any()) } returns kotlinx.coroutines.flow.flowOf(
            Result.success(
                com.ssbmax.core.domain.model.UserProfile(
                    userId = "student-456",
                    fullName = "Test Student",
                    age = 22,
                    gender = com.ssbmax.core.domain.model.Gender.MALE,
                    entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
                    profilePictureUrl = null,
                    subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.FREE,
                    currentStreak = 0,
                    lastLoginDate = null,
                    longestStreak = 0
                )
            )
        )

        viewModel = TestDetailGradingViewModel(
            mockTestSubmissionRepository,
            mockNotificationRepository,
            mockUserProfileRepository,
            mockObserveCurrentUser
        )
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertFalse("Initial loading should be false", state.isLoading)
        assertFalse("Initial submitting should be false", state.isSubmitting)
        assertNull("Initial submission should be null", state.submission)
        assertEquals("Initial grade should be 0", 0f, state.grade, 0.01f)
        assertEquals("Initial remarks should be empty", "", state.remarks)
        assertFalse("Initial gradingSubmitted should be false", state.gradingSubmitted)
        assertNull("Initial error should be null", state.error)
    }

    @Test
    fun `loadSubmission with success updates state`() = runTest {
        // Given
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)

        // When
        viewModel.loadSubmission("submission-123")

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false after completion", state.isLoading)
        assertNotNull("Submission should not be null", state.submission)
        assertEquals("Submission ID should match", "submission-123", state.submission?.id)
        assertEquals("Grade should be AI score", 75f, state.grade, 0.01f)
        assertNull("Error should be null on success", state.error)
    }

    @Test
    fun `loadSubmission with instructorScore uses that score`() = runTest {
        // Given
        val submissionWithInstructorScore = mockSubmission.copy(
            instructorScore = 85f,
            instructorFeedback = "Good work"
        )
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(submissionWithInstructorScore)

        // When
        viewModel.loadSubmission("submission-123")

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertEquals("Grade should be instructor score", 85f, state.grade, 0.01f)
        assertEquals("Remarks should match instructor feedback", "Good work", state.remarks)
    }

    @Test
    fun `loadSubmission with failure shows error`() = runTest {
        // Given
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.failure(Exception("Submission not found"))

        // When
        viewModel.loadSubmission("submission-123")

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error should not be null", state.error)
        // Error message is either the exception message or the default
        assertTrue("Error should be set", state.error == "Submission not found" || state.error == "Failed to load submission")
    }

    @Test
    fun `updateGrade updates grade in state`() {
        // When
        viewModel.updateGrade(88f)

        // Then
        val state = viewModel.uiState.value
        assertEquals("Grade should be updated", 88f, state.grade, 0.01f)
    }

    @Test
    fun `updateRemarks updates remarks in state`() {
        // When
        viewModel.updateRemarks("Excellent analysis")

        // Then
        val state = viewModel.uiState.value
        assertEquals("Remarks should be updated", "Excellent analysis", state.remarks)
    }

    @Test
    fun `submitGrading with invalid grade shows error`() = runTest {
        // Given - load a submission first
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)
        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)

        // When - try to submit with invalid grade
        viewModel.updateGrade(150f) // Invalid: > 100
        viewModel.submitGrading()

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention grade range", state.error?.contains("between 0 and 100") == true)
    }

    @Test
    fun `submitGrading without authenticated user shows error`() = runTest {
        // Given - no authenticated user
        mockCurrentUserFlow.value = null
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)
        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)

        // When
        viewModel.updateGrade(85f)
        viewModel.submitGrading()

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Submitting should be false", state.isSubmitting)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention login", state.error?.contains("logged in") == true)
    }

    @Test
    fun `submitGrading with success updates submission and sends notification`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)

        val submissionSlot = slot<TestSubmission>()
        coEvery { mockTestSubmissionRepository.updateSubmission(capture(submissionSlot)) } returns
            Result.success(Unit)

        val notificationSlot = slot<SSBMaxNotification>()
        coEvery { mockNotificationRepository.saveNotification(capture(notificationSlot)) } returns
            Result.success(Unit)

        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)

        // When
        viewModel.updateGrade(92f)
        viewModel.updateRemarks("Outstanding work!")
        viewModel.submitGrading()

        // Wait for async operation
        kotlinx.coroutines.delay(200)

        // Then - verify submission was updated
        coVerify { mockTestSubmissionRepository.updateSubmission(any()) }
        val updatedSubmission = submissionSlot.captured
        assertEquals("Instructor score should match", 92f, updatedSubmission.instructorScore ?: 0f, 0.01f)
        assertEquals("Final score should match", 92f, updatedSubmission.finalScore ?: 0f, 0.01f)
        assertEquals("Feedback should match", "Outstanding work!", updatedSubmission.instructorFeedback)
        assertEquals("Grading status should be GRADED", GradingStatus.GRADED, updatedSubmission.gradingStatus)
        assertEquals("Instructor ID should match", "instructor-123", updatedSubmission.instructorId)

        // Then - verify notification was sent
        coVerify { mockNotificationRepository.saveNotification(any()) }
        val notification = notificationSlot.captured
        assertEquals("Notification user ID should match student", "student-456", notification.userId)
        assertEquals("Notification type should be GRADING_COMPLETE", NotificationType.GRADING_COMPLETE, notification.type)
        assertEquals("Notification priority should be HIGH", NotificationPriority.HIGH, notification.priority)
        assertTrue("Notification title should mention test type", notification.title.contains("TAT"))
        assertTrue("Notification message should mention score", notification.message.contains("92"))

        // Then - verify state
        val state = viewModel.uiState.value
        assertFalse("Submitting should be false", state.isSubmitting)
        assertTrue("GradingSubmitted should be true", state.gradingSubmitted)
        assertNull("Error should be null", state.error)
    }

    @Test
    fun `submitGrading with repository failure shows error`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)
        coEvery { mockTestSubmissionRepository.updateSubmission(any()) } returns
            Result.failure(Exception("Network error"))

        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)

        // When
        viewModel.updateGrade(85f)
        viewModel.submitGrading()

        // Wait for async operation
        kotlinx.coroutines.delay(100)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Submitting should be false", state.isSubmitting)
        assertNotNull("Error should not be null", state.error)
        // Error message is either the exception message or the default
        assertTrue("Error should be set", state.error == "Network error" || state.error?.contains("Failed to submit grading") == true)
    }

    @Test
    fun `submitGrading with notification failure shows partial error`() = runTest {
        // Given
        mockCurrentUserFlow.value = mockInstructor
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)
        coEvery { mockTestSubmissionRepository.updateSubmission(any()) } returns
            Result.success(Unit)
        coEvery { mockNotificationRepository.saveNotification(any()) } returns
            Result.failure(Exception("FCM error"))

        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)

        // When
        viewModel.updateGrade(85f)
        viewModel.submitGrading()

        // Wait for async operation
        kotlinx.coroutines.delay(200)

        // Then
        val state = viewModel.uiState.value
        assertFalse("Submitting should be false", state.isSubmitting)
        assertNotNull("Error should not be null", state.error)
        assertTrue("Error should mention notification failure", state.error?.contains("notification failed") == true)
    }

    @Test
    fun `clearError clears error state`() {
        // Given - simulate an error
        viewModel.updateGrade(150f)
        viewModel.submitGrading()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.uiState.value
        assertNull("Error should be null after clearing", state.error)
    }

    @Test
    fun `resetSubmittedState resets gradingSubmitted flag`() = runTest {
        // Given - simulate successful submission
        mockCurrentUserFlow.value = mockInstructor
        coEvery { mockTestSubmissionRepository.getSubmissionById("submission-123") } returns
            Result.success(mockSubmission)
        coEvery { mockTestSubmissionRepository.updateSubmission(any()) } returns Result.success(Unit)
        coEvery { mockNotificationRepository.saveNotification(any()) } returns Result.success(Unit)

        viewModel.loadSubmission("submission-123")
        kotlinx.coroutines.delay(100)
        viewModel.updateGrade(85f)
        viewModel.submitGrading()
        kotlinx.coroutines.delay(200)

        // Verify gradingSubmitted is true
        assertTrue("GradingSubmitted should be true", viewModel.uiState.value.gradingSubmitted)

        // When
        viewModel.resetSubmittedState()

        // Then
        assertFalse("GradingSubmitted should be false after reset", viewModel.uiState.value.gradingSubmitted)
    }
}
