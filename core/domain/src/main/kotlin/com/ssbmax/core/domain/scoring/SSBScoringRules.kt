package com.ssbmax.core.domain.scoring

/**
 * Single Source of Truth for SSB Scoring Rules and Constants
 *
 * This object contains ALL constants related to SSB scoring patterns.
 * Any code needing SSB scoring constants MUST reference this object.
 *
 * Based on official SSB documentation:
 * - OLQ.pdf: 15 Officer-Like Qualities definitions
 * - TICKS & MARKS.pdf: Limitation system and scoring pattern
 *
 * Key Concepts:
 * - Limitation: Any OLQ score of 8 (on 1-10 scale where lower is better)
 * - Factor: Group of related OLQs (I, II, III, IV)
 * - Critical Quality: OLQ that auto-rejects if weak (score >= 8)
 */
object SSBScoringRules {

    // =========================================================================
    // SCORE RANGE CONSTANTS
    // =========================================================================
    
    /**
     * Minimum possible score (1 = Exceptional, rare)
     */
    const val MIN_SCORE = 1
    
    /**
     * Maximum possible score (10 = Very Poor)
     */
    const val MAX_SCORE = 10
    
    /**
     * Practical minimum for AI prompts (avoid unrealistic exceptional scores)
     * Most candidates don't score below 5
     */
    const val PRACTICAL_MIN_SCORE = 5
    
    /**
     * Practical maximum for AI prompts (9 = Fail/Gibberish)
     * Score of 10 is extremely rare
     */
    const val PRACTICAL_MAX_SCORE = 9
    
    /**
     * Expected average score for typical candidates
     * SSB bell curve centers around 7
     */
    const val AVERAGE_EXPECTED_SCORE = 7

    // =========================================================================
    // LIMITATION CONSTANTS
    // =========================================================================
    
    /**
     * Score threshold at which a quality becomes a "limitation"
     * SSB convention: Score of 8 = limitation (needs improvement)
     */
    const val LIMITATION_THRESHOLD = 8
    
    /**
     * Maximum limitations for NDA entry (most stringent)
     * NDA candidates are young and must be highly trainable
     */
    const val MAX_LIMITATIONS_NDA = 4
    
    /**
     * Maximum limitations for OTA entry
     * Short service commission - more lenient
     */
    const val MAX_LIMITATIONS_OTA = 7
    
    /**
     * Maximum limitations for Graduate entry (CDS, TGC, etc.)
     * Direct entry - same as OTA
     */
    const val MAX_LIMITATIONS_GRADUATE = 7

    // =========================================================================
    // FACTOR CONSISTENCY CONSTANTS
    // =========================================================================
    
    /**
     * Maximum allowed tick variation WITHIN a factor
     * Example: Factor II qualities should all be within ±1 of each other
     */
    const val MAX_TICK_VARIATION_WITHIN_FACTOR = 1
    
    /**
     * Maximum allowed tick variation BETWEEN factors
     * Example: Factor I avg vs Factor II avg should be within ±2
     */
    const val MAX_TICK_VARIATION_BETWEEN_FACTORS = 2

    // =========================================================================
    // CRITICAL FACTOR II THRESHOLDS
    // =========================================================================
    
    /**
     * Factor II average at or above this = AUTOMATIC REJECTION
     * Social Adjustment factor is most critical
     */
    const val FACTOR_II_CRITICAL_THRESHOLD = 8
    
    /**
     * Factor II average at this level = CLEAR WITH CAUTION
     * Borderline cases need careful consideration
     */
    const val FACTOR_II_CAUTION_THRESHOLD = 7

    // =========================================================================
    // SSB FACTOR DEFINITIONS
    // =========================================================================
    
    /**
     * Total number of SSB factors
     */
    const val FACTOR_COUNT = 4
    
    /**
     * Factor I: Planning & Organizing
     * Contains: EI, RA, OA, PoE (4 qualities)
     */
    const val FACTOR_I_NAME = "Planning & Organizing"
    
    /**
     * Factor II: Social Adjustment (MOST CRITICAL)
     * Contains: SA, CO-OP, SoR (3 qualities)
     */
    const val FACTOR_II_NAME = "Social Adjustment"
    
    /**
     * Factor III: Social Effectiveness
     * Contains: INI, SC, SoD, AIG, LIV (5 qualities)
     */
    const val FACTOR_III_NAME = "Social Effectiveness"
    
    /**
     * Factor IV: Dynamic
     * Contains: DET, COU, STA (3 qualities)
     */
    const val FACTOR_IV_NAME = "Dynamic"

    // =========================================================================
    // UTILITY FUNCTIONS
    // =========================================================================
    
    /**
     * Check if a score qualifies as a limitation
     *
     * @param score The OLQ score (1-10)
     * @return true if score >= LIMITATION_THRESHOLD (8)
     */
    fun isLimitation(score: Int): Boolean {
        return score >= LIMITATION_THRESHOLD
    }
    
    /**
     * Get maximum allowed limitations for an entry type
     *
     * @param entryType The candidate's entry type (NDA, OTA, GRADUATE)
     * @return Maximum number of limitations allowed
     */
    fun getMaxLimitations(entryType: EntryType): Int {
        return entryType.maxLimitations
    }
    
    /**
     * Check if scores within a factor are consistent
     *
     * SSB Rule: Scores within same factor should be within ±1 tick
     * Exception: Power of Expression can vary more (handled separately)
     *
     * @param scores List of scores for qualities in a factor
     * @return true if all scores are within MAX_TICK_VARIATION_WITHIN_FACTOR
     */
    fun isWithinFactorConsistency(scores: List<Int>): Boolean {
        if (scores.isEmpty() || scores.size == 1) return true
        
        val minScore = scores.minOrNull() ?: return true
        val maxScore = scores.maxOrNull() ?: return true
        
        return (maxScore - minScore) <= MAX_TICK_VARIATION_WITHIN_FACTOR
    }
}
