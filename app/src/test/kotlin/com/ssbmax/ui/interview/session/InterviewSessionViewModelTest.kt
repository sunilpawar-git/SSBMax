package com.ssbmax.ui.interview.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for InterviewSessionViewModel
 *
 * Tests cover:
 * - Session loading
 * - Response submission with local storage
 * - Question progression
 * - Interview completion with background analysis
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewSessionViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: InterviewSessionViewModel
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var workManager: WorkManager
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testSessionId = "test-session-123"
    private val testUserId = "user-123"
    private val testQuestionIds = listOf("q1", "q2", "q3")

    private val testSession = InterviewSession(
        id = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        status = InterviewStatus.IN_PROGRESS,
        startedAt = Instant.now(),
        completedAt = null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = testQuestionIds,
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
        interviewRepository = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to testSessionId))

        // Mock context string resources
        every { context.getString(R.string.interview_loading_session) } returns "Loading..."
        every { context.getString(R.string.interview_error_load_session) } returns "Failed to load"
        every { context.getString(R.string.interview_error_session_not_found) } returns "Not found"
        every { context.getString(R.string.interview_error_no_questions) } returns "No questions"
        every { context.getString(R.string.interview_error_load_question) } returns "Failed to load question"
        every { context.getString(R.string.interview_error_generic) } returns "Error occurred"
        every { context.getString(R.string.interview_submitting_answers) } returns "Submitting..."
        every { context.getString(R.string.interview_error_load_next_question) } returns "Failed to load next"
    }

    private fun createViewModel() {
        viewModel = InterviewSessionViewModel(
            interviewRepository = interviewRepository,
            workManager = workManager,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    // ============================================
    // LOADING TESTS
    // ============================================

    @Test
    fun `loadSession updates UI state correctly on success`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testSession, state.session)
        assertEquals(testQuestion, state.currentQuestion)
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(3, state.totalQuestions)
        assertNull(state.error)
    }

    @Test
    fun `loadSession handles session not found error`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.failure(Exception("Not found"))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Failed to load", state.error)
    }

    @Test
    fun `loadSession handles question fetch failure`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns
                Result.failure(Exception("Question error"))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Failed to load question", state.error)
    }

    @Test
    fun `loadSession handles session with invalid currentQuestionIndex`() = runTest {
        // Given - session where currentQuestionIndex points beyond available questions
        val sessionPastEnd = testSession.copy(
            currentQuestionIndex = 10 // Beyond the 3 questions available
        )
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.success(sessionPastEnd)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then - should show "no questions" error since index is out of bounds
        val state = viewModel.uiState.value
        assertEquals("No questions", state.error)
    }

    // ============================================
    // RESPONSE SUBMISSION TESTS
    // ============================================

    @Test
    fun `updateResponse updates UI state`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateResponse("My answer")

        // Then
        assertEquals("My answer", viewModel.uiState.value.responseText)
    }

    @Test
    fun `submitResponse stores response locally in pending list`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion(any()) } returns Result.success(testQuestion)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)
        createViewModel()
        advanceUntilIdle()
        viewModel.updateResponse("Test answer")

        // When
        viewModel.submitResponse()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.pendingResponses.size)
        assertEquals("Test answer", state.pendingResponses[0].responseText)
    }

    @Test
    fun `submitResponse advances to next question`() = runTest {
        // Given
        val question2 = testQuestion.copy(id = "q2", questionText = "Question 2")
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        coEvery { interviewRepository.getQuestion("q2") } returns Result.success(question2)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)
        createViewModel()
        advanceUntilIdle()
        viewModel.updateResponse("Answer 1")

        // When
        viewModel.submitResponse()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.currentQuestionIndex)
        assertEquals("Question 2", state.currentQuestion?.questionText)
        assertEquals("", state.responseText) // Reset for new question
    }

    @Test
    fun `submitResponse triggers completeInterview on last question`() = runTest {
        // Given
        val lastQuestionSession = testSession.copy(
            questionIds = listOf("q1"),
            currentQuestionIndex = 0
        )
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.success(lastQuestionSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        coEvery { interviewRepository.submitResponse(any()) } returns
                Result.success(mockk(relaxed = true))
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()
        viewModel.updateResponse("Final answer")

        // When
        viewModel.submitResponse()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertTrue(state.isResultPending)
    }

    // ============================================
    // COMPLETION TESTS
    // ============================================

    @Test
    fun `completeInterview saves all responses to Firestore`() = runTest {
        // Given
        val lastQuestionSession = testSession.copy(
            questionIds = listOf("q1"),
            currentQuestionIndex = 0
        )
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.success(lastQuestionSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        coEvery { interviewRepository.submitResponse(any()) } returns
                Result.success(mockk(relaxed = true))
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()
        viewModel.updateResponse("Answer")

        // When
        viewModel.submitResponse()
        advanceUntilIdle()

        // Then
        coVerify { interviewRepository.submitResponse(any()) }
    }

    @Test
    fun `completeInterview sets isResultPending flag`() = runTest {
        // Given
        val lastQuestionSession = testSession.copy(
            questionIds = listOf("q1"),
            currentQuestionIndex = 0
        )
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.success(lastQuestionSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion)
        coEvery { interviewRepository.submitResponse(any()) } returns
                Result.success(mockk(relaxed = true))
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()
        viewModel.updateResponse("Answer")

        // When
        viewModel.submitResponse()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isResultPending)
        assertTrue(state.isCompleted)
        assertNull(state.resultId) // No result yet - background processing
    }

    // ============================================
    // ERROR HANDLING TESTS
    // ============================================

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        coEvery { interviewRepository.getSession(testSessionId) } returns
                Result.failure(Exception("Error"))
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }
}

