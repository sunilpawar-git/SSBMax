package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.UUID

/**
 * Tests for PPDT prompt generation ensuring:
 * 1. Image context is cross-checked with user response
 * 2. OLQ scoring uses unified 5-9 scale (best=5, worst=9)
 * 3. Garbage detection triggers score 9 for all OLQs
 */
class PPDTPromptContextValidationTest {

    private fun createMockSubmission(story: String): PPDTSubmission {
        return PPDTSubmission(
            submissionId = UUID.randomUUID().toString(),
            questionId = "test_q1",
            userId = "user1",
            userName = "Test User",
            userEmail = "test@example.com",
            batchId = null,
            story = story,
            charactersCount = story.length,
            viewingTimeTakenSeconds = 30,
            writingTimeTakenMinutes = 4,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            instructorReview = null,
            analysisStatus = AnalysisStatus.PENDING_ANALYSIS,
            olqResult = null
        )
    }

    // =========================================================================
    // TEST GROUP 1: Context Cross-Checking
    // Ensures the prompt includes image context for Gemini to validate user story
    // =========================================================================

    @Test
    fun `prompt includes image context section for Gemini to cross-check`() {
        val imageContext = "A hazy sketch showing three soldiers planning around a map with a lantern."
        val story = "The soldiers were planning an attack strategy."
        val submission = createMockSubmission(story)

        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, imageContext, "male")

        assertTrue("Prompt must include Image Context header", prompt.contains("--- Image Context ---"))
        assertTrue("Prompt must include actual context", prompt.contains(imageContext))
        assertTrue("Prompt must include End Context marker", prompt.contains("--- End Context ---"))
    }

    @Test
    fun `prompt includes both image context and user story for comparison`() {
        val imageContext = "A young woman leading a village meeting under a Banyan tree."
        val story = "Meera organized a community meeting to discuss water conservation."
        val submission = createMockSubmission(story)

        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, imageContext, "female")

        // Both context and story must be present for cross-validation
        assertTrue("Image context must be in prompt", prompt.contains(imageContext))
        assertTrue("User story must be in prompt", prompt.contains(story))
        assertTrue("Story section must be marked", prompt.contains("--- Story ---"))
    }

    @Test
    fun `prompt provides relevance guidance for scoring`() {
        val imageContext = "Volunteers distributing relief material from a truck."
        val story = "They helped flood victims."
        val submission = createMockSubmission(story)

        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, imageContext, "male")

        // The prompt should guide Gemini to check story relevance to image
        assertTrue(
            "Prompt should reference evaluation criteria for organizing ability (relevance scoring)",
            prompt.contains("ORGANIZING_ABILITY")
        )
    }

    // =========================================================================
    // TEST GROUP 2: Unified OLQ Scoring System (Best=5, Worst=9)
    // =========================================================================

    @Test
    fun `prompt specifies OLQ score range 5-9`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue("Prompt must specify score 5 as best", prompt.contains("5:") && prompt.contains("Very Good"))
        assertTrue("Prompt must specify score 9 as worst/fail", prompt.contains("9:") && prompt.contains("Fail"))
    }

    @Test
    fun `prompt instructs to use only 5-9 range`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must explicitly restrict score range to 5-9",
            prompt.contains("Use ONLY 5-9") || prompt.contains("SCORE RANGE") && prompt.contains("5-9")
        )
    }

    @Test
    fun `prompt instructs NOT to use scores 1-4 or 10`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must forbid scores 1-4 and 10",
            prompt.contains("Do NOT assign scores 1-4") || 
            (prompt.contains("ONLY 5-9") && prompt.contains("NOT") && prompt.contains("1-4"))
        )
    }

    @Test
    fun `prompt defines score 5 as best possible`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Score 5 must be defined as best/excellent",
            prompt.contains("5:") && (prompt.contains("BEST") || prompt.contains("Excellent") || prompt.contains("Very Good"))
        )
    }

    @Test
    fun `prompt defines score 9 as fail for gibberish`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Score 9 must be defined as fail/gibberish",
            prompt.contains("9:") && (prompt.contains("Fail") || prompt.contains("Gibberish"))
        )
    }

    // =========================================================================
    // TEST GROUP 3: Garbage Detection
    // =========================================================================

    @Test
    fun `prompt includes garbage detection instruction`() {
        val submission = createMockSubmission("asdfghjkl qwerty gibberish text")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must include garbage/gibberish detection",
            prompt.contains("GARBAGE DETECTION") || prompt.contains("gibberish")
        )
    }

    @Test
    fun `prompt instructs to assign score 9 for gibberish responses`() {
        val submission = createMockSubmission("random text")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must instruct score 9 for gibberish",
            prompt.contains("score 9") && prompt.contains("gibberish")
        )
    }

    // =========================================================================
    // TEST GROUP 4: All 15 OLQs Coverage
    // =========================================================================

    @Test
    fun `prompt lists all 15 OLQs for evaluation`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        val expectedOLQs = listOf(
            "EFFECTIVE_INTELLIGENCE",
            "REASONING_ABILITY",
            "ORGANIZING_ABILITY",
            "POWER_OF_EXPRESSION",
            "SOCIAL_ADJUSTMENT",
            "COOPERATION",
            "SENSE_OF_RESPONSIBILITY",
            "INITIATIVE",
            "SELF_CONFIDENCE",
            "SPEED_OF_DECISION",
            "INFLUENCE_GROUP",
            "LIVELINESS",
            "DETERMINATION",
            "COURAGE",
            "STAMINA"
        )

        expectedOLQs.forEach { olq ->
            assertTrue("Prompt must include OLQ: $olq", prompt.contains(olq))
        }
    }

    @Test
    fun `prompt requires all 15 OLQs in response`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must require all 15 OLQs in response",
            prompt.contains("ALL 15 OLQs") || prompt.contains("all 15")
        )
    }

    // =========================================================================
    // TEST GROUP 5: Conservative Scoring Bias
    // =========================================================================

    @Test
    fun `prompt instructs conservative scoring bias`() {
        val submission = createMockSubmission("Test story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt must instruct conservative/strict scoring",
            prompt.contains("CONSERVATIVE") || prompt.contains("Bias towards the lower side") ||
            prompt.contains("NOT be lenient")
        )
    }
}
