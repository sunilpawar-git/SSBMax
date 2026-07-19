package com.ssbmax.shared.domain.usecase.auth

import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository

/**
 * Use case for updating user role (Student or Instructor)
 */
class UpdateUserRoleUseCase constructor(
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
