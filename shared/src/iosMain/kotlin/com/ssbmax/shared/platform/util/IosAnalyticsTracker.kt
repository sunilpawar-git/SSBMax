package com.ssbmax.shared.platform.util

import cocoapods.FirebaseAnalytics.FIRAnalytics
import com.ssbmax.shared.domain.util.AnalyticsTracker

/**
 * iOS actual: wraps Firebase Analytics via the `FirebaseAnalytics` pod
 * (added to `shared/build.gradle.kts`'s `cocoapods {}` block). Unverified
 * locally — same Xcode/Kotlin Native cinterop mismatch every prior phase in
 * this plan hit.
 */
class IosAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(name: String, params: Map<String, Any?>) {
        FIRAnalytics.logEventWithName(name, params.filterValues { it != null })
    }
}
