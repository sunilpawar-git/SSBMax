package com.ssbmax.core.domain.model.interview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for OLQ Score validation ensuring:
 * 1. Scores must be in range 1-10 (domain constraint)
 * 2. Best practical score is 5 (Very Good)
 * 3. Worst practical score is 9-10 (Poor/Fail)
 * 4. Rating labels are correctly mapped
 */
class OLQScoreValidationTest {

    // =========================================================================
    // TEST GROUP 1: Score Range Validation (1-10)
    // =========================================================================

    @Test
    fun `OLQScore accepts valid score 1`() {
        val score = OLQScore(score = 1, confidence = 80, reasoning = "Exceptional")
        assertEquals(1, score.score)
        assertEquals("Exceptional", score.rating)
    }

    @Test
    fun `OLQScore accepts valid score 5`() {
        val score = OLQScore(score = 5, confidence = 90, reasoning = "Very good performance")
        assertEquals(5, score.score)
        assertEquals("Very Good", score.rating)
    }

    @Test
    fun `OLQScore accepts valid score 9`() {
        val score = OLQScore(score = 9, confidence = 100, reasoning = "Gibberish response")
        assertEquals(9, score.score)
        assertEquals("Poor", score.rating)
    }

    @Test
    fun `OLQScore accepts valid score 10`() {
        val score = OLQScore(score = 10, confidence = 100, reasoning = "Completely irrelevant")
        assertEquals(10, score.score)
        assertEquals("Poor", score.rating)
    }

    @Test
    fun `OLQScore rejects score below 1`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OLQScore(score = 0, confidence = 50, reasoning = "Invalid")
        }
        assertTrue(exception.message!!.contains("must be between 1 and 10"))
    }

    @Test
    fun `OLQScore rejects score above 10`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OLQScore(score = 11, confidence = 50, reasoning = "Invalid")
        }
        assertTrue(exception.message!!.contains("must be between 1 and 10"))
    }

    @Test
    fun `OLQScore rejects negative score`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OLQScore(score = -5, confidence = 50, reasoning = "Invalid")
        }
        assertTrue(exception.message!!.contains("must be between 1 and 10"))
    }

    // =========================================================================
    // TEST GROUP 2: Unified Scoring Scale (Best=5, Worst=9)
    // =========================================================================

    @Test
    fun `score 5 is rated as Very Good - best common score`() {
        val score = OLQScore(score = 5, confidence = 85, reasoning = "Good")
        assertEquals("Very Good", score.rating)
    }

    @Test
    fun `score 6 is rated as Good - above average`() {
        val score = OLQScore(score = 6, confidence = 80, reasoning = "Good")
        assertEquals("Good", score.rating)
    }

    @Test
    fun `score 7 is rated as Average`() {
        val score = OLQScore(score = 7, confidence = 75, reasoning = "Average")
        assertEquals("Average", score.rating)
    }

    @Test
    fun `score 8 is rated as Below Average`() {
        val score = OLQScore(score = 8, confidence = 70, reasoning = "Below average")
        assertEquals("Below Average", score.rating)
    }

    @Test
    fun `score 9 is rated as Poor - fail threshold`() {
        val score = OLQScore(score = 9, confidence = 100, reasoning = "Poor")
        assertEquals("Poor", score.rating)
    }

    // =========================================================================
    // TEST GROUP 3: Exceptional Scores (1-4) - Rare Cases
    // =========================================================================

    @Test
    fun `scores 1-3 are rated as Exceptional`() {
        listOf(1, 2, 3).forEach { s ->
            val score = OLQScore(score = s, confidence = 95, reasoning = "Exceptional")
            assertEquals("Score $s should be Exceptional", "Exceptional", score.rating)
        }
    }

    @Test
    fun `score 4 is rated as Excellent`() {
        val score = OLQScore(score = 4, confidence = 90, reasoning = "Excellent")
        assertEquals("Excellent", score.rating)
    }

    // =========================================================================
    // TEST GROUP 4: Confidence Validation
    // =========================================================================

    @Test
    fun `confidence accepts valid range 0-100`() {
        listOf(0, 50, 100).forEach { c ->
            val score = OLQScore(score = 5, confidence = c, reasoning = "Test")
            assertEquals(c, score.confidence)
        }
    }

    @Test
    fun `confidence rejects negative values`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OLQScore(score = 5, confidence = -1, reasoning = "Invalid")
        }
        assertTrue(exception.message!!.contains("Confidence must be between 0 and 100"))
    }

    @Test
    fun `confidence rejects values above 100`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OLQScore(score = 5, confidence = 101, reasoning = "Invalid")
        }
        assertTrue(exception.message!!.contains("Confidence must be between 0 and 100"))
    }

    // =========================================================================
    // TEST GROUP 5: Rating Boundary Tests
    // =========================================================================

    @Test
    fun `rating transitions correctly at each boundary`() {
        val expectedRatings = mapOf(
            1 to "Exceptional",
            2 to "Exceptional",
            3 to "Exceptional",
            4 to "Excellent",
            5 to "Very Good",
            6 to "Good",
            7 to "Average",
            8 to "Below Average",
            9 to "Poor",
            10 to "Poor"
        )

        expectedRatings.forEach { (s, expectedRating) ->
            val score = OLQScore(score = s, confidence = 80, reasoning = "Test")
            assertEquals("Score $s should map to $expectedRating", expectedRating, score.rating)
        }
    }
}
