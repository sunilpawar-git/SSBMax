package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for updating user role
 * Note: This will be implemented when we extend the AuthRepository interface
 */
class UpdateUserRoleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // This will be implemented after we update the AuthRepository interface
}

