package com.ssbmax.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}

/**
 * Splash screen ViewModel
 * Checks authentication state and navigates accordingly
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    // TODO: Inject Firebase Auth and User Repository
) : ViewModel() {
    
    private val _navigationEvent = MutableStateFlow<SplashNavigationEvent?>(null)
    val navigationEvent: StateFlow<SplashNavigationEvent?> = _navigationEvent.asStateFlow()
    
    init {
        checkAuthenticationState()
    }
    
    private fun checkAuthenticationState() {
        viewModelScope.launch {
            // Show splash for minimum 2 seconds for branding
            delay(2000)
            
            // TODO: Replace with actual Firebase Auth check
            val isAuthenticated = false // FirebaseAuth.getInstance().currentUser != null
            
            if (!isAuthenticated) {
                _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
                return@launch
            }
            
            // TODO: Fetch user from repository
            // val user = userRepository.getCurrentUser()
            
            // For now, mock user role checking
            val mockUserRole = UserRole.STUDENT // This should come from repository
            
            when {
                mockUserRole == UserRole.STUDENT -> {
                    _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
                }
                mockUserRole == UserRole.INSTRUCTOR -> {
                    _navigationEvent.value = SplashNavigationEvent.NavigateToInstructorHome
                }
                mockUserRole == UserRole.BOTH -> {
                    // User can be both student and instructor
                    // Navigate to role selection or last used role
                    _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                }
                else -> {
                    // New user, needs to select role
                    _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                }
            }
        }
    }
}

