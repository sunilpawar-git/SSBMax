package com.ssbmax.shared.domain.usecase.auth

import com.ssbmax.shared.domain.repository.AuthRepository

/**
 * Use case for signing out
 */
class SignOutUseCase constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}

