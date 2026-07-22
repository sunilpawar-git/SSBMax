package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.InterviewSession
import kotlinx.datetime.Clock

/**
 * Pending response stored locally during interview (before AI analysis).
 * `System.currentTimeMillis()` (JVM-only) -> `Clock.System.now().toEpochMilliseconds()`.
 */
data class PendingResponse(
    val questionId: String,
    val questionText: String,
    val responseText: String,
    val thinkingTimeSec: Int,
    val respondedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * KMP port of `app/.../ui/interview/session/InterviewSessionUiState.kt`.
 * Field-for-field equivalent; `getThinkingTimeSeconds()`'s
 * `System.currentTimeMillis()` -> `Clock.System.now().toEpochMilliseconds()`.
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
    val isResultPending: Boolean = false,
    val pendingResponses: List<PendingResponse> = emptyList(),
    val isTTSSpeaking: Boolean = false,
    val isTTSReady: Boolean = false,
    val isTTSMuted: Boolean = false
) {
    val mode: InterviewMode
        get() = session?.mode ?: InterviewMode.VOICE_BASED

    fun getProgressPercentage(): Int {
        if (totalQuestions == 0) return 0
        return ((currentQuestionIndex.toFloat() / totalQuestions) * 100).toInt()
    }

    fun canSubmitResponse(): Boolean {
        return responseText.trim().isNotBlank() &&
            !isSubmittingResponse &&
            !isLoading &&
            currentQuestion != null
    }

    fun hasMoreQuestions(): Boolean = currentQuestionIndex < totalQuestions - 1

    fun getThinkingTimeSeconds(): Int {
        val startTime = thinkingStartTime ?: return 0
        return ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).toInt()
    }

    fun canEditResponse(): Boolean = !isSubmittingResponse && !isLoading && currentQuestion != null
}
