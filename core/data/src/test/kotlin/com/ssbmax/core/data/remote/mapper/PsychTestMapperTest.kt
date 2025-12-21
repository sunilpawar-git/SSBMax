package com.ssbmax.core.data.remote.mapper

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.interview.OLQ
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PsychTestMapper
 */
class PsychTestMapperTest {

    @Test
    fun `parseTATSubmission maps fields correctly`() {
        // Given
        val submissionData = mapOf(
            "id" to "tat_123",
            "userId" to "user_1",
            "testId" to "test_1",
            "status" to "ANALYZING",
            "submittedAt" to 1000L,
            "analysisStatus" to "ANALYZING",
            "stories" to list(
                mapOf(
                    "questionId" to "q1",
                    "story" to "A brave leader...",
                    "charactersCount" to 100
                )
            )
        )

        // When
        val result = PsychTestMapper.parseTATSubmission(submissionData)

        // Then
        assertEquals("tat_123", result.id)
        assertEquals("user_1", result.userId)
        assertEquals(SubmissionStatus.SUBMITTED_PENDING_REVIEW, result.status) // Fallback due to enum name handling or default
        assertEquals(AnalysisStatus.ANALYZING, result.analysisStatus)
        assertEquals(1, result.stories.size)
        assertEquals("A brave leader...", result.stories[0].story)
    }

    @Test
    fun `parseOLQResult maps scores correctly`() {
        // Given
        val olqData = mapOf(
            "submissionId" to "sub_1",
            "testType" to "TAT",
            "overallScore" to 4.5,
            "olqScores" to mapOf(
                "EFFECTIVE_INTELLIGENCE" to mapOf("score" to 4, "confidence" to 80),
                "INITIATIVE" to mapOf("score" to 5, "confidence" to 90)
            )
        )

        // When
        val result = PsychTestMapper.parseOLQResult(olqData)

        // Then
        assertNotNull(result)
        assertEquals("sub_1", result?.submissionId)
        assertEquals(TestType.TAT, result?.testType)
        assertEquals(4.5f, result?.overallScore)
        assertEquals(2, result?.olqScores?.size)
        assertEquals(4, result?.olqScores?.get(OLQ.EFFECTIVE_INTELLIGENCE)?.score)
    }
    
    @Test
    fun `parseWATSubmission maps fields correctly`() {
        val data = mapOf(
            "id" to "wat_1",
            "userId" to "u1",
            "responses" to list(
                mapOf("word" to "Lead", "response" to "Leadership is key")
            )
        )
        val result = PsychTestMapper.parseWATSubmission(data)
        assertEquals("wat_1", result.id)
        assertEquals(1, result.responses.size)
        assertEquals("Lead", result.responses[0].word)
    }

    @Test
    fun `parseSRTSubmission maps fields correctly`() {
        val data = mapOf(
            "id" to "srt_1",
            "userId" to "u1",
            "responses" to list(
                mapOf("situation" to "Fire...", "response" to "Call 911")
            )
        )
        val result = PsychTestMapper.parseSRTSubmission(data)
        assertEquals("srt_1", result.id)
        assertEquals(1, result.responses.size)
        assertEquals("Call 911", result.responses[0].response)
    }
    
    @Test
    fun `parseSDTSubmission maps fields correctly`() {
        val data = mapOf(
            "id" to "sdt_1",
            "userId" to "u1",
            "responses" to list(
                mapOf("question" to "Parents think...", "answer" to "They are proud")
            )
        )
        val result = PsychTestMapper.parseSDTSubmission(data)
        assertEquals("sdt_1", result.id)
        assertEquals(1, result.responses.size)
        assertEquals("They are proud", result.responses[0].answer)
    }

    // Helper for Java-like List creation if needed, though Kotlin needs listOf
    private fun <T> list(vararg items: T): List<T> = listOf(*items)
}
