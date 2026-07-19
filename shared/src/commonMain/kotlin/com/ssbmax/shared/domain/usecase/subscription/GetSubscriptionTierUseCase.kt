package com.ssbmax.shared.domain.usecase.subscription

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.repository.SubscriptionRepository

/**
 * Use case for getting the user's current subscription tier
 */
class GetSubscriptionTierUseCase constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    /**
     * Get the user's subscription tier
     * @param userId The user ID
     * @return Result containing the subscription tier or error
     */
    suspend operator fun invoke(userId: String): Result<SubscriptionTier> {
        return subscriptionRepository.getSubscriptionTier(userId)
    }
}
