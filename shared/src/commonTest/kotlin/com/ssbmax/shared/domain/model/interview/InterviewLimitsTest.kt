package com.ssbmax.shared.domain.model.interview

import com.ssbmax.shared.data.repository.SubscriptionLimits
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.SubscriptionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression guard for the production crash: `InterviewLimits.init` used to require
 * `used + remaining == totalLimit`, which is unsatisfiable once `used` exceeds `totalLimit`
 * (remaining floors at 0). Any FREE/PRO user whose usage exceeded their tier's limit — reachable
 * once the dev subscription override lets a tester switch tiers mid-session — threw
 * `IllegalArgumentException` opening Start Interview. Also pins that `forSubscription` derives its
 * numbers from [SubscriptionLimits] (the SSOT) rather than redeclaring them, so the enforcement
 * and display tables can't silently diverge again.
 */
class InterviewLimitsTest {

    // =========================================================================
    // Crash regression: used above totalLimit must not throw, at every tier
    // =========================================================================

    @Test
    fun `used above totalLimit constructs without throwing for FREE`() {
        val limits = InterviewLimits.forSubscription(SubscriptionType.FREE, used = 5)
        assertEquals(0, limits.totalLimit)
        assertEquals(0, limits.remaining)
    }

    @Test
    fun `used above totalLimit constructs without throwing for PRO`() {
        val limits = InterviewLimits.forSubscription(SubscriptionType.PRO, used = 5)
        assertEquals(1, limits.totalLimit)
        assertEquals(0, limits.remaining)
    }

    @Test
    fun `used above totalLimit constructs without throwing for PREMIUM`() {
        val limits = InterviewLimits.forSubscription(SubscriptionType.PREMIUM, used = 5)
        assertEquals(3, limits.totalLimit)
        assertEquals(0, limits.remaining)
    }

    // =========================================================================
    // forSubscription must match SubscriptionLimits.limitFor("Interview", tier) exactly
    // =========================================================================

    @Test
    fun `forSubscription totalLimit matches SubscriptionLimits table for every tier`() {
        val pairs = listOf(
            SubscriptionType.FREE to SubscriptionTier.FREE,
            SubscriptionType.PRO to SubscriptionTier.PRO,
            SubscriptionType.PREMIUM to SubscriptionTier.PREMIUM
        )
        pairs.forEach { (type, tier) ->
            val expected = SubscriptionLimits.limitFor("Interview", tier)
            val actual = InterviewLimits.forSubscription(type, used = 0).totalLimit
            assertEquals(expected, actual, "$type totalLimit must match SubscriptionLimits table")
        }
    }

    // =========================================================================
    // hasInterviewsRemaining boundaries
    // =========================================================================

    @Test
    fun `hasInterviewsRemaining is false when used equals limit`() {
        assertFalse(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PRO, used = 1))
        assertFalse(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PREMIUM, used = 3))
    }

    @Test
    fun `hasInterviewsRemaining is false when used exceeds limit`() {
        assertFalse(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PRO, used = 2))
        assertFalse(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PREMIUM, used = 4))
    }

    @Test
    fun `hasInterviewsRemaining is true just under the limit`() {
        assertTrue(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PRO, used = 0))
        assertTrue(InterviewLimits.hasInterviewsRemaining(SubscriptionType.PREMIUM, used = 2))
    }

    @Test
    fun `FREE tier has zero interviews remaining even when unused`() {
        assertFalse(InterviewLimits.hasInterviewsRemaining(SubscriptionType.FREE, used = 0))
    }
}
