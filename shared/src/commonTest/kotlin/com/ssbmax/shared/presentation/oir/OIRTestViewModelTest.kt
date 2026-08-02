@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.oir

import com.ssbmax.shared.domain.model.OIROption
import com.ssbmax.shared.domain.model.OIRQuestion
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.QuestionDifficulty
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.repository.UsageInfo
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeGTORepository
import com.ssbmax.shared.presentation.testing.FakeInterviewRepository
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import com.ssbmax.shared.presentation.testing.FakeSubscriptionRepository
import com.ssbmax.shared.presentation.testing.FakeTestContentRepository
import com.ssbmax.shared.presentation.testing.FakeTestSessionRepository
import com.ssbmax.shared.presentation.testing.FakeTestUsageRecorder
import com.ssbmax.shared.presentation.testing.FakeUserProfileRepository
import com.ssbmax.shared.presentation.testing.RecordingAnalyticsTracker
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Characterization test, written before converting [OIRTestViewModel] to
 * `androidx.lifecycle.ViewModel` (Phase 1 of the KMP-convergence plan's
 * strict-SSOT execution plan). Pins the current state-machine behaviour
 * (load -> answer -> navigate -> submit -> timer) so the ViewModel/scope
 * conversion is provably behaviour-preserving: this file must pass
 * unmodified against both the pre- and post-conversion class.
 */
class OIRTestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var testContentRepository: FakeTestContentRepository
    private lateinit var testSessionRepository: FakeTestSessionRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var submissionRepository: FakeSubmissionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
        testContentRepository = FakeTestContentRepository().apply {
            oirQuestionsResult = Result.success(listOf(question("q1"), question("q2")))
        }
        testSessionRepository = FakeTestSessionRepository()
        userProfileRepository = FakeUserProfileRepository()
        submissionRepository = FakeSubmissionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun question(id: String) = OIRQuestion(
        id = id,
        questionNumber = 1,
        type = OIRQuestionType.NON_VERBAL_REASONING,
        questionText = "Which shape completes the sequence?",
        options = listOf(
            OIROption("opt_a", "Circle"),
            OIROption("opt_b", "Square"),
            OIROption("opt_c", "Triangle"),
            OIROption("opt_d", "Hexagon")
        ),
        correctAnswerId = "opt_a",
        explanation = "The pattern repeats every three shapes.",
        difficulty = QuestionDifficulty.EASY
    )

    private fun buildViewModel(): OIRTestViewModel {
        val logger = NoOpLogger()
        val scoreCalculator = OIRTestScoreCalculator(logger)
        val submitOIRTestUseCase = SubmitOIRTestUseCase(
            scoreCalculator = scoreCalculator,
            usageRecorder = FakeTestUsageRecorder(),
            dashboardUseCase = GetOLQDashboardUseCase(
                submissionRepository = submissionRepository,
                gtoRepository = FakeGTORepository(),
                interviewRepository = FakeInterviewRepository(),
                logger = logger
            ),
            submissionRepository = submissionRepository,
            testSessionRepository = testSessionRepository,
            testContentRepository = testContentRepository
        )
        return OIRTestViewModel(
            testContentRepository = testContentRepository,
            testSessionRepository = testSessionRepository,
            observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
            userProfileRepository = userProfileRepository,
            checkTestEligibility = CheckTestEligibilityUseCase(subscriptionRepository, RecordingAnalyticsTracker()),
            scoreCalculator = scoreCalculator,
            submitOIRTestUseCase = submitOIRTestUseCase,
            logger = logger,
            analyticsTracker = RecordingAnalyticsTracker()
        )
    }

    @Test
    fun `unauthenticated access is blocked`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OIRErrorType.AUTH_REQUIRED, state.errorType)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `limit reached surfaces subscription details without loading questions`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        subscriptionRepository.monthlyUsageResult =
            Result.success(mapOf("OIR Tests" to UsageInfo(used = 1, limit = 1)))

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLimitReached)
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals(1, state.testsLimit)
        assertEquals(1, state.testsUsed)
        assertEquals(null, state.currentQuestion)
    }

    @Test
    fun `loads questions and starts timer when eligible`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.currentQuestion)
        assertEquals(2, state.totalQuestions)
        assertTrue(state.isTimerActive)
    }

    @Test
    fun `selecting an option records the answer and shows feedback`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.selectOption("opt_a")

        val state = viewModel.uiState.value
        assertEquals(setOf("opt_a"), state.selectedOptionIds)
        assertTrue(state.showFeedback)
        assertTrue(state.isCurrentAnswerCorrect)
        assertTrue(state.currentQuestionAnswered)
    }

    @Test
    fun `nextQuestion advances index and resets feedback for unanswered question`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.selectOption("opt_a")
        viewModel.nextQuestion()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentQuestionIndex)
        assertEquals(false, state.currentQuestionAnswered)
    }

    @Test
    fun `previousQuestion restores prior answer state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.selectOption("opt_a")
        viewModel.nextQuestion()

        viewModel.previousQuestion()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentQuestionIndex)
        assertTrue(state.currentQuestionAnswered)
        assertEquals(setOf("opt_a"), state.selectedOptionIds)
    }

    @Test
    fun `submitTest completes session and carries subscription type`() = runTest(testDispatcher) {
        userProfileRepository.profileFlow.value = Result.success(null)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.submitTest()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertNotNull(state.sessionId)
        assertEquals(SubscriptionType.FREE, state.subscriptionType)
        assertNotNull(state.testResult)
        assertEquals(false, state.isTimerActive)
    }

    @Test
    fun `submitTest failure surfaces SUBMIT_FAILED without completing`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()
        submissionRepository.submitResult = Result.failure(Exception("network down"))

        viewModel.submitTest()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OIRErrorType.SUBMIT_FAILED, state.errorType)
        assertEquals(false, state.isCompleted)
    }

    @Test
    fun `pauseTest stops the timer and ends the session`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.pauseTest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isTimerActive)
        assertTrue(testSessionRepository.endedSessionIds.isNotEmpty())
    }

    @Test
    fun `timer counts down one second at a time`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()
        val initialRemaining = viewModel.uiState.value.timeRemainingSeconds

        testDispatcher.scheduler.advanceTimeBy(1_000)
        testDispatcher.scheduler.runCurrent()

        assertEquals(initialRemaining - 1, viewModel.uiState.value.timeRemainingSeconds)
    }
}
