package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.PPDTImageContext

/**
 * PPDT-specific prompt builders for Gemini AI analysis.
 * Phase 7: minimal multimodal prompt. Phase 8 will inject per-picture rubric fully.
 */
internal object PPDTPrompts {

    /**
     * Builds the PPDT multimodal analysis prompt combining image context and candidate story.
     * The attached image gives Gemini direct visual access to the scene for accurate perception scoring.
     * Phase 8 will enhance this with full coreElements/penalizedThemes rubric injection.
     */
    fun generatePPDTMultimodalPrompt(
        story: String,
        imageContext: PPDTImageContext,
        candidateGender: String
    ): String = buildString {
        appendLine("You are an SSB PPDT examiner. The candidate viewed the attached picture for 30 seconds then wrote the story below.")
        appendLine()
        if (imageContext.sceneDescription.isNotBlank()) {
            appendLine("=== PICTURE BRIEFING ===")
            appendLine("Scene: ${imageContext.sceneDescription}")
            appendLine()
        }
        appendLine("=== CANDIDATE STORY ===")
        appendLine(story)
        appendLine()
        appendLine("=== CANDIDATE PROFILE ===")
        appendLine("Gender: $candidateGender")
        appendLine()
        appendLine("=== SCORING RULES ===")
        appendLine("Score all 15 OLQs. SSB scale: 5=Very Good 6=Good 7=Average 8=Poor 9=Fail (lower is better).")
        appendLine("Penalty: story ignores visible scene elements → reduce EFFECTIVE_INTELLIGENCE.")
        appendLine()
        appendLine("Return ONLY valid JSON (no markdown):")
        appendLine("""{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "REASONING_ABILITY": {"score": 7, "confidence": 70, "reasoning": "..."},
    "ORGANIZING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "POWER_OF_EXPRESSION": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SOCIAL_ADJUSTMENT": {"score": 6, "confidence": 75, "reasoning": "..."},
    "COOPERATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "SENSE_OF_RESPONSIBILITY": {"score": 6, "confidence": 75, "reasoning": "..."},
    "INITIATIVE": {"score": 6, "confidence": 80, "reasoning": "..."},
    "SELF_CONFIDENCE": {"score": 7, "confidence": 70, "reasoning": "..."},
    "SPEED_OF_DECISION": {"score": 7, "confidence": 75, "reasoning": "..."},
    "INFLUENCE_GROUP": {"score": 7, "confidence": 70, "reasoning": "..."},
    "LIVELINESS": {"score": 6, "confidence": 75, "reasoning": "..."},
    "DETERMINATION": {"score": 6, "confidence": 75, "reasoning": "..."},
    "COURAGE": {"score": 6, "confidence": 75, "reasoning": "..."},
    "STAMINA": {"score": 7, "confidence": 70, "reasoning": "..."}
  }
}""")
    }
}
