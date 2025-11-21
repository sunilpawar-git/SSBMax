package com.ssbmax.ui.interview.session

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession

/**
 * UI state for Interview Session screen
 *
 * Manages question display, response input, and session progress
 */
data class InterviewSessionUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val session: InterviewSession? = null,
    val currentQuestion: InterviewQuestion? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val responseText: String = "",
    val isSubmittingResponse: Boolean = false,
    val thinkingStartTime: Long? = null,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val resultId: String? = null
) {
    /**
     * Get interview mode
     */
    val mode: InterviewMode
        get() = session?.mode ?: InterviewMode.TEXT_BASED

    /**
     * Calculate progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        if (totalQuestions == 0) return 0
        return ((currentQuestionIndex.toFloat() / totalQuestions) * 100).toInt()
    }

    /**
     * Check if response is ready to submit
     */
    fun canSubmitResponse(): Boolean {
        return responseText.trim().isNotBlank() &&
                !isSubmittingResponse &&
                !isLoading &&
                currentQuestion != null
    }

    /**
     * Check if there are more questions
     */
    fun hasMoreQuestions(): Boolean {
        return currentQuestionIndex < totalQuestions - 1
    }

    /**
     * Get thinking time in seconds
     */
    fun getThinkingTimeSeconds(): Int {
        val startTime = thinkingStartTime ?: return 0
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }
}
