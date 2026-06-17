package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.domain.model.TATImageContext

/**
 * TAT per-story prompt builders for Gemini AI multimodal analysis.
 * Mirrors PPDTPrompts but scoped to a single TAT story+image pair.
 */
internal object TATStoryAnalysisPrompts {

    // Murray's 3-tier need taxonomy (R5) — shared by rubric builder and tests (DRY / SSOT)
    internal val MURRAY_NEEDS_TAXONOMY = """
        |Need tier | Examples                                  | Label
        |GOOD      | Achievement, Affiliation, Dominance, Order | Positive drive
        |AVERAGE   | Deference, Exhibition, Understanding       | Neutral/contextual
        |POOR      | Aggression, Harm-avoidance, Succorance     | Negative/escapist
    """.trimMargin()

    fun generateTATStoryMultimodalPrompt(
        story: String,
        imageContext: TATImageContext,
        candidateGender: String,
        storyIndex: Int,
        totalStories: Int,
        charactersCount: Int = story.length,
        imageGenderTag: String = "MIXED"
    ): String = buildString {
        appendLine(
            "You are an SSB TAT examiner. The candidate viewed the attached picture for " +
                "30 seconds then wrote story ${storyIndex + 1} of $totalStories."
        )
        appendLine()
        append(buildPictureBriefing(imageContext))
        appendLine("=== CANDIDATE STORY ===")
        appendLine(story)
        appendLine("(Length: $charactersCount chars)")
        appendLine()
        appendLine("=== CANDIDATE PROFILE ===")
        appendLine("Gender: $candidateGender")
        appendLine()
        append(buildStoryScoringRubric(charactersCount, imageGenderTag))
        append(buildScoringSection())
    }

    private fun buildPictureBriefing(ctx: TATImageContext): String = buildString {
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

    private fun buildStoryScoringRubric(charactersCount: Int, imageGenderTag: String): String = buildString {
        appendLine("=== STORY STRUCTURE RUBRIC ===")
        appendLine("Apply these SSB-specific rules when scoring:")
        appendLine()
        appendLine("NEED TAXONOMY (R5 — Murray):")
        appendLine(MURRAY_NEEDS_TAXONOMY)
        appendLine()
        appendLine("SCORING RULES (R1–R11):")
        appendLine("R1  EFFECTIVE_INTELLIGENCE  — Hero must acknowledge all core image elements; each missed element -1")
        appendLine("R2  ORGANIZING_ABILITY      — Logical 3-act structure (situation→action→resolution); missing act -1")
        appendLine("R3  POWER_OF_EXPRESSION     — Story < 500 chars: -1 POE; story > 1200 chars with padding: -1 POE")
        if (imageGenderTag == "MIXED") {
            appendLine("R4  SELF_CONFIDENCE         — MIXED image: protagonist gender must match candidate gender; mismatch -1 SC")
        }
        appendLine("R5  REASONING_ABILITY       — Dominant need must be GOOD tier; POOR need as dominant: -1 RA")
        appendLine("R6  COOPERATION             — At least one support character who aids the hero; absent: -1 COOP")
        appendLine("R7  SENSE_OF_RESPONSIBILITY — Hero must resolve the central problem; unresolved: -1 SOR")
        appendLine("R8  INITIATIVE              — Hero must take the first proactive action; reactive-only hero: -1 INI")
        appendLine("R9  SENSE_OF_RESPONSIBILITY — Hero acting for material reward (money/fame) rather than duty: -1 SOR")
        appendLine("R10 SPEED_OF_DECISION       — Hero must commit to a decision by mid-story; prolonged indecision: -1 SOD")
        appendLine("R11 COURAGE                 — At least one adversity or setback in the story; none present: -1 COU")
        appendLine()
        appendLine("NOTE: Scores are capped at 9. Multiple rules can stack on the same OLQ.")
        appendLine("Current story length: $charactersCount chars")
        appendLine()
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
