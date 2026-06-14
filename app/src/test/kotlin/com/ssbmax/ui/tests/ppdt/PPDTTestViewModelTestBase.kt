package com.ssbmax.ui.tests.ppdt

import androidx.work.WorkManager
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before

/**
 * Shared mock infrastructure for PPDTTestViewModel tests.
 * Each concrete subclass gets the full happy-path setup; individual tests override what they need.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class PPDTTestViewModelTestBase : BaseViewModelTest() {

    protected val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    protected val mockSessionRepo = mockk<TestSessionRepository>(relaxed = true)
    protected val mockSubmissionRepo =
        mockk<com.ssbmax.core.domain.repository.SubmissionRepository>(relaxed = true)
    protected val mockObserveCurrentUser =
        mockk<com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase>(relaxed = true)
    protected val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    protected val mockDifficultyManager =
        mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    protected val mockSubscriptionManager =
        mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    protected val mockGetOLQDashboard =
        mockk<com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase>(relaxed = true)
    protected val mockSecurityLogger =
        mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    protected val mockWorkManager = mockk<WorkManager>(relaxed = true)

    protected val mockQuestion = makeMockQuestion()
    protected val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    protected val mockUserProfile = UserProfile(
        userId = "test-user-123",
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE,
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setupMocks() {
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockSessionRepo.createTestSession(any(), any(), TestType.PPDT) } returns
            Result.success("session-ppdt-123")
        coEvery { mockTestContentRepo.getPPDTQuestion(genderTag = any()) } returns
            Result.success(mockQuestion)
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns
            flowOf(Result.success(mockUserProfile))
        coEvery { mockSubscriptionManager.canTakeTest(any(), any()) } returns
            com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 1)
        coEvery { mockSubscriptionManager.recordTestUsage(any(), any(), any()) } returns Unit
        coEvery { mockSubmissionRepo.submitPPDT(any(), any()) } returns
            Result.success("test-submission-id")
        coEvery { mockDifficultyManager.recordPerformance(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { mockGetOLQDashboard.invalidateCache(any()) } returns Unit
    }

    protected fun buildViewModel() = PPDTTestViewModel(
        mockTestContentRepo,
        mockSessionRepo,
        mockSubmissionRepo,
        mockObserveCurrentUser,
        mockUserProfileRepo,
        mockDifficultyManager,
        mockSubscriptionManager,
        mockGetOLQDashboard,
        mockSecurityLogger,
        mockWorkManager
    )

    protected fun makeMockQuestion() = PPDTQuestion(
        id = "ppdt_q1",
        imageUrl = "https://example.com/ppdt-image.jpg",
        imageDescription = "A hazy image showing people in a situation",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 200,
        maxCharacters = 1000
    )
}
