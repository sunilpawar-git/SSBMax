package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.PPDTImageContext

/**
 * PPDT-specific prompt builders for Gemini AI multimodal analysis.
 * Phase 8: full per-picture rubric injection (coreElements, penalizedThemes, etc.).
 */
internal object PPDTPrompts {

    fun generatePPDTMultimodalPrompt(
        story: String,
        imageContext: PPDTImageContext,
        candidateGender: String,
        charactersCount: Int = story.length,
        writingTimeMinutes: Int = 4
    ): String = buildString {
        appendLine(
            "You are an SSB PPDT examiner. The candidate viewed the attached picture for " +
                "30 seconds (shown hazy and low-resolution) then wrote the story below."
        )
        appendLine()
        append(buildPictureBriefing(imageContext))
        appendLine("=== CANDIDATE STORY ===")
        appendLine(story)
        appendLine("(Length: $charactersCount chars | Writing time: $writingTimeMinutes min)")
        appendLine()
        appendLine("=== CANDIDATE PROFILE ===")
        appendLine("Gender: $candidateGender")
        appendLine()
        append(buildScoringSection())
    }

    private fun buildPictureBriefing(ctx: PPDTImageContext): String = buildString {
        appendLine("=== PICTURE BRIEFING ===")
        if (ctx.sceneDescription.isNotBlank()) appendLine("Scene: ${ctx.sceneDescription}")
        appendListSection(
            "Core elements (MUST be acknowledged — EFFECTIVE_INTELLIGENCE penalty if missed):",
            ctx.coreElements
        )
        appendListSection(
            "Ambiguous elements (creative interpretation acceptable — picture is hazy):",
            ctx.ambiguousElements
        )
        if (ctx.expectedThemes.isNotEmpty()) {
            appendLine("Expected story directions: ${ctx.expectedThemes.joinToString()}")
        }
        appendListSection("Penalized story themes (heavy penalty):", ctx.penalizedThemes)
        if (ctx.primaryOLQs.isNotEmpty()) {
            appendLine("Primary OLQs this picture tests: ${ctx.primaryOLQs.joinToString()}")
        }
        appendLine("Deviation tolerance: ${ctx.deviationTolerance.name}")
        appendListSection("Story elements that score well:", ctx.exemplarGoodHints, "  + ")
        appendListSection("Story elements that score poorly:", ctx.exemplarBadHints)
        appendLine()
    }

    private fun StringBuilder.appendListSection(
        label: String,
        items: List<String>,
        prefix: String = "  - "
    ) {
        if (items.isNotEmpty()) {
            appendLine(label)
            items.forEach { appendLine("$prefix$it") }
        }
    }

    private fun buildScoringSection(): String = buildString {
        appendLine("=== EVALUATION CRITERIA — ALL 15 OLQs (MANDATORY) ===")
        appendLine("1. EFFECTIVE_INTELLIGENCE: Practical wisdom, common sense")
        appendLine("2. REASONING_ABILITY: Logical thinking, problem-solving")
        appendLine("3. ORGANIZING_ABILITY: Planning, systematic approach")
        appendLine("4. POWER_OF_EXPRESSION: Communication clarity")
        appendLine("5. SOCIAL_ADJUSTMENT: Adaptability, flexibility")
        appendLine("6. COOPERATION: Teamwork, helping others")
        appendLine("7. SENSE_OF_RESPONSIBILITY: Accountability, reliability")
        appendLine("8. INITIATIVE: Proactive action, self-starting")
        appendLine("9. SELF_CONFIDENCE: Composure, positive self-image")
        appendLine("10. SPEED_OF_DECISION: Quick decision-making")
        appendLine("11. INFLUENCE_GROUP: Leadership, persuasion")
        appendLine("12. LIVELINESS: Energy, optimism")
        appendLine("13. DETERMINATION: Persistence, goal-oriented")
        appendLine("14. COURAGE: Facing fears, standing up for beliefs")
        appendLine("15. STAMINA: Endurance, resilience")
        appendLine()
        appendLine("=== SSB SCORING SCALE (LOWER IS BETTER) ===")
        appendLine("5: Very Good/Excellent (BEST possible score)")
        appendLine("6: Good (Above average)")
        appendLine("7: Average (Typical performance)")
        appendLine("8: Poor (Needs improvement)")
        appendLine("9: Fail (Gibberish/Irrelevant/Blank)")
        appendLine()
        appendLine("=== CRITICAL VALIDATION ===")
        appendLine(
            "1. GARBAGE DETECTION: If story is gibberish, random characters, or clearly " +
                "irrelevant — assign score 9 for ALL OLQs, confidence 100."
        )
        appendLine("2. CONSERVATIVE SCORING: Bias towards lower side. Do NOT be lenient.")
        appendLine("3. SCORE RANGE: Use ONLY 5-9. Do NOT assign scores 1-4 or 10.")
        appendLine()
        appendLine("=== CRITICAL INSTRUCTIONS ===")
        appendLine("1. Return ONLY a single JSON object")
        appendLine("2. NO markdown code blocks (no backtick markers)")
        appendLine("3. ALL 15 OLQs MUST be present")
        appendLine("4. Use EXACT enum names shown above")
        appendLine("5. Response must START with open-brace and END with close-brace")
        appendLine()
        appendLine("Each OLQ entry: score (int 5-9), confidence (int 0-100), reasoning (string).")
        appendLine("Key: olqScores containing all 15 OLQ keys.")
    }
}
