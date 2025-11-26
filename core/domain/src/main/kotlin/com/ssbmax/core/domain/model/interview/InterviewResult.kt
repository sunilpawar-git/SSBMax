package com.ssbmax.core.domain.model.interview

import java.time.Instant

/**
 * Final interview assessment result
 *
 * SSB Rating Scale (1-10, LOWER is BETTER):
 * - 1-4: Outstanding (very rare, exceptional candidates)
 * - 5: Recommended (best typical score for selection)
 * - 6: Good (above average, borderline recommended)
 * - 7: Average (meets basic expectations)
 * - 8-10: Not Recommended (needs significant improvement)
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
 * @param strengths Top 3 OLQs demonstrated (lowest scores = strongest)
 * @param weaknesses Bottom 3 OLQs needing improvement (highest scores = weakest)
 * @param feedback Personalized feedback and recommendations
 * @param overallRating Final rating (SSB 1-10 scale, lower is better)
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
        require(overallRating in 1..10) { "Overall rating must be between 1 and 10 (SSB scale)" }
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
     * Check if result is passing (SSB scale: score <= 6 is recommended)
     */
    fun isPassing(): Boolean = overallRating <= 6

    /**
     * Get performance level based on overall rating (SSB 1-10 scale, lower is better)
     */
    fun getPerformanceLevel(): PerformanceLevel {
        return PerformanceLevel.fromRating(overallRating)
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
 * Performance level classification (SSB 1-10 scale, LOWER is BETTER)
 *
 * SSB Grading:
 * - 1-4: Outstanding (exceptional, very rare)
 * - 5: Excellent (recommended for selection)
 * - 6: Good (above average, borderline recommended)
 * - 7: Average (meets basic expectations)
 * - 8-10: Below Average to Poor (not recommended)
 */
enum class PerformanceLevel(val displayName: String, val description: String) {
    EXCELLENT("Excellent", "Outstanding performance - recommended for selection"),
    GOOD("Good", "Strong officer-like qualities demonstrated"),
    AVERAGE("Average", "Meets basic officer-like quality expectations"),
    BELOW_AVERAGE("Below Average", "Some qualities need development"),
    POOR("Poor", "Needs significant improvement across multiple areas");

    companion object {
        /**
         * Convert SSB rating (1-10, lower is better) to performance level
         */
        fun fromRating(rating: Int): PerformanceLevel {
            return when (rating) {
                in 1..5 -> EXCELLENT  // 1-5: Recommended
                6 -> GOOD             // 6: Good, borderline
                7 -> AVERAGE          // 7: Average
                8 -> BELOW_AVERAGE    // 8: Below average
                in 9..10 -> POOR      // 9-10: Not recommended
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
        require(overallRating in 1..10) { "Overall rating must be between 1 and 10 (SSB scale)" }
        require(averageScore >= 0) { "Average score cannot be negative" }
        require(durationMinutes > 0) { "Duration must be positive" }
        require(completionRate in 0f..100f) { "Completion rate must be between 0 and 100" }
    }
}
