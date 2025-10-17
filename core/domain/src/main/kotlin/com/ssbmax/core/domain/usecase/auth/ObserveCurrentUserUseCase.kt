package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing current user changes in real-time
 */
class ObserveCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<SSBMaxUser?> {
        return authRepository.currentUser
    }
}

