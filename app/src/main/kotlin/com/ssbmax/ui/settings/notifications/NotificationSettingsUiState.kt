package com.ssbmax.ui.settings.notifications

import com.ssbmax.core.domain.model.NotificationPreferences

/**
 * UI State for Notification Settings
 * Represents the state of notification preferences in the settings screen
 */
data class NotificationSettingsUiState(
    val isLoading: Boolean = false,
    val notificationPreferences: NotificationPreferences? = null,
    val error: String? = null
)
