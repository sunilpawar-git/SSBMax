package com.ssbmax.shared.domain.usecase.subscription

import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.UsageInfo
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for getting monthly test usage information
 */
class GetMonthlyUsageUseCase constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    /**
     * Get monthly usage for all test types
     * @param userId The user ID
     * @return Result containing map of test types to usage info or error
     */
    suspend operator fun invoke(userId: String): Result<Map<String, UsageInfo>> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
        return subscriptionRepository.getMonthlyUsage(userId, currentMonth)
    }
}
