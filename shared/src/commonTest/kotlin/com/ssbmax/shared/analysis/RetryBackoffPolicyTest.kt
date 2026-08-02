package com.ssbmax.shared.analysis

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Phase 8 (KMP-convergence plan): ported from `app`'s
 * `workers/retry/RetryBackoffPolicyTest.kt` onto `kotlin.test` (JUnit4's `org.junit` has no
 * Kotlin/Native equivalent) so this runs on both platforms now that [RetryBackoffPolicy]
 * backs every orchestrator's retry loop, not just TAT's.
 */
class RetryBackoffPolicyTest {

    @Test
    fun `returns expected bounded delays`() {
        repeat(200) {
            for (attempt in 0..4) {
                val delay = RetryBackoffPolicy.nextDelayMillis(attempt)
                assertTrue(
                    delay >= RetryBackoffPolicy.minDelayMillis(attempt),
                    "attempt=$attempt delay=$delay must be >= ${RetryBackoffPolicy.minDelayMillis(attempt)}"
                )
                assertTrue(
                    delay <= RetryBackoffPolicy.maxDelayMillis(attempt),
                    "attempt=$attempt delay=$delay must be <= ${RetryBackoffPolicy.maxDelayMillis(attempt)}"
                )
            }
        }
    }

    @Test
    fun `jitter remains within safe range`() {
        val seededRandom = Random(42)
        repeat(50) {
            val delay = RetryBackoffPolicy.nextDelayMillis(attempt = 2, random = seededRandom)
            val min = RetryBackoffPolicy.minDelayMillis(2)
            val max = RetryBackoffPolicy.maxDelayMillis(2)
            assertTrue(delay in min..max, "delay=$delay must be within [$min, $max]")
        }
    }

    @Test
    fun `retry sequence matches policy across increasing attempts`() {
        assertTrue(
            RetryBackoffPolicy.maxDelayMillis(0) < RetryBackoffPolicy.minDelayMillis(1),
            "delay must grow with attempt number"
        )
        assertTrue(RetryBackoffPolicy.maxDelayMillis(1) < RetryBackoffPolicy.minDelayMillis(2))
    }

    @Test
    fun `delay caps at max exponential delay after enough attempts`() {
        val cappedAtAttempt10 = RetryBackoffPolicy.maxDelayMillis(10)
        val cappedAtAttempt20 = RetryBackoffPolicy.maxDelayMillis(20)
        assertEquals(cappedAtAttempt10, cappedAtAttempt20)
    }

    @Test
    fun `rejects negative attempt`() {
        assertFailsWith<IllegalArgumentException> {
            RetryBackoffPolicy.nextDelayMillis(-1)
        }
    }
}
