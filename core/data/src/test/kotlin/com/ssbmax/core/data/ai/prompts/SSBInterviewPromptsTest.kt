package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.interview.OLQ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SSBInterviewPrompts
 *
 * Tests the prompt builders that generate comprehensive prompts
 * for Gemini AI to create personalized SSB interview questions.
 *
 * Key test areas:
 * - OLQ definitions completeness
 * - PIQ-to-OLQ mapping content
 * - Difficulty level descriptions
 * - Question generation prompt structure
 * - Response analysis prompt structure
 * - Feedback prompt structure
 */
class SSBInterviewPromptsTest {

    // ============================================
    // OLQ DEFINITIONS TESTS
    // ============================================

    @Test
    fun `OLQ_DEFINITIONS contains all 15 OLQs`() {
        val definitions = SSBInterviewPrompts.OLQ_DEFINITIONS

        // Verify all 15 OLQs are defined
        assertTrue("Should contain EFFECTIVE_INTELLIGENCE", 
            definitions.contains("EFFECTIVE_INTELLIGENCE"))
        assertTrue("Should contain REASONING_ABILITY", 
            definitions.contains("REASONING_ABILITY"))
        assertTrue("Should contain ORGANIZING_ABILITY", 
            definitions.contains("ORGANIZING_ABILITY"))
        assertTrue("Should contain POWER_OF_EXPRESSION", 
            definitions.contains("POWER_OF_EXPRESSION"))
        assertTrue("Should contain SOCIAL_ADJUSTMENT", 
            definitions.contains("SOCIAL_ADJUSTMENT"))
        assertTrue("Should contain COOPERATION", 
            definitions.contains("COOPERATION"))
        assertTrue("Should contain INFLUENCE_GROUP", 
            definitions.contains("INFLUENCE_GROUP"))
        assertTrue("Should contain INITIATIVE", 
            definitions.contains("INITIATIVE"))
        assertTrue("Should contain SELF_CONFIDENCE", 
            definitions.contains("SELF_CONFIDENCE"))
        assertTrue("Should contain SPEED_OF_DECISION", 
            definitions.contains("SPEED_OF_DECISION"))
        assertTrue("Should contain DETERMINATION", 
            definitions.contains("DETERMINATION"))
        assertTrue("Should contain COURAGE", 
            definitions.contains("COURAGE"))
        assertTrue("Should contain SENSE_OF_RESPONSIBILITY", 
            definitions.contains("SENSE_OF_RESPONSIBILITY"))
        assertTrue("Should contain STAMINA", 
            definitions.contains("STAMINA"))
        assertTrue("Should contain LIVELINESS", 
            definitions.contains("LIVELINESS"))
    }

    @Test
    fun `OLQ_DEFINITIONS contains behavioral indicators for each OLQ`() {
        val definitions = SSBInterviewPrompts.OLQ_DEFINITIONS

        // Verify structure includes Definition, Indicators, Questions to reveal
        assertTrue("Should contain Definition keyword", definitions.contains("Definition:"))
        assertTrue("Should contain Indicators keyword", definitions.contains("Indicators:"))
        assertTrue("Should contain Questions to reveal keyword", 
            definitions.contains("Questions to reveal:"))
    }

    @Test
    fun `OLQ_DEFINITIONS groups OLQs by factors`() {
        val definitions = SSBInterviewPrompts.OLQ_DEFINITIONS

        // Verify four factor groupings
        assertTrue("Should contain Intellectual Qualities", 
            definitions.contains("INTELLECTUAL QUALITIES"))
        assertTrue("Should contain Social Qualities", 
            definitions.contains("SOCIAL QUALITIES"))
        assertTrue("Should contain Dynamic Qualities", 
            definitions.contains("DYNAMIC QUALITIES"))
        assertTrue("Should contain Character", 
            definitions.contains("CHARACTER"))
    }

    // ============================================
    // PIQ TO OLQ MAPPING TESTS
    // ============================================

