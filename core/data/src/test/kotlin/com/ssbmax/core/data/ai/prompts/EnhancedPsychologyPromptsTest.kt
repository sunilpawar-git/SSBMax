package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.prompts.SSBPromptCore
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD Tests for Enhanced Psychology Test Prompts
 * Written BEFORE implementation as per project requirements.
 * 
 * Tests verify that enhanced prompts:
 * 1. Include SSBPromptCore content (SSOT)
 * 2. Include test-specific indicators
 * 3. Include penalizing/boosting behaviors
 * 4. Maintain JSON output format requirements
 */
class EnhancedPsychologyPromptsTest {

    // ===========================================
    // TAT ENHANCED PROMPT TESTS
    // ===========================================

    @Test
    fun `TAT prompt includes SSB factor context from SSOT`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text", "Story 2 text")
        )
        
        // Should include factor context from SSBPromptCore
        assertTrue(prompt.contains("Factor I") || prompt.contains("Planning"))
        assertTrue(prompt.contains("Factor II") || prompt.contains("Social"))
    }

    @Test
    fun `TAT prompt includes critical quality warnings`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text")
        )
        
        // Should include critical OLQ warnings
        assertTrue(
            prompt.contains("critical") ||
            prompt.contains("CRITICAL") ||
            prompt.contains("Reasoning Ability")
        )
    }

    @Test
    fun `TAT prompt includes scoring scale from SSOT`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text")
        )
        
        // Should include scoring scale explanation
        assertTrue(prompt.contains("1-10") || prompt.contains("lower"))
        assertTrue(prompt.contains("Exceptional") || prompt.contains("Average"))
    }

    @Test
    fun `TAT prompt includes test-specific penalizing indicators`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text")
        )
        
        // Should include TAT-specific negative indicators
        assertTrue(
            prompt.contains("Penalizing") ||
            prompt.contains("penalizing") ||
            prompt.contains("negative") ||
            prompt.contains("passive")
        )
    }

    @Test
    fun `TAT prompt includes test-specific boosting indicators`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text")
        )
        
        // Should include TAT-specific positive indicators
        assertTrue(
            prompt.contains("Boosting") ||
            prompt.contains("boosting") ||
            prompt.contains("initiative") ||
            prompt.contains("proactive")
        )
    }

    @Test
    fun `TAT prompt maintains JSON output requirements`() {
        val prompt = EnhancedPsychologyPrompts.buildTATPrompt(
            stories = listOf("Story 1 text")
        )
        
        // Should require JSON output
        assertTrue(prompt.contains("JSON"))
        assertTrue(prompt.contains("olqScores"))
    }

    // ===========================================
    // WAT ENHANCED PROMPT TESTS
    // ===========================================

    @Test
    fun `WAT prompt includes SSB context from SSOT`() {
        val responses = listOf("word1" to "response1", "word2" to "response2")
        val prompt = EnhancedPsychologyPrompts.buildWATPrompt(
            responses = responses,
            averageResponseTime = 2.5
        )
        
        assertTrue(prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `WAT prompt includes factor consistency rules`() {
        val responses = listOf("word1" to "response1")
        val prompt = EnhancedPsychologyPrompts.buildWATPrompt(
            responses = responses,
            averageResponseTime = 2.0
        )
        
        assertTrue(
            prompt.contains("consistency") ||
            prompt.contains("tick") ||
            prompt.contains("variation")
        )
    }

    @Test
    fun `WAT prompt includes WAT-specific indicators`() {
        val responses = listOf("word1" to "response1")
        val prompt = EnhancedPsychologyPrompts.buildWATPrompt(
            responses = responses,
            averageResponseTime = 2.0
        )
        
        // WAT-specific indicators
        assertTrue(
            prompt.contains("association") ||
            prompt.contains("word") ||
            prompt.contains("response time")
        )
    }

    // ===========================================
    // SRT ENHANCED PROMPT TESTS
    // ===========================================

    @Test
    fun `SRT prompt includes SSB context from SSOT`() {
        val situations = listOf("Situation 1" to "Response 1")
        val prompt = EnhancedPsychologyPrompts.buildSRTPrompt(
            situations = situations
        )
        
        assertTrue(prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `SRT prompt includes limitation guidance`() {
        val situations = listOf("Situation 1" to "Response 1")
        val prompt = EnhancedPsychologyPrompts.buildSRTPrompt(
            situations = situations
        )
        
        assertTrue(
            prompt.contains("limitation") ||
            prompt.contains("8") ||
            prompt.contains("threshold")
        )
    }

    @Test
    fun `SRT prompt includes SRT-specific indicators`() {
        val situations = listOf("Situation 1" to "Response 1")
        val prompt = EnhancedPsychologyPrompts.buildSRTPrompt(
            situations = situations
        )
        
        // SRT-specific indicators
        assertTrue(
            prompt.contains("situation") ||
            prompt.contains("action") ||
            prompt.contains("practical")
        )
    }

    // ===========================================
    // SDT ENHANCED PROMPT TESTS
    // ===========================================

    @Test
    fun `SDT prompt includes SSB context from SSOT`() {
        val descriptions = mapOf(
            "parents" to "Parents view",
            "teachers" to "Teachers view",
            "friends" to "Friends view",
            "self" to "Self view"
        )
        val prompt = EnhancedPsychologyPrompts.buildSDTPrompt(
            descriptions = descriptions
        )
        
        assertTrue(prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `SDT prompt includes critical quality warnings`() {
        val descriptions = mapOf(
            "parents" to "Parents view",
            "self" to "Self view"
        )
        val prompt = EnhancedPsychologyPrompts.buildSDTPrompt(
            descriptions = descriptions
        )
        
        assertTrue(
            prompt.contains("critical") ||
            prompt.contains("CRITICAL")
        )
    }

    @Test
    fun `SDT prompt includes SDT-specific indicators`() {
        val descriptions = mapOf("self" to "Self view")
        val prompt = EnhancedPsychologyPrompts.buildSDTPrompt(
            descriptions = descriptions
        )
        
        // SDT-specific indicators
        assertTrue(
            prompt.contains("self-awareness") ||
            prompt.contains("perspective") ||
            prompt.contains("consistency") ||
            prompt.contains("discrepancy")
        )
    }

    // ===========================================
    // PPDT ENHANCED PROMPT TESTS
    // ===========================================

    @Test
    fun `PPDT prompt includes SSB context from SSOT`() {
        val prompt = EnhancedPsychologyPrompts.buildPPDTPrompt(
            story = "Test story",
            charactersCount = 3,
            writingTimeMinutes = 4.0,
            imageContext = "Scene description",
            candidateGender = "male"
        )
        
        assertTrue(prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `PPDT prompt includes scoring scale instructions`() {
        val prompt = EnhancedPsychologyPrompts.buildPPDTPrompt(
            story = "Test story",
            charactersCount = 3,
            writingTimeMinutes = 4.0,
            imageContext = "Scene description",
            candidateGender = "male"
        )
        
        assertTrue(prompt.contains("1-10") || prompt.contains("lower"))
    }

    @Test
    fun `PPDT prompt includes PPDT-specific indicators`() {
        val prompt = EnhancedPsychologyPrompts.buildPPDTPrompt(
            story = "Test story",
            charactersCount = 3,
            writingTimeMinutes = 4.0,
            imageContext = "Scene description",
            candidateGender = "male"
        )
        
        // PPDT-specific indicators
        assertTrue(
            prompt.contains("hero") ||
            prompt.contains("protagonist") ||
            prompt.contains("picture") ||
            prompt.contains("story")
        )
    }

    @Test
    fun `PPDT prompt includes gender guidance`() {
        val malePrompt = EnhancedPsychologyPrompts.buildPPDTPrompt(
            story = "Test story",
            charactersCount = 3,
            writingTimeMinutes = 4.0,
            imageContext = "Scene",
            candidateGender = "male"
        )
        
        assertTrue(malePrompt.contains("male") || malePrompt.contains("protagonist"))
    }

    // ===========================================
    // COMPLETE SSB CONTEXT TESTS
    // ===========================================

    @Test
    fun `all prompts include 15 OLQs requirement`() {
        val tatPrompt = EnhancedPsychologyPrompts.buildTATPrompt(listOf("Story"))
        val watPrompt = EnhancedPsychologyPrompts.buildWATPrompt(listOf("w" to "r"), 2.0)
        val srtPrompt = EnhancedPsychologyPrompts.buildSRTPrompt(listOf("s" to "r"))
        val sdtPrompt = EnhancedPsychologyPrompts.buildSDTPrompt(mapOf("self" to "view"))
        val ppdtPrompt = EnhancedPsychologyPrompts.buildPPDTPrompt("story", 3, 4.0, "context", "male")
        
        listOf(tatPrompt, watPrompt, srtPrompt, sdtPrompt, ppdtPrompt).forEach { prompt ->
            assertTrue("Prompt should mention 15 OLQs", 
                prompt.contains("15") || prompt.contains("ALL"))
            assertTrue("Prompt should require JSON",
                prompt.contains("JSON"))
        }
    }

    @Test
    fun `all prompts include Factor II critical warning`() {
        val tatPrompt = EnhancedPsychologyPrompts.buildTATPrompt(listOf("Story"))
        val watPrompt = EnhancedPsychologyPrompts.buildWATPrompt(listOf("w" to "r"), 2.0)
        val srtPrompt = EnhancedPsychologyPrompts.buildSRTPrompt(listOf("s" to "r"))
        val sdtPrompt = EnhancedPsychologyPrompts.buildSDTPrompt(mapOf("self" to "view"))
        val ppdtPrompt = EnhancedPsychologyPrompts.buildPPDTPrompt("story", 3, 4.0, "context", "male")
        
        listOf(tatPrompt, watPrompt, srtPrompt, sdtPrompt, ppdtPrompt).forEach { prompt ->
            assertTrue("Prompt should warn about Factor II",
                prompt.contains("Factor II") || 
                prompt.contains("Social") ||
                prompt.contains("critical"))
        }
    }
}
