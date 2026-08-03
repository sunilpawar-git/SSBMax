package com.ssbmax.shared.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the kotlinx.serialization-based parser against the same response
 * shapes GeminiResponseParser.parseAnalysisResponse (org.json, Android-only)
 * handles, since this is a genuine rewrite, not a mechanical copy -- a parser
 * bug here would silently drop OLQ scores for every interview analysis call.
 */
class KtorGeminiResponseParserTest {

    @Test
    fun `parses a plain JSON object response`() {
        val text = """
            {
              "olqScores": [
                {"olq": "EFFECTIVE_INTELLIGENCE", "score": 5.0, "reasoning": "Solid reasoning", "evidence": ["clear plan"]}
              ],
              "overallConfidence": 80,
              "keyInsights": ["Shows initiative"],
              "suggestedFollowUp": "Tell me more"
            }
        """.trimIndent()

        val result = KtorGeminiResponseParser.parseAnalysisResponse(text).getOrThrow()

        assertEquals(80, result.overallConfidence)
        assertEquals(listOf("Shows initiative"), result.keyInsights)
        assertEquals("Tell me more", result.suggestedFollowUp)
        assertEquals(1, result.olqScores.size)
    }

    @Test
    fun `strips a markdown json fence before parsing`() {
        val text = """
            Here is the analysis:
            ```json
            {"olqScores": [{"olq": "COURAGE", "score": 4.0, "reasoning": "Steady under pressure"}], "overallConfidence": 60, "keyInsights": []}
            ```
        """.trimIndent()

        val result = KtorGeminiResponseParser.parseAnalysisResponse(text).getOrThrow()
        assertEquals(60, result.overallConfidence)
    }

    @Test
    fun `unrecognized OLQ names are dropped not fatal`() {
        val text = """
            {"olqScores": [
                {"olq": "NOT_A_REAL_OLQ", "score": 5.0, "reasoning": "n/a"},
                {"olq": "COURAGE", "score": 6.0, "reasoning": "Took initiative"}
            ], "overallConfidence": 50, "keyInsights": []}
        """.trimIndent()

        val result = KtorGeminiResponseParser.parseAnalysisResponse(text).getOrThrow()
        assertEquals(1, result.olqScores.size)
    }

    @Test
    fun `empty olqScores after filtering fails rather than returning an invalid ResponseAnalysis`() {
        val text = """{"olqScores": [{"olq": "NOT_REAL", "score": 1.0}], "overallConfidence": 50}"""
        val result = KtorGeminiResponseParser.parseAnalysisResponse(text)
        assertTrue(result.isFailure)
    }

    @Test
    fun `malformed json fails cleanly instead of throwing out of the Result boundary`() {
        val result = KtorGeminiResponseParser.parseAnalysisResponse("not json at all { [")
        assertTrue(result.isFailure)
    }

    // ---- parseGTOAnalysisResponse: ported from core:data's GeminiResponseParserTest
    // (Phase 9.0, when this became the only parser). These three guard behaviours the
    // commonTest suite never asserted directly before the port.

    @Test
    fun `parseGTOAnalysisResponse handles the flat array shape Gemini sometimes returns`() {
        // WHY: Gemini emits `[{olq,score,confidence,reasoning}]` instead of the canonical
        // olqScores object often enough that a TAT synthesis run once failed on it. The
        // array branch must stay wired, not just exist.
        val text = """
            [
              {"olq": "COURAGE", "score": 7.0, "confidence": 90, "reasoning": "Held the line"},
              {"olq": "INFLUENCE_GROUP", "score": 5.0, "confidence": 70, "reasoning": "Directed peers"}
            ]
        """.trimIndent()

        val result = KtorGeminiResponseParser.parseGTOAnalysisResponse(text).getOrThrow()

        assertEquals(2, result.olqScores.size)
        assertEquals(80, result.overallConfidence)
    }

    @Test
    fun `parseGTOAnalysisResponse surfaces notRecommended when the model sets it`() {
        // WHY: notRecommended is the R14 hard-stop signal downstream validation acts on.
        // Dropping it silently turns a "not recommended" grade into an ordinary one.
        val text = """
            {"olqScores": {"COURAGE": {"score": 8.5, "confidence": 90, "reasoning": "n/a"}}, "notRecommended": true}
        """.trimIndent()

        val result = KtorGeminiResponseParser.parseGTOAnalysisResponse(text).getOrThrow()

        assertTrue(result.notRecommended)
    }

    @Test
    fun `parseGTOAnalysisResponse defaults notRecommended to false when the field is absent`() {
        // WHY: Most responses omit the field entirely; absence must mean "recommended",
        // never an exception or a true default.
        val text = """{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 70, "reasoning": "n/a"}}}"""

        val result = KtorGeminiResponseParser.parseGTOAnalysisResponse(text).getOrThrow()

        assertEquals(false, result.notRecommended)
    }
}
