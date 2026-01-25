package com.ssbmax.core.domain.model.interview

/**
 * 15 Officer Like Qualities (OLQs) assessed during SSB interviews
 *
 * These qualities are grouped into 4 SSB Factors:
 * - Factor I (Planning & Organizing): EI, RA, OA, PoE
 * - Factor II (Social Adjustment): SA, CO-OP, SoR - MOST CRITICAL
 * - Factor III (Social Effectiveness): INI, SC, SoD, AIG, LIV
 * - Factor IV (Dynamic): DET, COU, STA
 *
 * Critical Qualities (auto-reject if score >= 8):
 * - REASONING_ABILITY (Factor I)
 * - All Factor II qualities (SA, CO-OP, SoR)
 * - LIVELINESS (Factor III)
 * - COURAGE (Factor IV)
 */
enum class OLQ(val displayName: String, val category: OLQCategory) {
    // Factor I: Planning & Organizing (Intellectual Qualities)
    EFFECTIVE_INTELLIGENCE("Effective Intelligence", OLQCategory.INTELLECTUAL),
    REASONING_ABILITY("Reasoning Ability", OLQCategory.INTELLECTUAL),
    ORGANIZING_ABILITY("Organizing Ability", OLQCategory.INTELLECTUAL),
    POWER_OF_EXPRESSION("Power of Expression", OLQCategory.INTELLECTUAL),

    // Factor II: Social Adjustment (Social Qualities) - MOST CRITICAL FACTOR
    SOCIAL_ADJUSTMENT("Social Adjustment", OLQCategory.SOCIAL),
    COOPERATION("Cooperation", OLQCategory.SOCIAL),
    SENSE_OF_RESPONSIBILITY("Sense of Responsibility", OLQCategory.SOCIAL),

    // Factor III: Social Effectiveness (Dynamic Qualities)
    INITIATIVE("Initiative", OLQCategory.DYNAMIC),
    SELF_CONFIDENCE("Self Confidence", OLQCategory.DYNAMIC),
    SPEED_OF_DECISION("Speed of Decision", OLQCategory.DYNAMIC),
    INFLUENCE_GROUP("Ability to Influence Group", OLQCategory.DYNAMIC),
    LIVELINESS("Liveliness", OLQCategory.DYNAMIC),

    // Factor IV: Dynamic (Character & Physical Qualities)
    DETERMINATION("Determination", OLQCategory.CHARACTER),
    COURAGE("Courage", OLQCategory.CHARACTER),
    STAMINA("Stamina", OLQCategory.CHARACTER);

    /**
     * Whether this OLQ is a critical quality that auto-rejects if score >= 8
     *
     * Critical qualities per SSB documentation:
     * - REASONING_ABILITY: Core officer thinking ability
     * - All Factor II: Social adjustment is paramount
     * - LIVELINESS: Morale and optimism
     * - COURAGE: Leadership under pressure
     */
    val isCritical: Boolean
        get() = this in CRITICAL_QUALITIES
    
    /**
     * Whether this OLQ belongs to Factor II (Social Adjustment)
     * Factor II is the most critical factor - if overall Factor II = 8, candidate is auto-rejected
     */
    val isFactorII: Boolean
        get() = category == OLQCategory.SOCIAL

    companion object {
        /**
         * Set of critical qualities that require special attention if score >= 8
         */
        private val CRITICAL_QUALITIES = setOf(
            REASONING_ABILITY,      // Factor I critical
            SOCIAL_ADJUSTMENT,      // Factor II (all are critical)
            COOPERATION,            // Factor II (all are critical)
            SENSE_OF_RESPONSIBILITY,// Factor II (all are critical)
            LIVELINESS,             // Factor III critical
            COURAGE                 // Factor IV critical
        )

        /**
         * Get all OLQs in a specific category
         */
        fun getByCategory(category: OLQCategory): List<OLQ> {
            return entries.filter { it.category == category }
        }
        
        /**
         * Get all critical qualities
         * @return List of OLQs that are critical for SSB selection
         */
        fun getCriticalQualities(): List<OLQ> {
            return entries.filter { it.isCritical }
        }
        
        /**
         * Get all Factor II qualities (Social Adjustment)
         * Factor II is the most critical factor for SSB selection
         * @return List of OLQs in Factor II
         */
        fun getFactorIIQualities(): List<OLQ> {
            return entries.filter { it.isFactorII }
        }
    }
}

