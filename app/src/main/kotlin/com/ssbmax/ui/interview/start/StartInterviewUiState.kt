package com.ssbmax.ui.interview.start

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult

/**
 * UI state for Start Interview screen
 *
 * Handles prerequisite checking and session creation
 */
data class StartInterviewUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val selectedMode: InterviewMode = InterviewMode.TEXT_BASED,
    val prerequisiteResult: PrerequisiteCheckResult? = null,
    val isEligible: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null,
    val isSessionCreated: Boolean = false
) {
    /**
     * Check if we're ready to start the interview
     */
    fun canStartInterview(): Boolean {
        return isEligible && !isLoading && error == null
    }

    /**
     * Get user-friendly failure reasons
     */
    fun getFailureReasons(): List<String> {
        return prerequisiteResult?.failureReasons ?: emptyList()
    }
}
