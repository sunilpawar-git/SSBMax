package com.ssbmax.core.domain.prompts

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.scoring.SSBScoringRules
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD Tests for SSBPromptCore
 * Written BEFORE implementation as per project requirements.
 * 
 * Tests cover:
 * 1. Factor context prompts (all 4 SSB factors)
 * 2. Critical quality warnings (6 critical OLQs)
 * 3. Factor consistency rules
 * 4. Scoring scale instructions
 * 5. Limitation threshold guidance
 */
class SSBPromptCoreTest {

    // ===========================================
    // FACTOR CONTEXT PROMPT TESTS
    // ===========================================

    @Test
    fun `getFactorContextPrompt includes all four SSB factors`() {
        val prompt = SSBPromptCore.getFactorContextPrompt()
        
        assertTrue(prompt.contains("Factor I") || prompt.contains("Planning"))
        assertTrue(prompt.contains("Factor II") || prompt.contains("Social Adjustment"))
        assertTrue(prompt.contains("Factor III") || prompt.contains("Social Effectiveness"))
        assertTrue(prompt.contains("Factor IV") || prompt.contains("Dynamic"))
    }

    @Test
    fun `getFactorContextPrompt lists OLQs for each factor`() {
        val prompt = SSBPromptCore.getFactorContextPrompt()
        
        // Factor I OLQs
        assertTrue(prompt.contains("Effective Intelligence") || prompt.contains("EI"))
        assertTrue(prompt.contains("Reasoning Ability") || prompt.contains("RA"))
        
        // Factor II OLQs
        assertTrue(prompt.contains("Social Adjustment") || prompt.contains("SA"))
        assertTrue(prompt.contains("Cooperation") || prompt.contains("CO-OP"))
        
        // Factor III OLQs
        assertTrue(prompt.contains("Initiative") || prompt.contains("INI"))
        assertTrue(prompt.contains("Liveliness") || prompt.contains("LIV"))
        
        // Factor IV OLQs
        assertTrue(prompt.contains("Courage") || prompt.contains("COU"))
        assertTrue(prompt.contains("Determination") || prompt.contains("DET"))
    }

    @Test
    fun `getFactorContextPrompt emphasizes Factor II importance`() {
        val prompt = SSBPromptCore.getFactorContextPrompt()
        
        // Factor II should be marked as most critical
        assertTrue(
            prompt.contains("most critical") ||
            prompt.contains("MOST CRITICAL") ||
            prompt.contains("paramount") ||
            prompt.contains("essential")
        )
    }

    @Test
    fun `getFactorContextForCategory returns specific factor info`() {
        val intellectualContext = SSBPromptCore.getFactorContextForCategory(OLQCategory.INTELLECTUAL)
        
        assertTrue(intellectualContext.contains("Factor I") || intellectualContext.contains("Planning"))
        assertTrue(intellectualContext.contains("Effective Intelligence") || intellectualContext.contains("EI"))
    }

    @Test
    fun `getFactorContextForCategory marks Social as critical`() {
        val socialContext = SSBPromptCore.getFactorContextForCategory(OLQCategory.SOCIAL)
        
        assertTrue(
            socialContext.contains("critical") ||
            socialContext.contains("CRITICAL") ||
            socialContext.contains("paramount")
        )
    }

    // ===========================================
    // CRITICAL QUALITY WARNING TESTS
    // ===========================================

    @Test
    fun `getCriticalQualityWarning lists all 6 critical OLQs`() {
        val warning = SSBPromptCore.getCriticalQualityWarning()
        
        // All 6 critical OLQs should be mentioned
        assertTrue(warning.contains("Reasoning Ability") || warning.contains("RA"))
        assertTrue(warning.contains("Social Adjustment") || warning.contains("SA"))
        assertTrue(warning.contains("Cooperation") || warning.contains("CO-OP"))
        assertTrue(warning.contains("Sense of Responsibility") || warning.contains("SoR"))
        assertTrue(warning.contains("Liveliness") || warning.contains("LIV"))
        assertTrue(warning.contains("Courage") || warning.contains("COU"))
    }

    @Test
    fun `getCriticalQualityWarning mentions limitation threshold`() {
        val warning = SSBPromptCore.getCriticalQualityWarning()
        
        // Should mention score of 8 as critical threshold
        assertTrue(
            warning.contains("8") ||
            warning.contains("limitation") ||
            warning.contains("threshold")
        )
    }

    @Test
    fun `getCriticalQualityWarning explains auto-reject for Factor II`() {
        val warning = SSBPromptCore.getCriticalQualityWarning()
        
        // Should explain Factor II auto-reject rule
        assertTrue(
            warning.contains("Factor II") ||
            warning.contains("Social") ||
            warning.contains("auto-reject") ||
            warning.contains("automatic rejection")
        )
    }

    @Test
    fun `getCriticalQualityWarningForOLQ returns specific warning for critical OLQ`() {
        val warningRA = SSBPromptCore.getCriticalQualityWarningForOLQ(OLQ.REASONING_ABILITY)
        
        assertTrue(warningRA.contains("Reasoning Ability") || warningRA.contains("critical"))
        assertTrue(warningRA.contains("8") || warningRA.contains("limitation"))
    }

    @Test
    fun `getCriticalQualityWarningForOLQ returns empty for non-critical OLQ`() {
        val warningEI = SSBPromptCore.getCriticalQualityWarningForOLQ(OLQ.EFFECTIVE_INTELLIGENCE)
        
        // EI is not critical, should return empty or minimal warning
        assertTrue(warningEI.isEmpty() || !warningEI.contains("critical"))
    }

