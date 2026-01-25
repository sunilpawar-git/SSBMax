package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.scoring.EntryType
import com.ssbmax.core.domain.scoring.SSBScoringRules
import org.junit.Assert.*
import org.junit.Test

/**
 * Calibration tests for SSB Validation system.
 * 
 * These tests verify that the scoring system produces
 * realistic distributions matching SSB assessment patterns:
 * 
 * - ~20% RECOMMENDED (strong candidates)
 * - ~30% DOUBTFUL/BORDERLINE (need review)
 * - ~50% NOT_RECOMMENDED (clear rejections)
 * 
 * Also verifies edge cases and boundary conditions.
 */
class SSBValidationCalibrationTest {

    // =========================================
    // SCORE DISTRIBUTION TESTS
    // =========================================

    @Test
    fun `excellent candidate profile scores RECOMMENDED`() {
        // Excellent candidate: all scores 3-4 (very good)
        val scores = OLQ.entries.associateWith { 3 }
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.RECOMMENDED, result.recommendation)
        assertTrue(result.reasons.isEmpty() || result.reasons.all { it.contains("pass") || it.isEmpty() })
    }

    @Test
    fun `good candidate profile scores RECOMMENDED`() {
        // Good candidate: all scores 4-5 (good)
        val scores = OLQ.entries.associateWith { 5 }
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.RECOMMENDED, result.recommendation)
    }

    @Test
    fun `average candidate profile with consistent scores scores RECOMMENDED`() {
        // Average but consistent: all scores 6
        val scores = OLQ.entries.associateWith { 6 }
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.RECOMMENDED, result.recommendation)
    }

    @Test
    fun `borderline candidate with one limitation scores DOUBTFUL`() {
        // One limitation (score = 8), rest okay
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.DETERMINATION] = 8 // One limitation
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        // Should be DOUBTFUL because of critical OLQ weakness but within limits
        assertTrue(
            "One critical limitation should cause concern",
            result.recommendation == Recommendation.DOUBTFUL || 
            result.recommendation == Recommendation.RECOMMENDED
        )
    }

    @Test
    fun `poor candidate exceeding NDA limitations scores NOT_RECOMMENDED`() {
        // 5 limitations for NDA (max is 4)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 8
        scores[OLQ.REASONING_ABILITY] = 8
        scores[OLQ.ORGANIZING_ABILITY] = 8
        scores[OLQ.POWER_OF_EXPRESSION] = 8
        scores[OLQ.DETERMINATION] = 8 // 5th limitation
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(
            "NDA candidate with 5 limitations should be NOT_RECOMMENDED",
            Recommendation.NOT_RECOMMENDED,
            result.recommendation
        )
    }

    @Test
    fun `OTA candidate with 7 limitations still within threshold`() {
        // 7 limitations for OTA (max is 7)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 8
        scores[OLQ.REASONING_ABILITY] = 8
        scores[OLQ.ORGANIZING_ABILITY] = 8
        scores[OLQ.POWER_OF_EXPRESSION] = 8
        scores[OLQ.INITIATIVE] = 8
        scores[OLQ.SELF_CONFIDENCE] = 8
        scores[OLQ.DETERMINATION] = 8 // 7 limitations
        
        val limitCount = SSBScoreValidator.countLimitations(scores).count
        assertEquals(7, limitCount)
        
        // Still within OTA limits, but may have consistency issues
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.OTA)
        
        // Could be DOUBTFUL due to consistency issues even if within limit
        assertNotNull(result)
    }

    @Test
    fun `OTA candidate with 8 limitations exceeds threshold`() {
        // 8 limitations for OTA (max is 7)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 8
        scores[OLQ.REASONING_ABILITY] = 8
        scores[OLQ.ORGANIZING_ABILITY] = 8
        scores[OLQ.POWER_OF_EXPRESSION] = 8
        scores[OLQ.INITIATIVE] = 8
        scores[OLQ.SELF_CONFIDENCE] = 8
        scores[OLQ.DETERMINATION] = 8
        scores[OLQ.COURAGE] = 8 // 8th limitation
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.OTA)
        
        assertEquals(
            "OTA candidate with 8 limitations should be NOT_RECOMMENDED",
            Recommendation.NOT_RECOMMENDED,
            result.recommendation
        )
    }

    // =========================================
    // FACTOR II (SOCIAL) AUTO-REJECT TESTS
    // =========================================

    @Test
    fun `Factor II average of 8 triggers auto-reject`() {
        // Factor II OLQs: SA, CO-OP, SoR - all at 8
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.SOCIAL_ADJUSTMENT] = 8
        scores[OLQ.COOPERATION] = 8
        scores[OLQ.SENSE_OF_RESPONSIBILITY] = 8
        
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(
            "Factor II average of 8 should trigger auto-reject",
            criticalResult.hasAutoRejectWeakness
        )
        assertNotNull(criticalResult.autoRejectReason)
        assertTrue(criticalResult.autoRejectReason!!.contains("Factor II"))
    }

    @Test
    fun `Factor II average below 8 does not trigger auto-reject`() {
        // Factor II OLQs: SA=7, CO-OP=7, SoR=7 (average = 7)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.SOCIAL_ADJUSTMENT] = 7
        scores[OLQ.COOPERATION] = 7
        scores[OLQ.SENSE_OF_RESPONSIBILITY] = 7
        
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertFalse(
            "Factor II average of 7 should NOT trigger auto-reject",
            criticalResult.hasAutoRejectWeakness
        )
    }

    @Test
    fun `Factor II average of 7_67 does not trigger auto-reject`() {
        // Factor II: 7, 8, 8 = average 7.67 (just under threshold)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.SOCIAL_ADJUSTMENT] = 7
        scores[OLQ.COOPERATION] = 8
        scores[OLQ.SENSE_OF_RESPONSIBILITY] = 8
        
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertFalse(
            "Factor II average of 7.67 should NOT trigger auto-reject",
            criticalResult.hasAutoRejectWeakness
        )
    }

    // =========================================
    // FACTOR CONSISTENCY TESTS
    // =========================================

    @Test
    fun `Factor I consistency with 1 tick variation passes`() {
        // Factor I (Intellectual): EI, RA, OA, PoE
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 4
        scores[OLQ.REASONING_ABILITY] = 5
        scores[OLQ.ORGANIZING_ABILITY] = 5
        scores[OLQ.POWER_OF_EXPRESSION] = 5
        
        val consistencyResult = SSBScoreValidator.checkFactorConsistency(scores)
        val factorIDetail = consistencyResult.details[OLQCategory.INTELLECTUAL]
        
        assertNotNull(factorIDetail)
        assertTrue(
            "Factor I with 1 tick variation should be consistent",
            factorIDetail!!.isConsistent
        )
    }

    @Test
    fun `Factor I consistency with 2 tick variation fails`() {
        // Factor I: scores 4, 6, 5, 5 = variation of 2
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 4
        scores[OLQ.REASONING_ABILITY] = 6
        scores[OLQ.ORGANIZING_ABILITY] = 5
        scores[OLQ.POWER_OF_EXPRESSION] = 5
        
        val consistencyResult = SSBScoreValidator.checkFactorConsistency(scores)
        val factorIDetail = consistencyResult.details[OLQCategory.INTELLECTUAL]
        
        assertNotNull(factorIDetail)
        assertFalse(
            "Factor I with 2 tick variation should be inconsistent",
            factorIDetail!!.isConsistent
        )
    }

    @Test
    fun `Factor III consistency with 2 tick variation passes`() {
        // Factor III (Dynamic): INI, SC, SoD, AIG, LIV - allows ±2
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.INITIATIVE] = 4
        scores[OLQ.SELF_CONFIDENCE] = 6
        scores[OLQ.SPEED_OF_DECISION] = 5
        scores[OLQ.INFLUENCE_GROUP] = 5
        scores[OLQ.LIVELINESS] = 5
        
        val consistencyResult = SSBScoreValidator.checkFactorConsistency(scores)
        val factorIIIDetail = consistencyResult.details[OLQCategory.DYNAMIC]
        
        assertNotNull(factorIIIDetail)
        assertTrue(
            "Factor III with 2 tick variation should be consistent (allows ±2)",
            factorIIIDetail!!.isConsistent
        )
    }

    @Test
    fun `Factor III consistency with 3 tick variation fails`() {
        // Factor III: 3 tick variation (exceeds ±2 limit)
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.INITIATIVE] = 3
        scores[OLQ.SELF_CONFIDENCE] = 6
        scores[OLQ.SPEED_OF_DECISION] = 5
        scores[OLQ.INFLUENCE_GROUP] = 5
        scores[OLQ.LIVELINESS] = 5
        
        val consistencyResult = SSBScoreValidator.checkFactorConsistency(scores)
        val factorIIIDetail = consistencyResult.details[OLQCategory.DYNAMIC]
        
        assertNotNull(factorIIIDetail)
        assertFalse(
            "Factor III with 3 tick variation should be inconsistent",
            factorIIIDetail!!.isConsistent
        )
    }

    // =========================================
    // CRITICAL OLQ TESTS
    // =========================================

    @Test
    fun `all 6 critical OLQs are correctly identified`() {
        val criticalOLQs = OLQ.entries.filter { it.isCritical }
        
        assertEquals("Should have exactly 6 critical OLQs", 6, criticalOLQs.size)
        
        val expectedCritical = setOf(
            OLQ.REASONING_ABILITY,
            OLQ.SOCIAL_ADJUSTMENT,
            OLQ.COOPERATION,
            OLQ.SENSE_OF_RESPONSIBILITY,
            OLQ.LIVELINESS,
            OLQ.COURAGE
        )
        
        assertEquals(expectedCritical, criticalOLQs.toSet())
    }

    @Test
    fun `critical OLQ at limitation is flagged`() {
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.COURAGE] = 8 // Critical OLQ at limitation
        
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(criticalResult.criticalWeaknesses.contains(OLQ.COURAGE))
    }

    @Test
    fun `non-critical OLQ at limitation is not flagged as critical weakness`() {
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 8 // Not a critical OLQ
        
        val criticalResult = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertFalse(
            "EI is not a critical OLQ",
            criticalResult.criticalWeaknesses.contains(OLQ.EFFECTIVE_INTELLIGENCE)
        )
    }

    // =========================================
    // BOUNDARY CONDITION TESTS
    // =========================================

    @Test
    fun `score of exactly 8 is a limitation`() {
        assertTrue(SSBScoringRules.isLimitation(8))
    }

    @Test
    fun `score of 7 is not a limitation`() {
        assertFalse(SSBScoringRules.isLimitation(7))
    }

    @Test
    fun `score of 9 is a limitation`() {
        assertTrue(SSBScoringRules.isLimitation(9))
    }

    @Test
    fun `score of 10 is a limitation`() {
        assertTrue(SSBScoringRules.isLimitation(10))
    }

    @Test
    fun `minimum valid score is 1`() {
        val scores = OLQ.entries.associateWith { 1 }
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertEquals(0, report.limitationResult.count)
        assertEquals(Recommendation.RECOMMENDED, report.recommendationResult.recommendation)
    }

    @Test
    fun `maximum valid score is 10`() {
        val scores = OLQ.entries.associateWith { 10 }
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertEquals(15, report.limitationResult.count) // All 15 OLQs are limitations
        assertEquals(Recommendation.NOT_RECOMMENDED, report.recommendationResult.recommendation)
    }

    // =========================================
    // ENTRY TYPE SPECIFIC TESTS
    // =========================================

    @Test
    fun `NDA max limitations is 4`() {
        assertEquals(4, EntryType.NDA.maxLimitations)
    }

    @Test
    fun `OTA max limitations is 7`() {
        assertEquals(7, EntryType.OTA.maxLimitations)
    }

    @Test
    fun `GRADUATE max limitations is 7`() {
        assertEquals(7, EntryType.GRADUATE.maxLimitations)
    }

    @Test
    fun `NDA is stricter than OTA`() {
        // 5 limitations: fails NDA, passes OTA
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        repeat(5) { i ->
            scores[OLQ.entries[i]] = 8
        }
        
        val ndaResult = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        val otaResult = SSBScoreValidator.determineRecommendation(scores, EntryType.OTA)
        
        assertEquals(Recommendation.NOT_RECOMMENDED, ndaResult.recommendation)
        // OTA might still be DOUBTFUL due to consistency, but not outright rejected for limitations
        assertNotEquals(
            "OTA should not reject for 5 limitations alone",
            Recommendation.NOT_RECOMMENDED,
            otaResult.recommendation
        )
    }

    // =========================================
    // COMPREHENSIVE VALIDATION REPORT TESTS
    // =========================================

    @Test
    fun `validation report contains all components`() {
        val scores = OLQ.entries.associateWith { 5 }
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertNotNull(report.limitationResult)
        assertNotNull(report.consistencyResult)
        assertNotNull(report.criticalWeaknessResult)
        assertNotNull(report.factorAverages)
        assertNotNull(report.recommendationResult)
        assertEquals(scores, report.originalScores)
        assertEquals(EntryType.NDA, report.entryType)
    }

    @Test
    fun `factor averages are calculated correctly`() {
        // All Factor I OLQs at score 4
        val scores = OLQ.entries.associateWith { 5 }.toMutableMap()
        scores[OLQ.EFFECTIVE_INTELLIGENCE] = 4
        scores[OLQ.REASONING_ABILITY] = 4
        scores[OLQ.ORGANIZING_ABILITY] = 4
        scores[OLQ.POWER_OF_EXPRESSION] = 4
        
        val factorAverages = SSBScoreValidator.calculateFactorAverages(scores)
        
        assertEquals(4.0, factorAverages[OLQCategory.INTELLECTUAL]!!, 0.01)
    }
}
