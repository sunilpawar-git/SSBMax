package com.ssbmax.ui.tests.ppdt

import androidx.work.WorkManager
import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.PPDTPhase
import com.ssbmax.shared.domain.model.PPDTQuestion
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.PPDTTestSession
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestResult
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
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

    protected val mockLoadPPDTTest = mockk<LoadPPDTTestUseCase>(relaxed = true)
    protected val mockSubmitPPDTTest = mockk<SubmitPPDTTestUseCase>(relaxed = true)
    protected val mockObserveCurrentUser =
        mockk<ObserveCurrentUserUseCase>(relaxed = true)
    protected val mockDifficultyManager =
        mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    protected val mockSubscriptionManager =
        mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
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
    protected val mockSession = PPDTTestSession(
        sessionId = "session-ppdt-123",
        userId = "test-user-123",
        questionId = mockQuestion.id,
        question = mockQuestion,
        startTime = System.currentTimeMillis(),
        imageViewingStartTime = null,
        writingStartTime = null,
        currentPhase = PPDTPhase.INSTRUCTIONS,
        story = "",
        isCompleted = false,
        isPaused = false
    )

    @Before
    fun setupMocks() {
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockSubscriptionManager.canTakeTest(any(), any()) } returns
            com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 1)
        coEvery { mockSubscriptionManager.recordTestUsage(any(), any(), any()) } returns Unit
        coEvery { mockLoadPPDTTest(any(), any()) } returns Result.success(mockSession)
        coEvery { mockSubmitPPDTTest(any()) } answers { call ->
            val session = call.invocation.args[0] as PPDTTestSession
            val submission = PPDTSubmission(
                submissionId = "test-submission-id",
                questionId = session.questionId,
                userId = session.userId,
                userName = "Test User",
                userEmail = "",
                batchId = null,
                story = session.story,
                charactersCount = session.story.length,
                viewingTimeTakenSeconds = 30,
                writingTimeTakenMinutes = 4,
                submittedAt = System.currentTimeMillis(),
                status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                instructorReview = null,
                analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
                olqResult = null
            )
            Result.success(SubmitPPDTTestResult("test-submission-id", submission, SubscriptionType.FREE))
        }
        coEvery { mockDifficultyManager.recordPerformance(any(), any(), any(), any(), any(), any()) } returns Unit
    }

    protected fun buildViewModel() = PPDTTestViewModel(
        mockObserveCurrentUser,
        mockLoadPPDTTest,
        mockSubmitPPDTTest,
        mockDifficultyManager,
        mockSubscriptionManager,
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
