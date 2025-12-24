package com.ssbmax.ui.tests.tat

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TATSubmissionResultViewModel
 * 
 * Tests cover:
 * - Submission loading success/failure
 * - Real-time status updates via Flow
 * - OLQ result parsing
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TATSubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: TATSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)

    private val mockOLQResult = OLQAnalysisResult(
        submissionId = "submission-tat-123",
        testType = TestType.TAT,
        olqScores = createMockOLQScores(),
        overallScore = 75.5f,
        overallRating = "Good",
        strengths = listOf("Leadership", "Courage"),
        weaknesses = listOf("Decision Speed"),
        recommendations = listOf("Practice more scenarios"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 85
    )

    private val mockSubmission = TATSubmission(
        id = "submission-tat-123",
        userId = "user-123",
        testId = "tat-standard",
        stories = emptyList(),
        totalTimeTakenMinutes = 48,
        submittedAt = System.currentTimeMillis(),
        analysisStatus = AnalysisStatus.COMPLETED,
        olqResult = mockOLQResult
    )

    @Before
    fun setup() {
        viewModel = TATSubmissionResultViewModel(mockSubmissionRepo)
    }

    @Test
    fun `loadSubmission success updates state with submission data`() = runTest {
        // Setup mock to return Firestore-like map structure
        val firestoreData = createFirestoreSubmissionMap(mockSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        viewModel.loadSubmission("submission-tat-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.submission)
            assertEquals("submission-tat-123", state.submission?.id)
            assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(null)

        viewModel.loadSubmission("submission-tat-123")
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

        viewModel.loadSubmission("submission-tat-123")
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
    fun `loadSubmission with analyzing status shows correct status`() = runTest {
        val analyzingSubmission = mockSubmission.copy(
            analysisStatus = AnalysisStatus.ANALYZING,
            olqResult = null
        )
        val firestoreData = createFirestoreSubmissionMap(analyzingSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        viewModel.loadSubmission("submission-tat-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.submission)
            assertEquals(AnalysisStatus.ANALYZING, state.submission?.analysisStatus)
            assertNull(state.submission?.olqResult)
        }
    }

    @Test
    fun `loadSubmission with completed status and OLQ result shows complete data`() = runTest {
        val firestoreData = createFirestoreSubmissionMap(mockSubmission)
        coEvery { mockSubmissionRepo.observeSubmission(any()) } returns flowOf(firestoreData)

        viewModel.loadSubmission("submission-tat-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.submission)
            assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
            assertNotNull(state.submission?.olqResult)
            assertEquals(75.5f, state.submission?.olqResult?.overallScore)
            assertEquals("Good", state.submission?.olqResult?.overallRating)
        }
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.error)
    }

    // Helper function to create mock OLQ scores
    private fun createMockOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.values().take(14).associateWith { olq ->
            OLQScore(
                score = (7..9).random(),
                confidence = 80,
                reasoning = "Good demonstration of $olq"
            )
        }
    }

    // Helper function to create Firestore-like map structure
    private fun createFirestoreSubmissionMap(submission: TATSubmission): Map<String, Any> {
        val dataMap = mutableMapOf<String, Any>(
            "id" to submission.id,
            "userId" to submission.userId,
            "testId" to submission.testId,
            "totalTimeTakenMinutes" to submission.totalTimeTakenMinutes,
            "submittedAt" to submission.submittedAt,
            "analysisStatus" to submission.analysisStatus.name
        )

        // Add OLQ result if present
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

