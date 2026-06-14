package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import androidx.work.WorkManager
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.GenderTag
import com.ssbmax.core.domain.model.PPDTQuestion
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.TestSessionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 3 tests: Profile gate and gender-based image routing.
 *
 * Split from PPDTTestViewModelTest.kt because that file already exceeds 280 lines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTProfileGateTest : BaseViewModelTest() {

    private lateinit var viewModel: PPDTTestViewModel

    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSessionRepo = mockk<TestSessionRepository>(relaxed = true)
    private val mockSubmissionRepo = mockk<com.ssbmax.core.domain.repository.SubmissionRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    private val mockGetOLQDashboard = mockk<com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)

    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    private val mockQuestion = PPDTQuestion(
        id = "ppdt_q1",
        imageUrl = "https://example.com/ppdt-image.jpg",
        imageDescription = "A hazy image showing people in a situation",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 200,
        maxCharacters = 1000
    )

    private fun buildProfile(gender: Gender) = UserProfile(
        userId = "test-user-123",
        fullName = "Test User",
        age = 22,
        gender = gender,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE,
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockSessionRepo.createTestSession(any(), any(), TestType.PPDT) } returns Result.success("session-123")
        coEvery { mockSubscriptionManager.canTakeTest(any(), any()) } returns
            com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 1)
        coEvery { mockTestContentRepo.getPPDTQuestion(genderTag = any()) } returns Result.success(mockQuestion)
        // Default profile: MALE (used by tests that don't override it)
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(buildProfile(Gender.MALE)))
    }

    private fun buildViewModel() = PPDTTestViewModel(
        mockTestContentRepo, mockSessionRepo, mockSubmissionRepo,
        mockObserveCurrentUser, mockUserProfileRepo, mockDifficultyManager,
        mockSubscriptionManager, mockGetOLQDashboard, mockSecurityLogger, mockWorkManager
    )

    // ==================== Profile Gate (3 tests) ====================

    @Test
    fun `loadTest emits isProfileIncomplete=true when profile is not found`() = runTest {
        // WHY: AI assessment accuracy depends on gender — candidate must set up profile first.
        // Gating on null profile (server explicitly says no profile) prevents misleading assessments.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(null))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        viewModel.uiState.test {
            assertTrue("Should mark profile incomplete when no profile found", awaitItem().isProfileIncomplete)
        }
    }

    @Test
    fun `loadTest proceeds normally when profile exists with gender set`() = runTest {
        // WHY: A user with a complete profile should never be blocked.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(buildProfile(Gender.MALE)))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not flag profile incomplete when profile exists", state.isProfileIncomplete)
            assertFalse("Should finish loading", state.isLoading)
            assertNull("Should have no error", state.error)
        }
    }

    @Test
    fun `loadTest does not gate on profile network error`() = runTest {
        // WHY: Network error ≠ confirmed missing profile. Blocking on transient failures ruins the
        // candidate's session. Proceed and default to full image pool.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.failure(Exception("timeout")))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should NOT gate on network error", state.isProfileIncomplete)
        }
    }

    // ==================== Gender Routing (3 tests) ====================

    @Test
    fun `loadTest calls getPPDTQuestion with FEMALE tag when user gender is FEMALE`() = runTest {
        // WHY: Female candidates must only see female-lead or mixed images for accurate PPDT scoring.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(buildProfile(Gender.FEMALE)))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        coVerify { mockTestContentRepo.getPPDTQuestion(genderTag = GenderTag.FEMALE) }
    }

    @Test
    fun `loadTest calls getPPDTQuestion with MALE tag when user gender is MALE`() = runTest {
        // WHY: Male candidates should see male-lead or mixed images for appropriate SSB scoring context.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(buildProfile(Gender.MALE)))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        coVerify { mockTestContentRepo.getPPDTQuestion(genderTag = GenderTag.MALE) }
    }

    @Test
    fun `loadTest calls getPPDTQuestion with null tag when gender is OTHER`() = runTest {
        // WHY: OTHER gender → full image pool (no filter). Same fallback used for network errors,
        // ensuring test continuity without discrimination.
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(buildProfile(Gender.OTHER)))

        viewModel = buildViewModel()
        viewModel.loadTest()  // screen's LaunchedEffect is SSOT — tests must mirror it
        advanceUntilIdle()

        coVerify { mockTestContentRepo.getPPDTQuestion(genderTag = null) }
    }
}
