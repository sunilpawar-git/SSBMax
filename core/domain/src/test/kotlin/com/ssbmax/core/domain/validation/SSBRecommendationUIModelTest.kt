package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.scoring.EntryType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SSBRecommendationUIModel.
 * 
 * Tests the transformation from OLQScoreValidationResult to UI-friendly model.
 */
class SSBRecommendationUIModelTest {

    // ===========================================
    // RECOMMENDED STATE TESTS
    // ===========================================

    @Test
    fun `fromValidationResult - RECOMMENDED shows correct text`() {
        val validationResult = createRecommendedResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(RecommendationOutcome.RECOMMENDED, uiModel.recommendation)
        assertEquals("RECOMMENDED", uiModel.recommendationText)
        assertEquals("You meet the SSB selection criteria", uiModel.subtitleText)
    }

    @Test
    fun `fromValidationResult - RECOMMENDED with zero limitations`() {
        val validationResult = createRecommendedResult(limitationCount = 0)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(0, uiModel.limitationCount)
        assertEquals(4, uiModel.maxLimitations) // NDA max = 4
        assertTrue(uiModel.limitationsOk)
        assertFalse(uiModel.hasCriticalWeakness)
    }

    @Test
    fun `fromValidationResult - RECOMMENDED with acceptable limitations for NDA`() {
        val validationResult = createRecommendedResult(limitationCount = 3)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(3, uiModel.limitationCount)
        assertEquals(4, uiModel.maxLimitations)
        assertTrue(uiModel.limitationsOk)
    }

    // ===========================================
    // BORDERLINE STATE TESTS
    // ===========================================

    @Test
    fun `fromValidationResult - BORDERLINE shows correct text`() {
        val validationResult = createBorderlineResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(RecommendationOutcome.BORDERLINE, uiModel.recommendation)
        assertEquals("BORDERLINE", uiModel.recommendationText)
        assertEquals("Performance is on the edge of passing", uiModel.subtitleText)
    }

    @Test
    fun `fromValidationResult - BORDERLINE with 4 limitations for NDA`() {
        val validationResult = createBorderlineResult(limitationCount = 4)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(4, uiModel.limitationCount)
        assertEquals(4, uiModel.maxLimitations)
        assertTrue(uiModel.limitationsOk) // Exactly at limit, still OK
    }

    // ===========================================
    // NOT_RECOMMENDED STATE TESTS
    // ===========================================

    @Test
    fun `fromValidationResult - NOT_RECOMMENDED shows correct text`() {
        val validationResult = createNotRecommendedResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(RecommendationOutcome.NOT_RECOMMENDED, uiModel.recommendation)
        assertEquals("NOT RECOMMENDED", uiModel.recommendationText)
    }

