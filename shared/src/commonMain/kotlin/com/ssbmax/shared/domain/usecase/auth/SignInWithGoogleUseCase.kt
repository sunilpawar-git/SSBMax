package com.ssbmax.shared.domain.usecase.auth

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.repository.AuthRepository

/**
 * Use case for signing in with Google
 * Handles the Google Sign-In result after user completes authentication
 * 
 * This use case uses platform-agnostic GoogleSignInData to maintain
 * domain layer independence from Android framework.
 */
class SignInWithGoogleUseCase constructor(
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
