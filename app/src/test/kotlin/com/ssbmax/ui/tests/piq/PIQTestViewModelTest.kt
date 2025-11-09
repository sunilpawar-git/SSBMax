package com.ssbmax.ui.tests.piq

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.TestEligibility
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PIQTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: PIQTestViewModel
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var difficultyManager: DifficultyProgressionManager
    private lateinit var securityLogger: SecurityEventLogger
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        submissionRepository = mockk()
        observeCurrentUser = mockk()
        userProfileRepository = mockk()
        subscriptionManager = mockk()
        difficultyManager = mockk()
        securityLogger = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("testId" to "piq_standard"))

        // Default mocks - observeCurrentUser returns SSBMaxUser
        every { observeCurrentUser() } returns flowOf(
            SSBMaxUser(
                id = "test-user-id",
                email = "test@example.com",
                displayName = "Test User",
                role = UserRole.STUDENT,
                subscriptionTier = SubscriptionTier.FREE
            )
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(PIQPage.PAGE_1, state.currentPage)
        assertTrue(state.answers.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.submissionComplete)
    }

    @Test
    fun `updateField updates answers map`() = runTest {
        viewModel = createViewModel()

        viewModel.updateField("fullName", "John Doe")
        advanceTimeBy(100) // Give time for update

        assertEquals("John Doe", viewModel.uiState.value.answers["fullName"])
    }

    @Test
    fun `navigateToPage updates currentPage`() = runTest {
        viewModel = createViewModel()

        viewModel.navigateToPage(PIQPage.PAGE_2)

        assertEquals(PIQPage.PAGE_2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `goToReview shows review screen`() = runTest {
        viewModel = createViewModel()

        viewModel.goToReview()

        assertTrue(viewModel.uiState.value.showReviewScreen)
    }

    @Test
    fun `editPage hides review and goes to page`() = runTest {
        viewModel = createViewModel()
        viewModel.goToReview()

        viewModel.editPage(PIQPage.PAGE_1)

        assertFalse(viewModel.uiState.value.showReviewScreen)
        assertEquals(PIQPage.PAGE_1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `submitTest fails when user not authenticated`() = runTest {
        every { observeCurrentUser() } returns flowOf(null)
        viewModel = createViewModel()

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("login") == true)
    }

    @Test
    fun `submitTest fails when subscription limit reached`() = runTest {
        coEvery { subscriptionManager.canTakeTest(TestType.PIQ, any()) } returns 
            TestEligibility.LimitReached(
                tier = SubscriptionTier.FREE,
                limit = 10,
                usedCount = 10,
                resetsAt = "2025-12-01"
            )
        
        viewModel = createViewModel()

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error?.contains("limit") == true)
    }

    @Test
    fun `submitTest succeeds when eligible`() = runTest {
        // Mock successful flow
        coEvery { subscriptionManager.canTakeTest(TestType.PIQ, any()) } returns 
            TestEligibility.Eligible(10)
        
        coEvery { userProfileRepository.getUserProfile(any()) } returns 
            flowOf(Result.success(
                UserProfile(
                    userId = "test-user-id",
                    fullName = "Test User",
                    age = 25,
                    gender = Gender.MALE,
                    entryType = EntryType.GRADUATE,
                    subscriptionType = SubscriptionType.FREE
                )
            ))
        
        coEvery { submissionRepository.submitPIQ(any(), any()) } returns 
            Result.success("submission-123")
        
        coEvery { subscriptionManager.recordTestUsage(any(), any(), any()) } just Runs
        
        coEvery { difficultyManager.recordPerformance(any(), any(), any(), any(), any(), any()) } just Runs

        viewModel = createViewModel()
        
        // Add required fields
        viewModel.updateField("fullName", "John Doe")
        viewModel.updateField("dateOfBirth", "01/01/2000")
        viewModel.updateField("phone", "1234567890")

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.submissionComplete)
        
        // Verify interactions
        coVerify { submissionRepository.submitPIQ(any(), any()) }
        coVerify { subscriptionManager.recordTestUsage(TestType.PIQ, "test-user-id", "submission-123") }
        coVerify { difficultyManager.recordPerformance(
            testType = "PIQ",
            difficulty = "STANDARD",
            score = 100f,
            correctAnswers = 1,
            totalQuestions = 1,
            timeSeconds = 0f
        ) }
    }

    @Test
    fun `clearError clears error message`() = runTest {
        every { observeCurrentUser() } returns flowOf(null)
        viewModel = createViewModel()

        viewModel.submitTest()
        advanceUntilIdle()
        
        // Error should be present
        assertTrue(viewModel.uiState.value.error != null)

        viewModel.clearError()

        // Error should be cleared
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `canSubmit is false without required fields`() = runTest {
        viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `canSubmit is true with required fields`() = runTest {
        viewModel = createViewModel()

        viewModel.updateField("fullName", "John Doe")
        viewModel.updateField("dateOfBirth", "01/01/2000")
        viewModel.updateField("phone", "1234567890")
        advanceTimeBy(100)

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    private fun createViewModel() = PIQTestViewModel(
        submissionRepository = submissionRepository,
        observeCurrentUser = observeCurrentUser,
        userProfileRepository = userProfileRepository,
        subscriptionManager = subscriptionManager,
        difficultyManager = difficultyManager,
        securityLogger = securityLogger,
        savedStateHandle = savedStateHandle
    )
}
