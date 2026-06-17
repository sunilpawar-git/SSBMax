package com.ssbmax.core.data.ai.prompts

import com.ssbmax.core.data.local.entity.TATStoryAssessmentEntity
import org.json.JSONArray

object TATSynthesisPrompts {

    fun buildPrompt(assessments: List<TATStoryAssessmentEntity>): String = buildString {
        appendLine("You are an SSB psychologist performing final TAT synthesis.")
        appendLine("You have received per-story AI assessments for ${assessments.size} TAT stories.")
        appendLine("Your task: synthesise a holistic OLQ profile by detecting patterns across all stories.")
        appendLine()
        appendLine("=== PER-STORY ASSESSMENTS ===")
        assessments.sortedBy { it.storyIndex }.forEach { a ->
            appendLine()
            appendLine(
                "--- Story ${a.storyIndex + 1} | score: ${a.overallScore}" +
                    " | rating: ${a.overallRating} | confidence: ${a.aiConfidence}% ---"
            )
            appendLine("Text: ${a.story.take(200)}${if (a.story.length > 200) "..." else ""}")
            appendLine("OLQ scores: ${compactOlqScores(a.olqScoresJson)}")
        }
        appendLine()
        appendLine("=== SYNTHESIS TASK ===")
        appendLine("1. PATTERN DETECTION: Which OLQs show consistent strength or weakness across stories?")
        appendLine("2. NARRATIVE EVOLUTION: Does storytelling quality improve or decline across the set?")
        appendLine("3. THEME DOMINANCE: Which recurring themes (positive or negative) appear in multiple stories?")
        appendLine("4. CONTRADICTION DETECTION: Flag inconsistent OLQ patterns (e.g. high Determination in 3 stories, low in 8).")
        appendLine("5. HOLISTIC SCORING: Synthesise a final score for all 15 OLQs that reflects the full picture.")
        appendLine()
        appendLine("=== SCORING RULES ===")
        appendLine("- Weight later stories slightly more (recency effect).")
        appendLine("- Consistent patterns are more reliable than single-story outliers.")
        appendLine("- A single exceptional story cannot compensate for a generally poor performance.")
        appendLine("- Use SSB scale: 5=Very Good (best), 6=Good, 7=Average, 8=Poor, 9=Fail.")
        appendLine()
        appendLine("=== ALL 15 OLQs (mandatory) ===")
        appendLine(
            "EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION, " +
                "SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY, INITIATIVE, SELF_CONFIDENCE, " +
                "SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS, DETERMINATION, COURAGE, STAMINA"
        )
        appendLine()
        appendLine("=== CRITICAL INSTRUCTIONS ===")
        appendLine("1. Return ONLY a single JSON object — NO arrays, NO markdown, NO extra text.")
        appendLine("2. olqScores is a JSON OBJECT keyed by OLQ name (not an array).")
        appendLine("3. ALL 15 OLQs MUST appear as keys inside olqScores.")
        appendLine("4. Each value: { \"score\": int 5-9, \"confidence\": int 0-100, \"reasoning\": string }.")
        appendLine("5. overallConfidence (int 0-100) at the top level.")
        appendLine("6. Response MUST start with { and end with }.")
        appendLine()
        appendLine("=== EXACT FORMAT (copy this structure) ===")
        appendLine(
            "{\"olqScores\":{\"EFFECTIVE_INTELLIGENCE\":{\"score\":7,\"confidence\":80,\"reasoning\":\"...\"}," +
                "\"REASONING_ABILITY\":{\"score\":6,\"confidence\":75,\"reasoning\":\"...\"}," +
                "\"... all 15 OLQs ...\"},\"overallConfidence\":78}"
        )
    }

    private fun compactOlqScores(olqScoresJson: String): String {
        return try {
            val arr = JSONArray(olqScoresJson)
            (0 until arr.length()).joinToString(", ") { i ->
                val obj = arr.getJSONObject(i)
                "${obj.getString("olq")}:${obj.getInt("score")}"
            }
        } catch (e: Exception) {
            olqScoresJson.take(100)
        }
    }
}
