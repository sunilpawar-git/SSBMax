package com.ssbmax.shared.presentation.settings.theme

import com.ssbmax.shared.domain.model.AppTheme
import com.ssbmax.shared.platform.settings.AppThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/settings/theme/ThemeSettingsViewModel.kt`.
 *
 * Same plain-class + own-`CoroutineScope` pattern as every other Phase 5
 * ViewModel. Reads/writes the theme choice via [AppThemeSettings] (the Phase 4
 * platform shim that replaced the old Android-only `ThemePreferenceManager`).
 *
 * Deviation from the Android original: the Android VM derived an extra
 * lifecycle-scoped `stateIn(...)` wrapper around `appThemeSettings.themeFlow`
 * purely to match `androidx.lifecycle.ViewModel`'s `viewModelScope`
 * lifecycle -- this plain-class ViewModel already owns its scope for exactly
 * as long as the screen is composed (cancelled in `close()`), so that
 * indirection is dropped and [uiState] collects `themeFlow` directly.
 * Behavior (persisted theme choice, reactive updates) is unchanged.
 */
class ThemeSettingsViewModel(
    private val appThemeSettings: AppThemeSettings
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(ThemeSettingsUiState(appTheme = appThemeSettings.getTheme()))
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    init {
        observeThemeChanges()
    }

    fun close() {
        scope.cancel()
    }

    private fun observeThemeChanges() {
        scope.launch {
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
