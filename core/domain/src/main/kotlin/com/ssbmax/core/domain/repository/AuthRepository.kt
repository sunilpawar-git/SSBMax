package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication
 */
interface AuthRepository {
    
    /**
     * Current authenticated user
     */
    val currentUser: Flow<User?>
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Sign up with email and password
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
}

