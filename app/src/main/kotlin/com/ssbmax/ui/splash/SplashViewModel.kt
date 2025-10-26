package com.ssbmax.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.UserRole
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
            android.util.Log.d("SplashViewModel", "Starting authentication check...")
            // Show splash for minimum 2 seconds for branding
            delay(2000)
            
            // Check authentication
            val user = authRepository.currentUser.first()
            android.util.Log.d("SplashViewModel", "Current user: ${user?.email ?: "null"}")
            
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
        }
    }
}

