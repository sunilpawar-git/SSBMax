package com.ssbmax.time

/**
 * Test-only [Clock] with manual time control.
 * Start at an arbitrary non-zero epoch to avoid edge cases around zero.
 */
class FakeClock(private var currentTimeMs: Long = 1_000_000L) : Clock {
    fun advanceBy(ms: Long) {
        currentTimeMs += ms
    }
    override fun nowMs(): Long = currentTimeMs
}
