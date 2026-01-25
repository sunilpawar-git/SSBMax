package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.scoring.EntryType

/**
 * Recommendation outcome from SSB validation.
 */
enum class RecommendationOutcome {
    /** Candidate passes all criteria - strong recommendation */
    RECOMMENDED,
    /** Candidate is on the edge of passing - conditional recommendation */
    BORDERLINE,
    /** Candidate fails one or more critical criteria */
    NOT_RECOMMENDED
}

/**
 * Result of validating OLQ scores against SSB rules.
 * 
 * This is a lightweight result object designed for use by workers
 * after AI scoring to enrich the analysis with SSB validation.
 */
data class OLQScoreValidationResult(
    /** Whether the scores pass basic validation (all 15 OLQs present, valid range) */
    val isValid: Boolean,
    
    /** Number of OLQs with score >= 8 (limitations) */
    val limitationCount: Int,
    
    /** OLQs that are limitations (score >= 8) */
    val limitationOLQs: List<OLQ>,
    
    /** Whether limitation count exceeds max for entry type */
    val exceedsMaxLimitations: Boolean,
    
    /** Whether any of the 6 critical OLQs has a limitation */
    val hasCriticalWeakness: Boolean,
    
    /** Critical OLQs that have limitations */
    val criticalWeaknessOLQs: List<OLQ>,
    
    /** Whether Factor II average >= 8 (auto-reject) */
    val factorIIAutoReject: Boolean,
    
    /** Whether any factor has score variation exceeding allowed range */
    val hasFactorInconsistency: Boolean,
    
    /** Factors with consistency issues */
    val inconsistentFactors: List<Int>,
    
    /** Average score per factor (1-4 mapping) */
    val factorAverages: Map<Int, Float>,
    
    /** Final recommendation based on all criteria */
    val recommendation: RecommendationOutcome,
    
    /** Human-readable summary of validation */
    val summary: String
)

/**
 * ValidationIntegration - Helper for workers to validate AI scores.
 * 
 * This is a stateless utility that wraps SSBScoreValidator functions
 * and provides a single entry point for workers to validate scores.
 * 
 * Usage in workers:
 * ```
 * val validationResult = ValidationIntegration.validateScores(olqScores, EntryType.NDA)
 * // Enrich OLQAnalysisResult with validationResult data
 * ```
 */
object ValidationIntegration {

    /**
     * Validate OLQ scores against SSB rules.
     * 
     * @param scores Map of OLQ to OLQScore from AI analysis
     * @param entryType NDA/OTA/GRADUATE determines limitation thresholds
     * @return Comprehensive validation result
     */
    fun validateScores(
        scores: Map<OLQ, OLQScore>,
        entryType: EntryType
    ): OLQScoreValidationResult {
        // Handle empty/invalid input
        if (scores.isEmpty()) {
            return createInvalidResult("No scores provided")
        }

        // Convert OLQScore map to score map for validator
        val scoreMap = scores.mapValues { it.value.score }

        // 1. Count limitations
        val limitationResult = SSBScoreValidator.countLimitations(scoreMap)
        val limitationOLQs = limitationResult.limitedOLQs.toList()

        // 2. Check if exceeds max for entry type
        val exceedsMax = SSBScoreValidator.exceedsMaxLimitations(scoreMap, entryType)

        // 3. Check critical quality weaknesses
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scoreMap)
        val criticalWeaknessOLQs = criticalResult.criticalWeaknesses.toList()

        // 4. Check factor consistency
        val consistencyResult = SSBScoreValidator.checkFactorConsistency(scoreMap)
        val inconsistentFactors = consistencyResult.inconsistentFactors.map { it.ssbFactorNumber }

        // 5. Calculate factor averages
        val factorAveragesRaw = SSBScoreValidator.calculateFactorAverages(scoreMap)
        val factorAverages = factorAveragesRaw
            .mapKeys { (category, _) -> category.ssbFactorNumber }
            .mapValues { it.value?.toFloat() ?: 0f }

