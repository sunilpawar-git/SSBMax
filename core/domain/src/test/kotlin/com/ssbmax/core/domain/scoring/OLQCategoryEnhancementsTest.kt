package com.ssbmax.core.domain.scoring

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OLQCategory enhancements related to SSB scoring
 *
 * Tests verify:
 * - SSB factor numbers are correctly assigned
 * - Factor names match SSB documentation
 * - Consistency rules are properly defined per category
 */
class OLQCategoryEnhancementsTest {

    // =========================================================================
    // TEST GROUP 1: SSB Factor Number Assignments
    // =========================================================================

    @Test
    fun `INTELLECTUAL should map to Factor I`() {
        assertEquals(1, OLQCategory.INTELLECTUAL.ssbFactorNumber)
    }

    @Test
    fun `SOCIAL should map to Factor II`() {
        assertEquals(2, OLQCategory.SOCIAL.ssbFactorNumber)
    }

    @Test
    fun `DYNAMIC should map to Factor III`() {
        assertEquals(3, OLQCategory.DYNAMIC.ssbFactorNumber)
    }

    @Test
    fun `CHARACTER should map to Factor IV`() {
        assertEquals(4, OLQCategory.CHARACTER.ssbFactorNumber)
    }

    // =========================================================================
    // TEST GROUP 2: SSB Factor Names
    // =========================================================================

    @Test
    fun `INTELLECTUAL should have SSB name Planning and Organizing`() {
        assertEquals("Planning & Organizing", OLQCategory.INTELLECTUAL.ssbFactorName)
    }

    @Test
    fun `SOCIAL should have SSB name Social Adjustment`() {
        assertEquals("Social Adjustment", OLQCategory.SOCIAL.ssbFactorName)
    }

    @Test
    fun `DYNAMIC should have SSB name Social Effectiveness`() {
        assertEquals("Social Effectiveness", OLQCategory.DYNAMIC.ssbFactorName)
    }

    @Test
    fun `CHARACTER should have SSB name Dynamic`() {
        assertEquals("Dynamic", OLQCategory.CHARACTER.ssbFactorName)
    }

    // =========================================================================
    // TEST GROUP 3: Max Tick Variation Per Category
    // =========================================================================

    @Test
    fun `INTELLECTUAL should allow max 1 tick variation`() {
        assertEquals(1, OLQCategory.INTELLECTUAL.maxTickVariation)
    }

    @Test
    fun `SOCIAL should allow max 1 tick variation`() {
        assertEquals(1, OLQCategory.SOCIAL.maxTickVariation)
    }

    @Test
    fun `DYNAMIC should allow max 2 tick variation`() {
        assertEquals(2, OLQCategory.DYNAMIC.maxTickVariation)
    }

    @Test
    fun `CHARACTER should allow max 2 tick variation`() {
        assertEquals(2, OLQCategory.CHARACTER.maxTickVariation)
    }

    // =========================================================================
    // TEST GROUP 4: Critical Factor Identification
    // =========================================================================

    @Test
    fun `SOCIAL should be marked as critical factor`() {
        assertTrue(OLQCategory.SOCIAL.isCriticalFactor)
    }

    @Test
    fun `INTELLECTUAL should NOT be marked as critical factor`() {
        assertFalse(OLQCategory.INTELLECTUAL.isCriticalFactor)
    }

    @Test
    fun `DYNAMIC should NOT be marked as critical factor`() {
        assertFalse(OLQCategory.DYNAMIC.isCriticalFactor)
    }

    @Test
    fun `CHARACTER should NOT be marked as critical factor`() {
        assertFalse(OLQCategory.CHARACTER.isCriticalFactor)
    }

    @Test
    fun `only one category should be critical factor`() {
        val criticalCount = OLQCategory.entries.count { it.isCriticalFactor }
        assertEquals(1, criticalCount)
    }

    // =========================================================================
    // TEST GROUP 5: Category to OLQ Mapping Consistency
    // =========================================================================

    @Test
    fun `all 4 categories should exist`() {
        assertEquals(4, OLQCategory.entries.size)
    }

    @Test
    fun `each OLQ should belong to exactly one category`() {
        val categoryCounts = OLQCategory.entries.associate { category ->
            category to OLQ.entries.count { it.category == category }
        }
        
        assertEquals(4, categoryCounts[OLQCategory.INTELLECTUAL])
        assertEquals(3, categoryCounts[OLQCategory.SOCIAL])
        assertEquals(5, categoryCounts[OLQCategory.DYNAMIC])
        assertEquals(3, categoryCounts[OLQCategory.CHARACTER])
    }

    // =========================================================================
    // TEST GROUP 6: getByFactorNumber() Helper
    // =========================================================================

    @Test
    fun `getByFactorNumber should return INTELLECTUAL for factor 1`() {
        assertEquals(OLQCategory.INTELLECTUAL, OLQCategory.getByFactorNumber(1))
    }

    @Test
    fun `getByFactorNumber should return SOCIAL for factor 2`() {
        assertEquals(OLQCategory.SOCIAL, OLQCategory.getByFactorNumber(2))
    }

    @Test
    fun `getByFactorNumber should return DYNAMIC for factor 3`() {
        assertEquals(OLQCategory.DYNAMIC, OLQCategory.getByFactorNumber(3))
    }

    @Test
    fun `getByFactorNumber should return CHARACTER for factor 4`() {
        assertEquals(OLQCategory.CHARACTER, OLQCategory.getByFactorNumber(4))
    }

    @Test
    fun `getByFactorNumber should return null for invalid factor number`() {
        assertNull(OLQCategory.getByFactorNumber(0))
        assertNull(OLQCategory.getByFactorNumber(5))
        assertNull(OLQCategory.getByFactorNumber(-1))
    }

    // =========================================================================
    // TEST GROUP 7: getCriticalFactor() Helper
    // =========================================================================

    @Test
    fun `getCriticalFactor should return SOCIAL`() {
        assertEquals(OLQCategory.SOCIAL, OLQCategory.getCriticalFactor())
    }
}
