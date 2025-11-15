package com.ssbmax.core.domain.usecase.profile

import com.ssbmax.core.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Use case for updating user login streak
 *
 * This encapsulates the business logic for:
 * - Incrementing login streak if user logs in on consecutive days
 * - Resetting streak if a day is missed
 * - Updating last login timestamp
 * - Awarding streak milestone badges
 */
class UpdateLoginStreakUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Update login streak for user
     *
     * The repository handles the logic for:
     * - Checking last login date
     * - Incrementing streak if consecutive
     * - Resetting to 1 if missed a day
     *
     * @param userId The user ID to update streak for
     * @return Result containing the new streak count or error
     */
    suspend operator fun invoke(userId: String): Result<Int> {
        return userProfileRepository.updateLoginStreak(userId)
    }
}
