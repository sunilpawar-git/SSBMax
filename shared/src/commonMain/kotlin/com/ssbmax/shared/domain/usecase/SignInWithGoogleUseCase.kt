package com.ssbmax.shared.domain.usecase

import com.ssbmax.shared.domain.model.GoogleSignInData
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.repository.AuthRepository

/**
 * Ported verbatim (structurally) from
 * core/domain/usecase/auth/SignInWithGoogleUseCase.kt for the Phase 0 KMP
 * spike. `@Inject` (Hilt/javax.inject) dropped — Koin constructor injection
 * needs no annotation.
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(data: GoogleSignInData): Result<SSBMaxUser> {
        return authRepository.handleGoogleSignInResult(data)
    }
}
