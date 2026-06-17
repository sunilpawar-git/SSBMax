package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ssbmax.core.data.local.dao.TATStoryAssessmentDao
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.TATStoryResponse
import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
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
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TATStoryAnalysisWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var aiService: AIService
    private lateinit var tatStoryAssessmentDao: TATStoryAssessmentDao

    private val testSubmissionId = "sub-123"
    private val testUserId = "user-456"
    private val testQuestionId = "tat_046_male"
    private val testImageUrl = "https://storage.googleapis.com/ssbmax.app/tat_046_male.jpg"

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
        userProfileRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        tatStoryAssessmentDao = mockk(relaxed = true)

        every { workerParams.runAttemptCount } returns 0
    }

    private fun buildInputData(
        imageUrl: String = testImageUrl,
        imageContextJson: String = "{}"
    ) = workDataOf(
        TATStoryAnalysisWorker.KEY_SUBMISSION_ID to testSubmissionId,
        TATStoryAnalysisWorker.KEY_QUESTION_ID to testQuestionId,
        TATStoryAnalysisWorker.KEY_STORY_INDEX to 0,
        TATStoryAnalysisWorker.KEY_IMAGE_URL to imageUrl,
        TATStoryAnalysisWorker.KEY_IMAGE_CONTEXT_JSON to imageContextJson
    )

    private fun buildSubmission(stories: List<TATStoryResponse> = listOf(buildStoryResponse())) =
        TATSubmission(
            id = testSubmissionId,
            userId = testUserId,
            testId = "tat_standard",
            stories = stories,
            totalTimeTakenMinutes = 45,
            submittedAt = System.currentTimeMillis(),
            analysisStatus = AnalysisStatus.PENDING_ANALYSIS
        )

    private fun buildStoryResponse() = TATStoryResponse(
        questionId = testQuestionId,
        story = "A brave officer crossed the flooded river to rescue the stranded civilian.",
        charactersCount = 72,
        viewingTimeTakenSeconds = 28,
        writingTimeTakenSeconds = 180,
        submittedAt = System.currentTimeMillis()
    )

    private fun buildFullOLQAnalysis() = ResponseAnalysis(
        olqScores = OLQ.entries.associate { olq ->
            olq to OLQScoreWithReasoning(olq = olq, score = 6f, reasoning = "Adequate")
        },
        overallConfidence = 75,
        keyInsights = listOf("Good initiative")
    )

    private fun buildUserProfile(gender: Gender = Gender.MALE) = UserProfile(
        userId = testUserId,
        fullName = "Test Aspirant",
        age = 22,
        gender = gender,
        entryType = EntryType.GRADUATE
    )

    private fun createWorker() = TATStoryAnalysisWorker(
        context = context,
        params = workerParams,
        submissionRepository = submissionRepository,
        userProfileRepository = userProfileRepository,
        aiService = aiService,
        tatStoryAssessmentDao = tatStoryAssessmentDao
    )

    // ───────────── imageUrl from inputData — no repository ─────────────

    @Test
    fun `diagnostic - confirm worker returns retry when exception thrown`() = runTest {
        // Intentionally cause a failure to confirm retry behavior
        every { workerParams.inputData } returns buildInputData(imageUrl = "")
        coEvery { submissionRepository.getTATSubmission(any()) } throws RuntimeException("TEST EXCEPTION - this is expected")

        val result = createWorker().doWork()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork reads imageUrl from inputData without any repository call`() = runTest {
        // WHY: Worker previously called getTATQuestions() to recover imageUrl.
        // That method ignores testId and returns a fresh random 12, causing imageBytes=0 on all
        // stories (June 2026 bug). imageUrl must come from inputData, not a repository.
        // Use blank imageUrl (blank card path) to avoid real network IO in unit tests.
        every { workerParams.inputData } returns buildInputData(imageUrl = "")
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.success(buildSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildUserProfile()))
        coEvery { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) } returns
            Result.success(buildFullOLQAnalysis())

        val result = createWorker().doWork()

        // Worker succeeds without a TestContentRepository dependency — structural proof
        // that imageUrl comes from inputData (the worker no longer has TestContentRepository)
        assertEquals("Worker must succeed when imageUrl is in inputData", ListenableWorker.Result.success(), result)
        // AI was called (analysis ran even with empty imageBytes — blank card / graceful degradation)
        coVerify(exactly = 1) { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `doWork proceeds with empty imageBytes when imageUrl is blank (blank card)`() = runTest {
        // WHY: Card 12 (blank card) has no image. Worker must still run AI analysis
        // using text-only mode — graceful degradation, not failure.
        every { workerParams.inputData } returns buildInputData(imageUrl = "")
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.success(buildSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildUserProfile()))
        coEvery { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) } returns
            Result.success(buildFullOLQAnalysis())

        val result = createWorker().doWork()

        assertNotEquals("Worker must not fail for blank card (empty imageUrl)", ListenableWorker.Result.failure(), result)
        // AI was still called even with no image bytes
        coVerify(exactly = 1) { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) }
    }

    // ───────────── chain safety — FAILED placeholder ─────────────

    @Test
    fun `doWork saves FAILED placeholder and returns success when story not found in submission`() = runTest {
        // WHY: A missing story (questionId mismatch) is a data anomaly, not a transient error.
        // Saving a placeholder and returning success prevents WorkContinuation.combine() from
        // blocking the synthesis worker permanently.
        every { workerParams.inputData } returns buildInputData()
        val submissionWithoutStory = buildSubmission(stories = emptyList())
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.success(submissionWithoutStory)

        val result = createWorker().doWork()

        assertEquals("Worker must return success (not failure) when story is missing", ListenableWorker.Result.success(), result)
        // AI must NOT be called — no story to analyze
        coVerify(exactly = 0) { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) }
        // Placeholder saved so synthesis can count this story
        coVerify(exactly = 1) { tatStoryAssessmentDao.insert(match { it.overallRating == TATStoryAnalysisWorker.FAILED_MARKER }) }
    }

    @Test
    fun `doWork saves FAILED placeholder and returns success when AI exhausts all retries`() = runTest {
        // WHY: AI failures must not break the synthesis chain. 6+ valid stories are enough
        // for synthesis to proceed. One failed story degrades quality, not correctness.
        every { workerParams.inputData } returns buildInputData()
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.success(buildSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildUserProfile()))
        coEvery { aiService.analyzeTATStoryMultimodal(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(Exception("Gemini API overloaded"))

        val result = createWorker().doWork()

        assertEquals("Worker must return success after AI exhausts retries", ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { tatStoryAssessmentDao.insert(match { it.overallRating == TATStoryAnalysisWorker.FAILED_MARKER }) }
    }

    @Test
    fun `doWork retries WorkManager when submission fetch fails (transient error)`() = runTest {
        // WHY: Firestore fetch failure is transient. Throwing causes WorkManager to retry
        // the worker (up to MAX_AI_RETRIES) rather than silently skipping the story.
        every { workerParams.inputData } returns buildInputData()
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.failure(Exception("network timeout"))

        val result = createWorker().doWork()

        // Should retry, not silently succeed with a placeholder
        assertEquals("Worker must retry on transient submission fetch failure", ListenableWorker.Result.retry(), result)
    }

    // ───────────── gender routing ─────────────

    @Test
    fun `doWork passes candidateGender MALE to AI when user profile is MALE`() = runTest {
        // WHY: Gemini prompt adjusts protagonist alignment scoring based on gender.
        // Wrong gender → wrong OLQ scoring rubric for 50% of candidates.
        val genderSlot = slot<String>()
        every { workerParams.inputData } returns buildInputData()
        coEvery { submissionRepository.getTATSubmission(testSubmissionId) } returns Result.success(buildSubmission())
        every { userProfileRepository.getUserProfile(testUserId) } returns flowOf(Result.success(buildUserProfile(Gender.MALE)))
        coEvery { aiService.analyzeTATStoryMultimodal(any(), any(), any(), capture(genderSlot), any(), any()) } returns
            Result.success(buildFullOLQAnalysis())

        createWorker().doWork()

        assertEquals("candidateGender must be 'Male' for MALE profile", "Male", genderSlot.captured)
    }
}
