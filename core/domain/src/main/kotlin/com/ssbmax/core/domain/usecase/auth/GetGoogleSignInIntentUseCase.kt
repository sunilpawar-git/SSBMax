package com.ssbmax.core.domain.usecase.auth

import android.content.Intent
import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for getting Google Sign-In intent
 * Returns the intent to launch Google Sign-In flow
 */
class GetGoogleSignInIntentUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Get the Google Sign-In intent for launching the sign-in activity
     * @return Intent to launch Google Sign-In
     */
    operator fun invoke(): Intent {
        return authRepository.getGoogleSignInIntent()
    }
}
