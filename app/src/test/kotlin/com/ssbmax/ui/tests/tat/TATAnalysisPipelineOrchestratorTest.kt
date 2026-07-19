package com.ssbmax.ui.tests.tat

import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATStoryResponse
import com.ssbmax.shared.domain.repository.SubmissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TATAnalysisPipelineOrchestratorTest {

    private lateinit var orchestrator: TATAnalysisPipelineOrchestrator
    private lateinit var workManager: WorkManager
    private lateinit var workContinuation: WorkContinuation
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var workPlanner: TATAnalysisWorkPlanner

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.wtf(any<String>(), any<String>()) } returns 0

        workManager = mockk(relaxed = true)
        workContinuation = mockk(relaxed = true)
        every { workManager.beginWith(any<List<OneTimeWorkRequest>>()) } returns workContinuation
        every { workContinuation.then(any<List<OneTimeWorkRequest>>()) } returns workContinuation
        every { workContinuation.then(any<OneTimeWorkRequest>()) } returns workContinuation
        every { workContinuation.enqueue() } returns mockk(relaxed = true)

        submissionRepository = mockk(relaxed = true)
        coEvery { submissionRepository.updateTATAnalysisStatus(any(), any()) } returns Result.success(Unit)

        workPlanner = TATAnalysisWorkPlanner()

        orchestrator = TATAnalysisPipelineOrchestrator(submissionRepository, workManager, workPlanner)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `sets analysis status to analyzing before enqueuing work`() = runTest {
        val callOrder = mutableListOf<String>()
        coEvery { submissionRepository.updateTATAnalysisStatus(any(), any()) } answers {
            callOrder.add("status-update")
            Result.success(Unit)
        }
        every { workManager.beginWith(any<List<OneTimeWorkRequest>>()) } answers {
            callOrder.add("work-begin")
            workContinuation
        }

        val result = orchestrator.startPipeline("sub-001", buildStories(), buildQuestions())

        assertTrue(result.isSuccess)
        assertTrue(
            "Status update must happen before work is enqueued",
            callOrder.indexOf("status-update") < callOrder.indexOf("work-begin")
        )
    }

    @Test
    fun `returns failure when status update fails`() = runTest {
        coEvery { submissionRepository.updateTATAnalysisStatus(any(), any()) } returns
            Result.failure(RuntimeException("Firestore unavailable"))

        val result = orchestrator.startPipeline("sub-002", buildStories(), buildQuestions())

        assertTrue(result.isFailure)
        verify(exactly = 0) { workManager.beginWith(any<List<OneTimeWorkRequest>>()) }
    }

    @Test
    fun `returns failure when work enqueue fails`() = runTest {
        every { workManager.beginWith(any<List<OneTimeWorkRequest>>()) } throws
            RuntimeException("WorkManager error")

        val result = orchestrator.startPipeline("sub-003", buildStories(), buildQuestions())

        assertTrue(result.isFailure)
    }

    @Test
    fun `builds pipeline only after successful submission id is available`() = runTest {
        val result = orchestrator.startPipeline("", buildStories(), buildQuestions())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { submissionRepository.updateTATAnalysisStatus(any(), any()) }
    }

    @Test
    fun `uses work planner instead of direct full fan out`() = runTest {
        val result = orchestrator.startPipeline("sub-004", buildStories(12), buildQuestions(12))

        assertTrue(result.isSuccess)
        // 12 stories at BATCH_SIZE 3 means 4 batches: one beginWith + 3 then(batch) calls.
        verify(exactly = 1) { workManager.beginWith(any<List<OneTimeWorkRequest>>()) }
        verify(exactly = 3) { workContinuation.then(any<List<OneTimeWorkRequest>>()) }
    }

    @Test
    fun `enqueues bounded concurrency graph`() = runTest {
        val firstBatch = mutableListOf<List<OneTimeWorkRequest>>()
        every { workManager.beginWith(capture(firstBatch)) } returns workContinuation
        val laterBatches = mutableListOf<List<OneTimeWorkRequest>>()
        every { workContinuation.then(capture(laterBatches)) } returns workContinuation

        orchestrator.startPipeline("sub-005", buildStories(12), buildQuestions(12))

        val allBatchSizes = firstBatch + laterBatches
        assertTrue(
            "every batch must be bounded to avoid hitting Gemini with all calls at once",
            allBatchSizes.all { it.size <= TATAnalysisWorkPlanner.BATCH_SIZE }
        )
    }

    @Test
    fun `fails safely when planner input is invalid`() = runTest {
        val throwingPlanner = mockk<TATAnalysisWorkPlanner>()
        every { throwingPlanner.plan(any(), any(), any()) } throws RuntimeException("bad plan input")
        val orchestratorWithThrowingPlanner =
            TATAnalysisPipelineOrchestrator(submissionRepository, workManager, throwingPlanner)

        val result = orchestratorWithThrowingPlanner.startPipeline("sub-006", buildStories(), buildQuestions())

        assertTrue(result.isFailure)
    }

    private fun buildStories(count: Int = 3): List<TATStoryResponse> = (1..count).map { i ->
        TATStoryResponse(
            questionId = "tat_q_$i",
            story = "Story $i text here.",
            charactersCount = 200,
            viewingTimeTakenSeconds = 15,
            writingTimeTakenSeconds = 120,
            submittedAt = 1_717_171_000L + i
        )
    }

    private fun buildQuestions(count: Int = 3): List<TATQuestion> = (1..count).map { i ->
        TATQuestion(
            id = "tat_q_$i",
            imageUrl = "https://example.com/tat_$i.jpg",
            cardPosition = i,
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 150,
            maxCharacters = 1500
        )
    }
}
