package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SubscriptionTier
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the FREE/PRO/PREMIUM per-test-type limit table extracted from the
 * Android SubscriptionRepositoryImpl's inlined when-blocks. Regression target:
 * a silent limit change here (e.g. FREE OIR going from 1 to 0) would let paid
 * features leak to free users or block free users from what they're entitled
 * to — exactly the kind of "fails silently" migration risk the KMP plan calls
 * out, since nothing else would catch a wrong number in this table.
 */
class SubscriptionLimitsTest {

    @Test
    fun `FREE tier matches the Android SubscriptionRepositoryImpl limits`() {
        assertEquals(1, SubscriptionLimits.limitFor("OIR Tests", SubscriptionTier.FREE))
        assertEquals(1, SubscriptionLimits.limitFor("PPDT Tests", SubscriptionTier.FREE))
        assertEquals(1, SubscriptionLimits.limitFor("PIQ Forms", SubscriptionTier.FREE))
        assertEquals(0, SubscriptionLimits.limitFor("TAT Tests", SubscriptionTier.FREE))
        assertEquals(0, SubscriptionLimits.limitFor("Interview", SubscriptionTier.FREE))
    }

    @Test
    fun `PRO tier matches the Android SubscriptionRepositoryImpl limits`() {
        assertEquals(5, SubscriptionLimits.limitFor("OIR Tests", SubscriptionTier.PRO))
        assertEquals(5, SubscriptionLimits.limitFor("PPDT Tests", SubscriptionTier.PRO))
        assertEquals(-1, SubscriptionLimits.limitFor("PIQ Forms", SubscriptionTier.PRO))
        assertEquals(3, SubscriptionLimits.limitFor("TAT Tests", SubscriptionTier.PRO))
        assertEquals(1, SubscriptionLimits.limitFor("Interview", SubscriptionTier.PRO))
    }

    @Test
    fun `PREMIUM tier is unlimited for every test type`() {
        SubscriptionLimits.testTypeKeys.forEach { key ->
            assertEquals(-1, SubscriptionLimits.limitFor(key, SubscriptionTier.PREMIUM), "expected $key unlimited for PREMIUM")
        }
    }

    @Test
    fun `unknown test type key defaults to zero, not unlimited`() {
        assertEquals(0, SubscriptionLimits.limitFor("Nonexistent Test", SubscriptionTier.PREMIUM))
    }
}
