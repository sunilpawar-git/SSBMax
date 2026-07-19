package com.ssbmax.shared.domain.usecase.profile

import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.repository.UserProfileRepository

/**
 * Use case for updating user profile information
 *
 * This encapsulates the business logic for:
 * - Validating profile updates
 * - Persisting changes to repository
 * - Handling update failures
 */
class UpdateUserProfileUseCase constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Update user profile
     *
     * @param profile The updated profile to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        return userProfileRepository.updateUserProfile(profile)
    }
}
