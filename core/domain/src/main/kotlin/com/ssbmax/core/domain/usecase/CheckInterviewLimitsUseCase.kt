package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Use case to check interview limits based on subscription tier
 *
 * Limits:
 * - Free: No access
 * - Pro: 2 text interviews per month
 * - Premium: 2 text + 2 voice interviews per month (4 total)
 */
class CheckInterviewLimitsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val interviewRepository: InterviewRepository
) {

    /**
     * Check if user has remaining interviews for the specified mode
     *
     * @param userId User to check
     * @param mode Interview mode (text or voice)
     * @return True if user has remaining interviews, false otherwise
     */
    suspend operator fun invoke(
        userId: String,
        mode: InterviewMode
    ): Result<Boolean> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(false)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(false)

            // Free tier has no access
            if (tier.displayName.uppercase() == "FREE") {
                return Result.success(false)
            }

            // Pro tier doesn't support voice mode
            if (tier.displayName.uppercase() == "PRO" && mode == InterviewMode.VOICE_BASED) {
                return Result.success(false)
            }

            // Check remaining interviews for this mode
            val remainingResult = interviewRepository.getRemainingInterviews(userId, mode)

            if (remainingResult.isFailure) {
                return Result.success(false)
            }

            val remaining = remainingResult.getOrNull() ?: 0

            Result.success(remaining > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get remaining interview count for user
     *
     * @param userId User to check
     * @param mode Interview mode
     * @return Number of remaining interviews
     */
    suspend fun getRemainingCount(
        userId: String,
        mode: InterviewMode
    ): Result<Int> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(0)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Free tier has no access
            if (tier.displayName.uppercase() == "FREE") {
                return Result.success(0)
            }

            // Pro tier doesn't support voice mode
            if (tier.displayName.uppercase() == "PRO" && mode == InterviewMode.VOICE_BASED) {
                return Result.success(0)
            }

            // Get remaining interviews
            interviewRepository.getRemainingInterviews(userId, mode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get maximum interview limit for user's subscription tier
     *
     * @param userId User to check
     * @param mode Interview mode
     * @return Maximum interviews allowed
     */
    suspend fun getMaxLimit(
        userId: String,
        mode: InterviewMode
    ): Result<Int> {
        return try {
            // Get current subscription tier
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)

            if (tierResult.isFailure) {
                return Result.success(0)
            }

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Determine limit based on tier and mode
            val tierName = tier.displayName.uppercase()
            val limit = when {
                tierName == "FREE" -> 0
                tierName == "PRO" && mode == InterviewMode.TEXT_BASED -> 2
                tierName == "PRO" && mode == InterviewMode.VOICE_BASED -> 0
                tierName == "PREMIUM" && mode == InterviewMode.TEXT_BASED -> 2
                tierName == "PREMIUM" && mode == InterviewMode.VOICE_BASED -> 2
                else -> 0
            }

            Result.success(limit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get used interview count for user
     *
     * @param userId User to check
     * @param mode Interview mode
     * @return Number of interviews used
     */
    suspend fun getUsedCount(
        userId: String,
        mode: InterviewMode
    ): Result<Int> {
        return try {
            val statsResult = interviewRepository.getInterviewStats(userId)

            if (statsResult.isFailure) {
                return Result.success(0)
            }

            val stats = statsResult.getOrNull() ?: return Result.success(0)
            val used = stats[mode] ?: 0

            Result.success(used)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get interview usage summary for user
     *
     * @param userId User to check
     * @return Map of mode to usage info (used, limit, remaining)
     */
    suspend fun getUsageSummary(userId: String): Result<Map<InterviewMode, UsageInfo>> {
        return try {
            val summary = mutableMapOf<InterviewMode, UsageInfo>()

            for (mode in InterviewMode.entries) {
                val maxLimit = getMaxLimit(userId, mode).getOrNull() ?: 0
                val used = getUsedCount(userId, mode).getOrNull() ?: 0
                val remaining = getRemainingCount(userId, mode).getOrNull() ?: 0

                summary[mode] = UsageInfo(
                    used = used,
                    limit = maxLimit,
                    remaining = remaining
                )
            }

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Interview usage information
 */
data class UsageInfo(
    val used: Int,
    val limit: Int,
    val remaining: Int
) {
    init {
        require(used >= 0) { "Used count cannot be negative" }
        require(limit >= 0) { "Limit cannot be negative" }
        require(remaining >= 0) { "Remaining count cannot be negative" }
        require(used + remaining <= limit) { "Used + remaining cannot exceed limit" }
    }

    /**
     * Calculate usage percentage (0-100)
     */
    fun getUsagePercentage(): Int {
        if (limit == 0) return 0
        return ((used.toFloat() / limit) * 100).toInt()
    }

    /**
     * Check if limit is reached
     */
    fun isLimitReached(): Boolean = remaining == 0

    /**
     * Check if user has any access
     */
    fun hasAccess(): Boolean = limit > 0
}
