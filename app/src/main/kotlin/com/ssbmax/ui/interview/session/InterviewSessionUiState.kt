package com.ssbmax.ui.interview.session

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewSession

/**
 * Pending response stored locally during interview (before AI analysis)
 */
data class PendingResponse(
    val questionId: String,
    val questionText: String,
    val responseText: String,
    val thinkingTimeSec: Int,
    val respondedAt: Long = System.currentTimeMillis()
)

/**
 * UI state for Interview Session screen
 *
 * Manages question display, response input, and session progress
 *
 * OPTIMIZATION: Uses BACKGROUND AI analysis via WorkManager.
 * Responses are saved to Firestore immediately (without OLQ scores),
 * and AI analysis happens in the background. User is navigated away
 * instantly and notified when results are ready.
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
    val resultId: String? = null,
    // Background analysis: navigate away while AI processes
    val isResultPending: Boolean = false, // True when analysis is happening in background
    // Local response storage during interview
    val pendingResponses: List<PendingResponse> = emptyList(),
    val isAnalyzingResponses: Boolean = false,
    val analysisProgress: String? = null
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
