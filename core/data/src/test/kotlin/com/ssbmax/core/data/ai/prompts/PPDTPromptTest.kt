package com.ssbmax.core.data.ai.prompts

import com.ssbmax.shared.domain.model.DeviationTolerance
import com.ssbmax.shared.domain.model.PPDTImageContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PPDTPromptTest {

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
        assertTrue("Core element 'water body' must appear in prompt", prompt.contains("water body"))
        assertTrue("Core element 'drowning figure' must appear in prompt", prompt.contains("drowning figure"))
    }

    @Test
    fun `generatePPDTMultimodalPrompt includes penalized themes`() {
        // WHY: Penalized themes tell Gemini what to score down — omitting them means incorrect scoring guidance
        val context = buildFakeImageContext(penalizedThemes = listOf("story unrelated to rescue"))
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt("test", context, "Female")
        assertTrue("Penalized theme must appear in prompt", prompt.contains("story unrelated to rescue"))
    }

    @Test
    fun `generatePPDTMultimodalPrompt includes candidate gender`() {
        // WHY: Gender context shapes protagonist-alignment scoring in the SSB rubric
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt(
            story = "test", imageContext = PPDTImageContext(), candidateGender = "Female"
        )
        assertTrue("Candidate gender 'Female' must appear in prompt", prompt.contains("Female"))
    }

    @Test
    fun `generatePPDTMultimodalPrompt output does not contain null or placeholder text when context is empty`() {
        // WHY: If context fields are empty, prompt must degrade gracefully — no "{coreElement1}" template placeholders
        val emptyContext = PPDTImageContext()
        val prompt = PPDTPrompts.generatePPDTMultimodalPrompt("story", emptyContext, "Male")
        assertFalse(
            "Prompt must not contain unfilled template placeholders",
            prompt.contains("{coreElement") || prompt.contains("{scene") || prompt.contains("{primaryOLQ")
        )
        assertFalse("Prompt must not contain literal null string", prompt.contains("null"))
    }
}
