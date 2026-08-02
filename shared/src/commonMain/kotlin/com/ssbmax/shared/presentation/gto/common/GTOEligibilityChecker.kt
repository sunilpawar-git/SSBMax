package com.ssbmax.shared.presentation.gto.common

import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.SecurityEvents
import kotlinx.coroutines.flow.first

/**
 * KMP port of `app/.../ui/tests/gto/common/GTOTestEligibilityChecker.kt`,
 * shared by GD and Lecturette (and future GPE/PGT/etc. GTO test types) rather
 * than duplicated per-vertical -- unlike the simple single-call eligibility
 * checks WAT/SRT/SDT inline directly in their own ViewModels, GTO tests need
 * a second real check (sequential-access: GD before GPE before Lecturette,
 * enforced by [GTORepository.canUserTakeTest]) that's identical across every
 * GTO test type, so a shared class earns its keep here.
 *
 * `core:data`'s `SecurityEventLogger` is not ported wholesale, but its
 * unauthenticated-access event is restored (Phase 7a) via the injected
 * [AnalyticsTracker] — [DomainLogger] still gets the same log line it always did.
 */
class GTOEligibilityChecker(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val gtoRepository: GTORepository,
    private val checkTestEligibility: CheckTestEligibilityUseCase,
    private val logger: DomainLogger,
    private val analyticsTracker: AnalyticsTracker
) {
    private val tag = "GTOEligibilityChecker"

    sealed class Result {
        data class Eligible(val userId: String, val subscriptionType: SubscriptionType) : Result()
        data class Error(val message: String) : Result()
        data class LimitReached(val message: String) : Result()
    }

    suspend fun checkEligibility(testType: TestType, gtoTestType: GTOTestType): Result {
        val userId = observeCurrentUser().first()?.id ?: run {
            logger.e(tag, "SECURITY: Unauthenticated GTO test access blocked", null)
            analyticsTracker.trackEvent(SecurityEvents.UNAUTHENTICATED_ACCESS, mapOf("test_type" to testType.name))
            return Result.Error("Authentication required. Please login to continue.")
        }

        val canAccess = gtoRepository.canUserTakeTest(userId, gtoTestType).getOrElse {
            logger.e(tag, "Failed to check GTO sequential access", it)
            return Result.Error("Unable to verify test access. Please try again.")
        }
        if (!canAccess) {
            val completed = gtoRepository.getCompletedTests(userId).getOrNull().orEmpty()
            val missing = GTOTestType.getPrerequisites(gtoTestType).filterNot { it in completed }
            return Result.Error(buildAccessDeniedMessage(gtoTestType, missing))
        }

        return when (val eligibility = checkTestEligibility(testType, userId)) {
            is TestEligibility.LimitReached -> Result.LimitReached(
                "You've completed ${eligibility.usedCount} of ${eligibility.limit} tests this month on the " +
                    "${eligibility.tier.displayName} plan. Your limit resets on ${eligibility.resetsAt}."
            )
            is TestEligibility.NetworkError -> Result.Error("No connection. Please check your network and try again.")
            is TestEligibility.Eligible -> {
                val subscriptionType = userProfileRepository.getUserProfile(userId).first()
                    .getOrNull()?.subscriptionType ?: SubscriptionType.FREE
                Result.Eligible(userId, subscriptionType)
            }
        }
    }

    private fun buildAccessDeniedMessage(testType: GTOTestType, missing: List<GTOTestType>): String = when {
        missing.isEmpty() -> "Complete previous GTO tests first"
        missing.size == 1 -> "Complete ${missing.first().displayName} before accessing ${testType.displayName}"
        else -> {
            val allButLast = missing.dropLast(1).joinToString(", ") { it.displayName }
            "Complete $allButLast, and ${missing.last().displayName} before accessing ${testType.displayName}"
        }
    }
}
