package com.ssbmax.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionTierTest {

    @Test
    fun tier_feature_flags_match_expectations() {
        assertFalse(SubscriptionTier.FREE.hasTestAccess)
        assertTrue(SubscriptionTier.PRO.hasTestAccess)
        assertTrue(SubscriptionTier.PREMIUM.hasTestAccess)

        assertFalse(SubscriptionTier.FREE.hasAIAnalysis)
        assertFalse(SubscriptionTier.PRO.hasAIAnalysis)
        assertTrue(SubscriptionTier.PREMIUM.hasAIAnalysis)
    }

    @Test
    fun pricing_progresses_monotonically() {
        assertEquals(0, SubscriptionTier.FREE.monthlyPriceInt)
        assertTrue(SubscriptionTier.PRO.monthlyPriceInt < SubscriptionTier.PREMIUM.monthlyPriceInt)
        assertEquals(999, SubscriptionTier.PREMIUM.monthlyPriceInt)

        assertEquals(999, SubscriptionTier.PRO.yearlyPriceInt)
        assertEquals(9999, SubscriptionTier.PREMIUM.yearlyPriceInt)
    }

    @Test
    fun user_subscription_expiry_and_renewal_flags() {
        val now = System.currentTimeMillis()
        val expired = UserSubscription(
            userId = "u1",
            tier = SubscriptionTier.PRO,
            expiryDate = now - 1_000L
        )
        assertTrue(expired.isExpired)
        assertTrue(expired.needsRenewal)

        val renewSoon = UserSubscription(
            userId = "u1",
            tier = SubscriptionTier.PRO,
            expiryDate = now + 3 * 24 * 60 * 60 * 1_000L // 3 days
        )
        assertFalse(renewSoon.isExpired)
        assertTrue(renewSoon.needsRenewal)

        val longTerm = UserSubscription(
            userId = "u1",
            tier = SubscriptionTier.PREMIUM,
            expiryDate = now + 10 * 24 * 60 * 60 * 1_000L // 10 days
        )
        assertFalse(longTerm.isExpired)
        assertFalse(longTerm.needsRenewal)
    }

    @Test
    fun billing_cycle_and_period_display_values() {
        assertEquals("Monthly", BillingCycle.MONTHLY.displayName)
        assertEquals("Quarterly", BillingCycle.QUARTERLY.displayName)
        assertEquals("Annually", BillingCycle.ANNUALLY.displayName)

        assertEquals("Monthly", BillingPeriod.MONTHLY.displayName)
        assertEquals(1, BillingPeriod.MONTHLY.months)
        assertEquals("Quarterly", BillingPeriod.QUARTERLY.displayName)
        assertEquals(3, BillingPeriod.QUARTERLY.months)
        assertEquals("Yearly", BillingPeriod.YEARLY.displayName)
        assertEquals(12, BillingPeriod.YEARLY.months)
    }
}





