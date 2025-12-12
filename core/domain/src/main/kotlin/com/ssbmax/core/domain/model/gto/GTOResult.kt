package com.ssbmax.core.domain.model.gto

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore

/**
 * GTO Test Result
 * Contains AI analysis with 15 OLQ scores
 */
data class GTOResult(
    val submissionId: String,
    val userId: String,
    val testType: GTOTestType,
    val olqScores: Map<OLQ, OLQScore>,
    val overallScore: Float, // 1-10 scale (lower is better, SSB convention)
    val overallRating: String, // "Exceptional", "Very Good", "Average", etc.
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val analyzedAt: Long = System.currentTimeMillis(),
    val aiConfidence: Int // 0-100% confidence in analysis
) {
    init {
        require(overallScore in 1f..10f) { "Overall score must be between 1 and 10" }
        require(aiConfidence in 0..100) { "AI confidence must be between 0 and 100" }
        require(olqScores.size == 15) { "All 15 OLQs must be scored" }
    }
    
    /**
     * Calculate overall rating based on average OLQ scores
     */
    val calculatedRating: String
        get() = when {
            overallScore <= 3f -> "Exceptional"
            overallScore <= 4f -> "Excellent"
            overallScore <= 5f -> "Very Good"
            overallScore <= 6f -> "Good"
            overallScore <= 7f -> "Average"
            overallScore <= 8f -> "Below Average"
            else -> "Poor"
        }
    
    /**
     * Get top 3 OLQs (lowest scores = best performance)
     */
    val topOLQs: List<Pair<OLQ, OLQScore>>
        get() = olqScores.entries
            .sortedBy { it.value.score }
            .take(3)
            .map { it.key to it.value }
    
    /**
     * Get bottom 3 OLQs (highest scores = needs improvement)
     */
    val improvementOLQs: List<Pair<OLQ, OLQScore>>
        get() = olqScores.entries
            .sortedByDescending { it.value.score }
            .take(3)
            .map { it.key to it.value }
}

/**
 * User's GTO progress tracking
 */
data class GTOProgress(
    val userId: String,
    val completedTests: List<GTOTestType> = emptyList(),
    val testsUsedThisMonth: Map<GTOTestType, Int> = emptyMap(),
    val lastResetDate: Long = System.currentTimeMillis(),
    val currentSequentialOrder: Int = 1, // Next test to unlock (1-8)
    val lastCompletedAt: Long? = null
) {
    /**
     * Check if a specific test is unlocked (prerequisite tests completed)
     */
    fun isTestUnlocked(testType: GTOTestType): Boolean {
        val prerequisites = GTOTestType.getPrerequisites(testType)
        return prerequisites.all { it in completedTests }
    }
    
    /**
     * Get the next available test to take
     */
    fun getNextTest(): GTOTestType? {
        return GTOTestType.fromOrder(currentSequentialOrder)
    }
    
    /**
     * Calculate overall completion percentage
     */
    val completionPercentage: Float
        get() = (completedTests.size.toFloat() / 8f) * 100f
}
