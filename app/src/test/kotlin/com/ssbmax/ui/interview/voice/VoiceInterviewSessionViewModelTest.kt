package com.ssbmax.ui.interview.voice

import android.content.Context
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
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

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
}
