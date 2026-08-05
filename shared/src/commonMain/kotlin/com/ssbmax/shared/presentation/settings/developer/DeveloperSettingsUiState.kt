package com.ssbmax.shared.presentation.settings.developer

import com.ssbmax.shared.domain.model.SubscriptionOverride

/**
 * UI state for [com.ssbmax.shared.ui.settings.DeveloperSection].
 *
 * @param isVisible whether this is a debug build -- read once at [DeveloperSettingsViewModel]
 *   construction, since the build type can't change at runtime.
 */
data class DeveloperSettingsUiState(
    val isVisible: Boolean = false,
    val override: SubscriptionOverride = SubscriptionOverride.FOLLOW_REAL,
    val bypassInterviewPrerequisites: Boolean = false
)
