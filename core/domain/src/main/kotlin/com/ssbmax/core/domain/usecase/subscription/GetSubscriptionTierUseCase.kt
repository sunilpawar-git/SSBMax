package com.ssbmax.core.domain.usecase.subscription

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Use case for getting the user's current subscription tier
 */
class GetSubscriptionTierUseCase @Inject constructor(
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
