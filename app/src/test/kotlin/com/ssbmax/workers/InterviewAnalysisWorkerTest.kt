package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.ResponseAnalysis
import com.ssbmax.notifications.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for InterviewAnalysisWorker
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewAnalysisWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var aiService: AIService
    private lateinit var notificationHelper: NotificationHelper

    private val testSessionId = "test-session-123"
    private val testUserId = "user-123"
    private val testResultId = "result-123"

    private val testSession = InterviewSession(
        id = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        status = InterviewStatus.PENDING_ANALYSIS,
        startedAt = Instant.now().minusSeconds(1800),
        completedAt = null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 2,
        estimatedDuration = 30
    )

    private val testResponses = listOf(
        InterviewResponse(
            id = "resp-1",
            sessionId = testSessionId,
            questionId = "q1",
            responseText = "My answer 1",
            responseMode = InterviewMode.TEXT_BASED,
            respondedAt = Instant.now().minusSeconds(1200),
            thinkingTimeSec = 30,
            audioUrl = null,
            olqScores = emptyMap(),
            confidenceScore = 0
        )
    )

    private val testAnalysisResult: ResponseAnalysis = mockk(relaxed = true)

    private val testInterviewResult = InterviewResult(
        id = testResultId,
        sessionId = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        completedAt = Instant.now(),
        durationSec = 1800,
        totalQuestions = 2,
        totalResponses = 2,
        overallOLQScores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(5, 80, "Aggregated"),
            OLQ.DETERMINATION to OLQScore(4, 75, "Aggregated")
        ),
        categoryScores = mapOf(
            OLQCategory.INTELLECTUAL to 5f,
            OLQCategory.SOCIAL to 5f
        ),
        overallConfidence = 78,
        strengths = listOf(OLQ.DETERMINATION),
        weaknesses = listOf(OLQ.POWER_OF_EXPRESSION),
        feedback = "Good performance",
        overallRating = 5
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        interviewRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        every { workerParams.inputData } returns workDataOf(
            InterviewAnalysisWorker.KEY_SESSION_ID to testSessionId
        )
        every { workerParams.runAttemptCount } returns 0
    }

    private fun createWorker(): InterviewAnalysisWorker {
        return InterviewAnalysisWorker(
            context = context,
            params = workerParams,
            interviewRepository = interviewRepository,
            aiService = aiService,
            notificationHelper = notificationHelper
        )
    }

    @Test
    fun `worker fetches session and responses`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getResponses(testSessionId) } returns Result.success(testResponses)
        coEvery { aiService.analyzeResponse(any(), any(), any()) } returns Result.success(testAnalysisResult)
        coEvery { interviewRepository.updateResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.completeInterview(testSessionId) } returns Result.success(testInterviewResult)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val worker = createWorker()
        val result = worker.doWork()

        coVerify { interviewRepository.getSession(testSessionId) }
        coVerify { interviewRepository.getResponses(testSessionId) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker sends success notification`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(testSession)
        coEvery { interviewRepository.getResponses(testSessionId) } returns Result.success(testResponses)
        coEvery { aiService.analyzeResponse(any(), any(), any()) } returns Result.success(testAnalysisResult)
        coEvery { interviewRepository.updateResponse(any()) } returns Result.success(mockk())
        coEvery { interviewRepository.completeInterview(testSessionId) } returns Result.success(testInterviewResult)
        coEvery { interviewRepository.updateSession(any()) } returns Result.success(Unit)

        val worker = createWorker()
        worker.doWork()

        verify { notificationHelper.showInterviewResultsReadyNotification(testSessionId, testResultId) }
    }

    @Test
    fun `worker returns failure when sessionId missing`() = runTest {
        every { workerParams.inputData } returns workDataOf()

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `worker returns failure when session not found`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.failure(Exception("Not found"))
        every { workerParams.runAttemptCount } returns 3

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