    @Test
    fun `PIQ_TO_OLQ_MAPPING contains all PIQ sections`() {
        val mapping = SSBInterviewPrompts.PIQ_TO_OLQ_MAPPING

        assertTrue("Should contain Personal Background", 
            mapping.contains("PERSONAL BACKGROUND"))
        assertTrue("Should contain Family Environment", 
            mapping.contains("FAMILY ENVIRONMENT"))
        assertTrue("Should contain Education Journey", 
            mapping.contains("EDUCATION JOURNEY"))
        assertTrue("Should contain Career & Work", 
            mapping.contains("CAREER & WORK"))
        assertTrue("Should contain Activities & Interests", 
            mapping.contains("ACTIVITIES & INTERESTS"))
        assertTrue("Should contain Leadership Exposure", 
            mapping.contains("LEADERSHIP EXPOSURE"))
        assertTrue("Should contain SSB Journey", 
            mapping.contains("SSB JOURNEY"))
        assertTrue("Should contain Self-Assessment", 
            mapping.contains("SELF-ASSESSMENT"))
    }

    @Test
    fun `PIQ_TO_OLQ_MAPPING provides OLQ connections`() {
        val mapping = SSBInterviewPrompts.PIQ_TO_OLQ_MAPPING

        // Verify specific mappings exist
        assertTrue("Should map rural background to SOCIAL_ADJUSTMENT", 
            mapping.contains("Rural") && mapping.contains("SOCIAL_ADJUSTMENT"))
        assertTrue("Should map defense family to COURAGE", 
            mapping.contains("Defense family") && mapping.contains("COURAGE"))
        assertTrue("Should map NCC to SENSE_OF_RESPONSIBILITY", 
            mapping.contains("NCC") && mapping.contains("SENSE_OF_RESPONSIBILITY"))
    }

    // ============================================
    // DIFFICULTY DESCRIPTION TESTS
    // ============================================

