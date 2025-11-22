package com.ssbmax.ui.settings.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Theme Settings
 * Handles theme preference management independently from main SettingsViewModel
 */
@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    // Lifecycle-aware theme Flow - automatically starts/stops with collectors
    private val themeFlow = themePreferenceManager.themeFlow
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
            themePreferenceManager.setTheme(theme)
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