    // ===========================================
    // FACTOR CONSISTENCY RULES TESTS
    // ===========================================

    @Test
    fun `getFactorConsistencyRules mentions tick variation limits`() {
        val rules = SSBPromptCore.getFactorConsistencyRules()
        
        // Should mention ±1 or ±2 tick variation
        assertTrue(
            rules.contains("±1") || rules.contains("+/-1") ||
            rules.contains("1 tick") || rules.contains("one tick")
        )
    }

    @Test
    fun `getFactorConsistencyRules differentiates factor strictness`() {
        val rules = SSBPromptCore.getFactorConsistencyRules()
        
        // Should mention that some factors are stricter than others
        assertTrue(
            (rules.contains("Factor I") && rules.contains("Factor III")) ||
            (rules.contains("Planning") && rules.contains("Dynamic")) ||
            rules.contains("stricter") || rules.contains("lenient")
        )
    }

    @Test
    fun `getConsistencyRuleForCategory returns correct limit for INTELLECTUAL`() {
        val rule = SSBPromptCore.getConsistencyRuleForCategory(OLQCategory.INTELLECTUAL)
        
        // INTELLECTUAL has strict ±1 tick
        assertTrue(rule.contains("1") || rule.contains("strict"))
    }

    @Test
    fun `getConsistencyRuleForCategory returns correct limit for DYNAMIC`() {
        val rule = SSBPromptCore.getConsistencyRuleForCategory(OLQCategory.DYNAMIC)
        
        // DYNAMIC has lenient ±2 tick
        assertTrue(rule.contains("2") || rule.contains("lenient"))
    }

    // ===========================================
    // SCORING SCALE INSTRUCTIONS TESTS
    // ===========================================

    @Test
    fun `getScoringScaleInstructions explains 1-10 scale`() {
        val instructions = SSBPromptCore.getScoringScaleInstructions()
        
        assertTrue(instructions.contains("1") && instructions.contains("10"))
        assertTrue(instructions.contains("scale") || instructions.contains("range"))
    }

    @Test
    fun `getScoringScaleInstructions clarifies lower is better`() {
        val instructions = SSBPromptCore.getScoringScaleInstructions()
        
        assertTrue(
            instructions.contains("lower") ||
            instructions.contains("1 = Exceptional") ||
            instructions.contains("better") ||
            instructions.contains("inverse")
        )
    }

    @Test
    fun `getScoringScaleInstructions explains score meanings`() {
        val instructions = SSBPromptCore.getScoringScaleInstructions()
        
        // Should explain what different ranges mean
        assertTrue(
            instructions.contains("Exceptional") ||
            instructions.contains("Average") ||
            instructions.contains("Poor")
        )
    }

    @Test
    fun `getScoringScaleInstructions mentions bell curve distribution`() {
        val instructions = SSBPromptCore.getScoringScaleInstructions()
        
        assertTrue(
            instructions.contains("bell curve") ||
            instructions.contains("distribution") ||
            instructions.contains("rare") ||
            instructions.contains("common")
        )
    }

    // ===========================================
    // LIMITATION GUIDANCE TESTS
    // ===========================================

    @Test
    fun `getLimitationGuidance mentions threshold of 8`() {
        val guidance = SSBPromptCore.getLimitationGuidance()
        
        assertTrue(guidance.contains("8"))
        assertTrue(
            guidance.contains("limitation") ||
            guidance.contains("threshold") ||
            guidance.contains("below average")
        )
    }

    @Test
    fun `getLimitationGuidance mentions NDA and OTA limits`() {
        val guidance = SSBPromptCore.getLimitationGuidance()
        
        assertTrue(
            (guidance.contains("NDA") && guidance.contains("4")) ||
            guidance.contains("entry type")
        )
        assertTrue(
            (guidance.contains("OTA") && guidance.contains("7")) ||
            guidance.contains("entry type")
        )
    }

    // ===========================================
    // COMPLETE PROMPT ASSEMBLY TESTS
    // ===========================================

    @Test
    fun `getCompleteSSBContext includes all sections`() {
        val context = SSBPromptCore.getCompleteSSBContext()
        
        // Should include factor context
        assertTrue(context.contains("Factor"))
        
        // Should include critical warnings
        assertTrue(context.contains("critical") || context.contains("Critical"))
        
        // Should include scoring scale
        assertTrue(context.contains("1") && context.contains("10"))
        
        // Should include consistency rules
        assertTrue(context.contains("consistency") || context.contains("tick"))
    }

    @Test
    fun `getCompleteSSBContext is substantial length`() {
        val context = SSBPromptCore.getCompleteSSBContext()
        
        // A complete context should be reasonably detailed
        assertTrue(context.length > 500)
    }

    @Test
    fun `getOLQSpecificPrompt includes OLQ name and category`() {
        val prompt = SSBPromptCore.getOLQSpecificPrompt(OLQ.COURAGE)
        
        assertTrue(prompt.contains("Courage"))
        assertTrue(prompt.contains("Character") || prompt.contains("Dynamic") || prompt.contains("Factor IV"))
    }

    @Test
    fun `getOLQSpecificPrompt adds critical warning for critical OLQs`() {
        val promptCourage = SSBPromptCore.getOLQSpecificPrompt(OLQ.COURAGE)
        val promptEI = SSBPromptCore.getOLQSpecificPrompt(OLQ.EFFECTIVE_INTELLIGENCE)
        
        // Courage is critical, EI is not
        assertTrue(promptCourage.contains("critical") || promptCourage.contains("Critical"))
        assertFalse(promptEI.contains("critical") && promptEI.contains("Critical"))
    }
}
