package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.model.GoogleSignInData
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in with Google
 * Handles the Google Sign-In result after user completes authentication
 * 
 * This use case uses platform-agnostic GoogleSignInData to maintain
 * domain layer independence from Android framework.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Handle Google Sign-In result
     * 
     * @param data Platform-agnostic sign-in result data
     * @return Result containing authenticated user or error
     */
    suspend operator fun invoke(data: GoogleSignInData): Result<SSBMaxUser> {
        return authRepository.handleGoogleSignInResult(data)
    }
}
