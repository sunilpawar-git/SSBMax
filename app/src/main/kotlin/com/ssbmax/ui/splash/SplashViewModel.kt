package com.ssbmax.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation events from splash screen
 */
sealed class SplashNavigationEvent {
    data object NavigateToLogin : SplashNavigationEvent()
    data object NavigateToStudentHome : SplashNavigationEvent()
    data object NavigateToInstructorHome : SplashNavigationEvent()
    data object NavigateToRoleSelection : SplashNavigationEvent()
    data object NavigateToProfileOnboarding : SplashNavigationEvent()
}

/**
 * Splash screen ViewModel
 * Checks authentication state and navigates accordingly
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: com.ssbmax.core.domain.repository.AuthRepository,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository
) : ViewModel() {
    
    private val _navigationEvent = MutableStateFlow<SplashNavigationEvent?>(null)
    val navigationEvent: StateFlow<SplashNavigationEvent?> = _navigationEvent.asStateFlow()
    
    init {
        checkAuthenticationState()
    }
    
    private fun checkAuthenticationState() {
        viewModelScope.launch {
            try {
                android.util.Log.d("SplashViewModel", "Starting authentication check...")
                val startTime = System.currentTimeMillis()

                // Check authentication first (fast for returning users)
                val user = authRepository.currentUser.first()
                android.util.Log.d("SplashViewModel", "Current user: ${user?.email ?: "null"}")

                // Ensure minimum splash time for branding, but shorter if already authenticated
                val elapsedTime = System.currentTimeMillis() - startTime
                val minSplashTime = if (user != null) 800L else 2000L  // Faster for logged-in users
                val remainingDelay = (minSplashTime - elapsedTime).coerceAtLeast(0)
                if (remainingDelay > 0) {
                    delay(remainingDelay)
                }

                if (user == null) {
                    android.util.Log.d("SplashViewModel", "No user found, navigating to login")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
                    return@launch
                }

                // Check if profile is complete
                val hasProfile = userProfileRepository.hasCompletedProfile(user.id).first()
                android.util.Log.d("SplashViewModel", "User has completed profile: $hasProfile")

                if (!hasProfile) {
                    android.util.Log.d("SplashViewModel", "Profile incomplete, navigating to onboarding")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToProfileOnboarding
                    return@launch
                }

                // Navigate based on user role
                when {
                    user.role == UserRole.STUDENT -> {
                        android.util.Log.d("SplashViewModel", "User is STUDENT, navigating to student home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
                    }
                    user.role == UserRole.INSTRUCTOR -> {
                        android.util.Log.d("SplashViewModel", "User is INSTRUCTOR, navigating to instructor home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToInstructorHome
                    }
                    user.role == UserRole.BOTH -> {
                        android.util.Log.d("SplashViewModel", "User has BOTH roles, navigating to role selection")
                        // User can be both student and instructor
                        // Navigate to role selection or last used role
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                    else -> {
                        android.util.Log.d("SplashViewModel", "User role unknown, navigating to role selection")
                        // New user, needs to select role
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to check authentication state")
                // Navigate to login on ANY error (per user decision)
                android.util.Log.d("SplashViewModel", "Error during authentication check, navigating to login")
                _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
            }
        }
    }
}

