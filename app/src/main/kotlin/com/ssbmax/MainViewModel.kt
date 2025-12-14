package com.ssbmax

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.ui.theme.ThemeState
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    val themeState: ThemeState = ThemeState(themePreferenceManager.getTheme())
    
    init {
        // Observe theme changes
        themePreferenceManager.themeFlow.value.let { theme ->
            themeState.updateTheme(theme)
        }
        
        // Update login streak on app startup
        updateLoginStreak()
    }
    
    private fun updateLoginStreak() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser.first()
                if (currentUser != null) {
                    userProfileRepository.updateLoginStreak(currentUser.id)
                }
            } catch (e: Exception) {
                // Silently fail - streak update is not critical for app functionality
                ErrorLogger.log(e, "Failed to update login streak")
            }
        }
    }
}

