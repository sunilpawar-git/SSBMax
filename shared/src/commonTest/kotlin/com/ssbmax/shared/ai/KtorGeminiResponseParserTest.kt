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
}
