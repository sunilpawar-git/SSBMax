package com.ssbmax.core.domain.model.interview

import com.ssbmax.core.domain.model.SubscriptionType

/**
 * TTS service type available based on subscription tier
 */
enum class TTSServiceType {
    ANDROID,      // Free tier - built-in Android TTS (robotic)
    SARVAM_AI     // Pro/Premium tier - Sarvam AI TTS (premium, with ElevenLabs fallback)
}

/**
 * Interview limits for unified interview system (TTS-based subscriptions)
 *
 * New model after removing text-based interview:
 * - All interviews use the same unified implementation
 * - Subscription tier determines TTS quality and monthly limit
 * - FREE: 1 interview/month with Android TTS
 * - PRO: 1 interview/month with Sarvam AI TTS
 * - PREMIUM: 3 interviews/month with Sarvam AI TTS
 *
 * @param subscriptionType User's subscription tier
 * @param totalLimit Total interviews allowed per month
 * @param used Number of interviews used this month
 * @param remaining Interviews remaining this month
 * @param ttsService TTS service provided for this tier
 */
data class InterviewLimits(
    val subscriptionType: SubscriptionType,
    val totalLimit: Int,
    val used: Int,
    val remaining: Int,
    val ttsService: TTSServiceType
) {
    init {
        require(totalLimit >= 0) { "Total limit cannot be negative" }
        require(used >= 0) { "Used count cannot be negative" }
        require(remaining >= 0) { "Remaining count cannot be negative" }
        require(used + remaining == totalLimit) { "Used + remaining must equal total limit" }
    }

    companion object {
        /**
         * Get interview limits for a subscription tier
         *
         * @param subscriptionType User's subscription tier
         * @param used Number of interviews already used this month
         * @return InterviewLimits with tier-specific values
         */
        fun forSubscription(subscriptionType: SubscriptionType, used: Int): InterviewLimits {
            return when (subscriptionType) {
                SubscriptionType.FREE -> InterviewLimits(
                    subscriptionType = subscriptionType,
                    totalLimit = 1,
                    used = used,
                    remaining = maxOf(0, 1 - used),
                    ttsService = TTSServiceType.ANDROID
                )
                SubscriptionType.PRO -> InterviewLimits(
                    subscriptionType = subscriptionType,
                    totalLimit = 1,
                    used = used,
                    remaining = maxOf(0, 1 - used),
                    ttsService = TTSServiceType.SARVAM_AI
                )
                SubscriptionType.PREMIUM -> InterviewLimits(
                    subscriptionType = subscriptionType,
                    totalLimit = 3,
                    used = used,
                    remaining = maxOf(0, 3 - used),
                    ttsService = TTSServiceType.SARVAM_AI
                )
            }
        }

        /**
         * Check if user has interviews remaining
         */
        fun hasInterviewsRemaining(subscriptionType: SubscriptionType, used: Int): Boolean {
            return forSubscription(subscriptionType, used).remaining > 0
        }

        /**
         * Get TTS service type for subscription tier
         */
        fun getTTSService(subscriptionType: SubscriptionType): TTSServiceType {
            return when (subscriptionType) {
                SubscriptionType.FREE -> TTSServiceType.ANDROID
                SubscriptionType.PRO, SubscriptionType.PREMIUM -> TTSServiceType.SARVAM_AI
            }
        }
    }

    /**
     * Check if user can start a new interview
     */
    fun canStartInterview(): Boolean = remaining > 0

    /**
     * Get percentage of limit used (0-100)
     */
    fun getUsagePercentage(): Int {
        if (totalLimit == 0) return 0
        return ((used.toFloat() / totalLimit) * 100).toInt()
    }

    /**
     * Get display string for TTS service
     */
    fun getTTSDisplayName(): String {
        return when (ttsService) {
            TTSServiceType.ANDROID -> "Standard Voice"
            TTSServiceType.SARVAM_AI -> "Premium AI Voice"
        }
    }
}
