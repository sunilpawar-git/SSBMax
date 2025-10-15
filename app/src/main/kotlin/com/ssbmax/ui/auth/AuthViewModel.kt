package com.ssbmax.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private var pendingUserId: String? = null
    private var pendingEmail: String? = null
    private var pendingDisplayName: String? = null
    
    /**
     * Sign in with Google
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // TODO: Implement actual Google Sign-In with Firebase
            // For now, mock the flow
            _uiState.value = AuthUiState.NeedsRoleSelection
            
            /* Actual implementation would be:
            authRepository.signInWithGoogle()
                .onSuccess { result ->
                    when (result) {
                        is AuthResult.Success -> {
                            _uiState.value = AuthUiState.Success(result.user)
                        }
                        is AuthResult.NeedsRoleSelection -> {
                            pendingUserId = result.userId
                            pendingEmail = result.email
                            pendingDisplayName = result.displayName
                            _uiState.value = AuthUiState.NeedsRoleSelection
                        }
                        is AuthResult.Error -> {
                            _uiState.value = AuthUiState.Error(result.message)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Google Sign-In failed")
                }
            */
        }
    }
    
    /**
     * Set user role after Google Sign-In
     */
    fun setUserRole(role: UserRole) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // TODO: Implement actual role setting
            // For now, mock success
            _uiState.value = AuthUiState.Success(
                SSBMaxUser(
                    id = "mock-user-id",
                    email = "user@example.com",
                    displayName = "Test User",
                    role = role
                )
            )
            
            /* Actual implementation:
            val userId = pendingUserId ?: return@launch
            val email = pendingEmail ?: return@launch
            val displayName = pendingDisplayName ?: return@launch
            
            authRepository.setUserRole(userId, role, email, displayName)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to set role")
                }
            */
        }
    }
    
    /**
     * Sign in with email and password (legacy, kept for compatibility)
     */
    @Deprecated("Use signInWithGoogle instead")
    fun signIn(email: String, password: String) {
        if (!validateInput(email, password)) {
            _uiState.value = AuthUiState.Error("Please enter valid email and password")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // TODO: Implement if needed
            _uiState.value = AuthUiState.Error("Email/Password sign-in not yet implemented. Please use Google Sign-In.")
        }
    }
    
    /**
     * Sign up with email, password, and display name (legacy)
     */
    @Deprecated("Use signInWithGoogle instead")
    fun signUp(email: String, password: String, displayName: String) {
        if (!validateInput(email, password) || displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill all fields correctly")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // TODO: Implement if needed
            _uiState.value = AuthUiState.Error("Email/Password sign-up not yet implemented. Please use Google Sign-In.")
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        return email.contains("@") && password.length >= 6
    }
}

/**
 * UI state for authentication
 */
sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: SSBMaxUser) : AuthUiState()
    data object NeedsRoleSelection : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

