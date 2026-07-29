package com.ssbmax.shared.analysis

import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.service.ResponseAnalysis
import kotlinx.coroutines.delay

/**
 * Shared retry/fill-missing-OLQs logic factored out of the near-identical blocks
 * repeated across every Android `*AnalysisWorker` (`analyzeSubmissionWithRetry` +
 * `fillMissingOLQs`, ~40 duplicated lines each in PPDT/WAT/SRT/SDT/TAT-story/TAT-synthesis).
 * Same retry count/backoff/score-range/acceptance-threshold as those originals.
 */
internal object AnalysisRetry {
    private const val MAX_AI_RETRIES = 3
    private const val RETRY_DELAY_MS = 2000L
    private const val MIN_ACCEPTABLE_OLQS = 14

    suspend fun withRetry(
        scoreRange: IntRange = 5..9,
        call: suspend () -> Result<ResponseAnalysis>
    ): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                val result = call()
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(scoreRange.first, scoreRange.last),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }
                    if (olqScores.size >= MIN_ACCEPTABLE_OLQS) {
                        return if (olqScores.size == OLQ.entries.size) olqScores else fillMissingOLQs(olqScores)
                    }
                }
            } catch (e: Exception) {
                // Swallow and retry -- caller decides how to handle a null return after
                // all attempts are exhausted, matching the Android originals' behavior.
            }
            if (attempt < MAX_AI_RETRIES - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return null
    }

    private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
        val mutable = scores.toMutableMap()
        OLQ.entries.forEach { olq ->
            if (olq !in mutable) {
                mutable[olq] = OLQScore(
                    score = 6,
                    confidence = 30,
                    reasoning = "AI did not assess this OLQ - neutral score assigned"
                )
            }
        }
        return mutable
    }

    fun ratingFromScore(score: Float): String = when {
        score <= 5.5f -> "Exceptional"
        score <= 6.5f -> "Good"
        score <= 7.5f -> "Average"
        else -> "Needs Improvement"
    }
}
