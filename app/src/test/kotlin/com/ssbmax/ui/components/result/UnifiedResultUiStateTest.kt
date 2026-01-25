package com.ssbmax.ui.components.result

import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UnifiedResultUiState interface and computed properties
 */
class UnifiedResultUiStateTest {

    // Test implementation of UnifiedResultUiState for testing
    private data class TestUiState(
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING_ANALYSIS,
        override val olqResult: OLQAnalysisResult? = null,
        override val ssbRecommendation: SSBRecommendationUIModel? = null
    ) : UnifiedResultUiState

    @Test
    fun `default state has correct values`() {
        val state = TestUiState()

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(AnalysisStatus.PENDING_ANALYSIS, state.analysisStatus)
        assertNull(state.olqResult)
        assertNull(state.ssbRecommendation)
    }

    @Test
    fun `isAnalyzing returns true when status is ANALYZING`() {
        val state = TestUiState(analysisStatus = AnalysisStatus.ANALYZING)

        assertTrue(state.isAnalyzing)
        assertFalse(state.isCompleted)
        assertFalse(state.isFailed)
        assertFalse(state.isPending)
    }

    @Test
    fun `isCompleted returns true when status is COMPLETED`() {
        val state = TestUiState(analysisStatus = AnalysisStatus.COMPLETED)

        assertTrue(state.isCompleted)
        assertFalse(state.isAnalyzing)
        assertFalse(state.isFailed)
        assertFalse(state.isPending)
    }

    @Test
    fun `isFailed returns true when status is FAILED`() {
        val state = TestUiState(analysisStatus = AnalysisStatus.FAILED)

        assertTrue(state.isFailed)
        assertFalse(state.isAnalyzing)
        assertFalse(state.isCompleted)
        assertFalse(state.isPending)
    }

    @Test
    fun `isPending returns true when status is PENDING_ANALYSIS`() {
        val state = TestUiState(analysisStatus = AnalysisStatus.PENDING_ANALYSIS)

        assertTrue(state.isPending)
        assertFalse(state.isAnalyzing)
        assertFalse(state.isCompleted)
        assertFalse(state.isFailed)
    }

    @Test
    fun `hasError returns true when error is not null`() {
        val state = TestUiState(error = "Some error")

        assertTrue(state.hasError)
    }

    @Test
    fun `hasError returns false when error is null`() {
        val state = TestUiState(error = null)

        assertFalse(state.hasError)
    }

    @Test
    fun `hasResults returns true when olqResult is not null`() {
        val olqResult = createTestOLQResult()
        val state = TestUiState(olqResult = olqResult)

        assertTrue(state.hasResults)
    }

    @Test
    fun `hasResults returns false when olqResult is null`() {
        val state = TestUiState(olqResult = null)

        assertFalse(state.hasResults)
    }

    @Test
    fun `showResults returns true when completed with results`() {
        val olqResult = createTestOLQResult()
        val state = TestUiState(
            analysisStatus = AnalysisStatus.COMPLETED,
            olqResult = olqResult
        )

        assertTrue(state.showResults)
    }

    @Test
    fun `showResults returns false when completed without results`() {
        val state = TestUiState(analysisStatus = AnalysisStatus.COMPLETED, olqResult = null)

        assertFalse(state.showResults)
    }

    @Test
    fun `showResults returns false when not completed`() {
        val olqResult = createTestOLQResult()
        val state = TestUiState(
            analysisStatus = AnalysisStatus.ANALYZING,
            olqResult = olqResult
        )

        assertFalse(state.showResults)
    }

    private fun createTestOLQResult(): OLQAnalysisResult {
        val olqScores = OLQ.entries.associateWith { olq ->
            OLQScore(score = 5, confidence = 80, reasoning = "Test reasoning for ${olq.displayName}")
        }

        return OLQAnalysisResult(
            submissionId = "test-submission-id",
            testType = TestType.TAT,
            olqScores = olqScores,
            overallScore = 5.0f,
            overallRating = "Good",
            strengths = listOf("Leadership", "Communication"),
            weaknesses = listOf("Time management"),
            recommendations = listOf("Practice more"),
            analyzedAt = System.currentTimeMillis(),
            aiConfidence = 85
        )
    }
}
