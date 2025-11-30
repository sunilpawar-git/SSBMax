package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Use case to check interview limits based on subscription tier
 *
 * New unified interview limits (TTS-based):
 * - FREE: 1 interview/month with Android TTS
 * - PRO: 1 interview/month with Sarvam AI TTS
 * - PREMIUM: 3 interviews/month with Sarvam AI TTS
 */
class CheckInterviewLimitsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val interviewRepository: InterviewRepository
) {

    /**
     * Check if user has remaining interviews
     *
     * @param userId User to check
     * @return True if user has remaining interviews, false otherwise
     */
    suspend operator fun invoke(userId: String): Result<Boolean> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(false)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(false)

            // Convert SubscriptionTier enum to SubscriptionType for InterviewLimits
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get used count from repository
            val used = getUsedCount(userId).getOrNull() ?: 0

            // Calculate limits using InterviewLimits
            val limits = InterviewLimits.forSubscription(subscriptionType, used)

            Result.success(limits.canStartInterview())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get remaining interview count for user
     *
     * @param userId User to check
     * @return Number of remaining interviews
     */
    suspend fun getRemainingCount(userId: String): Result<Int> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(0)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Convert SubscriptionTier enum to SubscriptionType
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get used count
            val used = getUsedCount(userId).getOrNull() ?: 0

            // Calculate limits
            val limits = InterviewLimits.forSubscription(subscriptionType, used)

            Result.success(limits.remaining)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get maximum interview limit for user's subscription tier
     *
     * @param userId User to check
     * @return Maximum interviews allowed
     */
    suspend fun getMaxLimit(userId: String): Result<Int> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(0)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Convert SubscriptionTier enum to SubscriptionType
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get limit from InterviewLimits (used=0 doesn't matter for totalLimit)
            val limits = InterviewLimits.forSubscription(subscriptionType, 0)

            Result.success(limits.totalLimit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get used interview count for user
     *
     * @param userId User to check
     * @return Number of interviews used this month
     */
    suspend fun getUsedCount(userId: String): Result<Int> {
        return try {
            // Get interview stats (returns map of mode -> count)
            val statsResult = interviewRepository.getInterviewStats(userId)

            if (statsResult.isFailure) {
                return Result.success(0)
            }

            val stats = statsResult.getOrNull() ?: return Result.success(0)

            // Sum all interview counts (unified system counts all interviews)
            val totalUsed = stats.values.sum()

            Result.success(totalUsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get interview limits summary for user
     *
     * @param userId User to check
     * @return Interview usage info (used, limit, remaining, TTS service)
     */
    suspend fun getInterviewLimits(userId: String): Result<InterviewLimits> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                // Return default FREE tier limits on error
                return Result.success(
                    InterviewLimits.forSubscription(
                        com.ssbmax.core.domain.model.SubscriptionType.FREE,
                        0
                    )
                )
            }

            val tier = tierResult.getOrNull()
                ?: return Result.success(
                    InterviewLimits.forSubscription(
                        com.ssbmax.core.domain.model.SubscriptionType.FREE,
                        0
                    )
                )

            // Convert SubscriptionTier to SubscriptionType
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get used count
            val used = getUsedCount(userId).getOrNull() ?: 0

            // Calculate and return limits
            val limits = InterviewLimits.forSubscription(subscriptionType, used)

            Result.success(limits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