    @Test
    fun `getDifficultyDescription returns correct description for level 1`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(1)
        assertTrue("Level 1 should be Icebreaker", description.contains("Icebreaker"))
    }

    @Test
    fun `getDifficultyDescription returns correct description for level 2`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(2)
        assertTrue("Level 2 should be Basic Probing", description.contains("Basic Probing"))
    }

    @Test
    fun `getDifficultyDescription returns correct description for level 3`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(3)
        assertTrue("Level 3 should be Moderate Challenge", description.contains("Moderate Challenge"))
    }

    @Test
    fun `getDifficultyDescription returns correct description for level 4`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(4)
        assertTrue("Level 4 should be Deep Probing", description.contains("Deep Probing"))
    }

    @Test
    fun `getDifficultyDescription returns correct description for level 5`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(5)
        assertTrue("Level 5 should be Stress Testing", description.contains("Stress Testing"))
    }

    @Test
    fun `getDifficultyDescription handles invalid level`() {
        val description = SSBInterviewPrompts.getDifficultyDescription(99)
        assertTrue("Invalid level should return Standard", description.contains("Standard"))
    }

    // ============================================
    // QUESTION GENERATION PROMPT TESTS
    // ============================================

    @Test
    fun `buildQuestionGenerationPrompt includes PIQ context`() {
        // Given
        val piqContext = "Test candidate: John Doe, Age: 25"

        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = piqContext,
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should include PIQ context", prompt.contains("John Doe"))
        assertTrue("Should include PIQ context", prompt.contains("Age: 25"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes OLQ definitions`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should include OLQ definitions", 
            prompt.contains("EFFECTIVE_INTELLIGENCE"))
        assertTrue("Should include OLQ behavioral indicators", 
            prompt.contains("Definition:"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes PIQ to OLQ mapping`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should include mapping section", 
            prompt.contains("PIQ TO OLQ MAPPING"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes question count`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 7,
            difficulty = 3
        )

        // Then
        assertTrue("Should include question count", prompt.contains("7"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes difficulty level`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 4
        )

        // Then
        assertTrue("Should include difficulty level", prompt.contains("4/5"))
        assertTrue("Should include difficulty description", prompt.contains("Deep Probing"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes personalization requirements`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should require personalization", 
            prompt.contains("PERSONALIZATION IS CRITICAL"))
        assertTrue("Should mention referencing PIQ details", 
            prompt.contains("reference specific details"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes quality constraints`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should prohibit yes/no questions", 
            prompt.contains("NO yes/no questions"))
        assertTrue("Should prohibit generic questions", 
            prompt.contains("NO generic questions"))
    }

    @Test
    fun `buildQuestionGenerationPrompt includes JSON output format`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3
        )

        // Then
        assertTrue("Should include JSON format", prompt.contains("OUTPUT FORMAT"))
        assertTrue("Should include questionText field", prompt.contains("questionText"))
        assertTrue("Should include targetOLQs field", prompt.contains("targetOLQs"))
        assertTrue("Should include reasoning field", prompt.contains("reasoning"))
    }

    @Test
    fun `buildQuestionGenerationPrompt handles target OLQs parameter`() {
        // Given
        val targetOLQs = listOf(OLQ.INITIATIVE, OLQ.COURAGE)

        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3,
            targetOLQs = targetOLQs
        )

        // Then
        assertTrue("Should include target OLQs", prompt.contains("INITIATIVE"))
        assertTrue("Should include target OLQs", prompt.contains("COURAGE"))
    }

    @Test
    fun `buildQuestionGenerationPrompt with null targetOLQs generates balanced mix`() {
        // When
        val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
            piqContext = "Test",
            count = 5,
            difficulty = 3,
            targetOLQs = null
        )

        // Then
        assertTrue("Should mention balanced mix", 
            prompt.contains("balanced mix") || prompt.contains("all OLQ clusters"))
    }

    // ============================================
    // ADAPTIVE QUESTION PROMPT TESTS
    // ============================================

    @Test
    fun `buildAdaptiveQuestionPrompt includes previous QA history`() {
        // Given
        val previousQA = listOf(
            "Q1: Tell me about yourself?" to "I am an engineer",
            "Q2: Why defense?" to "Want to serve nation"
        )
        val weakOLQs = listOf(OLQ.INITIATIVE, OLQ.DETERMINATION)

        // When
        val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test",
            previousQA = previousQA,
            weakOLQs = weakOLQs,
            count = 2
        )

        // Then
        assertTrue("Should include previous Q&A", prompt.contains("engineer"))
        assertTrue("Should include previous response", prompt.contains("serve nation"))
    }

    @Test
    fun `buildAdaptiveQuestionPrompt includes weak OLQs`() {
        // Given
        val weakOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.SPEED_OF_DECISION)

        // When
        val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test",
            previousQA = emptyList(),
            weakOLQs = weakOLQs,
            count = 2
        )

        // Then
        assertTrue("Should include weak OLQs", prompt.contains("SELF_CONFIDENCE"))
        assertTrue("Should include weak OLQs", prompt.contains("SPEED_OF_DECISION"))
    }

    @Test
    fun `buildAdaptiveQuestionPrompt specifies higher difficulty`() {
        // When
        val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
            piqContext = "Test",
            previousQA = emptyList(),
            weakOLQs = listOf(OLQ.COURAGE),
            count = 2
        )

        // Then
        assertTrue("Should specify higher difficulty", 
            prompt.contains("4") || prompt.contains("MORE CHALLENGING"))
    }

    // ============================================
    // RESPONSE ANALYSIS PROMPT TESTS
    // ============================================

    @Test
    fun `buildResponseAnalysisPrompt includes question and response`() {
        // When
        val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Why do you want to join the army?",
            responseText = "I want to serve my nation and protect borders",
            expectedOLQs = listOf(OLQ.DETERMINATION, OLQ.COURAGE),
            responseMode = "TEXT_BASED"
        )

        // Then
        assertTrue("Should include question", prompt.contains("Why do you want to join"))
        assertTrue("Should include response", prompt.contains("serve my nation"))
    }

    @Test
    fun `buildResponseAnalysisPrompt includes expected OLQs`() {
        // When
        val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test question",
            responseText = "Test response",
            expectedOLQs = listOf(OLQ.INITIATIVE, OLQ.ORGANIZING_ABILITY),
            responseMode = "TEXT_BASED"
        )

        // Then
        assertTrue("Should include expected OLQs", prompt.contains("INITIATIVE"))
        assertTrue("Should include expected OLQs", prompt.contains("ORGANIZING_ABILITY"))
    }

    @Test
    fun `buildResponseAnalysisPrompt includes scoring scale`() {
        // When
        val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test",
            responseText = "Test",
            expectedOLQs = listOf(OLQ.COURAGE),
            responseMode = "TEXT_BASED"
        )

        // Then
        assertTrue("Should include SSB scoring", prompt.contains("LOWER IS BETTER"))
        assertTrue("Should include score range", prompt.contains("1-10") || prompt.contains("1-2"))
        assertTrue("Should include Exceptional", prompt.contains("Exceptional"))
    }

    @Test
    fun `buildResponseAnalysisPrompt includes JSON output format`() {
        // When
        val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = "Test",
            responseText = "Test",
            expectedOLQs = listOf(OLQ.COURAGE),
            responseMode = "TEXT_BASED"
        )

        // Then
        assertTrue("Should include olqScores", prompt.contains("olqScores"))
        assertTrue("Should include overallConfidence", prompt.contains("overallConfidence"))
        assertTrue("Should include keyInsights", prompt.contains("keyInsights"))
        assertTrue("Should include evidence", prompt.contains("evidence"))
    }

    // ============================================
    // FEEDBACK PROMPT TESTS
    // ============================================

    @Test
    fun `buildFeedbackPrompt includes QA history`() {
        // Given
        val qaPairs = listOf(
            "Tell me about yourself" to "I am a software engineer",
            "Why defense?" to "To serve the nation"
        )
        val olqScores = mapOf(
            OLQ.SELF_CONFIDENCE to 5.5f,
            OLQ.POWER_OF_EXPRESSION to 6.0f
        )

        // When
        val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test candidate",
            questionAnswerPairs = qaPairs,
            olqScores = olqScores
        )

        // Then
        assertTrue("Should include Q&A", prompt.contains("software engineer"))
        assertTrue("Should include Q&A", prompt.contains("serve the nation"))
    }

    @Test
    fun `buildFeedbackPrompt includes OLQ scores`() {
        // Given
        val olqScores = mapOf(
            OLQ.INITIATIVE to 4.5f,
            OLQ.COURAGE to 7.0f
        )

        // When
        val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test",
            questionAnswerPairs = emptyList(),
            olqScores = olqScores
        )

        // Then
        assertTrue("Should include Initiative score", 
            prompt.contains("Initiative") || prompt.contains("INITIATIVE"))
        assertTrue("Should include Courage score", 
            prompt.contains("Courage") || prompt.contains("COURAGE"))
    }

    @Test
    fun `buildFeedbackPrompt requests structured feedback`() {
        // When
        val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test",
            questionAnswerPairs = emptyList(),
            olqScores = emptyMap()
        )

        // Then
        assertTrue("Should request overall performance", 
            prompt.contains("OVERALL PERFORMANCE") || prompt.contains("Overall"))
        assertTrue("Should request strengths", prompt.contains("STRENGTH") || prompt.contains("Strength"))
        assertTrue("Should request improvement areas", 
            prompt.contains("IMPROVEMENT") || prompt.contains("improvement"))
    }

    @Test
    fun `buildFeedbackPrompt specifies tone requirements`() {
        // When
        val prompt = SSBInterviewPrompts.buildFeedbackPrompt(
            piqContext = "Test",
            questionAnswerPairs = emptyList(),
            olqScores = emptyMap()
        )

        // Then
        assertTrue("Should specify professional tone", 
            prompt.contains("Professional") || prompt.contains("professional"))
        assertTrue("Should specify constructive", 
            prompt.contains("constructive") || prompt.contains("Constructive"))
    }
}
















