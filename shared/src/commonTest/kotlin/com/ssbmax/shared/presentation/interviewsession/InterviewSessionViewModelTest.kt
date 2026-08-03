@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.QuestionSource
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeInterviewRepository
import com.ssbmax.shared.presentation.testing.FakeSubmissionAnalysisTrigger
import com.ssbmax.shared.presentation.testing.FakeTTSService
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Characterization test for [InterviewSessionViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current session-load -> speak -> submit-response -> complete state machine.
 */
class InterviewSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var interviewRepository: FakeInterviewRepository
    private lateinit var ttsService: FakeTTSService
    private lateinit var analysisTrigger: FakeSubmissionAnalysisTrigger

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        interviewRepository = FakeInterviewRepository().apply {
            getSessionResult = Result.success(session())
            getQuestionResult = Result.success(question())
        }
        ttsService = FakeTTSService()
        analysisTrigger = FakeSubmissionAnalysisTrigger()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(index: Int = 0) = InterviewSession(
        id = "session-1",
        userId = testUser().id,
        mode = InterviewMode.VOICE_BASED,
        status = InterviewStatus.IN_PROGRESS,
        startedAt = Instant.fromEpochMilliseconds(0),
        piqSnapshotId = "piq-1",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = index
    )

    private fun question(id: String = "q1") = InterviewQuestion(
        id = id,
        questionText = "Tell me about a time you showed leadership.",
        expectedOLQs = emptyList(),
        source = QuestionSource.GENERIC_POOL
    )

    private fun buildViewModel() = InterviewSessionViewModel(
        interviewRepository = interviewRepository,
        ttsService = ttsService,
        analysisTrigger = analysisTrigger,
        logger = NoOpLogger()
    )

    @Test
    fun `loadSession loads the session and speaks the first question`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.currentQuestion)
        assertEquals(2, state.totalQuestions)
        assertTrue(ttsService.spoken.contains(question().questionText))
    }

    @Test
    fun `submitResponse is a no-op when the response text is blank`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitResponse()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.pendingResponses.size)
        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
    }

    @Test
    fun `submitResponse advances to the next question when more remain`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateResponseText("I organized my team to finish the task.")

        viewModel.submitResponse()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pendingResponses.size)
        assertEquals("", state.responseText)
        assertEquals(false, state.isSubmittingResponse)
    }

    @Test
    fun `submitResponse on the last question completes the interview`() = runTest(testDispatcher) {
        interviewRepository.getSessionResult = Result.success(session(index = 1))
        val viewModel = buildViewModel()
        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateResponseText("I took charge of the situation calmly.")

        viewModel.submitResponse()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertTrue(state.isResultPending)
        assertEquals("session-1", state.resultId)
        assertEquals(1, analysisTrigger.triggered.size)
        assertEquals(1, interviewRepository.submittedResponses.size)
    }

    @Test
    fun `toggleTTSMute mutes and unmutes speech`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()
        val spokenBeforeToggle = ttsService.spoken.size

        viewModel.toggleTTSMute()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isTTSMuted)
        assertEquals(1, ttsService.stopCalls)

        viewModel.toggleTTSMute()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isTTSMuted)
        assertTrue(ttsService.spoken.size > spokenBeforeToggle)
    }

    @Test
    fun `stopAll marks exiting and stops TTS`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.loadSession("session-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.stopAll()

        assertEquals(1, ttsService.stopCalls)
    }
}
