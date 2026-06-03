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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

                // Fast local Firebase check — no Firestore round-trip, no network call
                val isAuthenticated = authRepository.isAuthenticated()
                android.util.Log.d("SplashViewModel", "isAuthenticated: $isAuthenticated")

                val minSplashTime = if (isAuthenticated) 800L else 2000L
                val remainingDelay = (minSplashTime - (System.currentTimeMillis() - startTime)).coerceAtLeast(0)
                if (remainingDelay > 0) delay(remainingDelay)

                if (!isAuthenticated) {
                    android.util.Log.d("SplashViewModel", "No authenticated user, navigating to login")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
                    return@launch
                }

                // Wait for Firestore-backed user profile with a safety timeout.
                // currentUser emits null until callbackFlow resolves the Firebase auth state;
                // filterNotNull() skips the StateFlow initial null so we wait for the real value.
                val user = withTimeoutOrNull(5000L) {
                    authRepository.currentUser.filterNotNull().first()
                }
                android.util.Log.d("SplashViewModel", "Resolved user: ${user?.email ?: "timeout/null"}")

                if (user == null) {
                    // Profile load timed out (e.g. no network) but user IS authenticated locally.
                    // Do NOT send them back to login — navigate to home so they can use cached data.
                    android.util.Log.w("SplashViewModel", "User profile timed out but isAuthenticated=true, navigating to student home")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
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
                when (user.role) {
                    UserRole.STUDENT -> {
                        android.util.Log.d("SplashViewModel", "User is STUDENT, navigating to student home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
                    }
                    UserRole.INSTRUCTOR -> {
                        android.util.Log.d("SplashViewModel", "User is INSTRUCTOR, navigating to instructor home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToInstructorHome
                    }
                    UserRole.BOTH -> {
                        android.util.Log.d("SplashViewModel", "User has BOTH roles, navigating to role selection")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                    else -> {
                        android.util.Log.d("SplashViewModel", "User role unknown, navigating to role selection")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to check authentication state")
                android.util.Log.d("SplashViewModel", "Error during authentication check, navigating to login")
                _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
            }
        }
    }
}

