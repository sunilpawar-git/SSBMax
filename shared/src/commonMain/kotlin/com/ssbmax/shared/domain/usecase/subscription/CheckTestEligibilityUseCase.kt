package com.ssbmax.shared.domain.usecase.subscription

import com.ssbmax.shared.data.repository.SubscriptionLimits
import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.SecurityEvents
import com.ssbmax.shared.platform.settings.DeveloperSettings
import kotlin.time.Clock
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
 * - No local Room mirror of usage (the Android original also synced
 *   `TestUsageDao`) — `shared` has no equivalent cache table for this data;
 *   Firestore is read directly every time, matching this repo's own
 *   "SECURITY: reads from Firestore server-side to prevent bypass" comment
 *   (the Room sync was a read-path optimization, not a security requirement).
 *   Confirmed safe to drop outright (KMP-convergence Phase 9d): `core:data`'s
 *   `TestUsageDao` was written by `canTakeTest`/`recordTestUsage` but had zero
 *   readers anywhere except `SubscriptionManager`'s own dead
 *   `getTotalTestsUsedThisMonth` (itself grep-confirmed to have zero callers)
 *   — the mirror was already load-bearing for nothing.
 * - `SecurityEventLogger`'s limit-reached event is restored (Phase 7a) via
 *   the injected [AnalyticsTracker] — [SecurityEvents.LIMIT_REACHED], the one
 *   call `core:data`'s Android-only `SecurityEventLogger` made from this
 *   exact decision point.
 *
 * `bypassSubscriptionLimits` (KMP-convergence Phase 9d): the debug-only
 * `BYPASS_SUBSCRIPTION_LIMITS` escape hatch, restored. It had silently gone
 * dead the moment every test ViewModel migrated to this use case (still
 * documented as working in `CLAUDE.local.md`'s debug-flags section, but
 * `core:data`'s `SubscriptionManager.canTakeTest` — the only place that ever
 * read it — had zero production callers left, grep-confirmed). Supplied as a
 * Koin property ([com.ssbmax.shared.di.BYPASS_SUBSCRIPTION_LIMITS_PROPERTY],
 * same shape as [com.ssbmax.shared.di.GEMINI_API_KEY_PROPERTY]) rather than
 * ported as a `DebugConfig` interface — Android's `SSBMaxApplication` supplies
 * `BuildConfig.DEBUG && BuildConfig.BYPASS_SUBSCRIPTION_LIMITS`; iOS supplies
 * nothing, so the property's `false` default applies (the bypass was always
 * an Android-`BuildConfig`-only local-dev convenience, never an iOS one — this
 * restores that exact scope, not a new one).
 */
class CheckTestEligibilityUseCase(
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val bypassSubscriptionLimits: Boolean = false,
    private val developerSettings: DeveloperSettings? = null
) {
    suspend operator fun invoke(testType: TestType, userId: String): TestEligibility {
        if (bypassSubscriptionLimits) {
            return TestEligibility.Eligible(remainingTests = BYPASS_REMAINING_TESTS)
        }
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
            // Suppressed while a dev tier override is active -- toggling Force Free to see the
            // limit-reached dialog isn't a real user hitting a real limit, and shouldn't pollute
            // production security-event metrics.
            val isOverridden = developerSettings?.getOverride()?.let { it != SubscriptionOverride.FOLLOW_REAL } ?: false
            if (!isOverridden) {
                analyticsTracker.trackEvent(
                    SecurityEvents.LIMIT_REACHED,
                    mapOf("test_type" to testType.name, "tier" to tier.name)
                )
            }
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
        return "${now.year}-${(now.month.ordinal + 1).toString().padStart(2, '0')}"
    }

    /** e.g. "Aug 1, 2026" — first day of next calendar month, matching the Android original's format. */
    private fun nextMonthResetLabel(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayMonthNumber = today.month.ordinal + 1
        val nextMonthFirst = if (todayMonthNumber == 12) {
            LocalDate(today.year + 1, 1, 1)
        } else {
            LocalDate(today.year, todayMonthNumber + 1, 1)
        }
        val monthName = MONTH_NAMES[nextMonthFirst.month.ordinal]
        return "$monthName 1, ${nextMonthFirst.year}"
    }

    private companion object {
        const val BYPASS_REMAINING_TESTS = 999
        val MONTH_NAMES = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }
}
