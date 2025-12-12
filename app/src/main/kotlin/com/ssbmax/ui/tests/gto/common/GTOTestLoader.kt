package com.ssbmax.ui.tests.gto.common

import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.flow.first

/**
 * Shared helper for loading GTO tests with eligibility checks
 */
class GTOTestLoader(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val sequentialAccessManager: GTOSequentialAccessManager,
    private val securityLogger: SecurityEventLogger
) {
    
    suspend fun loadTest(
        testId: String,
        testType: TestType,
        gtoTestType: GTOTestType,
        loadContent: suspend (String) -> Result<*>
    ): LoadResult {
        return try {
            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                securityLogger.logUnauthenticatedAccess(
                    testType = testType,
                    context = "GTOTestLoader.loadTest"
                )
                return LoadResult.Error("Authentication required. Please login to continue.")
            }
            
            val (canAccess, accessError) = sequentialAccessManager.checkAccess(userId, gtoTestType)
            if (!canAccess) {
                return LoadResult.Error(accessError ?: "Complete previous GTO tests first")
            }
            
            val eligibility = subscriptionManager.canTakeTest(testType, userId)
            when (eligibility) {
                is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                    val message = "You've completed ${eligibility.usedCount} of ${eligibility.limit} tests this month on the ${eligibility.tier.displayName} plan. Your limit resets on ${eligibility.resetsAt}."
                    return LoadResult.LimitReached(message)
                }
                is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                    // Continue
                }
            }
            
            val contentResult = loadContent(userId)
            if (contentResult.isFailure) {
                return LoadResult.Error("Failed to load test content. Please try again.")
            }
            
            val profileFlow = userProfileRepository.getUserProfile(userId)
            val profileResult = profileFlow.first()
            val profile = profileResult.getOrNull()
            val subscriptionType = profile?.subscriptionType ?: SubscriptionType.FREE
            
            LoadResult.Success(userId, subscriptionType)
            
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to load GTO test")
            LoadResult.Error("Failed to load test. Please try again.")
        }
    }
    
    sealed class LoadResult {
        data class Success(val userId: String, val subscriptionType: SubscriptionType) : LoadResult()
        data class Error(val message: String) : LoadResult()
        data class LimitReached(val message: String) : LoadResult()
    }
}
