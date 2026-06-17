package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.data.local.entity.TATStoryAssessmentEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class TATSynthesisPromptRulesTest {

    private fun buildAssessment(index: Int) = TATStoryAssessmentEntity(
        id = "test-id-$index",
        submissionId = "sub-001",
        questionId = "q-$index",
        storyIndex = index,
        story = "Test story $index with sufficient narrative content for synthesis.",
        imageUrl = "https://example.com/image$index.jpg",
        olqScoresJson = "[]",
        overallScore = 6f,
        overallRating = "Good",
        aiConfidence = 80,
        analyzedAt = System.currentTimeMillis()
    )

    private fun buildPrompt(count: Int = 3): String {
        val assessments = (0 until count).map { buildAssessment(it) }
        return TATSynthesisPrompts.buildPrompt(assessments)
    }

    @Test
    fun `prompt contains EI-RA OLQ correlation constraint`() {
        // WHY: R13 — EI >= RA is an empirical SSB rule; missing it allows AI to emit invalid cross-factor combos
        val prompt = buildPrompt()
        assertTrue("EI score >= RA correlation rule must be in synthesis prompt",
            "EI" in prompt && "RA" in prompt && ("≥" in prompt || ">=" in prompt || "greater" in prompt.lowercase()))
    }

    @Test
    fun `prompt includes notRecommended in format spec`() {
        // WHY: R14 requires synthesis to emit notRecommended flag; absent field = parser cannot extract it
        assertTrue("notRecommended field must appear in synthesis format",
            "notRecommended" in buildPrompt())
    }

    @Test
    fun `prompt specifies blank card 1-point-5x weighting`() {
        // WHY: TAT_Pipeline.md §13 — blank card carries 1.5x weight; missing this under-weights blank card stories
        val prompt = buildPrompt()
        assertTrue("Blank card 1.5x weighting must be stated in synthesis prompt",
            "1.5" in prompt || ("blank" in prompt.lowercase() && "weight" in prompt.lowercase()))
    }
}
