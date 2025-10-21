package com.ssbmax

import androidx.lifecycle.ViewModel
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.ui.theme.ThemeState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {
    
    val themeState: ThemeState = ThemeState(themePreferenceManager.getTheme())
    
    init {
        // Observe theme changes
        themePreferenceManager.themeFlow.value.let { theme ->
            themeState.updateTheme(theme)
        }
    }
}

