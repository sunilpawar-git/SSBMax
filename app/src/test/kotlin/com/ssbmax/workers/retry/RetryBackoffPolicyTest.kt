package com.ssbmax.workers.retry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RetryBackoffPolicyTest {

    @Test
    fun `returns expected bounded delays`() {
        // Real Random across many iterations — proves nextDelayMillis never escapes its
        // own published bounds, which is what callers rely on to reason about worst-case wait.
        repeat(200) {
            for (attempt in 0..4) {
                val delay = RetryBackoffPolicy.nextDelayMillis(attempt)
                assertTrue(
                    "attempt=$attempt delay=$delay must be >= ${RetryBackoffPolicy.minDelayMillis(attempt)}",
                    delay >= RetryBackoffPolicy.minDelayMillis(attempt)
                )
                assertTrue(
                    "attempt=$attempt delay=$delay must be <= ${RetryBackoffPolicy.maxDelayMillis(attempt)}",
                    delay <= RetryBackoffPolicy.maxDelayMillis(attempt)
                )
            }
        }
    }

    @Test
    fun `jitter remains within safe range`() {
        // WHY: jitter exists to spread out a burst of simultaneous retries. If it could
        // exceed the published range, two workers retrying "at the same time" could still
        // collide instead of spreading apart.
        val seededRandom = Random(42)
        repeat(50) {
            val delay = RetryBackoffPolicy.nextDelayMillis(attempt = 2, random = seededRandom)
            val min = RetryBackoffPolicy.minDelayMillis(2)
            val max = RetryBackoffPolicy.maxDelayMillis(2)
            assertTrue("delay=$delay must be within [$min, $max]", delay in min..max)
        }
    }

    @Test
    fun `retry sequence matches policy across increasing attempts`() {
        // Exponential growth: attempt 1 must allow for roughly double attempt 0's delay,
        // not the old linear (attempt+1) growth — proves the policy actually changed.
        assertTrue(
            "delay must grow with attempt number",
            RetryBackoffPolicy.maxDelayMillis(0) < RetryBackoffPolicy.minDelayMillis(1)
        )
        assertTrue(
            RetryBackoffPolicy.maxDelayMillis(1) < RetryBackoffPolicy.minDelayMillis(2)
        )
    }

    @Test
    fun `delay caps at max exponential delay after enough attempts`() {
        // A burst of failures retrying many times must not grow unbounded — caps the
        // worst-case wait so a single submission can't stall WorkManager indefinitely.
        val cappedAtAttempt10 = RetryBackoffPolicy.maxDelayMillis(10)
        val cappedAtAttempt20 = RetryBackoffPolicy.maxDelayMillis(20)
        assertEquals(cappedAtAttempt10, cappedAtAttempt20)
    }

    @Test
    fun `rejects negative attempt`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetryBackoffPolicy.nextDelayMillis(-1)
        }
    }
}
