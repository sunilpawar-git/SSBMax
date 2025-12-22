package com.ssbmax.ui.tests.gto.lecturette

import com.ssbmax.core.domain.model.SubscriptionType

/**
 * Lecturette Test UI State
 */
data class LecturetteTestUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val error: String? = null,
    
    // Test info
    val testId: String = "",
    val userId: String = "",
    val topicChoices: List<String> = emptyList(),
    val selectedTopic: String = "",
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    
    // Phase management
    val phase: LecturettePhase = LecturettePhase.INSTRUCTIONS,
    
    // Speech phase
    val speechStartTime: Long = 0L,
    val timeRemaining: Int = 180, // 3 minutes
    val speechTranscript: String = "",
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
    
    val formattedTime: String
        get() {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            return "%d:%02d".format(minutes, seconds)
        }
    
    val isTimeLow: Boolean
        get() = timeRemaining < 30 && timeRemaining > 0
}

/**
 * Lecturette Test Phases
 */
enum class LecturettePhase {
    INSTRUCTIONS,       // Show test format and rules
    TOPIC_SELECTION,    // Show 4 topics, user picks 1
    SPEECH,             // 3-minute speech phase with white noise
    REVIEW,             // Review speech transcript
    SUBMITTED           // Test submitted
}
