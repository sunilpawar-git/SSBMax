package com.ssbmax.shared.ai.prompts

import com.ssbmax.shared.domain.model.TATImageContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from core:data's `TATStoryPromptRulesTest` (Phase 9.0, when
 * `shared`'s `TATStoryAnalysisPrompts` became the only copy) — the assertions
 * are unchanged, they now just run on both platforms instead of Android only.
 */
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
        assertTrue("STORY STRUCTURE RUBRIC" in buildPrompt(), "STORY STRUCTURE RUBRIC must be in prompt")
    }

    @Test
    fun `prompt contains 500-char threshold for R3 word-count penalty`() {
        // WHY: R3 penalty fires only for stories < 500 chars; wrong threshold gives wrong penalty
        assertTrue("500" in buildPrompt(), "500-char threshold must be stated for R3")
    }

    @Test
    fun `prompt injects MIXED gender tag when imageGenderTag is MIXED`() {
        // WHY: R4 hero-gender rule fires only for MIXED-tagged images; must be injected when applicable
        assertTrue(
            "MIXED" in buildPrompt(imageGenderTag = "MIXED"),
            "MIXED tag must appear in prompt for mixed-gender images"
        )
    }

    @Test
    fun `prompt omits gender rule when imageGenderTag is MALE`() {
        // WHY: R4 must NOT fire for single-gender images — injecting it penalises correct stories
        assertFalse(
            "protagonist gender" in buildPrompt(imageGenderTag = "MALE").lowercase(),
            "Gender rule section must be absent for MALE-only images"
        )
    }

    @Test
    fun `prompt contains Murray need category labels POOR and GOOD`() {
        // WHY: R5 Murray 3-tier taxonomy must guide need classification; missing labels skip categories
        val prompt = buildPrompt()
        assertTrue("POOR" in prompt, "Murray POOR need label must be present")
        assertTrue("GOOD" in prompt, "Murray GOOD need label must be present")
    }

    @Test
    fun `prompt contains material reward phrase for R9 SOR penalty`() {
        // WHY: R9 SOR score drops when hero acts for material reward; phrase must match rubric
        assertTrue(
            "material reward" in buildPrompt().lowercase(),
            "Material reward phrase must be present for R9 SOR penalty"
        )
    }
}
