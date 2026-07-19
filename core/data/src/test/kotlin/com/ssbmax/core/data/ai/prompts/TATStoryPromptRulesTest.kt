package com.ssbmax.core.data.ai.prompts

import com.ssbmax.shared.domain.model.TATImageContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TATStoryPromptRulesTest {

    private val defaultContext = TATImageContext()
    private val testStory = "The hero stood tall and faced the challenge bravely. He led the team."

    private fun buildPrompt(
        story: String = testStory,
        imageGenderTag: String = "MIXED"
    ) = TATStoryAnalysisPrompts.generateTATStoryMultimodalPrompt(
        story = story,
        imageContext = defaultContext,
        candidateGender = "MALE",
        storyIndex = 0,
        totalStories = 11,
        imageGenderTag = imageGenderTag
    )

    @Test
    fun `prompt contains STORY STRUCTURE RUBRIC header`() {
        // WHY: R1-R11 scoring rules injected via rubric; header guards against accidental omission
        assertTrue("STORY STRUCTURE RUBRIC must be in prompt",
            "STORY STRUCTURE RUBRIC" in buildPrompt())
    }

    @Test
    fun `prompt contains 500-char threshold for R3 word-count penalty`() {
        // WHY: R3 penalty fires only for stories < 500 chars; wrong threshold gives wrong penalty
        assertTrue("500-char threshold must be stated for R3",
            "500" in buildPrompt())
    }

    @Test
    fun `prompt injects MIXED gender tag when imageGenderTag is MIXED`() {
        // WHY: R4 hero-gender rule fires only for MIXED-tagged images; must be injected when applicable
        assertTrue("MIXED tag must appear in prompt for mixed-gender images",
            "MIXED" in buildPrompt(imageGenderTag = "MIXED"))
    }

    @Test
    fun `prompt omits gender rule when imageGenderTag is MALE`() {
        // WHY: R4 must NOT fire for single-gender images — injecting it penalises correct stories
        assertFalse("Gender rule section must be absent for MALE-only images",
            "protagonist gender" in buildPrompt(imageGenderTag = "MALE").lowercase())
    }

    @Test
    fun `prompt contains Murray need category labels POOR and GOOD`() {
        // WHY: R5 Murray 3-tier taxonomy must guide need classification; missing labels skip categories
        val prompt = buildPrompt()
        assertTrue("Murray POOR need label must be present", "POOR" in prompt)
        assertTrue("Murray GOOD need label must be present", "GOOD" in prompt)
    }

    @Test
    fun `prompt contains material reward phrase for R9 SOR penalty`() {
        // WHY: R9 SOR score drops when hero acts for material reward; phrase must match rubric
        assertTrue("Material reward phrase must be present for R9 SOR penalty",
            "material reward" in buildPrompt().lowercase())
    }
}
