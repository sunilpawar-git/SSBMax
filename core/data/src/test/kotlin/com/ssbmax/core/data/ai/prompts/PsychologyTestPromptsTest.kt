package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PsychologyTestPromptsTest {

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

    @Test
    fun `generatePPDTAnalysisPrompt includes image context when provided`() {
        val story = "This is a story about a hero."
        val context = "A young man is sitting in a park."
        val gender = "male"
        val submission = createMockSubmission(story)

        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, context, gender)

        assertTrue("Prompt should include image context", prompt.contains("A young man is sitting in a park"))
        assertTrue("Prompt should include the story", prompt.contains(story))
    }

    @Test
    fun `generatePPDTAnalysisPrompt includes gender hint for male`() {
        val submission = createMockSubmission("Story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue(
            "Prompt should include male gender hint",
            prompt.contains("Prefer a male protagonist in the story")
        )
    }

    @Test
    fun `generatePPDTAnalysisPrompt includes gender hint for female`() {
        val submission = createMockSubmission("Story")
        // Case insensitive check
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "Female")

        assertTrue(
            "Prompt should include female gender hint",
            prompt.contains("Prefer a female protagonist in the story")
        )
    }

    @Test
    fun `generatePPDTAnalysisPrompt includes gender hint for other`() {
        val submission = createMockSubmission("Story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "other")

        assertTrue(
            "Prompt should include neutral gender hint",
            prompt.contains("Gender of the protagonist is not constrained")
        )
    }
    
    @Test
    fun `generatePPDTAnalysisPrompt includes specific scoring hints`() {
        val submission = createMockSubmission("Story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission, "Context", "male")

        assertTrue("Should include speed of decision hint", prompt.contains("SPEED_OF_DECISION score"))
        assertTrue("Should include reward penalty hint", prompt.contains("material reward"))
        assertTrue("Should include structure bonus hint", prompt.contains("Past-Present-Future"))
    }

    @Test
    fun `legacy overload uses placeholder context and default gender`() {
        val submission = createMockSubmission("Story")
        val prompt = PsychologyTestPrompts.generatePPDTAnalysisPrompt(submission)

        // Legacy uses default "male" logic inside our new implementation setup 
        // (The implementation we wrote defaults to passing "male" and empty context)
        assertTrue(
            "Legacy prompt should default to male preference",
            prompt.contains("Prefer a male protagonist")
        )
        assertTrue(
            "Legacy prompt should contain Image Context section even if empty",
            prompt.contains("Image Context")
        )
    }
}
