package com.ssbmax.ui.tests.oir

import android.util.Log
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.core.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.core.domain.util.NoOpLogger
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.time.FakeClock
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.BeforeClass

/**
 * Shared infrastructure for OIRTestViewModel test classes.
 * Contains mocks, setup, and helper methods used by all OIR test files.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class OIRViewModelTestBase : BaseViewModelTest() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.v(any(), any()) } returns 0
        }
    }

    protected lateinit var viewModel: OIRTestViewModel
    protected val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    protected val mockSubmissionRepo = mockk<com.ssbmax.core.domain.repository.SubmissionRepository>(relaxed = true)
    protected val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    protected val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    protected val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    protected val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    protected val mockGetOLQDashboard = mockk<com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase>(relaxed = true)
    protected val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    protected val mockImageLoader = mockk<coil.ImageLoader>(relaxed = true)
    protected val realScoreCalculator = OIRTestScoreCalculator(NoOpLogger())
    protected val mockSubmitUseCase = mockk<SubmitOIRTestUseCase>(relaxed = true)
    protected val mockContext = mockk<android.content.Context>(relaxed = true)
    protected val fakeClock = FakeClock()

    protected val mockQuestions = createMockQuestions()
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
    fun setup() {
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(mockQuestions)

        coEvery {
            mockObserveCurrentUser()
        } returns flowOf(mockUser)

        coEvery {
            mockUserProfileRepo.getUserProfile(any())
        } returns flowOf(Result.success(mockUserProfile))

        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 1
        )

        coEvery { mockTestContentRepo.initializeOIRCache() } returns Result.success(Unit)
        coEvery { mockTestContentRepo.getOIRCacheStatus() } returns mockk(relaxed = true)

        coEvery {
            mockGetOLQDashboard.invalidateCache(any())
        } returns Unit

        coEvery { mockSubmitUseCase(any()) } answers {
            val session = firstArg<com.ssbmax.core.domain.model.OIRTestSession>()
            Result.success(session.sessionId)
        }
    }

    protected fun createViewModel(): OIRTestViewModel {
        return OIRTestViewModel(
            testContentRepository = mockTestContentRepo,
            observeCurrentUser    = mockObserveCurrentUser,
            userProfileRepository = mockUserProfileRepo,
            subscriptionManager   = mockSubscriptionManager,
            securityLogger        = mockSecurityLogger,
            scoreCalculator       = realScoreCalculator,
            submitOIRTestUseCase  = mockSubmitUseCase,
            clock                 = fakeClock,
            appContext            = mockContext,
            imageLoader           = mockImageLoader
        )
    }

    protected fun createMockQuestions(): List<OIRQuestion> {
        return listOf(
            OIRQuestion(
                id = "oir_q1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Which word is different?",
                options = listOf(
                    OIROption("opt1", "Dog"),
                    OIROption("opt2", "Cat"),
                    OIROption("opt3", "Table"),
                    OIROption("opt4", "Rabbit")
                ),
                correctAnswerId = "opt3",
                explanation = "Table is not an animal"
            ),
            OIRQuestion(
                id = "oir_q2",
                questionNumber = 2,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "Find the missing pattern",
                options = listOf(
                    OIROption("opt1", "A"),
                    OIROption("opt2", "B"),
                    OIROption("opt3", "C"),
                    OIROption("opt4", "D")
                ),
                correctAnswerId = "opt2",
                explanation = "Pattern follows sequence"
            ),
            OIRQuestion(
                id = "oir_q3",
                questionNumber = 3,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "What is 25% of 80?",
                options = listOf(
                    OIROption("opt1", "15"),
                    OIROption("opt2", "20"),
                    OIROption("opt3", "25"),
                    OIROption("opt4", "30")
                ),
                correctAnswerId = "opt2",
                explanation = "25% of 80 = 20"
            ),
            OIRQuestion(
                id = "oir_q4",
                questionNumber = 4,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.HARD,
                questionText = "Complete the analogy: Day is to Night as Summer is to?",
                options = listOf(
                    OIROption("opt1", "Winter"),
                    OIROption("opt2", "Spring"),
                    OIROption("opt3", "Autumn"),
                    OIROption("opt4", "Season")
                ),
                correctAnswerId = "opt1",
                explanation = "Opposite relationship"
            ),
            OIRQuestion(
                id = "oir_q5",
                questionNumber = 5,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.EASY,
                questionText = "What is 7 x 8?",
                options = listOf(
                    OIROption("opt1", "54"),
                    OIROption("opt2", "56"),
                    OIROption("opt3", "58"),
                    OIROption("opt4", "60")
                ),
                correctAnswerId = "opt2",
                explanation = "7 x 8 = 56"
            )
        )
    }
}
