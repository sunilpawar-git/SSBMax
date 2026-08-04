package com.ssbmax.shared.domain.model.results

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import com.ssbmax.shared.domain.model.TestType

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
     *
     * Builds "MMM dd, yyyy" manually rather than via kotlinx-datetime's format DSL —
     * simpler and avoids DSL-version API uncertainty for a one-off display string.
     */
    fun getFormattedDate(): String {
        val localDateTime = Instant.fromEpochMilliseconds(submittedAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val month = MONTH_ABBREVIATIONS[localDateTime.month.ordinal]
        val day = localDateTime.day.toString().padStart(2, '0')
        return "$month $day, ${localDateTime.year}"
    }

    companion object {
        private val MONTH_ABBREVIATIONS = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }

    /**
     * Get days since submission
     */
    fun getDaysSinceSubmission(): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return (now - submittedAt) / (1000 * 60 * 60 * 24)
    }
    
    /**
     * Check if result is recent (within 7 days)
     */
    val isRecent: Boolean
        get() = getDaysSinceSubmission() <= 7
}
