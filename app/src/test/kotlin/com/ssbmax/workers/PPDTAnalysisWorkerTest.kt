package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.shared.analysis.PPDTAnalysisOrchestrator
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Phase 8 (KMP-convergence plan): rewritten for the worker's shell conversion.
 * `PPDTAnalysisWorker` no longer builds prompts or calls `AIService` itself -- that flow
 * (including the gender-passing/multimodal-fallback behavior this file used to assert
 * directly) now lives in [PPDTAnalysisOrchestrator], covered by
 * `shared/commonTest/.../analysis/PPDTAnalysisOrchestratorTest.kt`. This file now covers
 * only the shell's own job: delegate, then map the submission's persisted before/after
 * status to a WorkManager [ListenableWorker.Result] and notification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTAnalysisWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var orchestrator: PPDTAnalysisOrchestrator
    private lateinit var notificationHelper: NotificationHelper

    private val testSubmissionId = "test-submission-123"

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
        submissionRepository = mockk(relaxed = true)
        orchestrator = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        every { workerParams.inputData } returns workDataOf(
            PPDTAnalysisWorker.KEY_SUBMISSION_ID to testSubmissionId
        )
        every { workerParams.runAttemptCount } returns 0

        startKoin {
            modules(module {
                single { submissionRepository }
                single { orchestrator }
                single { notificationHelper }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun buildFakeSubmission(analysisStatus: AnalysisStatus) = PPDTSubmission(
        submissionId = testSubmissionId,
        questionId = "ppdt-question-1",
        userId = "test-user-456",
        userName = "Test User",
        userEmail = "test@example.com",
        batchId = null,
        story = "A brave officer led the team across the flooded river to safety.",
        charactersCount = 64,
        viewingTimeTakenSeconds = 30,
        writingTimeTakenMinutes = 4,
        submittedAt = System.currentTimeMillis(),
        status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
        instructorReview = null,
        analysisStatus = analysisStatus
    )

    private fun createWorker() = PPDTAnalysisWorker(context, workerParams)

    @Test
    fun `doWork returns failure when submission not found`() = runTest {
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(null)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 0) { orchestrator.analyze(any()) }
    }

    @Test
    fun `doWork skips delegating to the orchestrator when status is not PENDING_ANALYSIS`() = runTest {
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(
            buildFakeSubmission(AnalysisStatus.COMPLETED)
        )

        val result = createWorker().doWork()

        coVerify(exactly = 0) { orchestrator.analyze(any()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork delegates to the orchestrator and returns success with a notification when it completes`() = runTest {
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returnsMany listOf(
            Result.success(buildFakeSubmission(AnalysisStatus.PENDING_ANALYSIS)),
            Result.success(buildFakeSubmission(AnalysisStatus.COMPLETED))
        )
        coEvery { orchestrator.analyze(testSubmissionId) } returns Unit

        val result = createWorker().doWork()

        coVerify(exactly = 1) { orchestrator.analyze(testSubmissionId) }
        coVerify(exactly = 1) { notificationHelper.showPPDTResultsReadyNotification(testSubmissionId) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns failure with a failure notification when the orchestrator leaves the submission non-COMPLETED`() = runTest {
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returnsMany listOf(
            Result.success(buildFakeSubmission(AnalysisStatus.PENDING_ANALYSIS)),
            Result.success(buildFakeSubmission(AnalysisStatus.FAILED))
        )
        coEvery { orchestrator.analyze(testSubmissionId) } returns Unit

        val result = createWorker().doWork()

        coVerify(exactly = 1) { notificationHelper.showPPDTAnalysisFailedNotification(testSubmissionId) }
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork retries on an unexpected exception, then fails after max attempts`() = runTest {
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } throws RuntimeException("Firestore down")
        every { workerParams.runAttemptCount } returns 3

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
