package com.ssbmax.core.domain.usecase.auth

import android.content.Intent
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in with Google
 * Handles the Google Sign-In result after user completes authentication
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Handle Google Sign-In result
     * @param data Intent data from Google Sign-In activity result
     * @return Result containing authenticated user or error
     */
    suspend operator fun invoke(data: Intent?): Result<SSBMaxUser> {
        return authRepository.handleGoogleSignInResult(data)
    }
}
