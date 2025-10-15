package com.ssbmax.core.domain.model

/**
 * User domain model
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val isPremium: Boolean = false
)

/**
 * Authentication state
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

