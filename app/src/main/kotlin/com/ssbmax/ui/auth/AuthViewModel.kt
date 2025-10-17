package com.ssbmax.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.repository.AuthRepositoryImpl
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
 * ViewModel for authentication screens with Firebase integration
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authRepositoryImpl: AuthRepositoryImpl
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    /**
     * Get Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent {
        return authRepositoryImpl.getGoogleSignInIntent()
    }
    
    /**
     * Handle Google Sign-In result
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            authRepositoryImpl.handleGoogleSignInResult(data)
                .onSuccess { user ->
                    // User authenticated successfully
                    // Check if user needs to select role (first time login)
                    if (user.role == UserRole.STUDENT && user.createdAt == user.lastLoginAt) {
                        // New user, might want to offer role selection
                        _uiState.value = AuthUiState.NeedsRoleSelection(user)
                    } else {
                        // Existing user, proceed
                        _uiState.value = AuthUiState.Success(user)
                    }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Google Sign-In failed")
                }
        }
    }
    
    /**
     * Set user role after Google Sign-In
     */
    fun setUserRole(role: UserRole) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            authRepositoryImpl.updateUserRole(role)
                .onSuccess {
                    // Reload user to get updated profile
                    authRepository.currentUser.collect { user ->
                        if (user != null) {
                            _uiState.value = AuthUiState.Success(user)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to set role")
                }
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.Initial
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
    data class NeedsRoleSelection(val user: SSBMaxUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

