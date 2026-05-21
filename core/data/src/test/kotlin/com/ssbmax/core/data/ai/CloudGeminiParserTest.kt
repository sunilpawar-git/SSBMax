package com.ssbmax.core.data.ai

import android.util.Log
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudGeminiParserTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @Test
    fun parseQuestionsResult_withValidData_returnsSuccess() {
        val mockData = mapOf(
            "success" to true,
            "questions" to listOf(
                mapOf(
                    "id" to "q1",
                    "questionText" to "Tell me about a time you showed leadership.",
                    "expectedOLQs" to listOf("SOCIAL_ADJUSTMENT", "INITIATIVE"),
                    "context" to "School leadership scenario"
                )
            )
        )

        val result = CloudGeminiParser.parseQuestionsResult(mockData)

        assertTrue(result.isSuccess)
        val questions = result.getOrNull()
        assertNotNull(questions)
        assertEquals(1, questions!!.size)
        val q = questions[0]
        assertEquals("q1", q.id)
        assertEquals("Tell me about a time you showed leadership.", q.questionText)
        assertEquals(listOf(OLQ.SOCIAL_ADJUSTMENT, OLQ.INITIATIVE), q.expectedOLQs)
        assertEquals("School leadership scenario", q.context)
        assertEquals(QuestionSource.AI_GENERATED, q.source)
    }

    @Test
    fun parseQuestionsResult_withMissingSuccess_returnsFailure() {
        val mockData = mapOf(
            "questions" to emptyList<Any>()
        )
        val result = CloudGeminiParser.parseQuestionsResult(mockData)
        assertTrue(result.isFailure)
    }

    @Test
    fun parseQuestionsResult_withSuccessFalse_returnsFailure() {
        val mockData = mapOf(
            "success" to false,
            "questions" to emptyList<Any>()
        )
        val result = CloudGeminiParser.parseQuestionsResult(mockData)
        assertTrue(result.isFailure)
    }

    @Test
    fun parseQuestionsResult_withMalformedData_returnsFailure() {
        val result = CloudGeminiParser.parseQuestionsResult("not a map")
        assertTrue(result.isFailure)
    }

    @Test
    fun parseAnalysisResult_withValidData_returnsSuccess() {
        val mockData = mapOf(
            "success" to true,
            "analysis" to mapOf(
                "overallConfidence" to 85,
                "keyInsights" to listOf("Insight 1", "Insight 2"),
                "suggestedFollowUp" to "Follow up question?",
                "olqScores" to listOf(
                    mapOf(
                        "olq" to "INITIATIVE",
                        "score" to 7.5,
                        "reasoning" to "Good initiative shown",
                        "evidence" to listOf("Evidence 1")
                    )
                )
            )
        )

        val result = CloudGeminiParser.parseAnalysisResult(mockData)

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()
        assertNotNull(analysis)
        assertEquals(85, analysis!!.overallConfidence)
        assertEquals(listOf("Insight 1", "Insight 2"), analysis.keyInsights)
        assertEquals("Follow up question?", analysis.suggestedFollowUp)
        
        val initiativeScore = analysis.olqScores[OLQ.INITIATIVE]
        assertNotNull(initiativeScore)
        assertEquals(OLQ.INITIATIVE, initiativeScore!!.olq)
        assertEquals(7.5f, initiativeScore.score)
        assertEquals("Good initiative shown", initiativeScore.reasoning)
        assertEquals(listOf("Evidence 1"), initiativeScore.evidence)
    }

    @Test
    fun parseAnalysisResult_withMissingAnalysis_returnsFailure() {
        val mockData = mapOf(
            "success" to true
        )
        val result = CloudGeminiParser.parseAnalysisResult(mockData)
        assertTrue(result.isFailure)
    }

    @Test
    fun generateMockQuestions_returnsCorrectCount() {
        val questions = CloudGeminiParser.generateMockQuestions(3)
        assertEquals(3, questions.size)
        questions.forEach { q ->
            assertTrue(q.id.isNotEmpty())
            assertTrue(q.questionText.startsWith("Mock question"))
            assertEquals(QuestionSource.GENERIC_POOL, q.source)
        }
    }
}
