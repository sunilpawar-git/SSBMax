package com.ssbmax.shared.domain.model

/**
 * Result of a subscription-limit eligibility check for starting a test.
 *
 * KMP port of the Android `core:data` `SubscriptionManager.TestEligibility`
 * sealed class (see `core/data/.../repository/SubscriptionManager.kt`) — same
 * shape, moved to `domain` so [com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase]
 * (and the ViewModels that call it) can depend on it without an Android import.
 *
 * [Eligible]     — user may proceed.
 * [LimitReached] — monthly limit exhausted; caller shows an upgrade prompt.
 * [NetworkError] — transient connectivity failure; caller surfaces a retry action.
 *                  Security invariant preserved from the Android original: unknown/
 *                  unexpected exceptions map to [LimitReached] (fail-closed), not
 *                  [NetworkError], so only genuine network failures produce a retry path.
 */
sealed class TestEligibility {
    data class Eligible(val remainingTests: Int) : TestEligibility()
    data class LimitReached(
        val tier: SubscriptionTier,
        val limit: Int,
        val usedCount: Int,
        val resetsAt: String
    ) : TestEligibility()
    object NetworkError : TestEligibility()
}
