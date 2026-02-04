package com.ssbmax.ui.interview.start

import android.content.Context
import com.ssbmax.core.domain.model.PIQSubmission
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OIRStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.interview.PIQStatus
import com.ssbmax.core.domain.model.interview.PPDTStatus
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.core.domain.model.interview.SubscriptionStatus
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
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
import java.time.Instant

/**
 * Unit tests for StartInterviewViewModel
 *
 * Tests cover:
 * - Interview history loading
 * - Mode selection
 * - Eligibility checking
 * - Session creation
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StartInterviewViewModelTest : BaseViewModelTest() {

    private lateinit var checkPrerequisites: CheckInterviewPrerequisitesUseCase
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var questionCacheRepository: com.ssbmax.core.domain.model.interview.QuestionCacheRepository
    private lateinit var context: Context

    private val testUserId = "user-123"

    private val testUser = SSBMaxUser(
        id = testUserId,
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT
    )

    private val testPrerequisiteResult = PrerequisiteCheckResult(
        isEligible = true,
        piqStatus = PIQStatus.Completed(submissionId = "piq-123", aiScore = 85f),
        oirStatus = OIRStatus.Completed(submissionId = "oir-123", score = 75f),
        ppdtStatus = PPDTStatus.Completed(submissionId = "ppdt-123"),
        subscriptionStatus = SubscriptionStatus.Available(
            tier = "PREMIUM",
            remaining = 5
        ),
        failureReasons = emptyList()
    )

    private val testInterviewResult = InterviewResult(
        id = "result-1",
        sessionId = "session-1",
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        completedAt = Instant.now(),
        durationSec = 1800L,
        totalQuestions = 10,
        totalResponses = 10,
        overallOLQScores = mapOf(OLQ.SELF_CONFIDENCE to OLQScore(score = 5, confidence = 85, reasoning = "Good confidence")),
        categoryScores = mapOf(OLQCategory.INTELLECTUAL to 75f),
        overallConfidence = 85,
        strengths = listOf(OLQ.SELF_CONFIDENCE),
        weaknesses = listOf(OLQ.SOCIAL_ADJUSTMENT),
        feedback = "Good performance overall",
        overallRating = 5
    )

    private val testPIQSubmission = PIQSubmission(
        id = "piq-123",
        userId = testUserId
    )

    private val testSession = InterviewSession(
        id = "session-123",
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        status = InterviewStatus.IN_PROGRESS,
        startedAt = Instant.now(),
        completedAt = null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 0,
        estimatedDuration = 30
    )

    @Before
    fun setUp() {
        checkPrerequisites = mockk(relaxed = true)
        interviewRepository = mockk(relaxed = true)
        submissionRepository = mockk(relaxed = true)
        observeCurrentUser = mockk(relaxed = true)
        questionCacheRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Default mocks
        every { observeCurrentUser() } returns flowOf(testUser)
        coEvery { interviewRepository.getUserResults(testUserId) } returns flowOf(emptyList())
        coEvery { questionCacheRepository.getPIQQuestions(any(), any(), any()) } returns Result.success(emptyList())
        every { context.getString(any()) } returns "Mocked String"
    }

    private fun createViewModel(): StartInterviewViewModel {
        return StartInterviewViewModel(
            checkPrerequisites = checkPrerequisites,
            interviewRepository = interviewRepository,
            submissionRepository = submissionRepository,
            observeCurrentUser = observeCurrentUser,
            questionCacheRepository = questionCacheRepository,
            context = context
        )
    }

    // ============================================
    // HISTORY LOADING TESTS
    // ============================================

    @Test
    fun `loadInterviewHistory loads past results on init`() = runTest {
        // Given
        val results = listOf(testInterviewResult)
        coEvery { interviewRepository.getUserResults(testUserId) } returns flowOf(results)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading history", state.isLoadingHistory)
        assertEquals("Should have 1 past result", 1, state.pastResults.size)
        assertEquals("Should have correct result", testInterviewResult, state.pastResults.first())
    }

    @Test
    fun `loadInterviewHistory handles empty results`() = runTest {
        // Given
        coEvery { interviewRepository.getUserResults(testUserId) } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading history", state.isLoadingHistory)
        assertTrue("Should have no past results", state.pastResults.isEmpty())
    }

    @Test
    fun `loadInterviewHistory handles failure gracefully`() = runTest {
        // Given - Repository throws exception
        coEvery { interviewRepository.getUserResults(testUserId) } throws RuntimeException("Network error")

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - Should not crash and should finish loading
        val state = viewModel.uiState.value
        assertFalse("Should not be loading history", state.isLoadingHistory)
    }

    // ============================================
    // ELIGIBILITY CHECK TESTS
    // ============================================

    @Test
    fun `checkEligibility success sets isEligible true`() = runTest {
        // Given
        coEvery { checkPrerequisites(any(), any(), any()) } returns Result.success(testPrerequisiteResult)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.checkEligibility()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertTrue("Should be eligible", state.isEligible)
        assertNotNull("Should have prerequisite result", state.prerequisiteResult)
        assertNull("Should have no error", state.error)
    }

    @Test
    fun `checkEligibility failure sets error message`() = runTest {
        // Given
        coEvery { checkPrerequisites(any(), any(), any()) } returns
            Result.failure(Exception("Prerequisites check failed"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.checkEligibility()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not be eligible", state.isEligible)
        assertEquals("Mocked String", state.error)
    }

    @Test
    fun `checkEligibility handles missing user`() = runTest {
        // Given - No user logged in
        every { observeCurrentUser() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.checkEligibility()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not be eligible", state.isEligible)
        assertEquals("Mocked String", state.error)
    }

    // ============================================
    // SESSION CREATION TESTS
    // ============================================

    @Test
    fun `createSession creates session and sets sessionId`() = runTest {
        // Given
        coEvery { checkPrerequisites(any(), any(), any()) } returns Result.success(testPrerequisiteResult)
        coEvery { submissionRepository.getLatestPIQSubmission(testUserId) } returns Result.success(testPIQSubmission)
        coEvery { interviewRepository.createSession(any(), any(), any(), any()) } returns Result.success(testSession)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Make eligible first
        viewModel.checkEligibility()
        advanceUntilIdle()

        // When
        viewModel.createSession(consentGiven = true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have session ID", "session-123", state.sessionId)
        assertTrue("Should be session created", state.isSessionCreated)
        assertNull("Should have no error", state.error)
    }

    @Test
    fun `createSession handles missing PIQ submission`() = runTest {
        // Given
        coEvery { checkPrerequisites(any(), any(), any()) } returns Result.success(testPrerequisiteResult)
        coEvery { submissionRepository.getLatestPIQSubmission(testUserId) } returns Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Make eligible first
        viewModel.checkEligibility()
        advanceUntilIdle()

        // When
        viewModel.createSession(consentGiven = true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have session ID", state.sessionId)
        assertFalse("Should not be session created", state.isSessionCreated)
        assertEquals("Mocked String", state.error)
    }

    @Test
    fun `createSession handles repository failure`() = runTest {
        // Given
        coEvery { checkPrerequisites(any(), any(), any()) } returns Result.success(testPrerequisiteResult)
        coEvery { submissionRepository.getLatestPIQSubmission(testUserId) } returns Result.success(testPIQSubmission)
        coEvery { interviewRepository.createSession(any(), any(), any(), any()) } returns
            Result.failure(Exception("Failed to create session"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Make eligible first
        viewModel.checkEligibility()
        advanceUntilIdle()

        // When
        viewModel.createSession(consentGiven = true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have session ID", state.sessionId)
        assertFalse("Should not be session created", state.isSessionCreated)
        assertEquals("Mocked String", state.error)
    }

    @Test
    fun `createSession blocked when not eligible`() = runTest {
        // Given - User is NOT eligible (default state)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - Try to create session without checking eligibility first
        viewModel.createSession(consentGiven = true)
        advanceUntilIdle()

        // Then - Session should not be created
        val state = viewModel.uiState.value
        assertNull("Should not have session ID", state.sessionId)
        assertFalse("Should not be session created", state.isSessionCreated)
        assertEquals("Mocked String", state.error)
    }

    // ============================================
    // ERROR HANDLING TESTS
    // ============================================

    @Test
    fun `clearError clears error state`() = runTest {
        // Given - Set an error state
        every { observeCurrentUser() } returns flowOf(null)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.checkEligibility()
        advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.uiState.value.error)

        // When
        viewModel.clearError()

        // Then
        assertNull("Error should be null after clearError", viewModel.uiState.value.error)
    }
}
