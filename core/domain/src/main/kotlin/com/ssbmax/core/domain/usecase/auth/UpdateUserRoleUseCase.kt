package com.ssbmax.core.domain.usecase.auth

import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for updating user role (Student or Instructor)
 */
class UpdateUserRoleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Update the user's role
     * @param role The new role to assign
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(role: UserRole): Result<Unit> {
        return authRepository.updateUserRole(role)
    }
}
