package com.ssbmax.shared.presentation.settings.theme

import com.ssbmax.shared.domain.model.AppTheme

/**
 * UI state for Theme Settings
 */
data class ThemeSettingsUiState(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val error: String? = null
)
