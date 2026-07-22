package com.ssbmax.shared.presentation.settings.notifications

import com.ssbmax.shared.domain.model.NotificationPreferences

/**
 * UI State for Notification Settings
 * Represents the state of notification preferences in the settings screen
 */
data class NotificationSettingsUiState(
    val isLoading: Boolean = false,
    val notificationPreferences: NotificationPreferences? = null,
    val error: String? = null
)
