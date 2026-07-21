package com.ssbmax.shared.platform.settings

import com.russhwolf.settings.Settings
import com.ssbmax.shared.domain.model.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads/writes the user's chosen app theme via [Settings], and exposes it as
 * a [StateFlow] for reactive UI updates.
 *
 * Replaces the Android-only `core:data` `ThemePreferenceManager` (raw
 * `SharedPreferences`) — same behavior, now backed by a KMP-portable store.
 */
class AppThemeSettings(private val settings: Settings) {

    private val _themeFlow = MutableStateFlow(getTheme())
    val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()

    fun getTheme(): AppTheme {
        val themeName = settings.getStringOrNull(KEY_THEME) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        settings.putString(KEY_THEME, theme.name)
        _themeFlow.value = theme
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
