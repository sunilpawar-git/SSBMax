package com.ssbmax.shared.analysis

import kotlin.random.Random

/**
 * KMP port of `app`'s `workers/retry/RetryBackoffPolicy` (Phase 8, KMP-convergence plan):
 * deterministic exponential backoff with jitter for AI-call retries. Was TAT-story-worker-only
 * on Android; promoted here as [AnalysisRetry]'s canonical backoff so every orchestrator gets
 * the same spread-retries-apart behavior on both platforms, not just TAT.
 *
 * Replaces a plain linear `RETRY_DELAY_MS * (attempt + 1)` delay, which made every retry land
 * at the exact same offsets — a burst of simultaneous failures (e.g. Gemini briefly overloaded)
 * all retried in lockstep and re-hit the same overload window. Jitter spreads retries out; the
 * exponential base + cap bounds total wait time.
 *
 * Public (not `internal`): `app`'s `TATStoryAnalysisWorkerTest`/`TATSynthesisWorkerTest`
 * (Phase 8) assert their worker's retry timing directly against this policy's published
 * bounds, the same way this class's own test does.
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
