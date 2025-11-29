package com.ssbmax.ui.interview.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for VoiceInterviewSessionViewModel
 *
 * Note: Some functionality like startRecording relies on internal VoiceRecordingHelper
 * which is created inside the ViewModel. These tests focus on observable behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInterviewSessionViewModelTest : BaseViewModelTest() {

    private lateinit var interviewRepository: InterviewRepository
    private lateinit var aiService: AIService
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testSessionId = "test-session-123"
    private val testUserId = "user-123"

    private val testSession = InterviewSession(
        id = testSessionId,
        userId = testUserId,
        mode = InterviewMode.VOICE_BASED,
        status = InterviewStatus.IN_PROGRESS,
        startedAt = Instant.now(),
        completedAt = null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 0,
        estimatedDuration = 30
    )

    private val testQuestion = InterviewQuestion(
        id = "q1",
        questionText = "Tell me about yourself",
        expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
        context = null,
        source = QuestionSource.PIQ_BASED
    )

    @Before
    fun setUp() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        
        // Mock Looper for Handler usage in VoiceRecordingHelper
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper
        
        // Mock Handler constructor
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().postDelayed(any(), any()) } returns true
        every { anyConstructed<Handler>().removeCallbacks(any()) } returns Unit

        interviewRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to testSessionId))

        every { context.getString(any()) } returns "Mocked String"
        every { context.applicationContext } returns context
    }

    private fun createViewModel(): VoiceInterviewSessionViewModel {
        return VoiceInterviewSessionViewModel(
            interviewRepository = interviewRepository,
            aiService = aiService,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `loadSession loads session and first question`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testSession, state.session)
        assertEquals(testQuestion, state.currentQuestion)
        assertNull(state.error)
    }

    @Test
    fun `loadSession handles session not found error`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns 
            Result.failure(Exception("Session not found"))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Mocked String", state.error) // Uses mocked getString
    }

    @Test
    fun `initial state has correct defaults`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(RecordingState.IDLE, state.recordingState)
        assertEquals(TranscriptionState.IDLE, state.transcriptionState)
        assertEquals("", state.liveTranscription)
        assertEquals("", state.finalTranscription)
    }

    // ==========================================================================
    // EXIT FLOW TESTS - Contract verification tests
    // 
    // Note: Full exit flow tests with stopAll() require instrumented tests 
    // because they involve Android SDK components (TextToSpeech, SpeechRecognizer).
    // These tests verify the ViewModel's contracts without calling Android SDK.
    // See: app/src/androidTest for instrumented tests.
    // ==========================================================================

    @Test
    fun `canStartRecording returns false when TTS is speaking`() = runTest {
        // Given - State where TTS is speaking
        val state = VoiceInterviewSessionUiState(
            hasRecordPermission = true,
            isTTSReady = true,
            isTTSSpeaking = true, // TTS is actively speaking
            recordingState = RecordingState.IDLE
        )

        // Then - Should not allow starting recording while TTS speaks
        assertFalse("canStartRecording should be false when TTS is speaking", state.canStartRecording())
    }

    @Test
    fun `canStartRecording returns true when conditions are met`() = runTest {
        // Given - State ready for recording (all conditions met)
        val state = VoiceInterviewSessionUiState(
            isLoading = false, // Not loading
            hasRecordPermission = true,
            isTTSReady = true,
            isTTSSpeaking = false, // TTS not speaking
            recordingState = RecordingState.IDLE,
            currentQuestion = testQuestion // Question loaded
        )

        // Then - Should allow starting recording
        assert(state.canStartRecording()) { "canStartRecording should be true when ready" }
    }

    @Test
    fun `recording state transitions are valid`() = runTest {
        // Verify valid state transitions
        assertEquals("IDLE is default state", RecordingState.IDLE, VoiceInterviewSessionUiState().recordingState)
        
        // State with recording in progress
        val recordingState = VoiceInterviewSessionUiState(
            recordingState = RecordingState.RECORDING,
            hasRecordPermission = true
        )
        assertFalse("Cannot start new recording while recording", recordingState.canStartRecording())
    }

    @Test
    fun `initial UI state has safe defaults for exit`() = runTest {
        // Given - Fresh UI state
        val state = VoiceInterviewSessionUiState()

        // Then - Should have safe defaults
        assertFalse("TTS should not be speaking initially", state.isTTSSpeaking)
        assertEquals("Recording should be IDLE initially", RecordingState.IDLE, state.recordingState)
        assertEquals("Transcription should be IDLE initially", TranscriptionState.IDLE, state.transcriptionState)
        assertFalse("Should not be completed initially", state.isCompleted)
    }

    @Test
    fun `clearError updates state correctly`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - clearError is called
        viewModel.clearError()
        advanceUntilIdle()

        // Then - Error should be cleared
        val state = viewModel.uiState.value
        assertNull("Error should be null after clearError", state.error)
    }

    // ==========================================================================
    // EXIT FUNCTIONALITY TESTS
    // 
    // Note: stopAll() calls Android SDK components (TextToSpeech, SpeechRecognizer)
    // which require instrumented tests. These unit tests verify observable behavior.
    // 
    // Exit flow is now handled by navigation with popUpTo (see SharedNavGraph.kt):
    // - onNavigateBack navigates to topic/INTERVIEW?selectedTab=2 with popUpTo
    // - This synchronously removes the interview session from back stack
    // - ViewModel is properly cleared via onCleared()
    // ==========================================================================

    @Test
    fun `ViewModel loads session on creation`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When - Create ViewModel
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then - Session should be loaded
        val state = viewModel.uiState.value
        assertEquals("Session should be loaded", testSession, state.session)
        assertEquals("Question should be loaded", testQuestion, state.currentQuestion)
    }
}
