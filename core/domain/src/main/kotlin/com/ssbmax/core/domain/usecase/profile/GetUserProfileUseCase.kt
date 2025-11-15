package com.ssbmax.core.domain.usecase.profile

import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving user profile information
 *
 * This encapsulates the business logic for:
 * - Fetching user profile from repository
 * - Handling profile not found scenarios
 * - Providing reactive Flow updates
 */
class GetUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Get user profile as a reactive Flow
     *
     * @param userId The user ID to fetch profile for
     * @return Flow emitting Result<UserProfile?> that updates when profile changes
     *         (null if profile not found)
     */
    operator fun invoke(userId: String): Flow<Result<UserProfile?>> {
        return userProfileRepository.getUserProfile(userId)
    }
}
