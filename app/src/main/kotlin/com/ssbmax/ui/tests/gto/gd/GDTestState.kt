package com.ssbmax.ui.tests.gto.gd

import com.ssbmax.core.domain.model.SubscriptionType

/**
 * GD Test UI State
 */
data class GDTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    
    // Test info
    val testId: String = "",
    val userId: String = "",
    val topic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    
    // Phase management
    val phase: GDPhase = GDPhase.INSTRUCTIONS,
    
    // Discussion phase
    val discussionStartTime: Long = 0L,
    val timeRemaining: Int = 1200, // 20 minutes
    val response: String = "",
    val charCount: Int = 0,
    val validationError: String? = null,
    
    // Submission
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submissionId: String? = null,
    val isCompleted: Boolean = false,
    
    // Dialogs
    val showLimitDialog: Boolean = false,
    val limitMessage: String? = null,
    val showUpgradeDialog: Boolean = false,
    val upgradeMessage: String? = null
) {
    val meetsMinCharCount: Boolean
        get() = charCount >= 50
    
    val meetsMaxCharCount: Boolean
        get() = charCount <= 1500
    
    val meetsCharCountRequirements: Boolean
        get() = meetsMinCharCount && meetsMaxCharCount
    
    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    
    val isTimeLow: Boolean
        get() = timeRemaining < 120 && timeRemaining > 0
}

/**
 * GD Test Phases
 */
enum class GDPhase {
    INSTRUCTIONS,   // Show test format and rules
    DISCUSSION,     // Active discussion phase (20 min)
    REVIEW,         // Review response before submission
    SUBMITTED       // Test submitted
}
