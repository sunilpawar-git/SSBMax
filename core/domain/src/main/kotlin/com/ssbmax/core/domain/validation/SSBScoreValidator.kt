package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.scoring.EntryType
import com.ssbmax.core.domain.scoring.SSBScoringRules

/**
 * SSB Score Validator - Pure functions for validating OLQ scores.
 * 
 * Implements official SSB scoring rules:
 * - Limitation threshold: Score of 8 (on 1-10 scale, lower is better)
 * - Max limitations: NDA=4, OTA/GRADUATE=7
 * - Factor consistency: ±1 tick within factor, ±2 between factors
 * - Critical OLQs: RA, SA, CO-OP, SoR, LIV, COU (auto-reject if Factor II overall = 8)
 * 
 * All functions are pure (no side effects) and stateless.
 * This object serves as SSOT for all score validation logic.
 */
object SSBScoreValidator {

    // ===========================================
    // LIMITATION COUNTING
    // ===========================================

    /**
     * Count the number of limitations (scores >= 8) in the given scores.
     * 
     * @param scores Map of OLQ to score (1-10, lower is better)
     * @return LimitationResult containing count and limited OLQs
     */
    fun countLimitations(scores: Map<OLQ, Int>): LimitationResult {
        val limitedOLQs = scores.filter { (_, score) -> 
            SSBScoringRules.isLimitation(score) 
        }
        
        return LimitationResult(
            count = limitedOLQs.size,
            limitedOLQs = limitedOLQs.keys,
            limitationScores = limitedOLQs
        )
    }

    /**
     * Check if the candidate exceeds the maximum allowed limitations for their entry type.
     * 
     * @param scores Map of OLQ to score
     * @param entryType NDA, OTA, or GRADUATE
     * @return True if limitations exceed the maximum allowed
     */
    fun exceedsMaxLimitations(scores: Map<OLQ, Int>, entryType: EntryType): Boolean {
        val limitationCount = countLimitations(scores).count
        return limitationCount > entryType.maxLimitations
    }

    // ===========================================
    // FACTOR CONSISTENCY
    // ===========================================

    /**
     * Check if scores within each factor are consistent (±1 tick variation).
     * 
     * SSB Rule: All OLQs within a factor should have similar scores (±1 tick).
     * 
     * @param scores Map of OLQ to score
     * @return ConsistencyResult with details about any inconsistencies
     */
    fun checkFactorConsistency(scores: Map<OLQ, Int>): ConsistencyResult {
        val inconsistentFactors = mutableSetOf<OLQCategory>()
        val details = mutableMapOf<OLQCategory, FactorConsistencyDetail>()
        var maxVariationFound = 0

        OLQCategory.entries.forEach { category ->
            val factorScores = scores.filterKeys { olq -> olq.category == category }
            
            if (factorScores.size >= 2) {
                val minScore = factorScores.values.minOrNull() ?: 0
                val maxScore = factorScores.values.maxOrNull() ?: 0
                val variation = maxScore - minScore
                
                if (variation > maxVariationFound) {
                    maxVariationFound = variation
                }
                
                val isConsistent = variation <= category.maxTickVariation
                
                if (!isConsistent) {
                    inconsistentFactors.add(category)
                }
                
                details[category] = FactorConsistencyDetail(
                    category = category,
                    minScore = minScore,
                    maxScore = maxScore,
                    variation = variation,
                    isConsistent = isConsistent
                )
            }
        }

        return ConsistencyResult(
            isConsistent = inconsistentFactors.isEmpty(),
            inconsistentFactors = inconsistentFactors,
            maxVariationFound = maxVariationFound,
            details = details
        )
    }

    // ===========================================
    // CRITICAL WEAKNESS DETECTION
    // ===========================================

    /**
     * Detect weaknesses in critical OLQs.
     * 
     * Critical OLQs (6): RA, SA, CO-OP, SoR, LIV, COU
     * Auto-reject condition: Factor II (Social) overall score = 8
     * 
     * @param scores Map of OLQ to score
     * @return CriticalWeaknessResult with detected weaknesses
     */
    fun detectCriticalWeaknesses(scores: Map<OLQ, Int>): CriticalWeaknessResult {
        // Find critical OLQs at limitation
        val criticalWeaknesses = scores
            .filterKeys { olq -> olq.isCritical }
            .filter { (_, score) -> SSBScoringRules.isLimitation(score) }
        
        // Check Factor II (Social) for auto-reject condition
        val factorIIScores = scores.filterKeys { olq -> olq.isFactorII }
        val hasAutoReject: Boolean
        val autoRejectReason: String?
        
        if (factorIIScores.isNotEmpty()) {
            val factorIIAverage = factorIIScores.values.average()
            hasAutoReject = factorIIAverage >= SSBScoringRules.FACTOR_II_CRITICAL_THRESHOLD
            autoRejectReason = if (hasAutoReject) {
                "Factor II (Social Adjustment) average score is ${String.format("%.1f", factorIIAverage)}, " +
                "which meets or exceeds the critical threshold of ${SSBScoringRules.FACTOR_II_CRITICAL_THRESHOLD}. " +
                "This is an automatic rejection criterion per SSB rules."
            } else null
        } else {
            hasAutoReject = false
            autoRejectReason = null
        }

        return CriticalWeaknessResult(
            criticalWeaknesses = criticalWeaknesses.keys,
            hasAutoRejectWeakness = hasAutoReject,
            autoRejectReason = autoRejectReason,
            weaknessScores = criticalWeaknesses
        )
    }

