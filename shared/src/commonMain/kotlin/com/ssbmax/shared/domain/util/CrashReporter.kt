package com.ssbmax.shared.domain.util

/**
 * Cross-platform non-fatal crash/error reporting seam (Phase 7a of the
 * KMP-convergence plan). Android/iOS actuals both wrap Firebase Crashlytics
 * — see `platform/util/AndroidCrashReporter.kt` and `IosCrashReporter.kt`.
 *
 * Native uncaught-crash capture itself does not go through this interface:
 * the Crashlytics SDK installs its own process-wide handler as soon as the
 * framework is linked and `FirebaseApp`/`FIRApp` is configured, on both
 * platforms, independently of any Kotlin call. This interface is only for
 * the same "log a caught error/warning" use [DomainLogger.e] already serves,
 * except the report also reaches the Crashlytics dashboard instead of just
 * the local console.
 */
interface CrashReporter {
    fun recordException(throwable: Throwable)
    fun setUserId(userId: String)
    fun log(message: String)
}
