package com.ssbmax.ui.tests.ppdt

import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.PPDTTestSession
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.SubscriptionType

data class PPDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val error: String? = null,
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
    val subscriptionType: SubscriptionType? = null,
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

