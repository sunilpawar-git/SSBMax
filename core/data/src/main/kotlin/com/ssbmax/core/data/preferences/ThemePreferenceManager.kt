package com.ssbmax.core.data.preferences

import android.content.Context
import com.ssbmax.core.domain.model.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app theme preferences using SharedPreferences
 * Provides a Flow for reactive theme changes
 */
@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
    
    private val _themeFlow = MutableStateFlow(getTheme())
    val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()
    
    fun getTheme(): AppTheme {
        val themeName = prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }
    
    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _themeFlow.value = theme
    }
}

