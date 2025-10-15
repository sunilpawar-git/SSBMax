package com.ssbmax.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.User
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
    
    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        if (!validateInput(email, password)) {
            _uiState.value = AuthUiState.Error("Please enter valid email and password")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                }
        }
    }
    
    /**
     * Sign up with email, password, and display name
     */
    fun signUp(email: String, password: String, displayName: String) {
        if (!validateInput(email, password) || displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill all fields correctly")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            authRepository.signUp(email, password, displayName)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign up failed")
                }
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
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

