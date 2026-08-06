package com.ssbmax.shared.presentation.srt

import com.ssbmax.shared.domain.model.SRTPhase
import com.ssbmax.shared.domain.model.SRTSituation
import com.ssbmax.shared.domain.model.SRTSituationResponse
import com.ssbmax.shared.domain.model.SRTSubmission
import com.ssbmax.shared.domain.model.SRTTestConfig
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.common.TestError

/**
 * KMP port of `app/.../ui/tests/srt/SRTTestViewModel.kt`'s `SRTTestUiState`.
 * Field-for-field match with the Android original. Unlike WAT (one phase,
 * one timer per word, auto-submit), SRT keeps the Android original's
 * three-phase flow: INSTRUCTIONS -> IN_PROGRESS (60 situations, one 30-minute
 * countdown for the whole test, not per-situation) -> REVIEW (edit any
 * response before submitting) -> SUBMITTED. `isTimeUp` mirrors the Android
 * original's non-dismissible "Time's Up" dialog state that blocks the UI for
 * a 2s grace period before auto-submitting.
 */
data class SRTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val situations: List<SRTSituation> = emptyList(),
    val config: SRTTestConfig? = null,
    val currentSituationIndex: Int = 0,
    val responses: List<SRTSituationResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: SRTPhase = SRTPhase.INSTRUCTIONS,
    val startTime: Long = 0L,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionTier? = null,
    val submission: SRTSubmission? = null,
    val error: TestError? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val timeRemaining: Int = 1800, // 30 minutes
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L, // generation token for timer race prevention
    val isTimeUp: Boolean = false
) {
    val currentSituation: SRTSituation?
        get() = situations.getOrNull(currentSituationIndex)

    val completedSituations: Int
        get() = responses.size

    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }

    val progress: Float
        get() = if (situations.isEmpty()) 0f else (completedSituations.toFloat() / situations.size)

    val canMoveToNext: Boolean
        get() {
            val minLength = config?.minResponseLength ?: 0
            val maxLength = config?.maxResponseLength ?: 200
            return currentResponse.length in minLength..maxLength
        }
}
