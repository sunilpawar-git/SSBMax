package com.ssbmax.shared.domain.usecase.subscription

import com.ssbmax.shared.data.repository.SubscriptionLimits
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.SecurityEvents
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * KMP port of the eligibility half of the Android `core:data`
 * `SubscriptionManager.canTakeTest` (the other half — [TestUsageRecorder]'s
 * `recordTestUsage` — already had a domain interface; this use case is new
 * for Phase 5's OIR test-taking port, since nothing in `shared` previously
 * needed to gate *starting* a test, only recording usage after one).
 *
 * Known, deliberate deviations from the Android original (both documented,
 * not silent):
 * - The debug-only `BYPASS_SUBSCRIPTION_LIMITS` escape hatch
 *   (`core:data`'s `DebugConfig`) is Android-`BuildConfig`-only and is NOT
 *   ported — this use case always enforces real limits. Fails safe (more
 *   restrictive, not less), but removes the local-dev convenience described
 *   in CLAUDE.local.md's debug-flags section for any screen that switches to
 *   this shared eligibility path.
 * - No local Room mirror of usage (the Android original also synced
 *   `TestUsageDao`) — `shared` has no equivalent cache table for this data;
 *   Firestore is read directly every time, matching this repo's own
 *   "SECURITY: reads from Firestore server-side to prevent bypass" comment
 *   (the Room sync was a read-path optimization, not a security requirement).
 * - `SecurityEventLogger`'s limit-reached event is restored (Phase 7a) via
 *   the injected [AnalyticsTracker] — [SecurityEvents.LIMIT_REACHED], the one
 *   call `core:data`'s Android-only `SecurityEventLogger` made from this
 *   exact decision point.
 */
class CheckTestEligibilityUseCase(
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke(testType: TestType, userId: String): TestEligibility {
        val tier = subscriptionRepository.getSubscriptionTier(userId).getOrElse {
            return TestEligibility.NetworkError
        }
        val month = currentYearMonth()
        val usage = subscriptionRepository.getMonthlyUsage(userId, month).getOrElse {
            return TestEligibility.NetworkError
        }
        val key = SubscriptionLimits.keyFor(testType)
        val info = usage[key]
        val limit = info?.limit ?: SubscriptionLimits.limitFor(key, tier)
        val used = info?.used ?: 0

        return if (limit == -1 || used < limit) {
            TestEligibility.Eligible(remainingTests = if (limit == -1) Int.MAX_VALUE else limit - used)
        } else {
            analyticsTracker.trackEvent(
                SecurityEvents.LIMIT_REACHED,
                mapOf("test_type" to testType.name, "tier" to tier.name)
            )
            TestEligibility.LimitReached(
                tier = tier,
                limit = limit,
                usedCount = used,
                resetsAt = nextMonthResetLabel()
            )
        }
    }

    private fun currentYearMonth(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }

    /** e.g. "Aug 1, 2026" — first day of next calendar month, matching the Android original's format. */
    private fun nextMonthResetLabel(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val nextMonthFirst = if (today.monthNumber == 12) {
            LocalDate(today.year + 1, 1, 1)
        } else {
            LocalDate(today.year, today.monthNumber + 1, 1)
        }
        val monthName = MONTH_NAMES[nextMonthFirst.monthNumber - 1]
        return "$monthName 1, ${nextMonthFirst.year}"
    }

    private companion object {
        val MONTH_NAMES = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }
}
