package com.ssbmax.ui.tests.gto.common

import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

/**
 * Helper for checking GTO test eligibility
 */
class GTOTestEligibilityChecker(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val sequentialAccessManager: GTOSequentialAccessManager,
    private val securityLogger: SecurityEventLogger
) {
    
    suspend fun checkEligibility(
        testType: TestType,
        gtoTestType: GTOTestType
    ): EligibilityResult {
        return try {
            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                securityLogger.logUnauthenticatedAccess(
                    testType = testType,
                    context = "GTOTestEligibilityChecker.checkEligibility"
                )
                return EligibilityResult.Error("Authentication required. Please login to continue.")
            }
            
            val (canAccess, accessError) = sequentialAccessManager.checkAccess(userId, gtoTestType)
            if (!canAccess) {
                return EligibilityResult.Error(accessError ?: "Complete previous GTO tests first")
            }
            
            val eligibility = subscriptionManager.canTakeTest(testType, userId)
            when (eligibility) {
                is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                    val message = "You've completed ${eligibility.usedCount} of ${eligibility.limit} tests this month on the ${eligibility.tier.displayName} plan. Your limit resets on ${eligibility.resetsAt}."
                    EligibilityResult.LimitReached(message)
                }
                is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                    val profileFlow = userProfileRepository.getUserProfile(userId)
                    val profileResult = profileFlow.first()
                    val profile = profileResult.getOrNull()
                    val subscriptionType = profile?.subscriptionType ?: SubscriptionType.FREE
                    EligibilityResult.Eligible(userId, subscriptionType)
                }
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to check GTO test eligibility")
            EligibilityResult.Error("Failed to check eligibility. Please try again.")
        }
    }
    
    sealed class EligibilityResult {
        data class Eligible(val userId: String, val subscriptionType: SubscriptionType) : EligibilityResult()
        data class Error(val message: String) : EligibilityResult()
        data class LimitReached(val message: String) : EligibilityResult()
    }
}
