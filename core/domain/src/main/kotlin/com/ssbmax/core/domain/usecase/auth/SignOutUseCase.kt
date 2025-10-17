package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing out
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}

