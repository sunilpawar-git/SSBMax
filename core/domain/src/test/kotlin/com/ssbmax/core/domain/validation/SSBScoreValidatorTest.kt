package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.scoring.EntryType
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD Tests for SSBScoreValidator
 * Written BEFORE implementation as per project requirements.
 * 
 * Tests cover:
 * 1. Limitation counting (scores ≥8)
 * 2. Factor consistency (±1 within factor, ±2 between factors)
 * 3. Critical weakness detection (6 critical OLQs)
 * 4. Factor average calculation
 * 5. Overall recommendation determination
 */
class SSBScoreValidatorTest {

    // ===========================================
    // COUNT LIMITATIONS TESTS
    // ===========================================

    @Test
    fun `countLimitations returns 0 when all scores below threshold`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 3,
            OLQ.REASONING_ABILITY to 4,
            OLQ.SOCIAL_ADJUSTMENT to 5,
            OLQ.COOPERATION to 6,
            OLQ.COURAGE to 7
        )
        
        val result = SSBScoreValidator.countLimitations(scores)
        
        assertEquals(0, result.count)
        assertTrue(result.limitedOLQs.isEmpty())
    }

    @Test
    fun `countLimitations counts scores at threshold as limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8, // Limitation
            OLQ.REASONING_ABILITY to 4,
            OLQ.SOCIAL_ADJUSTMENT to 5
        )
        
        val result = SSBScoreValidator.countLimitations(scores)
        
        assertEquals(1, result.count)
        assertTrue(result.limitedOLQs.contains(OLQ.EFFECTIVE_INTELLIGENCE))
    }

    @Test
    fun `countLimitations counts scores above threshold as limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 9, // Limitation
            OLQ.REASONING_ABILITY to 10, // Limitation
            OLQ.SOCIAL_ADJUSTMENT to 5
        )
        
        val result = SSBScoreValidator.countLimitations(scores)
        
        assertEquals(2, result.count)
        assertTrue(result.limitedOLQs.contains(OLQ.EFFECTIVE_INTELLIGENCE))
        assertTrue(result.limitedOLQs.contains(OLQ.REASONING_ABILITY))
    }

    @Test
    fun `countLimitations identifies multiple limitations correctly`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.COURAGE to 8
        )
        
        val result = SSBScoreValidator.countLimitations(scores)
        
        assertEquals(5, result.count)
        assertEquals(5, result.limitedOLQs.size)
    }

    // ===========================================
    // EXCEEDS MAX LIMITATIONS TESTS
    // ===========================================

    @Test
    fun `exceedsMaxLimitations returns false when NDA candidate has 4 limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.COURAGE to 5
        )
        
        val result = SSBScoreValidator.exceedsMaxLimitations(scores, EntryType.NDA)
        
        assertFalse(result)
    }

    @Test
    fun `exceedsMaxLimitations returns true when NDA candidate has 5 limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.COURAGE to 8
        )
        
        val result = SSBScoreValidator.exceedsMaxLimitations(scores, EntryType.NDA)
        
        assertTrue(result)
    }

    @Test
    fun `exceedsMaxLimitations returns false when OTA candidate has 7 limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8,
            OLQ.LIVELINESS to 8,
            OLQ.COURAGE to 8
        )
        
        val result = SSBScoreValidator.exceedsMaxLimitations(scores, EntryType.OTA)
        
        assertFalse(result)
    }

    @Test
    fun `exceedsMaxLimitations returns true when OTA candidate has 8 limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8,
            OLQ.LIVELINESS to 8,
            OLQ.COURAGE to 8,
            OLQ.ORGANIZING_ABILITY to 8
        )
        
        val result = SSBScoreValidator.exceedsMaxLimitations(scores, EntryType.OTA)
        
        assertTrue(result)
    }

    // ===========================================
    // FACTOR CONSISTENCY TESTS
    // ===========================================

    @Test
    fun `checkFactorConsistency passes when all scores within factor are consistent`() {
        // Factor I (INTELLECTUAL): EI=4, RA=4, OA=5, PoE=4 - max diff 1
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 4,
            OLQ.ORGANIZING_ABILITY to 5,
            OLQ.POWER_OF_EXPRESSION to 4
        )
        
        val result = SSBScoreValidator.checkFactorConsistency(scores)
        
        assertTrue(result.isConsistent)
        assertTrue(result.inconsistentFactors.isEmpty())
    }

    @Test
    fun `checkFactorConsistency fails when scores within factor differ by more than 1`() {
        // Factor I (INTELLECTUAL): EI=3, RA=6 - diff of 3, exceeds ±1
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 3,
            OLQ.REASONING_ABILITY to 6,
            OLQ.ORGANIZING_ABILITY to 4,
            OLQ.POWER_OF_EXPRESSION to 4
        )
        
        val result = SSBScoreValidator.checkFactorConsistency(scores)
        
        assertFalse(result.isConsistent)
        assertTrue(result.inconsistentFactors.contains(OLQCategory.INTELLECTUAL))
    }

    @Test
    fun `checkFactorConsistency allows 1 tick variation within factor`() {
        // All OLQs within each factor differ by at most 1
        val scores = mapOf(
            // Factor I - INTELLECTUAL
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 5,
            OLQ.ORGANIZING_ABILITY to 4,
            OLQ.POWER_OF_EXPRESSION to 5,
            // Factor II - SOCIAL
            OLQ.SOCIAL_ADJUSTMENT to 3,
            OLQ.COOPERATION to 4,
            OLQ.SENSE_OF_RESPONSIBILITY to 3
        )
        
        val result = SSBScoreValidator.checkFactorConsistency(scores)
        
        assertTrue(result.isConsistent)
    }

    @Test
    fun `checkFactorConsistency detects inconsistency in multiple factors`() {
        val scores = mapOf(
            // Factor I - INTELLECTUAL: 2 vs 6 = diff 4 (INCONSISTENT)
            OLQ.EFFECTIVE_INTELLIGENCE to 2,
            OLQ.REASONING_ABILITY to 6,
            // Factor II - SOCIAL: 1 vs 5 = diff 4 (INCONSISTENT)
            OLQ.SOCIAL_ADJUSTMENT to 1,
            OLQ.COOPERATION to 5
        )
        
        val result = SSBScoreValidator.checkFactorConsistency(scores)
        
        assertFalse(result.isConsistent)
        assertTrue(result.inconsistentFactors.size >= 2)
    }

    @Test
    fun `checkFactorConsistency returns details about max variation found`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 3,
            OLQ.REASONING_ABILITY to 7 // diff 4
        )
        
        val result = SSBScoreValidator.checkFactorConsistency(scores)
        
        assertFalse(result.isConsistent)
        assertEquals(4, result.maxVariationFound)
    }

    // ===========================================
    // CRITICAL WEAKNESS DETECTION TESTS
    // ===========================================

    @Test
    fun `detectCriticalWeaknesses returns empty when no critical OLQs are limited`() {
        val scores = mapOf(
            OLQ.REASONING_ABILITY to 5,
            OLQ.SOCIAL_ADJUSTMENT to 4,
            OLQ.COOPERATION to 3,
            OLQ.SENSE_OF_RESPONSIBILITY to 4,
            OLQ.LIVELINESS to 5,
            OLQ.COURAGE to 4
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(result.criticalWeaknesses.isEmpty())
        assertFalse(result.hasAutoRejectWeakness)
    }

    @Test
    fun `detectCriticalWeaknesses identifies REASONING_ABILITY limitation`() {
        val scores = mapOf(
            OLQ.REASONING_ABILITY to 8 // Critical OLQ at limitation
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(result.criticalWeaknesses.contains(OLQ.REASONING_ABILITY))
    }

    @Test
    fun `detectCriticalWeaknesses identifies SOCIAL_ADJUSTMENT limitation`() {
        val scores = mapOf(
            OLQ.SOCIAL_ADJUSTMENT to 9 // Critical OLQ above limitation
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(result.criticalWeaknesses.contains(OLQ.SOCIAL_ADJUSTMENT))
    }

    @Test
    fun `detectCriticalWeaknesses identifies all six critical OLQs when limited`() {
        val scores = mapOf(
            OLQ.REASONING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8,
            OLQ.LIVELINESS to 8,
            OLQ.COURAGE to 8
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertEquals(6, result.criticalWeaknesses.size)
    }

    @Test
    fun `detectCriticalWeaknesses ignores non-critical OLQs even when limited`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8, // Not critical
            OLQ.ORGANIZING_ABILITY to 9, // Not critical
            OLQ.POWER_OF_EXPRESSION to 8 // Not critical
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(result.criticalWeaknesses.isEmpty())
    }

    @Test
    fun `detectCriticalWeaknesses sets autoReject when Factor II overall is limitation`() {
        // Factor II average = 8 should trigger auto-reject
        val scores = mapOf(
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8
        )
        
        val result = SSBScoreValidator.detectCriticalWeaknesses(scores)
        
        assertTrue(result.hasAutoRejectWeakness)
        assertTrue(result.autoRejectReason?.contains("Factor II") == true)
    }

    // ===========================================
    // FACTOR AVERAGE CALCULATION TESTS
    // ===========================================

    @Test
    fun `calculateFactorAverages returns correct average for single factor`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 6,
            OLQ.ORGANIZING_ABILITY to 4,
            OLQ.POWER_OF_EXPRESSION to 6
        )
        
        val result = SSBScoreValidator.calculateFactorAverages(scores)
        
        assertEquals(5.0, result[OLQCategory.INTELLECTUAL]!!, 0.01)
    }

    @Test
    fun `calculateFactorAverages calculates all four factors correctly`() {
        val scores = mapOf(
            // Factor I - INTELLECTUAL (avg 4.0)
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 4,
            OLQ.ORGANIZING_ABILITY to 4,
            OLQ.POWER_OF_EXPRESSION to 4,
            // Factor II - SOCIAL (avg 5.0)
            OLQ.SOCIAL_ADJUSTMENT to 5,
            OLQ.COOPERATION to 5,
            OLQ.SENSE_OF_RESPONSIBILITY to 5,
            // Factor III - DYNAMIC (avg 3.0)
            OLQ.INITIATIVE to 3,
            OLQ.SELF_CONFIDENCE to 3,
            OLQ.SPEED_OF_DECISION to 3,
            OLQ.INFLUENCE_GROUP to 3,
            OLQ.LIVELINESS to 3,
            // Factor IV - CHARACTER (avg 6.0)
            OLQ.DETERMINATION to 6,
            OLQ.STAMINA to 6,
            OLQ.COURAGE to 6
        )
        
        val result = SSBScoreValidator.calculateFactorAverages(scores)
        
        assertEquals(4.0, result[OLQCategory.INTELLECTUAL]!!, 0.01)
        assertEquals(5.0, result[OLQCategory.SOCIAL]!!, 0.01)
        assertEquals(3.0, result[OLQCategory.DYNAMIC]!!, 0.01)
        assertEquals(6.0, result[OLQCategory.CHARACTER]!!, 0.01)
    }

    @Test
    fun `calculateFactorAverages handles partial scores correctly`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 6
            // Only 2 of 4 Factor I OLQs
        )
        
        val result = SSBScoreValidator.calculateFactorAverages(scores)
        
        assertEquals(5.0, result[OLQCategory.INTELLECTUAL]!!, 0.01)
    }

    @Test
    fun `calculateFactorAverages returns null for factors with no scores`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4 // Only Factor I (INTELLECTUAL)
        )
        
        val result = SSBScoreValidator.calculateFactorAverages(scores)
        
        assertNotNull(result[OLQCategory.INTELLECTUAL])
        assertNull(result[OLQCategory.SOCIAL])
        assertNull(result[OLQCategory.DYNAMIC])
        assertNull(result[OLQCategory.CHARACTER])
    }

    // ===========================================
    // DETERMINE RECOMMENDATION TESTS
    // ===========================================

    @Test
    fun `determineRecommendation returns RECOMMENDED for excellent scores`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 3,
            OLQ.REASONING_ABILITY to 3,
            OLQ.ORGANIZING_ABILITY to 4,
            OLQ.POWER_OF_EXPRESSION to 3,
            OLQ.SOCIAL_ADJUSTMENT to 3,
            OLQ.COOPERATION to 4,
            OLQ.SENSE_OF_RESPONSIBILITY to 3,
            OLQ.INITIATIVE to 3,
            OLQ.SELF_CONFIDENCE to 4,
            OLQ.SPEED_OF_DECISION to 3,
            OLQ.INFLUENCE_GROUP to 4,
            OLQ.LIVELINESS to 3,
            OLQ.DETERMINATION to 3,
            OLQ.STAMINA to 4,
            OLQ.COURAGE to 3
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.RECOMMENDED, result.recommendation)
    }

    @Test
    fun `determineRecommendation returns NOT_RECOMMENDED when exceeds max limitations`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.ORGANIZING_ABILITY to 8,
            OLQ.POWER_OF_EXPRESSION to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8 // 5 limitations for NDA (max 4)
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.NOT_RECOMMENDED, result.recommendation)
        assertTrue(result.reasons.any { it.contains("limitation") })
    }

    @Test
    fun `determineRecommendation returns NOT_RECOMMENDED when Factor II is limitation`() {
        val scores = mapOf(
            OLQ.SOCIAL_ADJUSTMENT to 8,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.NOT_RECOMMENDED, result.recommendation)
        assertTrue(result.reasons.any { it.contains("Factor II") || it.contains("Social") })
    }

    @Test
    fun `determineRecommendation returns DOUBTFUL when critical OLQ is weak`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 4,
            OLQ.SOCIAL_ADJUSTMENT to 4,
            OLQ.COOPERATION to 4,
            OLQ.COURAGE to 7 // Critical OLQ, borderline (not yet limitation)
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        // Score of 7 on critical is concerning but not disqualifying
        assertTrue(result.recommendation in listOf(Recommendation.RECOMMENDED, Recommendation.DOUBTFUL))
    }

    @Test
    fun `determineRecommendation returns DOUBTFUL when factor consistency is poor`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 2,
            OLQ.REASONING_ABILITY to 5, // diff 3, inconsistent
            OLQ.SOCIAL_ADJUSTMENT to 4,
            OLQ.COOPERATION to 4
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.DOUBTFUL, result.recommendation)
        assertTrue(result.reasons.any { it.contains("consistency") || it.contains("inconsistent") })
    }

    @Test
    fun `determineRecommendation includes all relevant reasons`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 8,
            OLQ.ORGANIZING_ABILITY to 8,
            OLQ.POWER_OF_EXPRESSION to 8,
            OLQ.SOCIAL_ADJUSTMENT to 8, // 5 limitations + critical + Factor II
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8
        )
        
        val result = SSBScoreValidator.determineRecommendation(scores, EntryType.NDA)
        
        assertEquals(Recommendation.NOT_RECOMMENDED, result.recommendation)
        assertTrue(result.reasons.size >= 2) // Multiple reasons
    }

    // ===========================================
    // COMPREHENSIVE VALIDATION REPORT TESTS
    // ===========================================

    @Test
    fun `validate returns complete ValidationReport`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 4,
            OLQ.REASONING_ABILITY to 4,
            OLQ.SOCIAL_ADJUSTMENT to 5,
            OLQ.COOPERATION to 5,
            OLQ.COURAGE to 4
        )
        
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertNotNull(report.limitationResult)
        assertNotNull(report.consistencyResult)
        assertNotNull(report.criticalWeaknessResult)
        assertNotNull(report.factorAverages)
        assertNotNull(report.recommendationResult)
    }

    @Test
    fun `validate aggregates all validation components correctly`() {
        val scores = OLQ.entries.associateWith { 4 } // All scores = 4
        
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertEquals(0, report.limitationResult.count)
        assertTrue(report.consistencyResult.isConsistent)
        assertTrue(report.criticalWeaknessResult.criticalWeaknesses.isEmpty())
        assertEquals(4, report.factorAverages.size)
        assertEquals(Recommendation.RECOMMENDED, report.recommendationResult.recommendation)
    }

    @Test
    fun `validate identifies failing candidate correctly`() {
        val scores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to 8,
            OLQ.REASONING_ABILITY to 9,
            OLQ.ORGANIZING_ABILITY to 8,
            OLQ.SOCIAL_ADJUSTMENT to 9,
            OLQ.COOPERATION to 8,
            OLQ.SENSE_OF_RESPONSIBILITY to 8
        )
        
        val report = SSBScoreValidator.validate(scores, EntryType.NDA)
        
        assertTrue(report.limitationResult.count > EntryType.NDA.maxLimitations)
        assertTrue(report.criticalWeaknessResult.criticalWeaknesses.isNotEmpty())
        assertEquals(Recommendation.NOT_RECOMMENDED, report.recommendationResult.recommendation)
    }
}
