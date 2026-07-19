package com.ssbmax.shared.domain.usecase.auth

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.repository.AuthRepository

/**
 * Use case for getting Google Sign-In launch data
 * Returns platform-agnostic data to launch Google Sign-In flow
 * 
 * This use case maintains domain layer independence by using
 * GoogleSignInData instead of Android-specific Intent.
 */
class GetGoogleSignInIntentUseCase constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Get the Google Sign-In launch data for initiating the sign-in flow
     * 
     * @return Platform-agnostic launch data containing platform-specific intent/action
     */
    operator fun invoke(): GoogleSignInData.LaunchData {
        return authRepository.getGoogleSignInIntent()
    }
}
