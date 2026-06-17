package com.ssbmax.core.data.ai

import android.util.Log
import com.ssbmax.core.domain.model.interview.OLQ
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiResponseParserTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    // ───────────── array format (June 2026 regression guard) ─────────────

    @Test
    fun `parseGTOAnalysisResponse handles flat array format from Gemini`() {
        // WHY: TATSynthesisWorker exhausted all 3 retries and returned FAILURE because Gemini
        // returned [{olq, score, confidence, reasoning}] instead of {"olqScores":{...}}.
        // This test encodes the exact failure shape so any future refactor catches the regression.
        val arrayJson = buildString {
            append("[")
            OLQ.entries.forEachIndexed { i, olq ->
                if (i > 0) append(",")
                append(
                    """{"olq":"${olq.name}","score":7,"confidence":75,"reasoning":"Test reasoning for ${olq.name}"}"""
                )
            }
            append("]")
        }

        val result = GeminiResponseParser.parseGTOAnalysisResponse(arrayJson)

        assertTrue("Array format must parse successfully", result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals("All 15 OLQs must be present", 15, analysis.olqScores.size)
        OLQ.entries.forEach { olq ->
            assertTrue("$olq must be present", olq in analysis.olqScores)
            assertEquals("Score for $olq must be 7", 7f, analysis.olqScores[olq]!!.score)
        }
        assertEquals("overallConfidence must be 75", 75, analysis.overallConfidence)
    }

    // ───────────── object format (primary path regression guard) ─────────────

    @Test
    fun `parseGTOAnalysisResponse handles canonical object format`() {
        // WHY: Canonical format is the primary path used by GTO/WAT/SRT/SD/interview analysis.
        // The array fallback must not break the object path.
        val entries = OLQ.entries.joinToString(",") { olq ->
            """"${olq.name}":{"score":6,"confidence":80,"reasoning":"Good performance"}"""
        }
        val objectJson = """{"olqScores":{$entries},"overallConfidence":80}"""

        val result = GeminiResponseParser.parseGTOAnalysisResponse(objectJson)

        assertTrue("Object format must parse successfully", result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals("All 15 OLQs must be present", 15, analysis.olqScores.size)
        assertEquals("overallConfidence must be 80", 80, analysis.overallConfidence)
        OLQ.entries.forEach { olq ->
            assertEquals("Score for $olq must be 6", 6f, analysis.olqScores[olq]!!.score)
        }
    }

    // ───────────── notRecommended field parsing (R14) ─────────────

    @Test
    fun `parseGTOAnalysisResponse with notRecommended true returns notRecommended = true`() {
        // WHY: R14 synthesis emits notRecommended:true when any OLQ >= 8;
        // parser must propagate it so ValidationIntegration can use it as a signal
        val entries = OLQ.entries.joinToString(",") { olq ->
            """"${olq.name}":{"score":8,"confidence":70,"reasoning":"Poor"}"""
        }
        val json = """{"olqScores":{$entries},"overallConfidence":70,"notRecommended":true}"""

        val result = GeminiResponseParser.parseGTOAnalysisResponse(json)

        assertTrue("Parse must succeed", result.isSuccess)
        assertTrue("notRecommended must be true when JSON field is true",
            result.getOrThrow().notRecommended)
    }

    @Test
    fun `parseGTOAnalysisResponse without notRecommended field defaults to false`() {
        // WHY: backward compat — existing JSON without notRecommended field must not break; safe default is false
        val entries = OLQ.entries.joinToString(",") { olq ->
            """"${olq.name}":{"score":6,"confidence":80,"reasoning":"Good"}"""
        }
        val json = """{"olqScores":{$entries},"overallConfidence":80}"""

        val result = GeminiResponseParser.parseGTOAnalysisResponse(json)

        assertTrue("Parse must succeed", result.isSuccess)
        assertFalse("notRecommended must default to false when field is absent",
            result.getOrThrow().notRecommended)
    }
}
