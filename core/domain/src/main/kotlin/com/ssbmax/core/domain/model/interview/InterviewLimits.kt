package com.ssbmax.core.domain.model.interview

/**
 * Interview limits based on subscription tiers
 *
 * Centralized constants to avoid duplication across use cases and repositories
 */
object InterviewLimits {

    /**
     * Get maximum interview limit for a subscription tier and mode
     *
     * @param tierName Subscription tier name (FREE, PRO, PREMIUM)
     * @param mode Interview mode
     * @return Maximum interviews allowed
     */
    fun getLimit(tierName: String, mode: InterviewMode): Int {
        val tier = tierName.uppercase()
        return when {
            tier == "FREE" -> 0
            tier == "PRO" && mode == InterviewMode.TEXT_BASED -> FREE_TIER_TEXT_LIMIT
            tier == "PRO" && mode == InterviewMode.VOICE_BASED -> 0
            tier == "PREMIUM" && mode == InterviewMode.TEXT_BASED -> PREMIUM_TIER_TEXT_LIMIT
            tier == "PREMIUM" && mode == InterviewMode.VOICE_BASED -> PREMIUM_TIER_VOICE_LIMIT
            else -> 0
        }
    }

    /**
     * Check if a tier supports a specific interview mode
     *
     * @param tierName Subscription tier name
     * @param mode Interview mode
     * @return True if tier supports the mode, false otherwise
     */
    fun supportsMode(tierName: String, mode: InterviewMode): Boolean {
        val tier = tierName.uppercase()
        return when {
            tier == "FREE" -> false
            tier == "PRO" && mode == InterviewMode.VOICE_BASED -> false
            tier == "PRO" && mode == InterviewMode.TEXT_BASED -> true
            tier == "PREMIUM" -> true
            else -> false
        }
    }

    // Tier-specific limits
    private const val FREE_TIER_TEXT_LIMIT = 2  // Free tier (renamed from PRO)
    private const val PREMIUM_TIER_TEXT_LIMIT = 2
    private const val PREMIUM_TIER_VOICE_LIMIT = 2

    /**
     * Total interview limit for premium tier (all modes)
     */
    const val PREMIUM_TOTAL_LIMIT = PREMIUM_TIER_TEXT_LIMIT + PREMIUM_TIER_VOICE_LIMIT
}
