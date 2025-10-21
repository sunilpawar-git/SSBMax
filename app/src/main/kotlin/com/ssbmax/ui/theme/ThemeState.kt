package com.ssbmax.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.ssbmax.core.domain.model.AppTheme

/**
 * Holds the current app theme state
 * Allows for dynamic theme switching without app restart
 */
@Stable
class ThemeState(
    initialTheme: AppTheme
) {
    var currentTheme by mutableStateOf(initialTheme)
        private set
    
    fun updateTheme(theme: AppTheme) {
        currentTheme = theme
    }
}

/**
 * CompositionLocal for accessing theme state
 */
val LocalThemeState = staticCompositionLocalOf<ThemeState> {
    error("No ThemeState provided")
}

