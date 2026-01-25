package com.ssbmax.ui.interview.result

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.interview.PerformanceLevel
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel

/**
 * UI state for Interview Result screen
 *
 * Displays OLQ scores, strengths/weaknesses, and feedback
 *
 * Note: With background analysis, the resultId is only available after
 * the InterviewAnalysisWorker completes. The notification provides the
 * resultId to navigate directly to results.
 */
data class InterviewResultUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val result: InterviewResult? = null,
    val ssbRecommendation: SSBRecommendationUIModel? = null,
    val error: String? = null,
    // True when analysis is still in progress (for future history view)
    val isAnalysisPending: Boolean = false
) {
    /**
     * Get interview mode
     */
    val mode: InterviewMode
        get() = result?.mode ?: InterviewMode.TEXT_BASED

    /**
     * Get performance level
     */
    val performanceLevel: PerformanceLevel
        get() = result?.getPerformanceLevel() ?: PerformanceLevel.AVERAGE

    /**
     * Get average OLQ score
     */
    val averageScore: Float
        get() = result?.getAverageOLQScore() ?: 0f

    /**
     * Get completion rate percentage
     */
    val completionRate: Float
        get() = result?.getCompletionRate() ?: 0f

    /**
     * Get duration in minutes
     */
    val durationMinutes: Int
        get() = result?.getDurationMinutes() ?: 0

    /**
     * Get strengths
     */
    val strengths: List<OLQ>
        get() = result?.strengths ?: emptyList()

    /**
     * Get weaknesses
     */
    val weaknesses: List<OLQ>
        get() = result?.weaknesses ?: emptyList()

    /**
     * Get OLQ scores sorted by score (descending)
     */
    fun getSortedOLQScores(): List<Pair<OLQ, OLQScore>> {
        return result?.overallOLQScores?.entries
            ?.map { it.key to it.value }
            ?.sortedByDescending { it.second.score }
            ?: emptyList()
    }

    /**
     * Get category scores sorted by score (descending)
     */
    fun getSortedCategoryScores(): List<Pair<OLQCategory, Float>> {
        return result?.categoryScores?.entries
            ?.map { it.key to it.value }
            ?.sortedByDescending { it.second }
            ?: emptyList()
    }

    /**
     * Check if result is passing (average score >= 3)
     */
    val isPassing: Boolean
        get() = result?.isPassing() ?: false
}
