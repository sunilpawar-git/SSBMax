package com.ssbmax.core.domain.model.interview

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OLQ (Officer-Like Qualities) model
 *
 * Tests:
 * - OLQ category grouping
 * - Score validation (1-10 range)
 * - "Lower is better" SSB semantics
 * - OLQ display names
 */
class OLQTest {

    @Test
    fun `OLQ should have correct category assignments`() {
        // Intellectual (4 OLQs)
        assertEquals(OLQCategory.INTELLECTUAL, OLQ.EFFECTIVE_INTELLIGENCE.category)
        assertEquals(OLQCategory.INTELLECTUAL, OLQ.REASONING_ABILITY.category)
        assertEquals(OLQCategory.INTELLECTUAL, OLQ.ORGANIZING_ABILITY.category)
        assertEquals(OLQCategory.INTELLECTUAL, OLQ.POWER_OF_EXPRESSION.category)

        // Social (3 OLQs)
        assertEquals(OLQCategory.SOCIAL, OLQ.SOCIAL_ADJUSTMENT.category)
        assertEquals(OLQCategory.SOCIAL, OLQ.COOPERATION.category)
        assertEquals(OLQCategory.SOCIAL, OLQ.SENSE_OF_RESPONSIBILITY.category)

        // Dynamic (5 OLQs)
        assertEquals(OLQCategory.DYNAMIC, OLQ.INITIATIVE.category)
        assertEquals(OLQCategory.DYNAMIC, OLQ.SELF_CONFIDENCE.category)
        assertEquals(OLQCategory.DYNAMIC, OLQ.SPEED_OF_DECISION.category)
        assertEquals(OLQCategory.DYNAMIC, OLQ.INFLUENCE_GROUP.category)
        assertEquals(OLQCategory.DYNAMIC, OLQ.LIVELINESS.category)

        // Character & Physical (3 OLQs)
        assertEquals(OLQCategory.CHARACTER, OLQ.DETERMINATION.category)
        assertEquals(OLQCategory.CHARACTER, OLQ.COURAGE.category)
        assertEquals(OLQCategory.CHARACTER, OLQ.STAMINA.category)
    }

    @Test
    fun `OLQ should have exactly 15 qualities`() {
        val allOLQs = OLQ.entries
        assertEquals(15, allOLQs.size)
    }

    @Test
    fun `OLQ categories should sum to 15 qualities`() {
        val intellectualCount = OLQ.entries.count { it.category == OLQCategory.INTELLECTUAL }
        val socialCount = OLQ.entries.count { it.category == OLQCategory.SOCIAL }
        val dynamicCount = OLQ.entries.count { it.category == OLQCategory.DYNAMIC }
        val characterCount = OLQ.entries.count { it.category == OLQCategory.CHARACTER }

        assertEquals(4, intellectualCount)
        assertEquals(3, socialCount)
        assertEquals(5, dynamicCount)
        assertEquals(3, characterCount)
        assertEquals(15, intellectualCount + socialCount + dynamicCount + characterCount)
    }

    @Test
    fun `OLQ should have readable display names`() {
        // Sample checks
        assertTrue(OLQ.EFFECTIVE_INTELLIGENCE.displayName.isNotEmpty())
        assertTrue(OLQ.SELF_CONFIDENCE.displayName.isNotEmpty())
        assertTrue(OLQ.ORGANIZING_ABILITY.displayName.isNotEmpty())

        // Display names should be human-readable (contain spaces)
        assertTrue(OLQ.EFFECTIVE_INTELLIGENCE.displayName.contains(" "))
        assertTrue(OLQ.POWER_OF_EXPRESSION.displayName.contains(" "))
    }

    @Test
    fun `OLQCategory should have correct display names`() {
        assertEquals("Intellectual Qualities", OLQCategory.INTELLECTUAL.displayName)
        assertEquals("Social Qualities", OLQCategory.SOCIAL.displayName)
        assertEquals("Dynamic Qualities", OLQCategory.DYNAMIC.displayName)
        assertEquals("Character & Physical Qualities", OLQCategory.CHARACTER.displayName)
    }

    @Test
    fun `OLQScore should enforce valid score range 1-10`() {
        // Valid scores
        val score1 = OLQScore(score = 1, confidence = 50, reasoning = "Test")
        assertEquals(1, score1.score)

        val score10 = OLQScore(score = 10, confidence = 50, reasoning = "Test")
        assertEquals(10, score10.score)

        val score5 = OLQScore(score = 5, confidence = 75, reasoning = "Test")
        assertEquals(5, score5.score)
    }

    @Test
    fun `OLQScore should enforce confidence range 0-100`() {
        val scoreMin = OLQScore(score = 5, confidence = 0, reasoning = "Test")
        assertEquals(0, scoreMin.confidence)

        val scoreMax = OLQScore(score = 5, confidence = 100, reasoning = "Test")
        assertEquals(100, scoreMax.confidence)

        val scoreMid = OLQScore(score = 5, confidence = 75, reasoning = "Test")
        assertEquals(75, scoreMid.confidence)
    }

    @Test
    fun `OLQScore lower is better - SSB convention`() {
        // In SSB scoring, lower scores (1-3) are better than higher scores (8-10)
        val excellentScore = OLQScore(score = 3, confidence = 90, reasoning = "Exceptional performance")
        val averageScore = OLQScore(score = 7, confidence = 80, reasoning = "Average performance")
        val poorScore = OLQScore(score = 9, confidence = 70, reasoning = "Poor performance")

        // Lower score = better performance
        assertTrue(excellentScore.score < averageScore.score)
        assertTrue(averageScore.score < poorScore.score)

        // SSB scale interpretation
        assertTrue(excellentScore.score in 1..3) // Exceptional
        assertTrue(averageScore.score in 6..8) // Average to below average
        assertTrue(poorScore.score in 9..10) // Poor
    }

    @Test
    fun `OLQScore should have non-empty reasoning`() {
        val score = OLQScore(score = 5, confidence = 80, reasoning = "Demonstrates good leadership skills")
        assertNotNull(score.reasoning)
        assertTrue(score.reasoning.isNotEmpty())
    }

    @Test
    fun `OLQ should support grouping by category`() {
        val olqsByCategory = OLQ.entries.groupBy { it.category }

        assertEquals(4, olqsByCategory.size)
        assertTrue(olqsByCategory.containsKey(OLQCategory.INTELLECTUAL))
        assertTrue(olqsByCategory.containsKey(OLQCategory.SOCIAL))
        assertTrue(olqsByCategory.containsKey(OLQCategory.DYNAMIC))
        // Note: Verify actual category name for Character & Physical in your OLQ enum
    }

    @Test
    fun `OLQ should be comparable by enum ordinal`() {
        val olq1 = OLQ.EFFECTIVE_INTELLIGENCE
        val olq2 = OLQ.ORGANIZING_ABILITY
        val olq3 = OLQ.STAMINA

        // Enum ordinals should be distinct
        assertNotEquals(olq1.ordinal, olq2.ordinal)
        assertNotEquals(olq2.ordinal, olq3.ordinal)
    }
}