    // ===========================================
    // FACTOR AVERAGE CALCULATION
    // ===========================================

    /**
     * Calculate the average score for each factor.
     * 
     * @param scores Map of OLQ to score
     * @return Map of OLQCategory to average score (null if no scores for factor)
     */
    fun calculateFactorAverages(scores: Map<OLQ, Int>): Map<OLQCategory, Double?> {
        return OLQCategory.entries.associateWith { category ->
            val factorScores = scores.filterKeys { olq -> olq.category == category }
            if (factorScores.isNotEmpty()) {
                factorScores.values.average()
            } else {
                null
            }
        }
    }

    // ===========================================
    // RECOMMENDATION DETERMINATION
    // ===========================================

    /**
     * Determine the overall recommendation based on all validation factors.
     * 
     * NOT_RECOMMENDED if:
     * - Exceeds max limitations for entry type
     * - Factor II (Social) is at limitation level
     * - Auto-reject critical weakness detected
     * 
     * DOUBTFUL if:
     * - Factor consistency is poor (>±1 within factor)
     * - Borderline limitations (close to max)
     * - Critical OLQs are weak but not at limitation
     * 
     * RECOMMENDED if:
     * - All criteria pass
     * 
     * @param scores Map of OLQ to score
     * @param entryType NDA, OTA, or GRADUATE
     * @return RecommendationResult with recommendation and reasons
     */
    fun determineRecommendation(
        scores: Map<OLQ, Int>,
        entryType: EntryType
    ): RecommendationResult {
        val reasons = mutableListOf<String>()
        
        // Check limitations
        val limitationResult = countLimitations(scores)
        val exceedsLimitations = limitationResult.count > entryType.maxLimitations
        
        if (exceedsLimitations) {
            reasons.add(
                "Candidate has ${limitationResult.count} limitation(s), " +
                "exceeding the maximum of ${entryType.maxLimitations} for ${entryType.name} entry."
            )
        }
        
        // Check critical weaknesses
        val criticalResult = detectCriticalWeaknesses(scores)
        
        if (criticalResult.hasAutoRejectWeakness) {
            reasons.add(criticalResult.autoRejectReason ?: "Factor II overall is at limitation level.")
        }
        
        if (criticalResult.criticalWeaknesses.isNotEmpty()) {
            val olqNames = criticalResult.criticalWeaknesses.joinToString(", ") { it.displayName }
            reasons.add("Critical OLQ(s) at limitation: $olqNames")
        }
        
        // Check consistency
        val consistencyResult = checkFactorConsistency(scores)
        
        if (!consistencyResult.isConsistent) {
            val factorNames = consistencyResult.inconsistentFactors.joinToString(", ") { it.ssbFactorName }
            reasons.add(
                "Score inconsistency detected in factor(s): $factorNames. " +
                "Maximum variation found: ${consistencyResult.maxVariationFound} ticks."
            )
        }
        
        // Determine recommendation
        return when {
            exceedsLimitations || criticalResult.hasAutoRejectWeakness -> {
                RecommendationResult.notRecommended(reasons)
            }
            !consistencyResult.isConsistent || criticalResult.criticalWeaknesses.isNotEmpty() -> {
                RecommendationResult.doubtful(reasons)
            }
            else -> {
                RecommendationResult.recommended(reasons)
            }
        }
    }

    // ===========================================
    // COMPREHENSIVE VALIDATION
    // ===========================================

    /**
     * Perform comprehensive validation and return a full ValidationReport.
     * 
     * This aggregates all validation checks into a single report for
     * easy consumption by UI and logging components.
     * 
     * @param scores Map of OLQ to score
     * @param entryType NDA, OTA, or GRADUATE
     * @return Complete ValidationReport
     */
    fun validate(scores: Map<OLQ, Int>, entryType: EntryType): ValidationReport {
        val limitationResult = countLimitations(scores)
        val consistencyResult = checkFactorConsistency(scores)
        val criticalWeaknessResult = detectCriticalWeaknesses(scores)
        val factorAverages = calculateFactorAverages(scores)
            .filterValues { it != null }
            .mapValues { it.value!! }
        val recommendationResult = determineRecommendation(scores, entryType)
        
        return ValidationReport(
            limitationResult = limitationResult,
            consistencyResult = consistencyResult,
            criticalWeaknessResult = criticalWeaknessResult,
            factorAverages = factorAverages,
            recommendationResult = recommendationResult,
            originalScores = scores,
            entryType = entryType
        )
    }
}
