package com.ssbmax.ui.interview.start

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import java.time.Instant

/**
 * UI state for Start Interview screen
 *
 * Handles prerequisite checking, session creation, and past results display
 */
data class StartInterviewUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val isGeneratingQuestions: Boolean = false,
    val selectedMode: InterviewMode = InterviewMode.TEXT_BASED,
    val prerequisiteResult: PrerequisiteCheckResult? = null,
    val isEligible: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null,
    val isSessionCreated: Boolean = false,
    // Past interview results
    val pastResults: List<InterviewResult> = emptyList(),
    val pendingSessions: List<PendingInterviewSession> = emptyList(),
    val isLoadingHistory: Boolean = false
) {
    /** Check if we're ready to start the interview */
    fun canStartInterview(): Boolean = isEligible && !isLoading && !isGeneratingQuestions && error == null

    /** Get user-friendly failure reasons */
    fun getFailureReasons(): List<String> = prerequisiteResult?.failureReasons ?: emptyList()

    /** Check if there are any past interviews to show */
    fun hasPastInterviews(): Boolean = pastResults.isNotEmpty() || pendingSessions.isNotEmpty()
}

/**
 * Represents an interview session that's still being analyzed
 */
data class PendingInterviewSession(
    val sessionId: String,
    val status: InterviewStatus,
    val startedAt: Instant,
    val questionCount: Int
)
