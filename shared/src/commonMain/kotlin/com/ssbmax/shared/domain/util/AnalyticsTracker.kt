package com.ssbmax.shared.domain.util

/**
 * Cross-platform analytics event seam (Phase 7a of the KMP-convergence
 * plan). Android/iOS actuals both wrap Firebase Analytics — see
 * `platform/util/AndroidAnalyticsTracker.kt` and `IosAnalyticsTracker.kt`.
 *
 * Replaces `core:data`'s Android-only `AnalyticsManager`/`SecurityEventLogger`
 * (both direct `FirebaseAnalytics` wrappers with no KMP artifact) as the one
 * place `shared` code fires an analytics event, on either platform.
 */
interface AnalyticsTracker {
    fun trackEvent(name: String, params: Map<String, Any?> = emptyMap())
}

/**
 * Security-event names ported from `core:data`'s `SecurityEventLogger`
 * (`EVENT_*` constants) — kept here as the SSOT for the two currently wired
 * `shared` call sites. The other four (`sec_limit_bypass_attempt`,
 * `sec_usage_record_fail`, `sec_transaction_fail`, `sec_suspicious_pattern`)
 * have no live `shared` call site yet — their Android originals sit in
 * repository/write paths not yet ported (Phase 9), so adding the constants
 * now without a caller would be speculative; add them alongside whichever
 * phase ports that call site.
 */
object SecurityEvents {
    const val UNAUTHENTICATED_ACCESS = "sec_unauth_test_access"
    const val LIMIT_REACHED = "sec_limit_reached"
}
