package com.ssbmax.shared.domain.usecase.auth

import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing current user changes in real-time
 */
class ObserveCurrentUserUseCase constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<SSBMaxUser?> {
        return authRepository.currentUser
    }
}

