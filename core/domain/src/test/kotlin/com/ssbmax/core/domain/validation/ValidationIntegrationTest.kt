package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.scoring.EntryType
import com.ssbmax.core.domain.scoring.SSBScoringRules
import org.junit.Assert.*
import org.junit.Test

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
        
        assertTrue("Good scores should be valid", result.isValid)
        assertEquals("Should have no limitations", 0, result.limitationCount)
        assertEquals("Should recommend", RecommendationOutcome.RECOMMENDED, result.recommendation)
    }

    @Test
    fun `validateScores detects limitations correctly`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Add some limitations (score >= 8)
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.REASONING_ABILITY] = OLQScore(8, 80, "Poor")
        scores[OLQ.ORGANIZING_ABILITY] = OLQScore(9, 80, "Very Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertEquals("Should detect 3 limitations", 3, result.limitationCount)
        assertTrue("Should list limitation OLQs", result.limitationOLQs.contains(OLQ.EFFECTIVE_INTELLIGENCE))
        assertTrue("Should list limitation OLQs", result.limitationOLQs.contains(OLQ.REASONING_ABILITY))
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
        
        assertEquals("Should have 5 limitations", 5, result.limitationCount)
        assertEquals("Should not recommend", RecommendationOutcome.NOT_RECOMMENDED, result.recommendation)
        assertTrue("Should have limitation exceeded flag", result.exceedsMaxLimitations)
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
        
        assertEquals("Should have 5 limitations", 5, result.limitationCount)
        assertFalse("Should not exceed OTA limit", result.exceedsMaxLimitations)
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
        
        assertTrue("Should detect critical weakness", result.hasCriticalWeakness)
        assertTrue("Should list critical OLQs", result.criticalWeaknessOLQs.contains(OLQ.COOPERATION))
    }

    @Test
    fun `validateScores triggers Factor II auto-reject`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // Set all Factor II OLQs to 8 (average = 8 triggers auto-reject)
        scores[OLQ.SOCIAL_ADJUSTMENT] = OLQScore(8, 80, "Poor")
        scores[OLQ.COOPERATION] = OLQScore(8, 80, "Poor")
        scores[OLQ.SENSE_OF_RESPONSIBILITY] = OLQScore(8, 80, "Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertTrue("Should trigger Factor II rejection", result.factorIIAutoReject)
        assertEquals("Should not recommend due to Factor II", RecommendationOutcome.NOT_RECOMMENDED, result.recommendation)
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
        
        assertTrue("Should detect inconsistency", result.hasFactorInconsistency)
        assertTrue("Inconsistent factors should not be empty", result.inconsistentFactors.isNotEmpty())
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
        
        assertFalse("Should not detect inconsistency for ±1 variation", result.hasFactorInconsistency)
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
        
        assertNotNull("Factor averages should be populated", result.factorAverages)
        assertEquals("Factor IV average should be 6.0", 6.0f, result.factorAverages[4]!!, 0.01f)
    }

    // ===========================================
    // RECOMMENDATION BOUNDARY TESTS
    // ===========================================

    @Test
    fun `validateScores returns BORDERLINE for close cases`() {
        val scores = createScoresMap(allScore = 5).toMutableMap()
        // 3-4 limitations for NDA (max 4) is borderline
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = OLQScore(8, 80, "Poor")
        scores[OLQ.DETERMINATION] = OLQScore(8, 80, "Poor")
        scores[OLQ.STAMINA] = OLQScore(8, 80, "Poor")
        
        val result = ValidationIntegration.validateScores(scores, EntryType.NDA)
        
        assertEquals("3 limitations for NDA should be borderline", 
            RecommendationOutcome.BORDERLINE, result.recommendation)
    }

    // ===========================================
    // EDGE CASE TESTS
    // ===========================================

    @Test
    fun `validateScores handles empty scores gracefully`() {
        val emptyScores = emptyMap<OLQ, OLQScore>()
        
        val result = ValidationIntegration.validateScores(emptyScores, EntryType.NDA)
        
        assertFalse("Empty scores should not be valid", result.isValid)
    }

    @Test
    fun `validateScores handles partial scores`() {
        val partialScores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(5, 80, "Good"),
            OLQ.COOPERATION to OLQScore(5, 80, "Good")
        )
        
        val result = ValidationIntegration.validateScores(partialScores, EntryType.NDA)
        
        // Should still process partial scores
        assertEquals("Should have 0 limitations from these scores", 0, result.limitationCount)
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
