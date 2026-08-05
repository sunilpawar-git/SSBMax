package com.ssbmax.shared.domain.model

/**
 * Authentication state
 */
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: SSBMaxUser) : AuthState()
    data class Error(val message: String) : AuthState()
    data object RequiresRoleSelection : AuthState()
}

/**
 * Authentication result
 */
sealed class AuthResult {
    data class Success(val user: SSBMaxUser) : AuthResult()
    data class NeedsRoleSelection(val userId: String, val email: String, val displayName: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

