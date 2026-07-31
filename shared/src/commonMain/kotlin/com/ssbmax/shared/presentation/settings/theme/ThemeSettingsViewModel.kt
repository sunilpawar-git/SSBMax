package com.ssbmax.shared.presentation.settings.theme

import com.ssbmax.shared.domain.model.AppTheme
import com.ssbmax.shared.platform.settings.AppThemeSettings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/settings/theme/ThemeSettingsViewModel.kt`.
 *
 * Phase 1 of the KMP-convergence plan: a real `androidx.lifecycle.ViewModel`
 * using `viewModelScope`, converged with `app`'s existing ViewModel idiom and
 * this module's own DI (`viewModelOf`) / screen (`koinViewModel()`)
 * conventions — no more manual `CoroutineScope` + `close()`. Reads/writes the
 * theme choice via [AppThemeSettings] (the Phase 4 platform shim that
 * replaced the old Android-only `ThemePreferenceManager`).
 *
 * Behavior (persisted theme choice, reactive updates) is unchanged from the
 * Android original.
 */
class ThemeSettingsViewModel(
    private val appThemeSettings: AppThemeSettings
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemeSettingsUiState(appTheme = appThemeSettings.getTheme()))
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    init {
        observeThemeChanges()
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            appThemeSettings.themeFlow.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    fun updateTheme(theme: AppTheme) {
        appThemeSettings.setTheme(theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
