package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.PPDTQuestion
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.service.OLQScoreWithReasoning
import com.ssbmax.shared.domain.service.ResponseAnalysis
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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

        // PPDTAnalysisWorker resolves its dependencies via KoinComponent/inject()
        // (converted from Hilt's @HiltWorker/@AssistedInject) — start a Koin instance
        // with the mocks above bound so `by inject()` resolves them in the worker.
        startKoin {
            modules(module {
                single { submissionRepository }
                single { aiService }
                single { notificationHelper }
                single { testContentRepository }
                single { getOLQDashboard }
                single { userProfileRepository }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
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

    private fun buildFakeQuestion(imageUrl: String = "https://example.com/ppdt.jpg") = PPDTQuestion(
        id = testQuestionId,
        imageUrl = imageUrl,
        imageDescription = "Scene description"
    )

    private fun buildFakeResponseAnalysis() = ResponseAnalysis(
        olqScores = OLQ.values().associate { olq ->
            olq to OLQScoreWithReasoning(olq = olq, score = 5f, reasoning = "Adequate performance")
        },
        overallConfidence = 80,
        keyInsights = listOf("Good initiative shown")
    )

    private fun setupHappyPathMocks(genderSlot: CapturingSlot<String>) {
        coEvery { submissionRepository.updatePPDTAnalysisStatus(any(), any()) } returns Result.success(Unit)
        coEvery { submissionRepository.updatePPDTOLQResult(any(), any()) } returns Result.success(Unit)
        coEvery { testContentRepository.getPPDTQuestion(testQuestionId) } returns Result.success(buildFakeQuestion())
        coEvery {
            aiService.analyzePPDTMultimodal(any(), any(), any(), capture(genderSlot))
        } returns Result.success(buildFakeResponseAnalysis())
        coEvery { getOLQDashboard.invalidateCache(any()) } returns Unit
    }

    private fun createWorker() = PPDTAnalysisWorker(context, workerParams)

    @Test
    fun `doWork passes Male candidateGender when user gender is MALE`() = runTest {
        // WHY: AI multimodal path must receive correct gender so protagonist-alignment scoring is accurate
        val genderSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.MALE)))
        setupHappyPathMocks(genderSlot)

        createWorker().doWork()

        assertEquals("candidateGender must be 'Male' for MALE profile", "Male", genderSlot.captured)
    }

    @Test
    fun `doWork passes Female candidateGender when user gender is FEMALE`() = runTest {
        // WHY: Female candidates must not receive male-protagonist scoring guidance
        val genderSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.FEMALE)))
        setupHappyPathMocks(genderSlot)

        createWorker().doWork()

        assertEquals("candidateGender must be 'Female' for FEMALE profile", "Female", genderSlot.captured)
    }

    @Test
    fun `doWork passes Unknown candidateGender when profile fetch fails`() = runTest {
        // WHY: Worker must not crash if profile unavailable — analysis continues with neutral gender
        val genderSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.failure(Exception("network error")))
        setupHappyPathMocks(genderSlot)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("candidateGender must be 'Unknown' when profile fetch fails", "Unknown", genderSlot.captured)
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

        coVerify(exactly = 0) { aiService.analyzePPDTMultimodal(any(), any(), any(), any()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork calls analyzePPDTMultimodal for analysis`() = runTest {
        // WHY: Verifies the multimodal code path is actually used after Phase 8
        val genderSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.MALE)))
        setupHappyPathMocks(genderSlot)

        createWorker().doWork()

        coVerify(exactly = 1) { aiService.analyzePPDTMultimodal(any(), any(), any(), any()) }
    }

    @Test
    fun `doWork falls back gracefully when image bytes unavailable`() = runTest {
        // WHY: Any failure to get image bytes (blank URL) must not abort analysis — text-only fallback still runs
        val genderSlot = slot<String>()
        coEvery { submissionRepository.getPPDTSubmission(testSubmissionId) } returns Result.success(buildFakeSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildFakeProfile(Gender.MALE)))
        coEvery { submissionRepository.updatePPDTAnalysisStatus(any(), any()) } returns Result.success(Unit)
        coEvery { submissionRepository.updatePPDTOLQResult(any(), any()) } returns Result.success(Unit)
        coEvery { testContentRepository.getPPDTQuestion(testQuestionId) } returns Result.success(
            buildFakeQuestion(imageUrl = "")  // blank URL → image bytes skip, analysis proceeds
        )
        coEvery {
            aiService.analyzePPDTMultimodal(any(), any(), any(), capture(genderSlot))
        } returns Result.success(buildFakeResponseAnalysis())
        coEvery { getOLQDashboard.invalidateCache(any()) } returns Unit

        val result = createWorker().doWork()

        assertNotEquals("Worker must not fail when image bytes are unavailable", ListenableWorker.Result.failure(), result)
    }
}
