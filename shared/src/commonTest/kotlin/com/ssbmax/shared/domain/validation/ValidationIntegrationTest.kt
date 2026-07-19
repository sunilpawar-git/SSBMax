package com.ssbmax.shared.domain.validation

import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.scoring.SSBScoringRules
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.fail
import kotlin.test.assertNotEquals
import kotlin.test.Test

/**
 * TDD Tests for Validation Integration
 * 
 * Tests verify that ValidationIntegration:
 * - Correctly wraps SSBScoreValidator for worker use
 * - Returns proper validation results
 * - Handles edge cases gracefully
 */
class ValidationIntegrationTest {

    // ===========================================
    // BASIC VALIDATION TESTS
    // ===========================================

    @Test
    fun `validateScores returns valid result for good scores`() {
        val scores = createScoresMap(allScore = 5)
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertTrue(result.isValid, "Good scores should be valid")
        assertEquals(0, result.limitationCount, "Should have no limitations")
        assertEquals(RecommendationOutcome.RECOMMENDED, result.recommendation, "Should recommend")
    }

    @Test
    fun `validateScores detects limitations correctly`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Add some limitations (score >= 8)
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.REASONING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(9, 80, "Very Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertEquals(3, result.limitationCount, "Should detect 3 limitations")
        assertTrue(result.limitationOLQs.contains(OLQ.EFFECTIVE_INTELLIGENCE), "Should list limitation OLQs")
        assertTrue(result.limitationOLQs.contains(OLQ.REASONING_ABILITY), "Should list limitation OLQs")
    }

