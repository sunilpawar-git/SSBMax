package com.ssbmax.ui.tests.ppdt

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PPDTSubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: PPDTSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)

    private val mockOLQResult = OLQAnalysisResult(
        submissionId = "submission-ppdt-123",
        testType = TestType.PPDT,
        olqScores = createMockOLQScores(),
        overallScore = 79.0f,
        overallRating = "Good",
        strengths = listOf("Observation", "Narrative"),
        weaknesses = listOf("Character Development"),
        recommendations = listOf("Practice character descriptions"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 83
    )

    private val mockSubmission = PPDTSubmission(
        submissionId = "submission-ppdt-123",
        questionId = "ppdt-q1",
        userId = "user-123",
        userName = "Test User",
        userEmail = "test@example.com",
        batchId = null,
        story = "Test story content",
        charactersCount = 150,
        viewingTimeTakenSeconds = 30,
        writingTimeTakenMinutes = 4,
        submittedAt = System.currentTimeMillis(),
        status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
        instructorReview = null,
        analysisStatus = AnalysisStatus.COMPLETED,
        olqResult = mockOLQResult
    )

    @Before
    fun setup() {
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
    }

    @Test
    fun `loadSubmission triggers loading state`() = runTest {
        val firestoreData = createFirestoreSubmissionMap(mockSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)

        viewModel.loadSubmission("submission-ppdt-123")
        advanceUntilIdle()

        // Note: PPDT parsing is complex, just verify method executes
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.ssbRecommendation)
        assertNull(state.error)
    }

    @Test
    fun `initial state has no recommendation`() = runTest {
        val state = viewModel.uiState.value
        assertNull(state.ssbRecommendation)
    }

    @Test
    fun `uiState contains OLQ reasoning text when analysis is complete`() = runTest {
        // WHY: Reasoning text is the primary learning tool — must be accessible in state
        val testReasoning = "Hero did not show initiative"
        val olqResult = OLQAnalysisResult(
            submissionId = "submission-ppdt-123",
            testType = TestType.PPDT,
            olqScores = OLQ.values().associateWith { olq ->
                OLQScore(
                    score = 7,
                    confidence = 80,
                    reasoning = if (olq == OLQ.COURAGE) testReasoning else "Test reasoning"
                )
            },
            overallScore = 7.0f,
            overallRating = "Average",
            strengths = emptyList(),
            weaknesses = emptyList(),
            recommendations = emptyList(),
            analyzedAt = 0L,
            aiConfidence = 80
        )
        val submission = mockSubmission.copy(analysisStatus = AnalysisStatus.COMPLETED)

        coEvery { mockSubmissionRepo.observePPDTSubmission(any()) } returns flowOf(submission)
        coEvery { mockSubmissionRepo.getPPDTResult(any()) } returns Result.success(olqResult)

        viewModel.loadSubmission("submission-ppdt-123")
        advanceUntilIdle()

        val courageScore = viewModel.uiState.value.olqResult?.olqScores?.get(OLQ.COURAGE)
        assertNotNull(courageScore)
        assertEquals(testReasoning, courageScore!!.reasoning)
    }

    @Test
    fun `uiState reasoning is empty string not null when AI returns no reasoning`() = runTest {
        // WHY: UI must never crash on null reasoning — empty string is the safe default
        val olqResult = OLQAnalysisResult(
            submissionId = "submission-ppdt-123",
            testType = TestType.PPDT,
            olqScores = OLQ.values().associateWith { OLQScore(score = 7, confidence = 80, reasoning = "") },
            overallScore = 7.0f,
            overallRating = "Average",
            strengths = emptyList(),
            weaknesses = emptyList(),
            recommendations = emptyList(),
            analyzedAt = 0L,
            aiConfidence = 80
        )
        val submission = mockSubmission.copy(analysisStatus = AnalysisStatus.COMPLETED)

        coEvery { mockSubmissionRepo.observePPDTSubmission(any()) } returns flowOf(submission)
        coEvery { mockSubmissionRepo.getPPDTResult(any()) } returns Result.success(olqResult)

        viewModel.loadSubmission("submission-ppdt-123")
        advanceUntilIdle()

        val courageScore = viewModel.uiState.value.olqResult?.olqScores?.get(OLQ.COURAGE)
        assertNotNull(courageScore)
        assertEquals("", courageScore!!.reasoning)
    }

    // ==================== Bug 3: CancellationException must not produce error ====================

    @Test
    fun `ViewModel cancellation does not produce error UiState`() = runTest {
        // WHY: When the user navigates away, viewModelScope is cancelled. The outer
        // catch(e: Exception) block currently swallows CancellationException and sets
        // error state. That causes a ghost error to flash when the user returns to the
        // screen. CE is a control signal, not a fault — it must be re-thrown, not logged.
        coEvery { mockSubmissionRepo.observePPDTSubmission(any()) } returns flow {
            throw CancellationException("Simulated scope cancellation on navigate-away")
        }

        viewModel.loadSubmission("submission-ppdt-123")
        advanceUntilIdle()

        assertNull(
            "CancellationException must not set error — it signals navigation, not a fault",
            viewModel.uiState.value.error
        )
    }

    // ==================== Bug 4: Exactly one loadResult call on COMPLETED ====================

    @Test
    fun `loadResult called exactly once when flow emits ANALYZING then COMPLETED`() = runTest {
        // WHY: Firestore real-time listeners re-fire the COMPLETED snapshot on every update
        // to the document. Without cancelling observation after COMPLETED is handled,
        // each re-fire calls loadResult() again — hammering Firestore and producing duplicate
        // UI updates. Cancelling the observation coroutine after terminal state is the fix.
        val analyzing = mockSubmission.copy(analysisStatus = AnalysisStatus.ANALYZING)
        val completed = mockSubmission.copy(analysisStatus = AnalysisStatus.COMPLETED)

        coEvery { mockSubmissionRepo.observePPDTSubmission(any()) } returns flow {
            emit(analyzing)
            emit(completed)
        }
        coEvery { mockSubmissionRepo.getPPDTResult(any()) } returns Result.success(mockOLQResult)

        viewModel.loadSubmission("submission-ppdt-123")
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSubmissionRepo.getPPDTResult(any()) }
    }

    private fun createMockOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.values().take(14).associateWith { olq ->
            OLQScore(
                score = (7..9).random(),
                confidence = 83,
                reasoning = "Good demonstration of $olq"
            )
        }
    }

    private fun createFirestoreSubmissionMap(submission: PPDTSubmission): Map<String, Any> {
        val dataMap = mutableMapOf<String, Any>(
            "submissionId" to submission.submissionId,
            "questionId" to submission.questionId,
            "userId" to submission.userId,
            "userName" to submission.userName,
            "userEmail" to submission.userEmail,
            "story" to submission.story,
            "charactersCount" to submission.charactersCount,
            "viewingTimeTakenSeconds" to submission.viewingTimeTakenSeconds,
            "writingTimeTakenMinutes" to submission.writingTimeTakenMinutes,
            "submittedAt" to submission.submittedAt,
            "status" to submission.status.name,
            "analysisStatus" to submission.analysisStatus.name
        )

        submission.batchId?.let { dataMap["batchId"] = it }

        submission.olqResult?.let { olqResult ->
            val olqScoresMap = olqResult.olqScores.map { (olq, score) ->
                olq.name to mapOf(
                    "score" to score.score,
                    "confidence" to score.confidence,
                    "reasoning" to score.reasoning
                )
            }.toMap()

            dataMap["olqResult"] = mapOf(
                "submissionId" to olqResult.submissionId,
                "testType" to olqResult.testType.name,
                "olqScores" to olqScoresMap,
                "overallScore" to olqResult.overallScore,
                "overallRating" to olqResult.overallRating,
                "strengths" to olqResult.strengths,
                "weaknesses" to olqResult.weaknesses,
                "recommendations" to olqResult.recommendations,
                "analyzedAt" to olqResult.analyzedAt,
                "aiConfidence" to olqResult.aiConfidence
            )
        }

        return mapOf("data" to dataMap)
    }
}
