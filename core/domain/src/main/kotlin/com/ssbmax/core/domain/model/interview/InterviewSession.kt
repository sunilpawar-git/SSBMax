package com.ssbmax.core.domain.model.interview

import java.time.Instant

/**
 * Interview session metadata and state
 *
 * @param id Unique session identifier
 * @param userId User who started the interview
 * @param mode Interview mode (text or voice)
 * @param status Current session status
 * @param startedAt Session start timestamp
 * @param completedAt Session completion timestamp (null if in progress)
 * @param piqSnapshotId Reference to the PIQ data used for question generation
 * @param consentGiven Whether user consented to transcript storage
 * @param questionIds List of question IDs presented in this session
 * @param currentQuestionIndex Current question being answered (0-based)
 * @param estimatedDuration Estimated total duration in minutes
 */
data class InterviewSession(
    val id: String,
    val userId: String,
    val mode: InterviewMode,
    val status: InterviewStatus,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val piqSnapshotId: String,
    val consentGiven: Boolean,
    val questionIds: List<String>,
    val currentQuestionIndex: Int = 0,
    val estimatedDuration: Int = 30 // 30 minutes default
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(piqSnapshotId.isNotBlank()) { "PIQ snapshot ID cannot be blank" }
        require(questionIds.isNotEmpty()) { "Session must have at least one question" }
        require(currentQuestionIndex >= 0) { "Current question index cannot be negative" }
        require(estimatedDuration > 0) { "Estimated duration must be positive" }

        // Completed sessions must have a completion timestamp
        if (status == InterviewStatus.COMPLETED) {
            require(completedAt != null) { "Completed session must have completion timestamp" }
        }
    }

    /**
     * Check if analysis is pending
     */
    fun isPendingAnalysis(): Boolean = status == InterviewStatus.PENDING_ANALYSIS

    /**
     * Calculate session duration in seconds
     */
    fun getDurationSeconds(): Long {
        val endTime = completedAt ?: Instant.now()
        return endTime.epochSecond - startedAt.epochSecond
    }

    /**
     * Calculate progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        if (questionIds.isEmpty()) return 0
        return ((currentQuestionIndex.toFloat() / questionIds.size) * 100).toInt()
    }

    /**
     * Check if session is active
     */
    fun isActive(): Boolean = status == InterviewStatus.IN_PROGRESS

    /**
     * Check if session is completed
     */
    fun isCompleted(): Boolean = status == InterviewStatus.COMPLETED
}

/**
 * Interview session status
 */
enum class InterviewStatus {
    /**
     * Interview is currently in progress
     */
    IN_PROGRESS,

    /**
     * Responses saved, awaiting background AI analysis
     */
    PENDING_ANALYSIS,

    /**
     * Interview completed successfully with results ready
     */
    COMPLETED,

    /**
     * Interview was abandoned before completion
     */
    ABANDONED,

    /**
     * AI analysis failed - can be retried
     */
    FAILED;

    val displayName: String
        get() = when (this) {
            IN_PROGRESS -> "In Progress"
            PENDING_ANALYSIS -> "Analyzing..."
            COMPLETED -> "Completed"
            ABANDONED -> "Abandoned"
            FAILED -> "Failed"
        }

    /**
     * Check if results are ready to view
     */
    fun isResultsReady(): Boolean = this == COMPLETED

    /**
     * Check if analysis is still pending
     */
    fun isPending(): Boolean = this == PENDING_ANALYSIS
}
