package com.ssbmax.ui.tests.srt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
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
class SRTSubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: SRTSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)

    private val mockOLQResult = OLQAnalysisResult(
        submissionId = "submission-srt-123",
        testType = TestType.SRT,
        olqScores = createMockOLQScores(),
        overallScore = 76.0f,
        overallRating = "Good",
        strengths = listOf("Decision Making", "Leadership"),
        weaknesses = listOf("Reaction Speed"),
        recommendations = listOf("Practice more scenarios"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 80
    )

    private val mockSubmission = SRTSubmission(
        id = "submission-srt-123",
        userId = "user-123",
        testId = "srt-standard",
        responses = emptyList(),
        totalTimeTakenMinutes = 30,
        submittedAt = System.currentTimeMillis(),
        analysisStatus = AnalysisStatus.COMPLETED,
        olqResult = mockOLQResult
    )

    @Before
    fun setup() {
        viewModel = SRTSubmissionResultViewModel(mockSubmissionRepo)
    }

    @Test
    fun `loadSubmission success updates state with submission data`() = runTest {
        val firestoreData = createFirestoreSubmissionMap(mockSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        viewModel.loadSubmission("submission-srt-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.submission)
            assertEquals("submission-srt-123", state.submission?.id)
            assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(null)

        viewModel.loadSubmission("submission-srt-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.submission)
            assertNotNull(state.error)
            assertEquals("Submission not found", state.error)
        }
    }

    @Test
    fun `loadSubmission with pending analysis shows correct status`() = runTest {
        val pendingSubmission = mockSubmission.copy(
            analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
            olqResult = null
        )
        val firestoreData = createFirestoreSubmissionMap(pendingSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        viewModel.loadSubmission("submission-srt-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.submission)
            assertEquals(AnalysisStatus.PENDING_ANALYSIS, state.submission?.analysisStatus)
            assertNull(state.submission?.olqResult)
        }
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.error)
    }

    private fun createMockOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.values().take(14).associateWith { olq ->
            OLQScore(
                score = (7..9).random(),
                confidence = 80,
                reasoning = "Good demonstration of $olq"
            )
        }
    }

    private fun createFirestoreSubmissionMap(submission: SRTSubmission): Map<String, Any> {
        val dataMap = mutableMapOf<String, Any>(
            "id" to submission.id,
            "userId" to submission.userId,
            "testId" to submission.testId,
            "totalTimeTakenMinutes" to submission.totalTimeTakenMinutes,
            "submittedAt" to submission.submittedAt,
            "analysisStatus" to submission.analysisStatus.name
        )

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

