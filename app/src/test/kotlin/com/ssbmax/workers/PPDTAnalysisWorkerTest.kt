package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.PPDTQuestion
import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.notifications.NotificationHelper
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PPDTAnalysisWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var aiService: AIService
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var getOLQDashboard: GetOLQDashboardUseCase
    private lateinit var userProfileRepository: UserProfileRepository

    private val testSubmissionId = "test-submission-123"
    private val testUserId = "test-user-456"
    private val testQuestionId = "ppdt-question-1"

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
        aiService = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        testContentRepository = mockk(relaxed = true)
        getOLQDashboard = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)

        every { workerParams.inputData } returns workDataOf(
            PPDTAnalysisWorker.KEY_SUBMISSION_ID to testSubmissionId
        )
        every { workerParams.runAttemptCount } returns 0
    }

    private fun buildFakeSubmission(
        analysisStatus: AnalysisStatus = AnalysisStatus.PENDING_ANALYSIS
    ) = PPDTSubmission(
        submissionId = testSubmissionId,
        questionId = testQuestionId,
        userId = testUserId,
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

    private fun buildFakeProfile(gender: Gender) = UserProfile(
        userId = testUserId,
        fullName = "Test Aspirant",
        age = 22,
        gender = gender,
        entryType = EntryType.GRADUATE
    )

    private fun buildFakeResponseAnalysis() = ResponseAnalysis(
        olqScores = OLQ.values().associate { olq ->
            olq to OLQScoreWithReasoning(olq = olq, score = 5f, reasoning = "Adequate performance")
        },
        overallConfidence = 80,
        keyInsights = listOf("Good initiative shown")
    )

    private fun setupHappyPathMocks(promptSlot: CapturingSlot<String>) {
        coEvery { submissionRepository.updatePPDTAnalysisStatus(any(), any()) } returns Result.success(Unit)
        coEvery { submissionRepository.updatePPDTOLQResult(any(), any()) } returns Result.success(Unit)
        coEvery { testContentRepository.getPPDTQuestion(testQuestionId) } returns Result.success(
            PPDTQuestion(id = testQuestionId, imageUrl = "https://example.com/ppdt.jpg", imageDescription = "Scene desc")
        )
        coEvery { aiService.analyzePPDTResponse(capture(promptSlot)) } returns Result.success(buildFakeResponseAnalysis())
        coEvery { getOLQDashboard.invalidateCache(any()) } returns Unit
    }

    private fun createWorker() = PPDTAnalysisWorker(
        context = context,
        params = workerParams,
        submissionRepository = submissionRepository,
        aiService = aiService,
        notificationHelper = notificationHelper,
        testContentRepository = testContentRepository,
        getOLQDashboard = getOLQDashboard,
        userProfileRepository = userProfileRepository
    )

    @Test
    fun `doWork uses male gender hint in prompt when user gender is MALE`() = runTest {
        // WHY: AI prompt must reflect actual candidate gender for accurate OLQ assessment
        val promptSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.MALE)))
        setupHappyPathMocks(promptSlot)

        createWorker().doWork()

        assertTrue(
            "Prompt must contain male protagonist hint for MALE candidates",
            promptSlot.captured.contains("male protagonist", ignoreCase = true)
        )
    }

    @Test
    fun `doWork uses female gender hint in prompt when user gender is FEMALE`() = runTest {
        // WHY: Female candidates must not receive male-protagonist scoring guidance
        val promptSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.FEMALE)))
        setupHappyPathMocks(promptSlot)

        createWorker().doWork()

        assertTrue(
            "Prompt must contain female protagonist hint for FEMALE candidates",
            promptSlot.captured.contains("female protagonist", ignoreCase = true)
        )
    }

    @Test
    fun `doWork falls back to neutral gender hint when profile fetch fails`() = runTest {
        // WHY: Worker must not crash if profile unavailable — analysis continues with neutral gender
        val promptSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.failure(Exception("network error")))
        setupHappyPathMocks(promptSlot)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(
            "Prompt must use unconstrained gender hint when profile is unavailable",
            promptSlot.captured.contains("not constrained", ignoreCase = true)
        )
    }

    @Test
    fun `doWork returns failure when submission not found`() = runTest {
        // WHY: Idempotency guard — stale WorkManager jobs must not proceed with missing data
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(null)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork skips AI analysis when status is not PENDING_ANALYSIS`() = runTest {
        // WHY: Prevents double-analysis if WorkManager retries an already-completed job
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(
            buildFakeSubmission(analysisStatus = AnalysisStatus.COMPLETED)
        )

        val result = createWorker().doWork()

        coVerify(exactly = 0) { aiService.analyzePPDTResponse(any()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
