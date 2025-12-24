package com.ssbmax.ui.tests.gpe

import app.cash.turbine.test
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.TestEligibility
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GPETestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: GPETestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<SubscriptionManager>(relaxed = true)
    private val mockSecurityLogger = mockk<SecurityEventLogger>(relaxed = true)
    private val mockSubmissionHelper = mockk<GTOTestSubmissionHelper>(relaxed = true)
    private val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)

    private val mockUser = SSBMaxUser(
        id = "user-gpe-123",
        email = "test@ssbmax.com",
        displayName = "Test User",
        role = UserRole.STUDENT
    )

    private val mockUserProfile = com.ssbmax.core.domain.model.UserProfile(
        userId = "user-gpe-123",
        fullName = "Test User",
        age = 25,
        gender = com.ssbmax.core.domain.model.Gender.MALE,
        entryType = com.ssbmax.core.domain.model.EntryType.GRADUATE,
        subscriptionType = SubscriptionType.PREMIUM
    )

    private val mockQuestion = GPEQuestion(
        id = "gpe-q1",
        imageUrl = "https://example.com/scenario.jpg",
        scenario = "Plan a rescue operation",
        solution = "Detailed solution",
        imageDescription = "A tactical scenario showing a rescue operation",
        resources = listOf("10 soldiers", "2 vehicles"),
        minCharacters = 50,
        maxCharacters = 1500
    )

    @Before
    fun setup() {
        // Setup default success behavior
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockSubscriptionManager.canTakeTest(TestType.GTO_GPE, any()) } returns 
            TestEligibility.Eligible(remainingTests = 8)
        coEvery { mockTestContentRepo.createTestSession(any(), any(), any()) } returns 
            Result.success("session-gpe-123")
        coEvery { mockTestContentRepo.getGPEQuestions(any()) } returns 
            Result.success(listOf(mockQuestion))
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns 
            flowOf(Result.success(mockUserProfile))
        coEvery { mockDifficultyManager.recordPerformance(any(), any(), any(), any(), any(), any()) } just Runs
        every { mockSecurityLogger.logUnauthenticatedAccess(any(), any()) } just Runs
        
        // Mock submission helper
        val submissionSlot = slot<GTOSubmission.GPESubmission>()
        coEvery { 
            mockSubmissionHelper.submitTest(
                submission = capture(submissionSlot),
                testType = GTOTestType.GROUP_PLANNING_EXERCISE,
                userId = any(),
                onSuccess = any(),
                onError = any()
            )
        } answers {
            val onSuccess = arg<(String) -> Unit>(3)
            onSuccess(submissionSlot.captured.id)
        }

        viewModel = GPETestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockSubmissionHelper
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Wait for init to complete
        advanceUntilIdle()
        
        val state = viewModel.uiState.value

        assertTrue(state.isLoading || state.currentPhase == GPEPhase.INSTRUCTIONS)
        assertNull(state.error)
    }

    @Test
    fun `loadTest success loads scenario and updates state`() = runTest {
        viewModel.loadTest("gpe-standard")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isLimitReached)
        assertEquals(mockQuestion.imageUrl, state.imageUrl)
        assertEquals(mockQuestion.scenario, state.scenario)
        assertEquals(mockQuestion.resources, state.resources)
        assertEquals(GPEPhase.INSTRUCTIONS, state.currentPhase)

        coVerify { mockTestContentRepo.createTestSession(mockUser.id, "gpe-standard", TestType.GTO_GPE) }
        coVerify { mockTestContentRepo.getGPEQuestions("gpe-standard") }
    }

    @Test
    fun `loadTest shows limit reached when subscription limit exceeded`() = runTest {
        coEvery { mockSubscriptionManager.canTakeTest(TestType.GTO_GPE, any()) } returns 
            TestEligibility.LimitReached(
                tier = SubscriptionTier.FREE,
                limit = 3,
                usedCount = 3,
                resetsAt = "Jan 1, 2026"
            )

        viewModel.loadTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.isLimitReached)
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals(3, state.testsLimit)
        assertEquals(3, state.testsUsed)
    }

    @Test
    fun `loadTest shows error when user not authenticated`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(null)

        viewModel.loadTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Authentication required"))

        verify { mockSecurityLogger.logUnauthenticatedAccess(TestType.GTO_GPE, any()) }
    }

    @Test
    fun `startTest transitions to PLANNING phase`() = runTest {
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.startTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(GPEPhase.PLANNING, state.currentPhase)
        // Timer functionality tested separately in integration tests
    }

    @Test
    fun `updatePlanningResponse updates character count and validation`() = runTest {
        viewModel.loadTest()
        advanceUntilIdle()

        val response = "A".repeat(100) // 100 characters
        viewModel.updatePlanningResponse(response)

        val state = viewModel.uiState.value

        assertEquals(response, state.planningResponse)
        assertEquals(100, state.charactersCount)
        assertTrue(state.canProceedToNextPhase) // > minCharacters (50)
    }

    @Test
    fun `proceedToNextPhase moves to REVIEW when response is valid`() = runTest {
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.startTest()
        advanceUntilIdle()

        // Add valid response
        val validResponse = "A".repeat(100)
        viewModel.updatePlanningResponse(validResponse)

        viewModel.proceedToNextPhase()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(GPEPhase.REVIEW, state.currentPhase)
        assertFalse(state.isTimerActive) // Timer should stop
    }

    @Test
    fun `submitTest creates submission and marks as completed`() = runTest {
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.startTest()
        advanceUntilIdle()

        val response = "Detailed planning response with sufficient characters"
        viewModel.updatePlanningResponse(response)

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.isSubmitted)
        assertNotNull(state.submissionId)
        assertEquals(GPEPhase.SUBMITTED, state.currentPhase)
        assertNotNull(state.submission)
        assertEquals(response, state.submission?.plan)

        coVerify { 
            mockSubmissionHelper.submitTest(
                submission = any<GTOSubmission.GPESubmission>(),
                testType = GTOTestType.GROUP_PLANNING_EXERCISE,
                userId = mockUser.id,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // Timer functionality is tested in integration tests
    // Unit tests focus on state management and business logic

    @Test
    fun `pauseTest stops timer and marks as paused`() = runTest {
        viewModel.loadTest()
        advanceUntilIdle()

        viewModel.startTest()
        advanceUntilIdle()

        viewModel.pauseTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isTimerActive)
        assertTrue(state.session?.isPaused == true)
    }

    // returnToPlanning() functionality tested in integration tests
    // Complex phase transitions require full UI flow testing
}

