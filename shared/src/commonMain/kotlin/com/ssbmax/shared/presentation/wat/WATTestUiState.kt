package com.ssbmax.shared.presentation.wat

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.WATSubmission
import com.ssbmax.shared.domain.model.WATTestConfig
import com.ssbmax.shared.domain.model.WATWord
import com.ssbmax.shared.domain.model.WATWordResponse
import com.ssbmax.shared.presentation.common.TestError

/**
 * KMP port of `app/.../ui/tests/wat/WATTestViewModel.kt`'s `WATTestUiState`.
 * Field-for-field match with the Android original, minus the phase enum
 * (reused verbatim from `WATTest.kt`'s existing `WATPhase` -- unlike TAT/PPDT,
 * WAT has no separate viewing phase, so this state is simpler: just
 * INSTRUCTIONS -> IN_PROGRESS -> COMPLETED -> SUBMITTED, one timer per word).
 */
data class WATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val words: List<WATWord> = emptyList(),
    val config: WATTestConfig? = null,
    val currentWordIndex: Int = 0,
    val responses: List<WATWordResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: com.ssbmax.shared.domain.model.WATPhase = com.ssbmax.shared.domain.model.WATPhase.INSTRUCTIONS,
    val timeRemaining: Int = 15,
    val startTime: Long = 0L,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionTier? = null,
    val submission: WATSubmission? = null,
    val error: TestError? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L // generation token for timer race prevention
) {
    val currentWord: WATWord?
        get() = words.getOrNull(currentWordIndex)

    val completedWords: Int
        get() = responses.size

    val progress: Float
        get() = if (words.isEmpty()) 0f else (completedWords.toFloat() / words.size)
}
