package com.ssbmax.shared.presentation.sdt

import com.ssbmax.shared.domain.model.SDTPhase
import com.ssbmax.shared.domain.model.SDTQuestion
import com.ssbmax.shared.domain.model.SDTQuestionResponse
import com.ssbmax.shared.domain.model.SDTSubmission
import com.ssbmax.shared.domain.model.SDTTestConfig
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.common.TestError

/**
 * KMP port of `app/.../ui/tests/sdt/SDTTestViewModel.kt`'s `SDTTestUiState`.
 * Field-for-field match with the Android original. Structurally distinct from
 * TAT/WAT/SRT: SDT is a small fixed set of 4 essay-style self-description
 * questions (not a rapid-fire timed sequence), answered against one
 * whole-test 15-minute countdown, with a REVIEW phase (any answer editable
 * via [SDTTestUiState] index) before an explicit final submit -- same
 * three-phase shape as SRT (INSTRUCTIONS -> IN_PROGRESS -> REVIEW ->
 * SUBMITTED), confirmed by reading the real Android `SDTTestViewModel`.
 */
data class SDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<SDTQuestion> = emptyList(),
    val config: SDTTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<SDTQuestionResponse> = emptyList(),
    val currentAnswer: String = "",
    val phase: SDTPhase = SDTPhase.INSTRUCTIONS,
    val totalTimeRemaining: Int = 900, // 15 minutes
    val startTime: Long = 0L,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionTier? = null,
    val submission: SDTSubmission? = null,
    val error: TestError? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L // generation token for timer race prevention
) {
    val currentQuestion: SDTQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val completedQuestions: Int
        get() = responses.size

    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedQuestions.toFloat() / questions.size)

    val currentCharCount: Int
        get() = currentAnswer.trim().length

    val canMoveToNext: Boolean
        get() {
            val minChars = config?.minCharsPerQuestion ?: 50
            val maxChars = config?.maxCharsPerQuestion ?: 1500
            return currentCharCount in minChars..maxChars
        }
}
