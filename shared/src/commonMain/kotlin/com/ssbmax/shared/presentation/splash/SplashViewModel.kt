package com.ssbmax.shared.presentation.splash

import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

/**
 * Navigation events from splash screen. Ported unchanged from the Android
 * original (app/.../ui/splash/SplashViewModel.kt) — pure sealed-class data,
 * zero Android dependency to begin with.
 */
sealed class SplashNavigationEvent {
    data object NavigateToLogin : SplashNavigationEvent()
    data object NavigateToStudentHome : SplashNavigationEvent()
    data object NavigateToInstructorHome : SplashNavigationEvent()
    data object NavigateToRoleSelection : SplashNavigationEvent()
    data object NavigateToProfileOnboarding : SplashNavigationEvent()
}

/**
 * Splash screen ViewModel — Phase 5 KMP port of the Android original.
 *
 * Plain class + own CoroutineScope, matching the KMP-portable ViewModel
 * pattern already established by [com.ssbmax.shared.presentation.auth.AuthViewModel]
 * in Phase 0 (NOT `androidx.lifecycle.ViewModel`), so Compose call sites use
 * `koinInject()`, not `koinViewModel()`.
 *
 * Two real Kotlin/Native-illegal APIs fixed during this port (both on this
 * plan's own "recurring compile gotchas" list):
 * - `System.currentTimeMillis()` (JVM-only) -> `Clock.System.now().toEpochMilliseconds()`.
 * - `android.util.Log.d(...)` (Android-only) -> injected [DomainLogger], the
 *   established cross-platform logging SSOT for `shared` (currently bound to
 *   a no-op impl in [com.ssbmax.shared.di.SharedModule] — a real platform
 *   logger is out of this port's scope, tracked as pre-existing).
 */
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "SplashViewModel"

    private val _navigationEvent = MutableStateFlow<SplashNavigationEvent?>(null)
    val navigationEvent: StateFlow<SplashNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        scope.launch {
            try {
                logger.d(tag, "Starting authentication check...")
                val startTime = Clock.System.now().toEpochMilliseconds()

                // Fast local Firebase check -- no Firestore round-trip, no network call
                val isAuthenticated = authRepository.isAuthenticated()
                logger.d(tag, "isAuthenticated: $isAuthenticated")

                val minSplashTime = if (isAuthenticated) 800L else 2000L
                val remainingDelay =
                    (minSplashTime - (Clock.System.now().toEpochMilliseconds() - startTime)).coerceAtLeast(0)
                if (remainingDelay > 0) delay(remainingDelay)

                if (!isAuthenticated) {
                    logger.d(tag, "No authenticated user, navigating to login")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
                    return@launch
                }

                // Wait for Firestore-backed user profile with a safety timeout.
                // currentUser emits null until callbackFlow resolves the Firebase auth state;
                // filterNotNull() skips the StateFlow initial null so we wait for the real value.
                val user = withTimeoutOrNull(5000L) {
                    authRepository.currentUser.filterNotNull().first()
                }
                logger.d(tag, "Resolved user: ${user?.email ?: "timeout/null"}")

                if (user == null) {
                    // Profile load timed out (e.g. no network) but user IS authenticated locally.
                    // Do NOT send them back to login -- navigate to home so they can use cached data.
                    logger.w(tag, "User profile timed out but isAuthenticated=true, navigating to student home")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
                    return@launch
                }

                // Check if profile is complete
                val hasProfile = userProfileRepository.hasCompletedProfile(user.id).first()
                logger.d(tag, "User has completed profile: $hasProfile")

                if (!hasProfile) {
                    logger.d(tag, "Profile incomplete, navigating to onboarding")
                    _navigationEvent.value = SplashNavigationEvent.NavigateToProfileOnboarding
                    return@launch
                }

                // Navigate based on user role
                when (user.role) {
                    UserRole.STUDENT -> {
                        logger.d(tag, "User is STUDENT, navigating to student home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToStudentHome
                    }
                    UserRole.INSTRUCTOR -> {
                        logger.d(tag, "User is INSTRUCTOR, navigating to instructor home")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToInstructorHome
                    }
                    UserRole.BOTH -> {
                        logger.d(tag, "User has BOTH roles, navigating to role selection")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                    else -> {
                        logger.d(tag, "User role unknown, navigating to role selection")
                        _navigationEvent.value = SplashNavigationEvent.NavigateToRoleSelection
                    }
                }
            } catch (e: Exception) {
                logger.e(tag, "Failed to check authentication state", e)
                _navigationEvent.value = SplashNavigationEvent.NavigateToLogin
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
