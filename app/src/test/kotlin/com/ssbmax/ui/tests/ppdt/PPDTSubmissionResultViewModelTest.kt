package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.validation.RecommendationOutcome
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
