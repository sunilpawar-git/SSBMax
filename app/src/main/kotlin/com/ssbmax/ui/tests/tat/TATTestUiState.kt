package com.ssbmax.ui.tests.tat

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TATPhase
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TATStoryResponse
import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.TATTestConfig

data class TATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<TATQuestion> = emptyList(),
    val config: TATTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<TATStoryResponse> = emptyList(),
    val currentStory: String = "",
    val phase: TATPhase = TATPhase.INSTRUCTIONS,
    val viewingTimeRemaining: Int = 30,
    val writingTimeRemaining: Int = 240,
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: TATSubmission? = null,
    val error: String? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L   // generation token for timer race prevention
) {
    val currentQuestion: TATQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val completedStories: Int
        get() = responses.size

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedStories.toFloat() / questions.size)

    val canMoveToNextQuestion: Boolean
        get() = when (phase) {
            TATPhase.WRITING -> currentStory.length >= (currentQuestion?.minCharacters ?: 150) &&
                                currentStory.length <= (currentQuestion?.maxCharacters ?: 1500)
            TATPhase.REVIEW -> true
            else -> false
        }

    val canSubmitTest: Boolean
        get() = completedStories >= questions.size
}
