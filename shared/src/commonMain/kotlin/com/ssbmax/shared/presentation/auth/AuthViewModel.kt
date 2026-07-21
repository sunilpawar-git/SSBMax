package com.ssbmax.shared.presentation.auth

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.shared.domain.usecase.auth.SignOutUseCase
import com.ssbmax.shared.domain.usecase.auth.UpdateUserRoleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Koin-injected ViewModel for the auth vertical (Login + RoleSelection),
 * fleshed out in Phase 5 from the Phase 0 spike's login-only stub — full
 * feature parity with the pre-KMP `app/.../ui/auth/AuthViewModel.kt`
 * (role-selection detection, `setUserRole`, `signOut`), minus its
 * Android-`Intent`-specific plumbing.
 *
 * Not `androidx.lifecycle.ViewModel` (Android/JVM-only) — a plain class with
 * its own CoroutineScope, matching the KMP-portable ViewModel pattern this
 * codebase already established here in Phase 0 and reused by
 * [com.ssbmax.shared.presentation.splash.SplashViewModel]. Compose call
 * sites use `koinInject()`, not `koinViewModel()` (the KMP `lifecycle-viewmodel`
 * artifact is a commonMain dependency of `:shared`, but no screen in this
 * codebase uses it yet -- conforms to the established convention rather
 * than introducing a second ViewModel pattern; see this phase's exit report).
 *
 * [GoogleSignInLauncher] is deliberately NOT a constructor dependency (see
 * that interface's class doc) -- `LoginScreen` obtains the platform-specific
 * [GoogleSignInData] itself (via `LocalGoogleSignInLauncher`) and hands it to
 * [handleGoogleSignInResult] as a plain parameter.
 */
class AuthViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Handle the result of the platform's Google Sign-In flow (obtained via
     * `GoogleSignInLauncher.signIn()` at the call site). Ported from the
     * Android original's `handleGoogleSignInResult`, minus the
     * Android-`Intent`-unwrapping step (the launcher already produces
     * platform-agnostic [GoogleSignInData]).
     */
    fun handleGoogleSignInResult(data: GoogleSignInData) {
        when (data) {
            is GoogleSignInData.Cancelled -> _uiState.update { AuthUiState.Initial }
            is GoogleSignInData.Error -> {
                _uiState.update { AuthUiState.Error(data.message) }
            }
            else -> scope.launch {
                _uiState.update { AuthUiState.Loading }
                signInWithGoogleUseCase(data)
                    .onSuccess { user ->
                        _uiState.update {
                            if (needsRoleSelection(user)) {
                                AuthUiState.NeedsRoleSelection(user)
                            } else {
                                AuthUiState.Success(user)
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { AuthUiState.Error(error.message ?: "Google Sign-In failed") }
                    }
            }
        }
    }

    /**
     * A brand-new user (never logged in before `createdAt == lastLoginAt`)
     * defaults to STUDENT until they explicitly pick a role -- same
     * heuristic as the Android original.
     */
    private fun needsRoleSelection(user: SSBMaxUser): Boolean =
        user.role == UserRole.STUDENT && user.createdAt == user.lastLoginAt

    /** Set user role after Google Sign-In (role-selection screen). */
    fun setUserRole(role: UserRole) {
        scope.launch {
            _uiState.update { AuthUiState.Loading }
            updateUserRoleUseCase(role)
                .onSuccess {
                    val user = observeCurrentUserUseCase().first()
                    _uiState.update {
                        if (user != null) {
                            AuthUiState.Success(user)
                        } else {
                            AuthUiState.Error("Failed to load updated user")
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { AuthUiState.Error(error.message ?: "Failed to set role") }
                }
        }
    }

    /** Sign out the current user. */
    fun signOut() {
        scope.launch {
            signOutUseCase()
            _uiState.update { AuthUiState.Initial }
        }
    }

    /** Reset UI state (e.g. after showing an error). */
    fun resetState() {
        _uiState.update { AuthUiState.Initial }
    }

    fun close() {
        scope.cancel()
    }
}

sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: SSBMaxUser) : AuthUiState()
    data class NeedsRoleSelection(val user: SSBMaxUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
