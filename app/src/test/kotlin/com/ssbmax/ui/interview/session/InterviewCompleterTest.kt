package com.ssbmax.ui.interview.session

import androidx.work.WorkManager
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.repository.InterviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for InterviewCompleter
 *
 * Tests interview completion, response saving, and WorkManager scheduling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewCompleterTest {

    private lateinit var interviewRepository: InterviewRepository
    private lateinit var workManager: WorkManager
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
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 1,
        estimatedDuration = 30
    )

    private val testPendingResponses = listOf(
        PendingResponse(
            questionId = "q1",
            questionText = "Tell me about yourself",
            responseText = "I am a dedicated individual...",
            thinkingTimeSec = 30
        ),
        PendingResponse(
            questionId = "q2",
            questionText = "Why do you want to join?",
            responseText = "I want to serve my country...",
            thinkingTimeSec = 25
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        interviewRepository = mockk()
        workManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Complete Tests =====

    @Test
    fun `complete saves all responses to repository`() = runTest {
        coEvery { interviewRepository.submitResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        val result = completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = testPendingResponses,
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { interviewRepository.submitResponse(any()) }
    }

    @Test
    fun `complete updates session status to PENDING_ANALYSIS`() = runTest {
        coEvery { interviewRepository.submitResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = testPendingResponses,
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        coVerify {
            interviewRepository.updateSession(match { it.status == InterviewStatus.PENDING_ANALYSIS })
        }
    }

    @Test
    fun `complete enqueues WorkManager task`() = runTest {
        coEvery { interviewRepository.submitResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = testPendingResponses,
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        verify { workManager.enqueueUniqueWork(any(), any(), any<androidx.work.OneTimeWorkRequest>()) }
    }

    @Test
    fun `complete returns sessionId on success`() = runTest {
        coEvery { interviewRepository.submitResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        val result = completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = testPendingResponses,
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(testSessionId, result.getOrNull())
    }

    @Test
    fun `complete handles save failure gracefully`() = runTest {
        // First response fails, second succeeds
        coEvery { interviewRepository.submitResponse(any()) } returns Result.failure(Exception("Network error")) andThen Result.success(mockk())
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        val result = completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = testPendingResponses,
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        // Should still complete (best-effort save)
        assertTrue(result.isSuccess)
        // Both responses attempted
        coVerify(exactly = 2) { interviewRepository.submitResponse(any()) }
    }

    @Test
    fun `complete with empty responses still completes`() = runTest {
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val completer = InterviewCompleter(interviewRepository, workManager)
        val result = completer.complete(
            sessionId = testSessionId,
            session = testSession,
            pendingResponses = emptyList(),
            mode = InterviewMode.VOICE_BASED
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { interviewRepository.submitResponse(any()) }
        coVerify { interviewRepository.updateSession(any()) }
    }
}
