package com.ssbmax.ui.tests.oir

import androidx.annotation.StringRes
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRTestResult
import com.ssbmax.core.domain.model.OIRTestSession
import com.ssbmax.core.domain.model.SubscriptionTier

/**
 * Immutable UI state for [OIRTestViewModel].
 *
 * All fields have safe defaults so the initial empty state renders the loading spinner.
 * The ViewModel emits copies of this class via [kotlinx.coroutines.flow.StateFlow]; the
 * Screen collects it with [androidx.lifecycle.compose.collectAsStateWithLifecycle].
 */
data class OIRTestUiState(
    val isLoading: Boolean = true,
    @StringRes val errorResId: Int? = null,
    val currentQuestion: OIRQuestion? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val selectedOptionId: String? = null,
    val showFeedback: Boolean = false,
    val isCurrentAnswerCorrect: Boolean = false,
    val currentQuestionAnswered: Boolean = false,
    val isCompleted: Boolean = false,
    val sessionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    /** Result calculated locally after submission — no extra Firestore read needed. */
    val testResult: OIRTestResult? = null,
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    /** Active test session kept in observable state for lifecycle-safe access. */
    val session: OIRTestSession? = null
)
