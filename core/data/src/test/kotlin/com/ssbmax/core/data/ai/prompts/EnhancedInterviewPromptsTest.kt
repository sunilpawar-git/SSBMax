package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.interview.OLQ
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD Tests for Enhanced Interview Prompts
 * 
 * Tests verify that interview prompts use SSBPromptCore SSOT for:
 * - Factor context (4 SSB factors)
 * - Critical quality warnings
 * - Factor consistency rules
 * - Scoring scale (1-10, lower = better)
 * - Test-specific penalizing/boosting indicators
 */
class EnhancedInterviewPromptsTest {

    // ===========================================
    // QUESTION GENERATION TESTS
    // ===========================================

    @Test
    fun `Question generation prompt includes SSB factor context`() {
        val prompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Candidate is from Delhi, studied engineering, plays cricket",
            count = 5,
            difficulty = 3,
            targetOLQs = null
        )
        
        // Should include factor context from SSBPromptCore
        assertTrue("Question gen prompt should include Factor I (Planning)", 
            prompt.contains("Factor I") || prompt.contains("Planning") || prompt.contains("Intellectual"))
        assertTrue("Question gen prompt should include Factor II (Social)", 
            prompt.contains("Factor II") || prompt.contains("Social"))
    }

    @Test
    fun `Question generation prompt includes critical quality warnings`() {
        val prompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test PIQ context",
            count = 3,
            difficulty = 4,
            targetOLQs = listOf(OLQ.REASONING_ABILITY, OLQ.COOPERATION)
        )
        
        // Should include critical quality warnings
        assertTrue("Question gen prompt should warn about critical OLQs", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
    }

    @Test
    fun `Question generation prompt includes correct scoring context`() {
        val prompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test context",
            count = 3,
            difficulty = 2,
            targetOLQs = null
        )
        
        // Should include scoring context
        assertTrue("Question gen prompt should explain scoring scale", 
            prompt.contains("1-10") || prompt.contains("1 to 10"))
    }

    @Test
    fun `Question generation prompt includes penalizing and boosting indicators`() {
        val prompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test context",
            count = 5,
            difficulty = 3,
            targetOLQs = null
        )
        
        // Should include interview-specific indicators
        assertTrue("Question gen prompt should include behavioral indicators", 
            (prompt.contains("penaliz") || prompt.contains("Penaliz") || prompt.contains("PENALIZ")) ||
            (prompt.contains("boost") || prompt.contains("Boost") || prompt.contains("BOOST")) ||
            prompt.contains("indicator") || prompt.contains("Indicator"))
    }

    // ===========================================
    // ADAPTIVE FOLLOW-UP TESTS
    // ===========================================

    @Test
    fun `Adaptive follow-up prompt includes SSB factor context`() {
        val prompt = EnhancedInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Candidate background info",
            previousQA = listOf(
                Pair("Tell me about your leadership experience", "I led a college project team"),
                Pair("What challenges did you face?", "Managing different personalities was hard")
            ),
            weakOLQs = listOf(OLQ.COOPERATION, OLQ.INFLUENCE_GROUP),
            count = 3
        )
        
        // Should include factor context
        assertTrue("Adaptive prompt should include factor context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `Adaptive follow-up prompt includes critical quality warnings`() {
        val prompt = EnhancedInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test context",
            previousQA = listOf(Pair("Q1", "A1")),
            weakOLQs = listOf(OLQ.SOCIAL_ADJUSTMENT),
            count = 2
        )
        
        assertTrue("Adaptive prompt should include critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
    }

    @Test
    fun `Adaptive follow-up prompt includes scoring guidance`() {
        val prompt = EnhancedInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test context",
            previousQA = listOf(Pair("Q1", "A1")),
            weakOLQs = listOf(OLQ.INITIATIVE),
            count = 2
        )
        
        val promptLower = prompt.lowercase()
        assertTrue("Adaptive prompt should indicate lower is better", 
            promptLower.contains("lower") && promptLower.contains("better"))
    }

    // ===========================================
    // RESPONSE ANALYSIS TESTS
    // ===========================================

    @Test
    fun `Response analysis prompt includes SSB factor context`() {
        val prompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Tell me about a time you led a team",
            responseText = "In my college fest, I organized the tech event...",
            expectedOLQs = listOf(OLQ.INITIATIVE, OLQ.ORGANIZING_ABILITY, OLQ.INFLUENCE_GROUP),
            responseMode = "Text"
        )
        
        assertTrue("Response analysis prompt should include factor context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `Response analysis prompt includes critical quality warnings`() {
        val prompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "How do you handle conflicts?",
            responseText = "I try to understand both sides...",
            expectedOLQs = listOf(OLQ.COOPERATION, OLQ.SOCIAL_ADJUSTMENT),
            responseMode = "Text"
        )
        
        assertTrue("Response analysis prompt should include critical warnings", 
            prompt.contains("critical") || prompt.contains("CRITICAL"))
    }

    @Test
    fun `Response analysis prompt includes correct scoring scale`() {
        val prompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test question",
            responseText = "Test response",
            expectedOLQs = listOf(OLQ.DETERMINATION),
            responseMode = "Text"
        )
        
        assertTrue("Response analysis should explain 1-10 scale", 
            prompt.contains("1-10") || prompt.contains("1 to 10"))
        val promptLower = prompt.lowercase()
        assertTrue("Response analysis should indicate lower is better", 
            promptLower.contains("lower") && promptLower.contains("better"))
    }

    @Test
    fun `Response analysis prompt includes limitation guidance`() {
        val prompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test question",
            responseText = "Test response",
            expectedOLQs = listOf(OLQ.COURAGE, OLQ.SENSE_OF_RESPONSIBILITY),
            responseMode = "Text"
        )
        
        assertTrue("Response analysis should include limitation guidance", 
            prompt.contains("limitation") || prompt.contains("Limitation") || 
            prompt.contains("8") || prompt.contains("threshold"))
    }

    @Test
    fun `Response analysis prompt includes penalizing and boosting indicators`() {
        val prompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test question",
            responseText = "Test response",
            expectedOLQs = listOf(OLQ.SELF_CONFIDENCE),
            responseMode = "Text"
        )
        
        assertTrue("Response analysis should include penalizing/boosting", 
            (prompt.contains("penaliz") || prompt.contains("Penaliz")) ||
            (prompt.contains("boost") || prompt.contains("Boost")))
    }

    // ===========================================
    // FEEDBACK GENERATION TESTS
    // ===========================================

    @Test
    fun `Feedback prompt includes SSB factor context`() {
        val prompt = EnhancedInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test PIQ context",
            questionAnswerPairs = listOf(
                Pair("Q1", "A1"),
                Pair("Q2", "A2")
            ),
            olqScores = mapOf(
                OLQ.EFFECTIVE_INTELLIGENCE to 5.5f,
                OLQ.COOPERATION to 6.0f,
                OLQ.INITIATIVE to 5.0f
            )
        )
        
        assertTrue("Feedback prompt should include factor context", 
            prompt.contains("Factor") || prompt.contains("OLQ"))
    }

    @Test
    fun `Feedback prompt includes critical quality analysis`() {
        val prompt = EnhancedInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test context",
            questionAnswerPairs = listOf(Pair("Q1", "A1")),
            olqScores = mapOf(
                OLQ.SOCIAL_ADJUSTMENT to 7.5f,
                OLQ.REASONING_ABILITY to 8.0f
            )
        )
        
        assertTrue("Feedback prompt should include critical quality analysis", 
            prompt.contains("critical") || prompt.contains("CRITICAL") ||
            prompt.contains("Factor II") || prompt.contains("limitation"))
    }

    @Test
    fun `Feedback prompt includes scoring scale explanation`() {
        val prompt = EnhancedInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test context",
            questionAnswerPairs = listOf(Pair("Q1", "A1")),
            olqScores = mapOf(OLQ.DETERMINATION to 6.0f)
        )
        
        val promptLower = prompt.lowercase()
        assertTrue("Feedback should explain scoring (lower is better)", 
            promptLower.contains("lower") && promptLower.contains("better"))
    }

    // ===========================================
    // JSON OUTPUT FORMAT TESTS
    // ===========================================

    @Test
    fun `All interview prompts include JSON output instructions`() {
        val questionGen = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            "PIQ", 3, 3, null
        )
        val adaptive = EnhancedInterviewPrompts.buildAdaptiveQuestionPrompt(
            "PIQ", listOf(Pair("Q", "A")), listOf(OLQ.INITIATIVE), 2
        )
        val analysis = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            "Q", "A", listOf(OLQ.COURAGE), "Text"
        )

        assertTrue("Question gen should mention JSON", 
            questionGen.contains("JSON") || questionGen.contains("json"))
        assertTrue("Adaptive prompt should mention JSON", 
            adaptive.contains("JSON") || adaptive.contains("json"))
        assertTrue("Analysis prompt should mention JSON", 
            analysis.contains("JSON") || analysis.contains("json"))
    }

    @Test
    fun `Interview prompts include factor consistency guidance`() {
        val analysisPrompt = EnhancedInterviewPrompts.buildResponseAnalysisPrompt(
            "Test question",
            "Test response",
            listOf(OLQ.EFFECTIVE_INTELLIGENCE, OLQ.REASONING_ABILITY, OLQ.ORGANIZING_ABILITY),
            "Text"
        )
        
        assertTrue("Analysis prompt should include consistency guidance", 
            analysisPrompt.contains("consistency") || analysisPrompt.contains("Consistency") ||
            analysisPrompt.contains("±1") || analysisPrompt.contains("±2") ||
            analysisPrompt.contains("Factor"))
    }

    // ===========================================
    // DIFFICULTY LEVEL TESTS
    // ===========================================

    @Test
    fun `Question generation respects difficulty levels`() {
        val easyPrompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            "PIQ", 3, 1, null
        )
        val hardPrompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            "PIQ", 3, 5, null
        )
        
        assertTrue("Easy prompt should mention icebreaker/rapport", 
            easyPrompt.lowercase().contains("icebreaker") || 
            easyPrompt.lowercase().contains("rapport") ||
            easyPrompt.lowercase().contains("easy"))
        assertTrue("Hard prompt should mention stress/pressure", 
            hardPrompt.lowercase().contains("stress") || 
            hardPrompt.lowercase().contains("pressure") ||
            hardPrompt.lowercase().contains("rapid") ||
            hardPrompt.lowercase().contains("challenging"))
    }

    // ===========================================
    // OLQ TARGETING TESTS
    // ===========================================

    @Test
    fun `Question generation targets specific OLQs when provided`() {
        val targetedPrompt = EnhancedInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test PIQ",
            count = 3,
            difficulty = 3,
            targetOLQs = listOf(OLQ.COURAGE, OLQ.DETERMINATION, OLQ.STAMINA)
        )
        
        assertTrue("Targeted prompt should mention target OLQs", 
            targetedPrompt.contains("COURAGE") && 
            targetedPrompt.contains("DETERMINATION"))
    }

    @Test
    fun `Adaptive prompt focuses on weak OLQs`() {
        val adaptivePrompt = EnhancedInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test PIQ",
            previousQA = listOf(Pair("Q1", "A1")),
            weakOLQs = listOf(OLQ.LIVELINESS, OLQ.SOCIAL_ADJUSTMENT),
            count = 2
        )
        
        assertTrue("Adaptive prompt should mention weak OLQs", 
            adaptivePrompt.contains("LIVELINESS") || 
            adaptivePrompt.contains("SOCIAL_ADJUSTMENT") ||
            adaptivePrompt.contains("weak") ||
            adaptivePrompt.contains("deeper"))
    }
}