        // 6. Determine recommendation
        val recommendationResult = SSBScoreValidator.determineRecommendation(scoreMap, entryType)
        val recommendation = when (recommendationResult.recommendation) {
            Recommendation.NOT_RECOMMENDED -> RecommendationOutcome.NOT_RECOMMENDED
            Recommendation.DOUBTFUL -> RecommendationOutcome.BORDERLINE
            Recommendation.RECOMMENDED -> RecommendationOutcome.RECOMMENDED
        }

        // Build summary
        val summaryParts = mutableListOf<String>()
        if (limitationResult.count > 0) {
            summaryParts.add("${limitationResult.count} limitation(s)")
        }
        if (criticalResult.criticalWeaknesses.isNotEmpty()) {
            summaryParts.add("Critical OLQ weakness")
        }
        if (criticalResult.hasAutoRejectWeakness) {
            summaryParts.add("Factor II auto-reject")
        }
        if (!consistencyResult.isConsistent) {
            summaryParts.add("Factor inconsistency detected")
        }
        val summary = if (summaryParts.isEmpty()) {
            "Scores pass all validation criteria"
        } else {
            summaryParts.joinToString("; ")
        }

        return OLQScoreValidationResult(
            isValid = scores.size >= 14, // Allow 1 missing OLQ
            limitationCount = limitationResult.count,
            limitationOLQs = limitationOLQs,
            exceedsMaxLimitations = exceedsMax,
            hasCriticalWeakness = criticalResult.criticalWeaknesses.isNotEmpty(),
            criticalWeaknessOLQs = criticalWeaknessOLQs,
            factorIIAutoReject = criticalResult.hasAutoRejectWeakness,
            hasFactorInconsistency = !consistencyResult.isConsistent,
            inconsistentFactors = inconsistentFactors,
            factorAverages = factorAverages,
            recommendation = recommendation,
            summary = summary
        )
    }

    /**
     * Create an invalid result for error cases.
     */
    private fun createInvalidResult(reason: String): OLQScoreValidationResult {
        return OLQScoreValidationResult(
            isValid = false,
            limitationCount = 0,
            limitationOLQs = emptyList(),
            exceedsMaxLimitations = false,
            hasCriticalWeakness = false,
            criticalWeaknessOLQs = emptyList(),
            factorIIAutoReject = false,
            hasFactorInconsistency = false,
            inconsistentFactors = emptyList(),
            factorAverages = emptyMap(),
            recommendation = RecommendationOutcome.NOT_RECOMMENDED,
            summary = reason
        )
    }

    /**
     * Generate a human-readable validation report.
     * Useful for logging and debugging.
     */
    fun generateReport(result: OLQScoreValidationResult): String {
        return buildString {
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("SSB SCORE VALIDATION REPORT")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("RECOMMENDATION: ${result.recommendation}")
            appendLine()
            appendLine("SUMMARY: ${result.summary}")
            appendLine()
            appendLine("DETAILS:")
            appendLine("  • Limitations: ${result.limitationCount}")
            if (result.limitationOLQs.isNotEmpty()) {
                appendLine("    - ${result.limitationOLQs.joinToString(", ") { it.displayName }}")
            }
            appendLine("  • Exceeds Max Limitations: ${result.exceedsMaxLimitations}")
            appendLine("  • Critical Weakness: ${result.hasCriticalWeakness}")
            if (result.criticalWeaknessOLQs.isNotEmpty()) {
                appendLine("    - ${result.criticalWeaknessOLQs.joinToString(", ") { it.displayName }}")
            }
            appendLine("  • Factor II Auto-Reject: ${result.factorIIAutoReject}")
            appendLine("  • Factor Inconsistency: ${result.hasFactorInconsistency}")
            appendLine()
            appendLine("FACTOR AVERAGES:")
            result.factorAverages.forEach { (factor, avg) ->
                val factorName = when (factor) {
                    1 -> "Factor I (Planning)"
                    2 -> "Factor II (Social)"
                    3 -> "Factor III (Effectiveness)"
                    4 -> "Factor IV (Dynamic)"
                    else -> "Factor $factor"
                }
                appendLine("  • $factorName: ${"%.2f".format(avg)}")
            }
            appendLine("═══════════════════════════════════════════════════════════════")
        }
    }
}