/**
 * OLQ Categories mapping to SSB Factors
 *
 * SSB Factor Structure:
 * - Factor I (Planning & Organizing): INTELLECTUAL - ±1 tick variance
 * - Factor II (Social Adjustment): SOCIAL - ±1 tick variance, MOST CRITICAL
 * - Factor III (Social Effectiveness): DYNAMIC - ±2 tick variance
 * - Factor IV (Dynamic): CHARACTER - ±2 tick variance
 *
 * @param displayName Human-readable category name
 * @param ssbFactorNumber SSB factor number (I=1, II=2, III=3, IV=4)
 * @param ssbFactorName Official SSB factor name
 * @param maxTickVariation Maximum allowed score variation within this factor
 * @param isCriticalFactor Whether this factor auto-rejects if overall = 8
 */
enum class OLQCategory(
    val displayName: String,
    val ssbFactorNumber: Int,
    val ssbFactorName: String,
    val maxTickVariation: Int,
    val isCriticalFactor: Boolean
) {
    /**
     * Factor I: Planning & Organizing
     * Contains: EI, RA, OA, PoE
     * Strict consistency (±1 tick)
     */
    INTELLECTUAL(
        displayName = "Intellectual Qualities",
        ssbFactorNumber = 1,
        ssbFactorName = "Planning & Organizing",
        maxTickVariation = 1,
        isCriticalFactor = false
    ),
    
    /**
     * Factor II: Social Adjustment - MOST CRITICAL
     * Contains: SA, CO-OP, SoR
     * Strict consistency (±1 tick)
     * If Factor II overall = 8 → Automatic rejection
     */
    SOCIAL(
        displayName = "Social Qualities",
        ssbFactorNumber = 2,
        ssbFactorName = "Social Adjustment",
        maxTickVariation = 1,
        isCriticalFactor = true
    ),
    
    /**
     * Factor III: Social Effectiveness
     * Contains: INI, SC, SoD, AIG, LIV
     * More lenient consistency (±2 tick)
     */
    DYNAMIC(
        displayName = "Dynamic Qualities",
        ssbFactorNumber = 3,
        ssbFactorName = "Social Effectiveness",
        maxTickVariation = 2,
        isCriticalFactor = false
    ),
    
    /**
     * Factor IV: Dynamic
     * Contains: DET, COU, STA
     * More lenient consistency (±2 tick)
     */
    CHARACTER(
        displayName = "Character & Physical Qualities",
        ssbFactorNumber = 4,
        ssbFactorName = "Dynamic",
        maxTickVariation = 2,
        isCriticalFactor = false
    );
    
    companion object {
        /**
         * Get category by SSB factor number
         * @param factorNumber Factor number (1-4)
         * @return OLQCategory or null if invalid
         */
        fun getByFactorNumber(factorNumber: Int): OLQCategory? {
            return entries.find { it.ssbFactorNumber == factorNumber }
        }
        
        /**
         * Get the critical factor (Factor II - Social Adjustment)
         * @return The critical factor category
         */
        fun getCriticalFactor(): OLQCategory {
            return entries.first { it.isCriticalFactor }
        }
    }
}

/**
 * OLQ Score with confidence level (SSB Convention)
 *
 * SSB uses 1-10 scale with LOWER numbers indicating BETTER performance (bell curve distribution)
 * Typical distribution: 1-4 (Exceptional, rare), 5-6 (Good, common), 7 (Average), 8+ (Below Average/Poor)
 *
 * @param score Score from 1-10 (1 = Exceptional, 5 = Very Good, 8 = Below Average, 10 = Poor)
 * @param confidence AI confidence in this assessment (0-100%)
 * @param reasoning Brief explanation for the score
 */
data class OLQScore(
    val score: Int,
    val confidence: Int,
    val reasoning: String
) {
    init {
        require(score in 1..10) { "OLQ score must be between 1 and 10" }
        require(confidence in 0..100) { "Confidence must be between 0 and 100" }
    }

    val rating: String
        get() = when (score) {
            1, 2, 3 -> "Exceptional"  // SSB: Rare, outstanding performance
            4 -> "Excellent"          // SSB: Top tier
            5 -> "Very Good"          // SSB: Best common score
            6 -> "Good"               // SSB: Above average
            7 -> "Average"            // SSB: Typical performance
            8 -> "Below Average"      // SSB: Lowest acceptable
            9, 10 -> "Poor"           // SSB: Usually rejected
            else -> "Unknown"
        }
}
