package com.ssbmax.ui.settings.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.shared.domain.model.AppTheme
import com.ssbmax.shared.platform.settings.AppThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Theme Settings
 * Handles theme preference management independently from main SettingsViewModel
 */
class ThemeSettingsViewModel(
    private val appThemeSettings: AppThemeSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    // Lifecycle-aware theme Flow - automatically starts/stops with collectors
    private val themeFlow = appThemeSettings.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    init {
        observeThemeChanges()
    }

    /**
     * Observe theme changes from ThemePreferenceManager
     */
    private fun observeThemeChanges() {
        viewModelScope.launch {
            themeFlow.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    /**
     * Update the app theme
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            appThemeSettings.setTheme(theme)
            _uiState.update { it.copy(appTheme = theme) }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}




