package com.ssbmax.core.domain.model.interview

/**
 * 15 Officer Like Qualities (OLQs) assessed during SSB interviews
 *
 * These qualities are grouped into 4 categories:
 * 1. Intellectual (EI, RA, OA, PE)
 * 2. Social (SA, C, SR)
 * 3. Dynamic (I, SC, SOD)
 * 4. Physical (IG, L, D, C, S)
 */
enum class OLQ(val displayName: String, val category: OLQCategory) {
    // Intellectual Qualities
    EFFECTIVE_INTELLIGENCE("Effective Intelligence", OLQCategory.INTELLECTUAL),
    REASONING_ABILITY("Reasoning Ability", OLQCategory.INTELLECTUAL),
    ORGANIZING_ABILITY("Organizing Ability", OLQCategory.INTELLECTUAL),
    POWER_OF_EXPRESSION("Power of Expression", OLQCategory.INTELLECTUAL),

    // Social Qualities
    SOCIAL_ADJUSTMENT("Social Adjustment", OLQCategory.SOCIAL),
    COOPERATION("Cooperation", OLQCategory.SOCIAL),
    SENSE_OF_RESPONSIBILITY("Sense of Responsibility", OLQCategory.SOCIAL),

    // Dynamic Qualities
    INITIATIVE("Initiative", OLQCategory.DYNAMIC),
    SELF_CONFIDENCE("Self Confidence", OLQCategory.DYNAMIC),
    SPEED_OF_DECISION("Speed of Decision", OLQCategory.DYNAMIC),
    INFLUENCE_GROUP("Ability to Influence Group", OLQCategory.DYNAMIC),
    LIVELINESS("Liveliness", OLQCategory.DYNAMIC),

    // Physical/Character Qualities
    DETERMINATION("Determination", OLQCategory.CHARACTER),
    COURAGE("Courage", OLQCategory.CHARACTER),
    STAMINA("Stamina", OLQCategory.CHARACTER);

    companion object {
        /**
         * Get all OLQs in a specific category
         */
        fun getByCategory(category: OLQCategory): List<OLQ> {
            return entries.filter { it.category == category }
        }
    }
}

/**
 * OLQ Categories for grouping
 */
enum class OLQCategory(val displayName: String) {
    INTELLECTUAL("Intellectual Qualities"),
    SOCIAL("Social Qualities"),
    DYNAMIC("Dynamic Qualities"),
    CHARACTER("Character & Physical Qualities")
}

/**
 * OLQ Score with confidence level
 *
 * @param score Score from 1-5 (1 = Poor, 5 = Excellent)
 * @param confidence AI confidence in this assessment (0-100%)
 * @param reasoning Brief explanation for the score
 */
data class OLQScore(
    val score: Int,
    val confidence: Int,
    val reasoning: String
) {
    init {
        require(score in 1..5) { "OLQ score must be between 1 and 5" }
        require(confidence in 0..100) { "Confidence must be between 0 and 100" }
    }

    val rating: String
        get() = when (score) {
            1 -> "Poor"
            2 -> "Below Average"
            3 -> "Average"
            4 -> "Good"
            5 -> "Excellent"
            else -> "Unknown"
        }
}