    @Test
    fun `fromValidationResult - NOT_RECOMMENDED with Factor II auto-reject`() {
        val validationResult = createNotRecommendedResult(factorIIAutoReject = true)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.factorIIAutoReject)
        assertEquals(
            "Factor II (Social Adjustment) auto-reject triggered", 
            uiModel.subtitleText
        )
    }

    @Test
    fun `fromValidationResult - NOT_RECOMMENDED with exceeds max limitations`() {
        val validationResult = createNotRecommendedResult(
            limitationCount = 6,
            exceedsMaxLimitations = true
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(6, uiModel.limitationCount)
        assertFalse(uiModel.limitationsOk)
        assertEquals("Too many limitations detected", uiModel.subtitleText)
    }

    @Test
    fun `fromValidationResult - NOT_RECOMMENDED with critical weakness`() {
        val validationResult = createNotRecommendedResult(
            hasCriticalWeakness = true,
            criticalWeaknessOLQs = listOf(OLQ.REASONING_ABILITY, OLQ.COOPERATION)
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.hasCriticalWeakness)
        assertEquals(2, uiModel.criticalWeaknessNames.size)
        assertTrue(uiModel.criticalWeaknessNames.contains("Reasoning Ability"))
        assertTrue(uiModel.criticalWeaknessNames.contains("Cooperation"))
        assertEquals("Critical OLQ weakness found", uiModel.subtitleText)
    }

    @Test
    fun `fromValidationResult - NOT_RECOMMENDED with factor inconsistency`() {
        val validationResult = createNotRecommendedResult(
            hasFactorInconsistency = true
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.hasFactorInconsistency)
        assertEquals("Factor score inconsistency detected", uiModel.subtitleText)
    }

    // ===========================================
    // ENTRY TYPE TESTS
    // ===========================================

    @Test
    fun `fromValidationResult - NDA has max 4 limitations`() {
        val validationResult = createRecommendedResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(4, uiModel.maxLimitations)
    }

    @Test
    fun `fromValidationResult - OTA has max 7 limitations`() {
        val validationResult = createRecommendedResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.OTA
        )
        
        assertEquals(7, uiModel.maxLimitations)
    }

    @Test
    fun `fromValidationResult - GRADUATE has max 7 limitations`() {
        val validationResult = createRecommendedResult()
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.GRADUATE
        )
        
        assertEquals(7, uiModel.maxLimitations)
    }

    // ===========================================
    // DETAILED SUMMARY TESTS
    // ===========================================

    @Test
    fun `fromValidationResult - detailed summary includes limitation count`() {
        val validationResult = createRecommendedResult(limitationCount = 2)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.detailedSummary.contains("Limitations: 2/4"))
    }

    @Test
    fun `fromValidationResult - detailed summary shows Critical OK when no weakness`() {
        val validationResult = createRecommendedResult(hasCriticalWeakness = false)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.detailedSummary.contains("Critical: OK"))
    }

    @Test
    fun `fromValidationResult - detailed summary shows Critical OLQ names`() {
        val validationResult = createNotRecommendedResult(
            hasCriticalWeakness = true,
            criticalWeaknessOLQs = listOf(OLQ.SOCIAL_ADJUSTMENT)
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.detailedSummary.contains("Critical: Social Adjustment"))
    }

    @Test
    fun `fromValidationResult - detailed summary shows Factor II status`() {
        val validationResult = createNotRecommendedResult(factorIIAutoReject = true)
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertTrue(uiModel.detailedSummary.contains("Factor II: ALERT"))
    }

    // ===========================================
    // EMPTY STATE TEST
    // ===========================================

    @Test
    fun `empty - returns loading state model`() {
        val emptyModel = SSBRecommendationUIModel.empty()
        
        assertEquals("Analyzing...", emptyModel.recommendationText)
        assertEquals("Validating scores against SSB criteria", emptyModel.subtitleText)
        assertEquals(0, emptyModel.limitationCount)
        assertTrue(emptyModel.limitationsOk)
        assertFalse(emptyModel.hasCriticalWeakness)
        assertFalse(emptyModel.factorIIAutoReject)
    }

    // ===========================================
    // PRIORITY ORDER TESTS (Subtitle Priority)
    // ===========================================

    @Test
    fun `fromValidationResult - Factor II auto-reject takes priority in subtitle`() {
        // When multiple issues exist, Factor II auto-reject should be shown first
        val validationResult = createNotRecommendedResult(
            factorIIAutoReject = true,
            exceedsMaxLimitations = true,
            hasCriticalWeakness = true
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals(
            "Factor II (Social Adjustment) auto-reject triggered", 
            uiModel.subtitleText
        )
    }

    @Test
    fun `fromValidationResult - exceeds max takes second priority`() {
        // When multiple issues exist but no Factor II, exceeds max should show
        val validationResult = createNotRecommendedResult(
            factorIIAutoReject = false,
            exceedsMaxLimitations = true,
            hasCriticalWeakness = true
        )
        
        val uiModel = SSBRecommendationUIModel.fromValidationResult(
            validationResult, 
            EntryType.NDA
        )
        
        assertEquals("Too many limitations detected", uiModel.subtitleText)
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    private fun createRecommendedResult(
        limitationCount: Int = 1,
        hasCriticalWeakness: Boolean = false
    ): OLQScoreValidationResult {
        return OLQScoreValidationResult(
            isValid = true,
            limitationCount = limitationCount,
            limitationOLQs = emptyList(),
            exceedsMaxLimitations = false,
            hasCriticalWeakness = hasCriticalWeakness,
            criticalWeaknessOLQs = emptyList(),
            factorIIAutoReject = false,
            hasFactorInconsistency = false,
            inconsistentFactors = emptyList(),
            factorAverages = mapOf(1 to 4.5f, 2 to 5.0f, 3 to 4.0f, 4 to 4.5f),
            recommendation = RecommendationOutcome.RECOMMENDED,
            summary = "Scores pass all validation criteria"
        )
    }

    private fun createBorderlineResult(
        limitationCount: Int = 4
    ): OLQScoreValidationResult {
        return OLQScoreValidationResult(
            isValid = true,
            limitationCount = limitationCount,
            limitationOLQs = emptyList(),
            exceedsMaxLimitations = false,
            hasCriticalWeakness = false,
            criticalWeaknessOLQs = emptyList(),
            factorIIAutoReject = false,
            hasFactorInconsistency = false,
            inconsistentFactors = emptyList(),
            factorAverages = mapOf(1 to 6.5f, 2 to 7.0f, 3 to 6.0f, 4 to 6.5f),
            recommendation = RecommendationOutcome.BORDERLINE,
            summary = "Performance on edge of passing"
        )
    }

    private fun createNotRecommendedResult(
        limitationCount: Int = 5,
        exceedsMaxLimitations: Boolean = false,
        hasCriticalWeakness: Boolean = false,
        criticalWeaknessOLQs: List<OLQ> = emptyList(),
        factorIIAutoReject: Boolean = false,
        hasFactorInconsistency: Boolean = false
    ): OLQScoreValidationResult {
        return OLQScoreValidationResult(
            isValid = true,
            limitationCount = limitationCount,
            limitationOLQs = emptyList(),
            exceedsMaxLimitations = exceedsMaxLimitations,
            hasCriticalWeakness = hasCriticalWeakness,
            criticalWeaknessOLQs = criticalWeaknessOLQs,
            factorIIAutoReject = factorIIAutoReject,
            hasFactorInconsistency = hasFactorInconsistency,
            inconsistentFactors = emptyList(),
            factorAverages = mapOf(1 to 8.0f, 2 to 8.5f, 3 to 8.0f, 4 to 8.5f),
            recommendation = RecommendationOutcome.NOT_RECOMMENDED,
            summary = "Does not meet criteria"
        )
    }
}
