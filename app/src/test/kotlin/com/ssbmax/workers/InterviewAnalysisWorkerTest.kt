package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.InterviewAnalysisOrchestrator
import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.repository.InterviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

/**
 * Phase 8 (KMP-convergence plan): rewritten for the worker's shell conversion.
 * `InterviewAnalysisWorker` no longer analyzes responses or aggregates scores itself --
 * that flow now lives in [InterviewAnalysisOrchestrator], covered by
 * `shared/commonTest/.../analysis/InterviewAnalysisOrchestratorTest.kt`. This file now
 * covers only the shell's own job: delegate, then map the session's persisted before/after
 * status to a WorkManager [ListenableWorker.Result] and notification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewAnalysisWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var orchestrator: InterviewAnalysisOrchestrator
    private lateinit var notificationHelper: NotificationHelper

    private val testSessionId = "test-session-123"
    private val testUserId = "user-123"
    private val testResultId = "result-123"

    private fun session(status: InterviewStatus) = InterviewSession(
        id = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        status = status,
        startedAt = Clock.System.now() - 1800.seconds,
        completedAt = if (status == InterviewStatus.COMPLETED) Clock.System.now() else null,
        piqSnapshotId = "piq-123",
        consentGiven = true,
        questionIds = listOf("q1", "q2"),
        currentQuestionIndex = 2,
        estimatedDuration = 30
    )

    private val testInterviewResult = InterviewResult(
        id = testResultId,
        sessionId = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        completedAt = Clock.System.now(),
        durationSec = 1800,
        totalQuestions = 2,
        totalResponses = 2,
        overallOLQScores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(5, 80, "Aggregated")),
        categoryScores = mapOf(OLQCategory.INTELLECTUAL to 5f),
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
        orchestrator = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        every { workerParams.inputData } returns workDataOf(
            InterviewAnalysisWorker.KEY_SESSION_ID to testSessionId
        )
        every { workerParams.runAttemptCount } returns 0

        startKoin {
            modules(module {
                single { interviewRepository }
                single { orchestrator }
                single { notificationHelper }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun createWorker() = InterviewAnalysisWorker(context, workerParams)

    @Test
    fun `doWork delegates to the orchestrator and returns success with a notification when it completes`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returnsMany listOf(
            Result.success(session(InterviewStatus.PENDING_ANALYSIS)),
            Result.success(session(InterviewStatus.COMPLETED))
        )
        coEvery { orchestrator.analyze(testSessionId) } returns Unit
        coEvery { interviewRepository.getLatestResult(testUserId) } returns Result.success(testInterviewResult)

        val result = createWorker().doWork()

        coVerify(exactly = 1) { orchestrator.analyze(testSessionId) }
        coVerify(exactly = 1) { notificationHelper.showInterviewResultsReadyNotification(testSessionId, testResultId) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns failure with a failure notification when the orchestrator leaves the session non-COMPLETED`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returnsMany listOf(
            Result.success(session(InterviewStatus.PENDING_ANALYSIS)),
            Result.success(session(InterviewStatus.FAILED))
        )
        coEvery { orchestrator.analyze(testSessionId) } returns Unit

        val result = createWorker().doWork()

        coVerify(exactly = 1) { notificationHelper.showInterviewAnalysisFailedNotification(testSessionId) }
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork skips delegating to the orchestrator when status is not PENDING_ANALYSIS`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.success(session(InterviewStatus.COMPLETED))

        val result = createWorker().doWork()

        coVerify(exactly = 0) { orchestrator.analyze(any()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns failure when sessionId missing`() = runTest {
        every { workerParams.inputData } returns workDataOf()

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when session not found`() = runTest {
        coEvery { interviewRepository.getSession(testSessionId) } returns Result.failure(Exception("Not found"))
        every { workerParams.runAttemptCount } returns 3

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 0) { orchestrator.analyze(any()) }
    }
}
