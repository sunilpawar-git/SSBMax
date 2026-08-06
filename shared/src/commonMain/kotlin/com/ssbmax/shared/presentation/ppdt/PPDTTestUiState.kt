package com.ssbmax.shared.presentation.ppdt

import com.ssbmax.shared.domain.model.PPDTPhase
import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.PPDTTestSession
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.common.TestError

/**
 * KMP port of `app/.../ui/tests/ppdt/PPDTTestUiState.kt`. Field-for-field
 * match with the Android original (see that file) so [PPDTTestViewModel]'s
 * logic is a near drop-in.
 */
data class PPDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val error: TestError? = null,
    val currentPhase: PPDTPhase = PPDTPhase.INSTRUCTIONS,
    val imageUrl: String = "",
    val story: String = "",
    val charactersCount: Int = 0,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,
    val timeRemainingSeconds: Int = 0,
    val canProceedToNextPhase: Boolean = false,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionTier? = null,
    val submission: PPDTSubmission? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val session: PPDTTestSession? = null,
    val isProfileIncomplete: Boolean = false
)