    @Test
    fun `validateScores returns NOT_RECOMMENDED when exceeding NDA limits`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // NDA allows max 4 limitations, add 5
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.REASONING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.POWER_OF_EXPRESSION] = OLQScore(8, 80, "Poor")
        scores[OLQ.SOCIAL_ADJUSTMENT] = OLQScore(8, 80, "Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertEquals(5, result.limitationCount, "Should have 5 limitations")
        assertEquals(RecommendationOutcome.NOT_RECOMMENDED, result.recommendation, "Should not recommend")
        assertTrue(result.exceedsMaxLimitations, "Should have limitation exceeded flag")
    }

    @Test
    fun `validateScores allows more limitations for OTA`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // OTA allows max 7 limitations, add 5 (should still be OK)
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.REASONING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.POWER_OF_EXPRESSION] = OLQScore(8, 80, "Poor")
        scores[OLQ.INITIATIVE] = OLQScore(8, 80, "Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.OTA)
        
        assertEquals(5, result.limitationCount, "Should have 5 limitations")
        assertFalse(result.exceedsMaxLimitations, "Should not exceed OTA limit")
    }

    // ===========================================
    // CRITICAL QUALITY TESTS
    // ===========================================

    @Test
    fun `validateScores detects critical quality weaknesses`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Set a critical OLQ to limitation
        scores[OLQ.COOPERATION] = OLQScore(8, 80, "Poor cooperation")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertTrue(result.hasCriticalWeakness, "Should detect critical weakness")
        assertTrue(result.criticalWeaknessOLQs.contains(OLQ.COOPERATION), "Should list critical OLQs")
    }

    @Test
    fun `validateScores triggers Factor II auto-reject`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Set all Factor II OLQs to 8 (average = 8 triggers auto-reject)
        scores[OLQ.SOCIAL_ADJUSTMENT] = OLQScore(8, 80, "Poor")
        scores[OLQ.COOPERATION] = OLQScore(8, 80, "Poor")
        scores[OLQ.SENSE_OF_RESPONSIBILITY] = OLQScore(8, 80, "Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertTrue(result.factorIIAutoReject, "Should trigger Factor II rejection")
        assertEquals(RecommendationOutcome.NOT_RECOMMENDED, result.recommendation, "Should not recommend due to Factor II")
    }

    // ===========================================
    // FACTOR CONSISTENCY TESTS
    // ===========================================

    @Test
    fun `validateScores detects factor inconsistency`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Factor I has ±1 tolerance - make one OLQ very different
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(3, 80, "Excellent")
        scores[OLQ.REASONING_ABILITY] = OLQScore(7, 80, "Average") // 4 tick difference!
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertTrue(result.hasFactorInconsistency, "Should detect inconsistency")
        assertTrue(result.inconsistentFactors.isNotEmpty(), "Inconsistent factors should not be empty")
    }

    @Test
    fun `validateScores passes consistent Factor I scores`() {
        val scores = createScoresMap(allScore = 6).toMutableMap()
        // Factor I has ±1 tolerance - these should pass
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(5, 80, "Good")
        scores[OLQ.REASONING_ABILITY] = OLQScore(6, 80, "Good")
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(5, 80, "Good")
        scores[OLQ.POWER_OF_EXPRESSION] = OLQScore(6, 80, "Good")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertFalse(result.hasFactorInconsistency, "Should not detect inconsistency for ±1 variation")
    }

    // ===========================================
    // FACTOR AVERAGES TESTS
    // ===========================================

    @Test
    fun `validateScores calculates factor averages correctly`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Set Factor IV to average of 6
        scores[OLQ.DETERMINATION] = OLQScore(6, 80, "Good")
        scores[OLQ.COURAGE] = OLQScore(6, 80, "Good")
        scores[OLQ.STAMINA] = OLQScore(6, 80, "Good")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertNotNull(result.factorAverages, "Factor averages should be populated")
        assertEquals(6.0f, result.factorAverages[4]!!, 0.01f, "Factor IV average should be 6.0")
    }

    // ===========================================
    // RECOMMENDATION BOUNDARY TESTS
    // ===========================================

    @Test
    fun `validateScores returns NOT_RECOMMENDED for any limitations`() {
        // R14: any limitation = NOT_RECOMMENDED (supersedes old entry-type-based borderline logic)
        val scores = createScoresMap(allScore = 5).toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.DETERMINATION] = OLQScore(8, 80, "Poor")
        scores[OLQ.STAMINA] = OLQScore(8, 80, "Poor")

        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)

        assertEquals(RecommendationOutcome.NOT_RECOMMENDED, result.recommendation, "Any limitation must yield NOT_RECOMMENDED (R14)")
    }

    // ===========================================
    // EDGE CASE TESTS
    // ===========================================

    @Test
    fun `validateScores handles empty scores gracefully`() {
        val emptyScores = emptyMap<OLQ, OLQScore>()
        
        val result = ValidationIntegration.validateScores(emptyScores, EntryType.NDA)
        
        assertFalse(result.isValid, "Empty scores should not be valid")
    }

    @Test
    fun `validateScores handles partial scores`() {
        val partialScores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(5, 80, "Good"),
            OLQ.COOPERATION to OLQScore(5, 80, "Good")
        )
        
        val result = ValidationIntegration.validateScores(partialScores, EntryType.NDA)
        
        // Should still process partial scores
        assertEquals(0, result.limitationCount, "Should have 0 limitations from these scores")
    }

    // ===========================================
    // R14: ANY OLQ >= 8 → NOT_RECOMMENDED
    // ===========================================

    @Test
    fun `any single OLQ score of 8 yields NOT_RECOMMENDED`() {
        // WHY: R14 — SSB doctrine: one limitation = not recommended; current code allows up to 4 for NDA
        val scores = createScoresMap(allScore = 6).toMutableMap()
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(8, 80, "Poor organizing")

        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)

        assertEquals(RecommendationOutcome.NOT_RECOMMENDED, result.recommendation, "Single OLQ >= 8 must yield NOT_RECOMMENDED (R14)")
    }

    @Test
    fun `all OLQ scores at 7 do not trigger NOT_RECOMMENDED`() {
        // WHY: boundary guard — 7 is average, not a limitation; R14 must not fire for average candidates
        val scores = createScoresMap(allScore = 7)

        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)

        assertNotEquals(RecommendationOutcome.NOT_RECOMMENDED, result.recommendation, "Scores of 7 must not trigger NOT_RECOMMENDED")
    }

    // ===========================================
    // HELPER FUNCTIONS
    // ===========================================

    private fun createScoresMap(allScore: Int): Map<OLQ, OLQScore> {
        return OLQ.entries.associateWith { olq ->
            OLQScore(
                score = allScore,
                confidence = 80,
                reasoning = "Test score"
            )
        }
    }
}
