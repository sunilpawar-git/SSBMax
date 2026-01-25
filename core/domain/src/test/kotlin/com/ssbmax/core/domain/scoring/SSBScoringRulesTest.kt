package com.ssbmax.core.domain.scoring

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SSBScoringRules - Single Source of Truth for SSB scoring constants
 *
 * Tests verify:
 * - All constants match official SSB documentation
 * - Limitation limits are correct per entry type
 * - Critical qualities are properly identified
 * - Score range is valid (5-9 for normal, 1-10 for edge cases)
 */
class SSBScoringRulesTest {

    // =========================================================================
    // TEST GROUP 1: Limitation Limits by Entry Type
    // =========================================================================

    @Test
    fun `NDA entry should have max 4 limitations`() {
        assertEquals(4, SSBScoringRules.MAX_LIMITATIONS_NDA)
    }

    @Test
    fun `OTA entry should have max 7 limitations`() {
        assertEquals(7, SSBScoringRules.MAX_LIMITATIONS_OTA)
    }

    @Test
    fun `Graduate entry should have max 7 limitations`() {
        assertEquals(7, SSBScoringRules.MAX_LIMITATIONS_GRADUATE)
    }

    // =========================================================================
    // TEST GROUP 2: Limitation Definition
    // =========================================================================

    @Test
    fun `limitation score threshold should be 8`() {
        assertEquals(8, SSBScoringRules.LIMITATION_THRESHOLD)
    }

    @Test
    fun `score 8 should be considered a limitation`() {
        assertTrue(SSBScoringRules.isLimitation(8))
    }

    @Test
    fun `score 7 should NOT be considered a limitation`() {
        assertFalse(SSBScoringRules.isLimitation(7))
    }

    @Test
    fun `score 9 should be considered a limitation`() {
        assertTrue(SSBScoringRules.isLimitation(9))
    }

    @Test
    fun `score 5 should NOT be considered a limitation`() {
        assertFalse(SSBScoringRules.isLimitation(5))
    }

    // =========================================================================
    // TEST GROUP 3: Score Range Constants
    // =========================================================================

    @Test
    fun `minimum score should be 1`() {
        assertEquals(1, SSBScoringRules.MIN_SCORE)
    }

    @Test
    fun `maximum score should be 10`() {
        assertEquals(10, SSBScoringRules.MAX_SCORE)
    }

    @Test
    fun `practical minimum score for prompts should be 5`() {
        assertEquals(5, SSBScoringRules.PRACTICAL_MIN_SCORE)
    }

    @Test
    fun `practical maximum score for prompts should be 9`() {
        assertEquals(9, SSBScoringRules.PRACTICAL_MAX_SCORE)
    }

    @Test
    fun `average expected score should be 7`() {
        assertEquals(7, SSBScoringRules.AVERAGE_EXPECTED_SCORE)
    }

    // =========================================================================
    // TEST GROUP 4: Factor Consistency Rules
    // =========================================================================

    @Test
    fun `max tick variation within factor should be 1`() {
        assertEquals(1, SSBScoringRules.MAX_TICK_VARIATION_WITHIN_FACTOR)
    }

    @Test
    fun `max tick variation between factors should be 2`() {
        assertEquals(2, SSBScoringRules.MAX_TICK_VARIATION_BETWEEN_FACTORS)
    }

    // =========================================================================
    // TEST GROUP 5: Critical Factor (Factor II)
    // =========================================================================

    @Test
    fun `Factor II critical threshold should be 8`() {
        assertEquals(8, SSBScoringRules.FACTOR_II_CRITICAL_THRESHOLD)
    }

    @Test
    fun `Factor II caution threshold should be 7`() {
        assertEquals(7, SSBScoringRules.FACTOR_II_CAUTION_THRESHOLD)
    }

    // =========================================================================
    // TEST GROUP 6: SSB Factor Numbers
    // =========================================================================

    @Test
    fun `should have exactly 4 SSB factors`() {
        assertEquals(4, SSBScoringRules.FACTOR_COUNT)
    }

    @Test
    fun `Factor I should be Planning and Organizing`() {
        assertEquals("Planning & Organizing", SSBScoringRules.FACTOR_I_NAME)
    }

    @Test
    fun `Factor II should be Social Adjustment`() {
        assertEquals("Social Adjustment", SSBScoringRules.FACTOR_II_NAME)
    }

    @Test
    fun `Factor III should be Social Effectiveness`() {
        assertEquals("Social Effectiveness", SSBScoringRules.FACTOR_III_NAME)
    }

    @Test
    fun `Factor IV should be Dynamic`() {
        assertEquals("Dynamic", SSBScoringRules.FACTOR_IV_NAME)
    }

    // =========================================================================
    // TEST GROUP 7: Utility Functions
    // =========================================================================

    @Test
    fun `getMaxLimitations should return correct value for NDA`() {
        assertEquals(4, SSBScoringRules.getMaxLimitations(EntryType.NDA))
    }

    @Test
    fun `getMaxLimitations should return correct value for OTA`() {
        assertEquals(7, SSBScoringRules.getMaxLimitations(EntryType.OTA))
    }

    @Test
    fun `getMaxLimitations should return correct value for GRADUATE`() {
        assertEquals(7, SSBScoringRules.getMaxLimitations(EntryType.GRADUATE))
    }

    @Test
    fun `isWithinFactorConsistency should return true for spread of 1`() {
        assertTrue(SSBScoringRules.isWithinFactorConsistency(listOf(6, 7, 6, 7)))
    }

    @Test
    fun `isWithinFactorConsistency should return false for spread of 3`() {
        assertFalse(SSBScoringRules.isWithinFactorConsistency(listOf(5, 8, 6, 7)))
    }

    @Test
    fun `isWithinFactorConsistency should return true for identical scores`() {
        assertTrue(SSBScoringRules.isWithinFactorConsistency(listOf(7, 7, 7)))
    }

    @Test
    fun `isWithinFactorConsistency should handle empty list`() {
        assertTrue(SSBScoringRules.isWithinFactorConsistency(emptyList()))
    }

    @Test
    fun `isWithinFactorConsistency should handle single score`() {
        assertTrue(SSBScoringRules.isWithinFactorConsistency(listOf(7)))
    }
}
