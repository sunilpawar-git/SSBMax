package com.ssbmax.core.domain.scoring

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OLQ enhancements related to SSB scoring
 *
 * Tests verify:
 * - Critical qualities are correctly identified
 * - SSB factor mappings are correct
 * - Helper functions work correctly
 */
class OLQEnhancementsTest {

    // =========================================================================
    // TEST GROUP 1: Critical Qualities Identification
    // =========================================================================

    @Test
    fun `REASONING_ABILITY should be critical`() {
        assertTrue(OLQ.REASONING_ABILITY.isCritical)
    }

    @Test
    fun `SOCIAL_ADJUSTMENT should be critical`() {
        assertTrue(OLQ.SOCIAL_ADJUSTMENT.isCritical)
    }

    @Test
    fun `COOPERATION should be critical`() {
        assertTrue(OLQ.COOPERATION.isCritical)
    }

    @Test
    fun `SENSE_OF_RESPONSIBILITY should be critical`() {
        assertTrue(OLQ.SENSE_OF_RESPONSIBILITY.isCritical)
    }

    @Test
    fun `LIVELINESS should be critical`() {
        assertTrue(OLQ.LIVELINESS.isCritical)
    }

    @Test
    fun `COURAGE should be critical`() {
        assertTrue(OLQ.COURAGE.isCritical)
    }

    @Test
    fun `EFFECTIVE_INTELLIGENCE should NOT be critical`() {
        assertFalse(OLQ.EFFECTIVE_INTELLIGENCE.isCritical)
    }

    @Test
    fun `ORGANIZING_ABILITY should NOT be critical`() {
        assertFalse(OLQ.ORGANIZING_ABILITY.isCritical)
    }

    @Test
    fun `POWER_OF_EXPRESSION should NOT be critical`() {
        assertFalse(OLQ.POWER_OF_EXPRESSION.isCritical)
    }

    @Test
    fun `INITIATIVE should NOT be critical`() {
        assertFalse(OLQ.INITIATIVE.isCritical)
    }

    @Test
    fun `SELF_CONFIDENCE should NOT be critical`() {
        assertFalse(OLQ.SELF_CONFIDENCE.isCritical)
    }

    @Test
    fun `SPEED_OF_DECISION should NOT be critical`() {
        assertFalse(OLQ.SPEED_OF_DECISION.isCritical)
    }

    @Test
    fun `INFLUENCE_GROUP should NOT be critical`() {
        assertFalse(OLQ.INFLUENCE_GROUP.isCritical)
    }

    @Test
    fun `DETERMINATION should NOT be critical`() {
        assertFalse(OLQ.DETERMINATION.isCritical)
    }

    @Test
    fun `STAMINA should NOT be critical`() {
        assertFalse(OLQ.STAMINA.isCritical)
    }

    @Test
    fun `exactly 6 OLQs should be critical`() {
        val criticalCount = OLQ.entries.count { it.isCritical }
        assertEquals(6, criticalCount)
    }

    // =========================================================================
    // TEST GROUP 2: getCriticalQualities() Helper
    // =========================================================================

    @Test
    fun `getCriticalQualities should return exactly 6 qualities`() {
        val criticalQualities = OLQ.getCriticalQualities()
        assertEquals(6, criticalQualities.size)
    }

    @Test
    fun `getCriticalQualities should contain all critical OLQs`() {
        val criticalQualities = OLQ.getCriticalQualities()
        assertTrue(criticalQualities.contains(OLQ.REASONING_ABILITY))
        assertTrue(criticalQualities.contains(OLQ.SOCIAL_ADJUSTMENT))
        assertTrue(criticalQualities.contains(OLQ.COOPERATION))
        assertTrue(criticalQualities.contains(OLQ.SENSE_OF_RESPONSIBILITY))
        assertTrue(criticalQualities.contains(OLQ.LIVELINESS))
        assertTrue(criticalQualities.contains(OLQ.COURAGE))
    }

    @Test
    fun `getCriticalQualities should NOT contain non-critical OLQs`() {
        val criticalQualities = OLQ.getCriticalQualities()
        assertFalse(criticalQualities.contains(OLQ.EFFECTIVE_INTELLIGENCE))
        assertFalse(criticalQualities.contains(OLQ.ORGANIZING_ABILITY))
        assertFalse(criticalQualities.contains(OLQ.POWER_OF_EXPRESSION))
        assertFalse(criticalQualities.contains(OLQ.INITIATIVE))
        assertFalse(criticalQualities.contains(OLQ.SELF_CONFIDENCE))
        assertFalse(criticalQualities.contains(OLQ.SPEED_OF_DECISION))
        assertFalse(criticalQualities.contains(OLQ.INFLUENCE_GROUP))
        assertFalse(criticalQualities.contains(OLQ.DETERMINATION))
        assertFalse(criticalQualities.contains(OLQ.STAMINA))
    }

    // =========================================================================
    // TEST GROUP 3: Factor II Detection
    // =========================================================================

    @Test
    fun `isFactorII should return true for SOCIAL_ADJUSTMENT`() {
        assertTrue(OLQ.SOCIAL_ADJUSTMENT.isFactorII)
    }

    @Test
    fun `isFactorII should return true for COOPERATION`() {
        assertTrue(OLQ.COOPERATION.isFactorII)
    }

    @Test
    fun `isFactorII should return true for SENSE_OF_RESPONSIBILITY`() {
        assertTrue(OLQ.SENSE_OF_RESPONSIBILITY.isFactorII)
    }

    @Test
    fun `isFactorII should return false for Factor I qualities`() {
        assertFalse(OLQ.EFFECTIVE_INTELLIGENCE.isFactorII)
        assertFalse(OLQ.REASONING_ABILITY.isFactorII)
        assertFalse(OLQ.ORGANIZING_ABILITY.isFactorII)
        assertFalse(OLQ.POWER_OF_EXPRESSION.isFactorII)
    }

    @Test
    fun `isFactorII should return false for Factor III qualities`() {
        assertFalse(OLQ.INITIATIVE.isFactorII)
        assertFalse(OLQ.SELF_CONFIDENCE.isFactorII)
        assertFalse(OLQ.SPEED_OF_DECISION.isFactorII)
        assertFalse(OLQ.INFLUENCE_GROUP.isFactorII)
        assertFalse(OLQ.LIVELINESS.isFactorII)
    }

    @Test
    fun `isFactorII should return false for Factor IV qualities`() {
        assertFalse(OLQ.DETERMINATION.isFactorII)
        assertFalse(OLQ.COURAGE.isFactorII)
        assertFalse(OLQ.STAMINA.isFactorII)
    }

    // =========================================================================
    // TEST GROUP 4: getFactorIIQualities() Helper
    // =========================================================================

    @Test
    fun `getFactorIIQualities should return exactly 3 qualities`() {
        val factorIIQualities = OLQ.getFactorIIQualities()
        assertEquals(3, factorIIQualities.size)
    }

    @Test
    fun `getFactorIIQualities should contain all Factor II OLQs`() {
        val factorIIQualities = OLQ.getFactorIIQualities()
        assertTrue(factorIIQualities.contains(OLQ.SOCIAL_ADJUSTMENT))
        assertTrue(factorIIQualities.contains(OLQ.COOPERATION))
        assertTrue(factorIIQualities.contains(OLQ.SENSE_OF_RESPONSIBILITY))
    }
}
