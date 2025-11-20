package com.ssbmax.core.domain.repository

import android.content.Intent
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication
 */
interface AuthRepository {

    /**
     * Current authenticated user as StateFlow for lifecycle-aware collection
     */
    val currentUser: StateFlow<SSBMaxUser?>

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<SSBMaxUser>

    /**
     * Sign up with email and password
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<SSBMaxUser>

    /**
     * Get Google Sign-In intent for launching sign-in flow
     */
    fun getGoogleSignInIntent(): Intent

    /**
     * Handle Google Sign-In result after user completes sign-in
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<SSBMaxUser>

    /**
     * Update user role (Student or Instructor)
     */
    suspend fun updateUserRole(role: UserRole): Result<Unit>

    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
}
