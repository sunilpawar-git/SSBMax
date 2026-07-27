package com.ssbmax.workers.retry

import com.ssbmax.workers.retry.RetryBackoffPolicy.nextDelayMillis
import kotlin.random.Random

/**
 * Deterministic exponential backoff with jitter for AI-call retries inside TAT workers.
 *
 * Replaces the previous linear `RETRY_DELAY_MS * (attempt + 1)` delay, which made every
 * worker retry at the exact same offsets — a burst of simultaneous story-worker failures
 * (e.g. Gemini briefly overloaded) all retried in lockstep and re-hit the same overload
 * window. Jitter spreads retries out; the exponential base + cap bounds total wait time.
 */
object RetryBackoffPolicy {
    private const val BASE_DELAY_MS = 1_000L
    private const val MAX_EXPONENTIAL_DELAY_MS = 8_000L
    private const val JITTER_FRACTION = 0.2

    /**
     * @param attempt zero-based retry attempt number (0 = delay before the first retry)
     */
    fun nextDelayMillis(attempt: Int, random: Random = Random.Default): Long {
        require(attempt >= 0) { "attempt must be >= 0, was $attempt" }
        val jitterRange = jitterRangeMillis(attempt)
        val jitter = if (jitterRange > 0) random.nextLong(-jitterRange, jitterRange + 1) else 0L
        return (exponentialDelayMillis(attempt) + jitter).coerceAtLeast(0L)
    }

    /** Lower bound of [nextDelayMillis] for the given attempt — useful for test assertions. */
    fun minDelayMillis(attempt: Int): Long =
        (exponentialDelayMillis(attempt) - jitterRangeMillis(attempt)).coerceAtLeast(0L)

    /** Upper bound of [nextDelayMillis] for the given attempt — useful for test assertions. */
    fun maxDelayMillis(attempt: Int): Long =
        exponentialDelayMillis(attempt) + jitterRangeMillis(attempt)

    private fun exponentialDelayMillis(attempt: Int): Long =
        (BASE_DELAY_MS shl attempt).coerceAtMost(MAX_EXPONENTIAL_DELAY_MS)

    private fun jitterRangeMillis(attempt: Int): Long =
        (exponentialDelayMillis(attempt) * JITTER_FRACTION).toLong()
}
