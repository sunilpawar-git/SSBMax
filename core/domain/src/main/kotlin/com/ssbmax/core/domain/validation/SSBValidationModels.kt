package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory

/**
 * Validation result models for SSB Score Validation.
 * All classes are immutable data classes following SOLID principles.
 */

/**
 * Result of limitation counting.
 * A limitation is any OLQ score >= 8 (on 1-10 scale where lower is better).
 */
data class LimitationResult(
    /** Number of OLQs at or above the limitation threshold */
    val count: Int,
    /** Set of OLQs that are at limitation level */
    val limitedOLQs: Set<OLQ>,
    /** Map of limited OLQs to their scores */
    val limitationScores: Map<OLQ, Int> = limitedOLQs.associateWith { 0 }
) {
    companion object {
        val NONE = LimitationResult(count = 0, limitedOLQs = emptySet(), limitationScores = emptyMap())
    }
}

/**
 * Result of factor consistency check.
 * SSB rules: ±1 tick variation within factors, ±2 between factors.
 */
data class ConsistencyResult(
    /** True if all factors are internally consistent (±1 tick) */
    val isConsistent: Boolean,
    /** Factors that have internal inconsistency */
    val inconsistentFactors: Set<OLQCategory>,
    /** Maximum variation found within any single factor */
    val maxVariationFound: Int,
    /** Details about each inconsistent factor */
    val details: Map<OLQCategory, FactorConsistencyDetail> = emptyMap()
) {
    companion object {
        val CONSISTENT = ConsistencyResult(
            isConsistent = true,
            inconsistentFactors = emptySet(),
            maxVariationFound = 0
        )
    }
}

/**
 * Detailed consistency information for a single factor.
 */
data class FactorConsistencyDetail(
    val category: OLQCategory,
    val minScore: Int,
    val maxScore: Int,
    val variation: Int,
    val isConsistent: Boolean
)

/**
 * Result of critical weakness detection.
 * 6 Critical OLQs: RA, SA, CO-OP, SoR, LIV, COU
 */
data class CriticalWeaknessResult(
    /** Critical OLQs that are at limitation level */
    val criticalWeaknesses: Set<OLQ>,
    /** True if any condition triggers automatic rejection */
    val hasAutoRejectWeakness: Boolean,
    /** Explanation for auto-reject if applicable */
    val autoRejectReason: String? = null,
    /** Map of critical weaknesses to their scores */
    val weaknessScores: Map<OLQ, Int> = emptyMap()
) {
    companion object {
        val NONE = CriticalWeaknessResult(
            criticalWeaknesses = emptySet(),
            hasAutoRejectWeakness = false
        )
    }
}

/**
 * Recommendation categories following SSB assessment patterns.
 */
enum class Recommendation {
    /** Candidate meets all criteria for recommendation */
    RECOMMENDED,
    /** Candidate does not meet minimum criteria */
    NOT_RECOMMENDED,
    /** Borderline case requiring careful consideration */
    DOUBTFUL
}

/**
 * Final recommendation result with supporting reasons.
 */
data class RecommendationResult(
    val recommendation: Recommendation,
    /** List of reasons supporting this recommendation */
    val reasons: List<String>,
    /** Overall assessment summary */
    val summary: String = ""
) {
    companion object {
        fun recommended(reasons: List<String> = emptyList()) = RecommendationResult(
            recommendation = Recommendation.RECOMMENDED,
            reasons = reasons,
            summary = "Candidate demonstrates adequate Officer-Like Qualities across all factors."
        )
        
        fun notRecommended(reasons: List<String>) = RecommendationResult(
            recommendation = Recommendation.NOT_RECOMMENDED,
            reasons = reasons,
            summary = "Candidate does not meet minimum criteria for recommendation."
        )
        
        fun doubtful(reasons: List<String>) = RecommendationResult(
            recommendation = Recommendation.DOUBTFUL,
            reasons = reasons,
            summary = "Borderline case with areas of concern requiring careful consideration."
        )
    }
}

/**
 * Comprehensive validation report aggregating all validation results.
 */
data class ValidationReport(
    /** Limitation count and details */
    val limitationResult: LimitationResult,
    /** Factor consistency results */
    val consistencyResult: ConsistencyResult,
    /** Critical weakness detection results */
    val criticalWeaknessResult: CriticalWeaknessResult,
    /** Average score per factor */
    val factorAverages: Map<OLQCategory, Double>,
    /** Final recommendation */
    val recommendationResult: RecommendationResult,
    /** Original scores used for validation */
    val originalScores: Map<OLQ, Int>,
    /** Entry type used for validation */
    val entryType: com.ssbmax.core.domain.scoring.EntryType
) {
    /**
     * Quick check if candidate passes all validations.
     */
    val isPassingCandidate: Boolean
        get() = recommendationResult.recommendation == Recommendation.RECOMMENDED
    
    /**
     * Quick check if candidate has any critical issues.
     */
    val hasCriticalIssues: Boolean
        get() = criticalWeaknessResult.hasAutoRejectWeakness ||
                limitationResult.count > entryType.maxLimitations
}
