package com.ssbmax.shared.ai.prompts

import com.ssbmax.shared.domain.model.DeviationTolerance
import com.ssbmax.shared.domain.model.PPDTImageContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from core:data's `PPDTPromptTest` (Phase 9.0, when `shared`'s
 * `PPDTPrompts` became the only copy) — the assertions are unchanged, they
 * now just run on both platforms instead of Android only.
 */
class PPDTPromptsTest {

    private fun buildFakeImageContext(
        sceneDescription: String = "Officers planning by a map",
        coreElements: List<String> = listOf("map", "officers"),
        ambiguousElements: List<String> = listOf("rank", "location"),
        expectedThemes: List<String> = listOf("leadership", "strategy"),
        penalizedThemes: List<String> = listOf("unrelated drama"),
        primaryOLQs: List<String> = listOf("ORGANIZING_ABILITY", "INFLUENCE_GROUP"),
        deviationTolerance: DeviationTolerance = DeviationTolerance.MEDIUM,
        exemplarGoodHints: List<String> = listOf("Hero takes charge of planning"),
        exemplarBadHints: List<String> = listOf("Story unrelated to scene")
    ) = PPDTImageContext(
        sceneDescription = sceneDescription,
        coreElements = coreElements,
        ambiguousElements = ambiguousElements,
        expectedThemes = expectedThemes,
        penalizedThemes = penalizedThemes,
        primaryOLQs = primaryOLQs,
        deviationTolerance = deviationTolerance,
        exemplarGoodHints = exemplarGoodHints,
        exemplarBadHints = exemplarBadHints
    )

    @Test
    fun `generatePPDTMultimodalPrompt includes core elements from imageContext`() {
        // WHY: Core elements drive scene-accuracy scoring — must appear in prompt so Gemini penalises stories that miss them
        val context = buildFakeImageContext(coreElements = listOf("water body", "drowning figure"))
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt(
            story = "test story", imageContext = context, candidateGender = "Male"
        )
        assertTrue(prompt.contains("water body"), "Core element 'water body' must appear in prompt")
        assertTrue(prompt.contains("drowning figure"), "Core element 'drowning figure' must appear in prompt")
    }

    @Test
    fun `generatePPDTMultimodalPrompt includes penalized themes`() {
        // WHY: Penalized themes tell Gemini what to score down — omitting them means incorrect scoring guidance
        val context = buildFakeImageContext(penalizedThemes = listOf("story unrelated to rescue"))
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt("test", context, "Female")
        assertTrue(prompt.contains("story unrelated to rescue"), "Penalized theme must appear in prompt")
    }

    @Test
    fun `generatePPDTMultimodalPrompt includes candidate gender`() {
        // WHY: Gender context shapes protagonist-alignment scoring in the SSB rubric
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt(
            story = "test", imageContext = PPDTImageContext(), candidateGender = "Female"
        )
        assertTrue(prompt.contains("Female"), "Candidate gender 'Female' must appear in prompt")
    }

    @Test
    fun `generatePPDTMultimodalPrompt output does not contain null or placeholder text when context is empty`() {
        // WHY: If context fields are empty, prompt must degrade gracefully — no "{coreElement1}" template placeholders
        val emptyContext = PPDTImageContext()
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt("story", emptyContext, "Male")
        assertFalse(
            prompt.contains("{coreElement") || prompt.contains("{scene") || prompt.contains("{primaryOLQ"),
            "Prompt must not contain unfilled template placeholders"
        )
        assertFalse(prompt.contains("null"), "Prompt must not contain literal null string")
    }
}
