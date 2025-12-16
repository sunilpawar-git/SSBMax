package com.ssbmax.core.domain.model.results

import com.ssbmax.core.domain.model.TestType

/**
 * Lightweight historic result for listing view
 * Shows past test submissions from last 6 months
 */
data class HistoricResult(
    val submissionId: String,
    val testType: TestType,
    val submittedAt: Long,
    val overallScore: Float?,
    val rating: String?,
    val isArchived: Boolean = false
) {
    /**
     * Format submitted date for display
     */
    fun getFormattedDate(): String {
        val instant = java.time.Instant.ofEpochMilli(submittedAt)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return formatter.format(instant.atZone(java.time.ZoneId.systemDefault()))
    }
    
    /**
     * Get days since submission
     */
    fun getDaysSinceSubmission(): Long {
        val now = System.currentTimeMillis()
        return (now - submittedAt) / (1000 * 60 * 60 * 24)
    }
    
    /**
     * Check if result is recent (within 7 days)
     */
    val isRecent: Boolean
        get() = getDaysSinceSubmission() <= 7
}
