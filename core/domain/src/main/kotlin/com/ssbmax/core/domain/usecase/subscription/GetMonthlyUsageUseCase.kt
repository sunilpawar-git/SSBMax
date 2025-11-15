package com.ssbmax.core.domain.usecase.subscription

import com.ssbmax.core.domain.repository.SubscriptionRepository
import com.ssbmax.core.domain.repository.UsageInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for getting monthly test usage information
 */
class GetMonthlyUsageUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    companion object {
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    }

    /**
     * Get monthly usage for all test types
     * @param userId The user ID
     * @return Result containing map of test types to usage info or error
     */
    suspend operator fun invoke(userId: String): Result<Map<String, UsageInfo>> {
        val currentMonth = monthFormat.format(Date())
        return subscriptionRepository.getMonthlyUsage(userId, currentMonth)
    }
}
