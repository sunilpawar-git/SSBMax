package com.ssbmax.shared.platform.util

import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.CrashReporter
import platform.Foundation.NSLog

/**
 * Fallbacks used when [com.ssbmax.shared.platform.ensureKoinStarted] is
 * reached without Swift-supplied observability — i.e. any entry point that
 * bypasses `AppDelegate.swift` (today only [com.ssbmax.shared.ui.MainViewController]'s
 * defensive re-call, and iOS test binaries).
 *
 * Firebase Crashlytics/Analytics are owned by `iosApp`'s Swift side (SPM),
 * not by Kotlin — see [com.ssbmax.shared.di.iosObservabilityModule] for why.
 * Kotlin therefore has no implementation to fall back to, and these exist so
 * that gap is *loud* rather than a silent drop (Rule 12): every call NSLogs
 * what was dropped, to the same Xcode console [IosDomainLogger] writes to.
 *
 * They deliberately do not throw, unlike
 * [com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher]'s stub. A crash
 * reporter that crashes the app while reporting an error turns a logged
 * non-fatal into a real fatal — strictly worse than the dropped report it
 * would be announcing.
 */
class UninstalledCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) {
        warn("recordException(${throwable::class.simpleName}: ${throwable.message})")
    }

    override fun setUserId(userId: String) = warn("setUserId")

    override fun log(message: String) = warn("log($message)")
}

/** Analytics counterpart of [UninstalledCrashReporter]. */
class UninstalledAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(name: String, params: Map<String, Any?>) = warn("trackEvent($name)")
}

private fun warn(call: String) {
    NSLog(
        "SSBMax: observability not installed — dropped $call. " +
            "Swift did not supply CrashReporter/AnalyticsTracker to ensureKoinStarted()."
    )
}
