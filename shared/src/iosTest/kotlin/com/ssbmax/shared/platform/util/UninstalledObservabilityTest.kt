package com.ssbmax.shared.platform.util

import kotlin.test.Test

/**
 * Pins the one invariant that makes these fallbacks safe to have at all: a
 * missing observability install must degrade to a logged drop, never to a
 * thrown error.
 *
 * This matters because of *where* [UninstalledCrashReporter] gets called
 * from. Its whole purpose is handling already-failed paths -- catch blocks
 * reporting a non-fatal. If it threw (the way
 * `IosGoogleSignInLauncher`'s stub deliberately does), a Swift entry point
 * that forgot to pass a real reporter would convert every caught,
 * recovered-from error in the app into a hard crash, and would do it
 * precisely on the error paths that are least exercised in testing. The
 * fallback would then be more dangerous than the gap it announces.
 */
class UninstalledObservabilityTest {

    @Test
    fun `crash reporter drops rather than throws on every call`() {
        val reporter = UninstalledCrashReporter()
        reporter.recordException(IllegalStateException("boom"))
        reporter.setUserId("user-1")
        reporter.log("message")
    }

    /**
     * `recordException` reads `throwable::class.simpleName` and `.message`
     * to build its log line; a Throwable with a null message must not turn
     * the reporting path into an NPE.
     */
    @Test
    fun `crash reporter handles a throwable with no message`() {
        UninstalledCrashReporter().recordException(RuntimeException())
    }

    @Test
    fun `analytics tracker drops rather than throws`() {
        UninstalledAnalyticsTracker().trackEvent("test_event", mapOf("k" to null))
    }
}
