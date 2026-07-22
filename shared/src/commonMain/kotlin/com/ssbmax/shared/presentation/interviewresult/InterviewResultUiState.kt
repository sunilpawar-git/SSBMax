package com.ssbmax.shared.presentation.interviewresult

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.interview.PerformanceLevel
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel

/**
 * KMP port of `app/.../ui/interview/result/InterviewResultUiState.kt`.
 * Field-for-field equivalent.
 */
data class InterviewResultUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val result: InterviewResult? = null,
    val ssbRecommendation: SSBRecommendationUIModel? = null,
    val error: String? = null,
    val isAnalysisPending: Boolean = false
) {
    val mode: InterviewMode
        get() = result?.mode ?: InterviewMode.VOICE_BASED

    val performanceLevel: PerformanceLevel
        get() = result?.getPerformanceLevel() ?: PerformanceLevel.AVERAGE

    val averageScore: Float
        get() = result?.getAverageOLQScore() ?: 0f

    val completionRate: Float
        get() = result?.getCompletionRate() ?: 0f

    val durationMinutes: Int
        get() = result?.getDurationMinutes() ?: 0

    val strengths: List<OLQ>
        get() = result?.strengths ?: emptyList()

    val weaknesses: List<OLQ>
        get() = result?.weaknesses ?: emptyList()

    fun getSortedOLQScores(): List<Pair<OLQ, OLQScore>> {
        return result?.overallOLQScores?.entries
            ?.map { it.key to it.value }
            ?.sortedByDescending { it.second.score }
            ?: emptyList()
    }

    fun getSortedCategoryScores(): List<Pair<OLQCategory, Float>> {
        return result?.categoryScores?.entries
            ?.map { it.key to it.value }
            ?.sortedByDescending { it.second }
            ?: emptyList()
    }

    val isPassing: Boolean
        get() = result?.isPassing() ?: false
}
