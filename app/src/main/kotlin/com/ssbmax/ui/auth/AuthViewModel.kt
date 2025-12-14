package com.ssbmax.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.GoogleSignInData
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.model.getPlatformData
import com.ssbmax.core.domain.usecase.auth.GetGoogleSignInIntentUseCase
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.core.domain.usecase.auth.SignOutUseCase
import com.ssbmax.core.domain.usecase.auth.UpdateUserRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens with Firebase integration
 * 
 * This ViewModel handles the conversion between Android-specific Intent objects
 * and platform-agnostic GoogleSignInData for the domain layer.
 * 
 * REFACTORED: Now uses use cases instead of direct repository implementation injection
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getGoogleSignInIntent: GetGoogleSignInIntentUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val updateUserRole: UpdateUserRoleUseCase,
    private val signOut: SignOutUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    /**
     * Get Google Sign-In intent for launching authentication flow
     * 
     * Extracts Android Intent from platform-agnostic GoogleSignInData
     */
    fun getGoogleSignInIntent(): Intent {
        val launchData = getGoogleSignInIntent.invoke()
        return launchData.getPlatformData<Intent>() 
            ?: throw IllegalStateException("Failed to get Android Intent from GoogleSignInData")
    }
    
    /**
     * Handle Google Sign-In result from Android activity
     * 
     * Wraps Android Intent in platform-agnostic GoogleSignInData before
     * passing to domain layer use case.
     * 
     * @param data Intent returned from Google Sign-In activity, or null if cancelled
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "handleGoogleSignInResult called, data=$data")
            _uiState.value = AuthUiState.Loading
            
            // Wrap Intent in platform-agnostic GoogleSignInData
            val signInData = if (data != null) {
                GoogleSignInData.ResultData(platformData = data)
            } else {
                GoogleSignInData.Cancelled
            }
            
            signInWithGoogle(signInData)
                .onSuccess { user ->
                    android.util.Log.d("AuthViewModel", "Sign-in SUCCESS: user=${user.email}, role=${user.role}")
                    // User authenticated successfully
                    // Check if user needs to select role (first time login)
                    if (user.role == UserRole.STUDENT && user.createdAt == user.lastLoginAt) {
                        // New user, might want to offer role selection
                        android.util.Log.d("AuthViewModel", "New user detected, showing role selection")
                        _uiState.value = AuthUiState.NeedsRoleSelection(user)
                    } else {
                        // Existing user, proceed
                        android.util.Log.d("AuthViewModel", "Existing user, proceeding to home")
                        _uiState.value = AuthUiState.Success(user)
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("AuthViewModel", "Sign-in FAILED: ${error.message}", error)
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
            
            updateUserRole(role)
                .onSuccess {
                    // Reload user to get updated profile
                    val user = observeCurrentUser().first()
                    if (user != null) {
                        _uiState.value = AuthUiState.Success(user)
                    } else {
                        _uiState.value = AuthUiState.Error("Failed to load updated user")
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
            signOut.invoke()
            _uiState.value = AuthUiState.Initial
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = AuthUiState.Initial
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
