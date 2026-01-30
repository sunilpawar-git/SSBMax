package com.ssbmax.ui.interview.session

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.InterviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SessionManager
 *
 * Tests session loading, question navigation, and state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private lateinit var interviewRepository: InterviewRepository
    private val testDispatcher = StandardTestDispatcher()

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
        questionIds = listOf("q1", "q2", "q3"),
        currentQuestionIndex = 0,
        estimatedDuration = 30
    )

    private val testQuestion1 = InterviewQuestion(
        id = "q1",
        questionText = "Tell me about yourself",
        expectedOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
        source = QuestionSource.GENERIC_POOL
    )

    private val testQuestion2 = InterviewQuestion(
        id = "q2",
        questionText = "Why do you want to join the armed forces?",
        expectedOLQs = listOf(OLQ.INITIATIVE, OLQ.DETERMINATION),
        source = QuestionSource.GENERIC_POOL
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        interviewRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Load Session Tests =====

    @Test
    fun `loadSession returns session and first question on success`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion1)

        val manager = SessionManager(interviewRepository)
        val result = manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(testSession, manager.session.value)
        assertEquals(testQuestion1, manager.currentQuestion.value)
        assertEquals(0, manager.currentIndex.value)
        assertEquals(3, manager.totalQuestions)
    }

    @Test
    fun `loadSession returns failure for missing session`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.failure(Exception("Session not found"))

        val manager = SessionManager(interviewRepository)
        val result = manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertTrue(result.isFailure)
        assertNull(manager.session.value)
    }

    @Test
    fun `loadSession returns failure on repository error`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.failure(Exception("Network error"))

        val manager = SessionManager(interviewRepository)
        val result = manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertTrue(result.isFailure)
        assertNull(manager.session.value)
    }

    @Test
    fun `loadSession handles empty question IDs`() = runTest {
        // This should not happen due to InterviewSession validation, but test defensive handling
        val sessionWithEmptyQuestions = testSession.copy(questionIds = listOf("q1")) // At least 1 required
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(sessionWithEmptyQuestions)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.failure(Exception("Not found"))

        val manager = SessionManager(interviewRepository)
        val result = manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertTrue(result.isFailure)
    }

    // ===== Load Next Question Tests =====

    @Test
    fun `loadNextQuestion advances index and returns question`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion1)
        coEvery { interviewRepository.getQuestion("q2") } returns Result.success(testQuestion2)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val manager = SessionManager(interviewRepository)
        manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertEquals(0, manager.currentIndex.value)

        val result = manager.loadNextQuestion()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(testQuestion2, result.getOrNull())
        assertEquals(1, manager.currentIndex.value)
        coVerify { interviewRepository.updateSession(any()) }
    }

    @Test
    fun `loadNextQuestion returns null at end of questions`() = runTest {
        val sessionAtEnd = testSession.copy(currentQuestionIndex = 2) // Last question
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(sessionAtEnd)
        coEvery { interviewRepository.getQuestion("q3") } returns Result.success(testQuestion1)

        val manager = SessionManager(interviewRepository)
        manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertEquals(2, manager.currentIndex.value)
        assertFalse(manager.hasMoreQuestions())

        val result = manager.loadNextQuestion()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `loadNextQuestion updates session in repository`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion1)
        coEvery { interviewRepository.getQuestion("q2") } returns Result.success(testQuestion2)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val manager = SessionManager(interviewRepository)
        manager.loadSession(testSessionId)
        advanceUntilIdle()

        manager.loadNextQuestion()
        advanceUntilIdle()

        coVerify {
            interviewRepository.updateSession(match { it.currentQuestionIndex == 1 })
        }
    }

    // ===== Has More Questions Tests =====

    @Test
    fun `hasMoreQuestions returns true when questions remain`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getQuestion("q1") } returns Result.success(testQuestion1)

        val manager = SessionManager(interviewRepository)
        manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertTrue(manager.hasMoreQuestions()) // 3 questions, at index 0
    }

    @Test
    fun `hasMoreQuestions returns false at last question`() = runTest {
        val sessionAtEnd = testSession.copy(currentQuestionIndex = 2)
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(sessionAtEnd)
        coEvery { interviewRepository.getQuestion("q3") } returns Result.success(testQuestion1)

        val manager = SessionManager(interviewRepository)
        manager.loadSession(testSessionId)
        advanceUntilIdle()

        assertFalse(manager.hasMoreQuestions()) // 3 questions, at index 2 (last)
    }

    // ===== State Tests =====

    @Test
    fun `session state is null initially`() = runTest {
        val manager = SessionManager(interviewRepository)
        assertNull(manager.session.value)
        assertNull(manager.currentQuestion.value)
        assertEquals(0, manager.currentIndex.value)
        assertEquals(0, manager.totalQuestions)
    }
}
