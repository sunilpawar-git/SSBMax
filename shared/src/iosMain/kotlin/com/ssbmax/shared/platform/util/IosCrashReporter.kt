package com.ssbmax.shared.platform.util

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import com.ssbmax.shared.domain.util.CrashReporter
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

/**
 * iOS actual: wraps Firebase Crashlytics via the `FirebaseCrashlytics` pod
 * (added to `shared/build.gradle.kts`'s `cocoapods {}` block). Unverified
 * locally — same Xcode/Kotlin Native cinterop mismatch every prior phase in
 * this plan hit; this is source-correct against the documented Objective-C
 * API, not compiler-checked on this machine.
 *
 * Kotlin `Throwable` isn't bridgeable to `NSError` losslessly, so
 * [recordException] wraps it in a minimal `NSError` carrying the message —
 * Crashlytics receives a real non-fatal record, but not a native Kotlin
 * stack trace. Acceptable for this seam's purpose (the same "log a caught
 * error" role [DomainLogger.e] already serves); a faithful stack trace
 * would need Crashlytics' `recordExceptionModel` custom-exception API,
 * out of scope here.
 */
class IosCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) {
        val error = NSError(
            domain = "KotlinException.${throwable::class.simpleName ?: "Unknown"}",
            code = 0,
            userInfo = mapOf(NSLocalizedDescriptionKey to (throwable.message ?: throwable.toString()))
        )
        FIRCrashlytics.crashlytics().recordError(error)
    }

    override fun setUserId(userId: String) {
        FIRCrashlytics.crashlytics().setUserID(userId)
    }

    override fun log(message: String) {
        FIRCrashlytics.crashlytics().log(message)
    }
}
