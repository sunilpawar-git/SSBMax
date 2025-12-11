package com.ssbmax.ui.settings.theme

import com.ssbmax.core.domain.model.AppTheme

/**
 * UI state for Theme Settings
 */
data class ThemeSettingsUiState(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val error: String? = null
)




