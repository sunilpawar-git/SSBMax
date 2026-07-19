package com.ssbmax.shared.domain.model

import com.ssbmax.shared.domain.model.OIRQuestionType.NON_VERBAL_REASONING
import com.ssbmax.shared.domain.model.OIRQuestionType.NUMERICAL_ABILITY
import com.ssbmax.shared.domain.model.OIRQuestionType.VERBAL_REASONING

/**
 * Single source of truth for the OIR test's question-type distribution.
 *
 * The bank contains zero SPATIAL_REASONING questions, so the live distribution
 * targets the three populated types only: V=40% / NV=40% / N=20%.
 * Both the runtime selector (data layer) and any UI/config consumer read these
 * ratios from here — there is no second copy of the numbers anywhere.
 */
object OIRQuestionDistribution {

    val ratios: Map<OIRQuestionType, Float> = mapOf(
        VERBAL_REASONING to 0.40f,
        NON_VERBAL_REASONING to 0.40f,
        NUMERICAL_ABILITY to 0.20f
    )

    /**
     * Splits [total] across the [ratios] using the largest-remainder method so
     * the returned counts always sum to exactly [total] (no lost/extra slots).
     */
    fun counts(total: Int): Map<OIRQuestionType, Int> {
        if (total <= 0) return ratios.mapValues { 0 }

        // Floor each share, then hand out the leftover units to the types with
        // the largest fractional remainders (deterministic by remainder desc).
        val exact = ratios.mapValues { (_, ratio) -> total * ratio }
        val floored = exact.mapValues { (_, v) -> v.toInt() }.toMutableMap()
        var remainder = total - floored.values.sum()

        ratios.keys
            .sortedByDescending { exact.getValue(it) - floored.getValue(it) }
            .forEach { type ->
                if (remainder <= 0) return@forEach
                floored[type] = floored.getValue(type) + 1
                remainder--
            }

        return floored
    }
}
