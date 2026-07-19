package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SubscriptionTier
import kotlinx.serialization.Serializable

/** Wire-format DTO for users/{userId}/data/subscription. */
@Serializable
data class SubscriptionTierDto(val tier: String = "FREE") {
    fun toDomain(): SubscriptionTier = when (tier.uppercase()) {
        "PRO" -> SubscriptionTier.PRO
        "PREMIUM" -> SubscriptionTier.PREMIUM
        else -> SubscriptionTier.FREE
    }
}

/** Wire-format DTO for users/{userId}/subscription/usage_{yyyy-MM}. */
@Serializable
data class SubscriptionUsageDto(
    val oirTestsUsed: Int = 0,
    val ppdtTestsUsed: Int = 0,
    val piqTestsUsed: Int = 0,
    val tatTestsUsed: Int = 0,
    val watTestsUsed: Int = 0,
    val srtTestsUsed: Int = 0,
    val sdTestsUsed: Int = 0,
    val gtoTestsUsed: Int = 0,
    val interviewTestsUsed: Int = 0
)

/**
 * Test-type usage limits by tier — the SSOT the Android SubscriptionRepositoryImpl
 * inlined as a repeated when-block per test type. Extracted here as a pure,
 * directly unit-testable table (see SubscriptionLimitsTest), matching this
 * repo's CLAUDE.md preference for testing WHY behavior matters rather than
 * re-asserting Firestore plumbing.
 */
object SubscriptionLimits {
    private val limits: Map<String, Map<SubscriptionTier, Int>> = mapOf(
        "OIR Tests" to mapOf(SubscriptionTier.FREE to 1, SubscriptionTier.PRO to 5, SubscriptionTier.PREMIUM to -1),
        "PPDT Tests" to mapOf(SubscriptionTier.FREE to 1, SubscriptionTier.PRO to 5, SubscriptionTier.PREMIUM to -1),
        "PIQ Forms" to mapOf(SubscriptionTier.FREE to 1, SubscriptionTier.PRO to -1, SubscriptionTier.PREMIUM to -1),
        "TAT Tests" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 3, SubscriptionTier.PREMIUM to -1),
        "WAT Tests" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 3, SubscriptionTier.PREMIUM to -1),
        "SRT Tests" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 3, SubscriptionTier.PREMIUM to -1),
        "Self Description" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 3, SubscriptionTier.PREMIUM to -1),
        "GTO Tests" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 3, SubscriptionTier.PREMIUM to -1),
        "Interview" to mapOf(SubscriptionTier.FREE to 0, SubscriptionTier.PRO to 1, SubscriptionTier.PREMIUM to -1)
    )

    val testTypeKeys: Set<String> get() = limits.keys

    fun limitFor(testTypeKey: String, tier: SubscriptionTier): Int =
        limits[testTypeKey]?.get(tier) ?: 0
}
