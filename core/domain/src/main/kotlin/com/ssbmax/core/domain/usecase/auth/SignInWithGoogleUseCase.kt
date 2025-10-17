package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in with Google
 * Note: Google Sign-In flow is handled in the presentation layer
 * This use case will be expanded when we add the sign-in flow to the repository interface
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // This will be implemented after we update the AuthRepository interface
    // For now, Google Sign-In is handled directly in the ViewModel
}

