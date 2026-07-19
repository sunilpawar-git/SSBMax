package com.ssbmax.shared.presentation.auth

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Koin-injected ViewModel for the Phase 0 KMP spike's login slice.
 *
 * Not `androidx.lifecycle.ViewModel` (Android/JVM-only) — a plain class with
 * its own CoroutineScope, matching the KMP-portable ViewModel pattern.
 * `close()` cancels the scope; Koin can call it via a factory-scoped
 * `onClose` or the platform layer's lifecycle owner in later phases. Still
 * follows the repo's StateFlow conventions: private MutableStateFlow,
 * `.update {}` for thread-safe mutation, expose read-only StateFlow.
 */
class AuthViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(data: GoogleSignInData) {
        scope.launch {
            _uiState.update { AuthUiState.Loading }
            signInWithGoogleUseCase(data)
                .onSuccess { user -> _uiState.update { AuthUiState.Success(user) } }
                .onFailure { error ->
                    _uiState.update { AuthUiState.Error(error.message ?: "Sign-in failed") }
                }
        }
    }

    fun close() {
        scope.cancel()
    }
}

sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: SSBMaxUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
