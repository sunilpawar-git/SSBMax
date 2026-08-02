@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.srt

import com.ssbmax.shared.domain.model.SRTCategory
import com.ssbmax.shared.domain.model.SRTPhase
import com.ssbmax.shared.domain.model.SRTSituation
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.repository.UsageInfo
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.domain.util.ObservabilitySeam
import com.ssbmax.shared.presentation.testing.clearForTest
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeSubmissionAnalysisTrigger
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
 * Characterization test, written before converting [SRTTestViewModel] to
 * `androidx.lifecycle.ViewModel` (Phase 1). Pins the current state machine:
 * one whole-test countdown, edit-before-submit REVIEW phase, `editResponse`.
 */
class SRTTestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var testContentRepository: com.ssbmax.shared.domain.repository.TestContentRepository
    private lateinit var testSessionRepository: FakeTestSessionRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var submissionRepository: FakeSubmissionRepository
    private lateinit var usageRecorder: FakeTestUsageRecorder
    private lateinit var analysisTrigger: FakeSubmissionAnalysisTrigger
    private lateinit var analyticsTracker: RecordingAnalyticsTracker

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
        val base = FakeTestContentRepository()
        testContentRepository = object : com.ssbmax.shared.domain.repository.TestContentRepository by base {
            override suspend fun getSRTQuestions(testId: String) = Result.success(situations())
        }
        testSessionRepository = FakeTestSessionRepository()
        userProfileRepository = FakeUserProfileRepository()
        submissionRepository = FakeSubmissionRepository()
        usageRecorder = FakeTestUsageRecorder()
        analysisTrigger = FakeSubmissionAnalysisTrigger()
        analyticsTracker = RecordingAnalyticsTracker()
        // "SRT Tests" is limit 0 on FREE (SubscriptionLimits) -- default to PRO so
        // tests are eligible unless a test explicitly overrides to exercise LimitReached.
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun situations() = listOf(
        SRTSituation(id = "s1", situation = "You see a fire.", sequenceNumber = 1, category = SRTCategory.CRISIS_MANAGEMENT),
        SRTSituation(id = "s2", situation = "Your team disagrees.", sequenceNumber = 2, category = SRTCategory.TEAMWORK)
    )

    private fun buildViewModel() = SRTTestViewModel(
        testContentRepository = testContentRepository,
        testSessionRepository = testSessionRepository,
        submitSRTTest = SubmitSRTTestUseCase(submissionRepository),
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        checkTestEligibility = CheckTestEligibilityUseCase(subscriptionRepository, RecordingAnalyticsTracker()),
        userProfileRepository = userProfileRepository,
        usageRecorder = usageRecorder,
        analysisTrigger = analysisTrigger,
        observability = ObservabilitySeam(NoOpLogger(), analyticsTracker)
    )

    @Test
    fun `unauthenticated access is blocked`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()

        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Authentication required. Please login to continue.", viewModel.uiState.value.error)
        assertEquals(listOf(com.ssbmax.shared.domain.util.SecurityEvents.UNAUTHENTICATED_ACCESS), analyticsTracker.events.map { it.name })
    }

    @Test
    fun `limit reached surfaces subscription details`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        subscriptionRepository.monthlyUsageResult =
            Result.success(mapOf("SRT Tests" to UsageInfo(used = 1, limit = 1)))
        val viewModel = buildViewModel()

        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLimitReached)
    }

    @Test
    fun `loads test into INSTRUCTIONS phase with situations`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.situations.size)
        assertEquals(SRTPhase.INSTRUCTIONS, state.phase)
    }

    @Test
    fun `startTest enters IN_PROGRESS and starts the countdown`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SRTPhase.IN_PROGRESS, state.phase)
        assertTrue(state.isTimerActive)
        // SRT's countdown timer uses a real-wall-clock endTime (see SRTTestViewModel.startTimer),
        // not a virtual-time-friendly counter, and nothing in this VM ever cancels that timerJob
        // except onCleared()/submitTest(). runTest()'s own end-of-test cleanup drains the shared
        // test scheduler to idle regardless of what this test body calls explicitly, which would
        // spin trying to catch that job up to real elapsed time. Now that SRTTestViewModel is a
        // real androidx.lifecycle.ViewModel there's no public close() to cancel the scope with --
        // clearForTest() (see ViewModelTestUtils.kt) forces onCleared() via a throwaway
        // ViewModelStore so cleanup is instant.
        viewModel.clearForTest()
    }

    @Test
    fun `moveToNext on the last situation enters REVIEW`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()

        viewModel.updateResponse("Raise the alarm and evacuate.")
        viewModel.moveToNext()
        viewModel.moveToNext()

        val state = viewModel.uiState.value
        assertEquals(SRTPhase.REVIEW, state.phase)
        assertEquals(2, state.responses.size)
        viewModel.clearForTest() // see the timer note in the previous test
    }

    @Test
    fun `editResponse jumps back into IN_PROGRESS with the prior answer`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.updateResponse("Evacuate calmly.")
        viewModel.moveToNext()

        viewModel.editResponse(0)

        val state = viewModel.uiState.value
        assertEquals(SRTPhase.IN_PROGRESS, state.phase)
        assertEquals("Evacuate calmly.", state.currentResponse)
        viewModel.clearForTest() // see the timer note in the first test
    }

    @Test
    fun `submitTest builds a submission and triggers analysis`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadTest()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()

        viewModel.submitTest()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitted)
        assertNotNull(state.submissionId)
        assertEquals(1, analysisTrigger.triggered.size)
        viewModel.clearForTest() // see the timer note on the first startTest() test
    }
}
