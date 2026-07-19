package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Ported (trimmed) from core/domain/repository/AuthRepository.kt for the
 * Phase 0 KMP spike. Email/password sign-in and role update dropped — the
 * existing Android impl doesn't implement email/password either (always
 * returns Result.failure), so it wasn't real behavior to preserve.
 */
interface AuthRepository {
    val currentUser: StateFlow<SSBMaxUser?>

    suspend fun handleGoogleSignInResult(data: GoogleSignInData): Result<SSBMaxUser>

    suspend fun signOut(): Result<Unit>

    suspend fun isAuthenticated(): Boolean
}
