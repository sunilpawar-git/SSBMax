package com.ssbmax.core.domain.model.interview

import java.time.Instant

/**
 * Final interview assessment result
 *
 * @param id Unique result identifier
 * @param sessionId Parent interview session ID
 * @param userId User who completed the interview
 * @param mode Interview mode used
 * @param completedAt When the interview was completed
 * @param durationSec Total interview duration in seconds
 * @param totalQuestions Number of questions asked
 * @param totalResponses Number of responses provided
 * @param overallOLQScores Aggregated OLQ scores across all responses
 * @param categoryScores Average scores by OLQ category
 * @param overallConfidence Average AI confidence across all assessments
 * @param strengths Top 3 OLQs demonstrated
 * @param weaknesses Bottom 3 OLQs needing improvement
 * @param feedback Personalized feedback and recommendations
 * @param overallRating Final rating (1-5 scale)
 */
data class InterviewResult(
    val id: String,
    val sessionId: String,
    val userId: String,
    val mode: InterviewMode,
    val completedAt: Instant,
    val durationSec: Long,
    val totalQuestions: Int,
    val totalResponses: Int,
    val overallOLQScores: Map<OLQ, OLQScore>,
    val categoryScores: Map<OLQCategory, Float>,
    val overallConfidence: Int,
    val strengths: List<OLQ>,
    val weaknesses: List<OLQ>,
    val feedback: String,
    val overallRating: Int
) {
    init {
        require(id.isNotBlank()) { "Result ID cannot be blank" }
        require(sessionId.isNotBlank()) { "Session ID cannot be blank" }
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(durationSec > 0) { "Duration must be positive" }
        require(totalQuestions > 0) { "Must have at least one question" }
        require(totalResponses > 0) { "Must have at least one response" }
        require(totalResponses <= totalQuestions) { "Responses cannot exceed questions" }
        require(overallConfidence in 0..100) { "Overall confidence must be between 0 and 100" }
        require(overallRating in 1..5) { "Overall rating must be between 1 and 5" }
        require(strengths.size <= 3) { "Maximum 3 strengths allowed" }
        require(weaknesses.size <= 3) { "Maximum 3 weaknesses allowed" }
        require(feedback.isNotBlank()) { "Feedback cannot be blank" }
    }

    /**
     * Calculate completion rate (responses / questions)
     */
    fun getCompletionRate(): Float {
        return (totalResponses.toFloat() / totalQuestions) * 100
    }

    /**
     * Get average OLQ score across all qualities
     */
    fun getAverageOLQScore(): Float {
        if (overallOLQScores.isEmpty()) return 0f
        return overallOLQScores.values.map { it.score }.average().toFloat()
    }

    /**
     * Get duration in minutes (rounded)
     */
    fun getDurationMinutes(): Int = (durationSec / 60).toInt()

    /**
     * Check if result is passing (average score >= 3)
     */
    fun isPassing(): Boolean = getAverageOLQScore() >= 3.0f

    /**
     * Get performance level based on overall rating
     */
    fun getPerformanceLevel(): PerformanceLevel {
        return when (overallRating) {
            1 -> PerformanceLevel.POOR
            2 -> PerformanceLevel.BELOW_AVERAGE
            3 -> PerformanceLevel.AVERAGE
            4 -> PerformanceLevel.GOOD
            5 -> PerformanceLevel.EXCELLENT
            else -> PerformanceLevel.AVERAGE
        }
    }

    /**
     * Get top category by average score
     */
    fun getTopCategory(): OLQCategory? {
        return categoryScores.maxByOrNull { it.value }?.key
    }

    /**
     * Get weakest category by average score
     */
    fun getWeakestCategory(): OLQCategory? {
        return categoryScores.minByOrNull { it.value }?.key
    }
}

/**
 * Performance level classification
 */
enum class PerformanceLevel(val displayName: String, val description: String) {
    POOR("Poor", "Needs significant improvement across multiple areas"),
    BELOW_AVERAGE("Below Average", "Some qualities need development"),
    AVERAGE("Average", "Meets basic officer-like quality expectations"),
    GOOD("Good", "Demonstrates strong officer-like qualities"),
    EXCELLENT("Excellent", "Outstanding performance across all qualities");

    companion object {
        fun fromRating(rating: Int): PerformanceLevel {
            return when (rating) {
                1 -> POOR
                2 -> BELOW_AVERAGE
                3 -> AVERAGE
                4 -> GOOD
                5 -> EXCELLENT
                else -> AVERAGE
            }
        }
    }
}

/**
 * Interview result summary for display
 */
data class InterviewResultSummary(
    val sessionId: String,
    val completedAt: Instant,
    val mode: InterviewMode,
    val overallRating: Int,
    val averageScore: Float,
    val durationMinutes: Int,
    val completionRate: Float
) {
    init {
        require(overallRating in 1..5) { "Overall rating must be between 1 and 5" }
        require(averageScore >= 0) { "Average score cannot be negative" }
        require(durationMinutes > 0) { "Duration must be positive" }
        require(completionRate in 0f..100f) { "Completion rate must be between 0 and 100" }
    }
}
