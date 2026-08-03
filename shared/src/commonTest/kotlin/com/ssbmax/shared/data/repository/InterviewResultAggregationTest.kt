package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the OLQ-aggregation/feedback-generation math ported from the Android original's
 * `FirestoreInterviewRepository.completeInterview`/`generateFeedback` into
 * [aggregateInterviewResult]/[generateInterviewFeedback], matching the rationale the
 * `averageOlqScores`/`rankOlqsAscending` tests used earlier in this phase for
 * `UnifiedResultRepositoryImpl`: prove the math, not just the shape.
 */
class InterviewResultAggregationTest {

    private fun session(questionIds: List<String>) = InterviewSession(
        id = "session-1",
        userId = "user-1",
        mode = InterviewMode.VOICE_BASED,
        status = InterviewStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        completedAt = Instant.fromEpochMilliseconds(1_700_000_900_000L),
        piqSnapshotId = "piq-1",
        consentGiven = true,
        questionIds = questionIds
    )

    private fun response(id: String, questionId: String, scores: Map<OLQ, OLQScore>, confidence: Int) =
        InterviewResponse(
            id = id,
            sessionId = "session-1",
            questionId = questionId,
            responseText = "answer",
            responseMode = InterviewMode.VOICE_BASED,
            respondedAt = Instant.fromEpochMilliseconds(1_700_000_500_000L),
            thinkingTimeSec = 10,
            olqScores = scores,
            confidenceScore = confidence
        )

    @Test
    fun `aggregateInterviewResult averages OLQ scores across responses that share a quality`() {
        val responses = listOf(
            response("r1", "q1", mapOf(OLQ.COURAGE to OLQScore(score = 2, confidence = 80, reasoning = "great")), 80),
            response("r2", "q2", mapOf(OLQ.COURAGE to OLQScore(score = 4, confidence = 60, reasoning = "good")), 60)
        )

        val result = aggregateInterviewResult(session(listOf("q1", "q2")), responses)

        // (2 + 4) / 2 = 3
        assertEquals(3, result.overallOLQScores.getValue(OLQ.COURAGE).score)
        assertEquals(70, result.overallOLQScores.getValue(OLQ.COURAGE).confidence)
        assertEquals(2, result.totalResponses.let { responses.size })
        assertEquals(70, result.overallConfidence)
    }

    @Test
    fun `aggregateInterviewResult picks the 3 lowest scores as strengths since SSB scale is lower-is-better`() {
        val scores = mapOf(
            OLQ.COURAGE to OLQScore(score = 2, confidence = 80, reasoning = "r"),
            OLQ.INITIATIVE to OLQScore(score = 9, confidence = 80, reasoning = "r"),
            OLQ.COOPERATION to OLQScore(score = 3, confidence = 80, reasoning = "r"),
            OLQ.LIVELINESS to OLQScore(score = 8, confidence = 80, reasoning = "r"),
            OLQ.SENSE_OF_RESPONSIBILITY to OLQScore(score = 1, confidence = 80, reasoning = "r")
        )
        val result = aggregateInterviewResult(session(listOf("q1")), listOf(response("r1", "q1", scores, 80)))

        assertEquals(
            setOf(OLQ.SENSE_OF_RESPONSIBILITY, OLQ.COURAGE, OLQ.COOPERATION),
            result.strengths.toSet()
        )
        assertEquals(setOf(OLQ.INITIATIVE, OLQ.LIVELINESS), result.weaknesses.toSet().intersect(setOf(OLQ.INITIATIVE, OLQ.LIVELINESS)))
    }

    @Test
    fun `generateInterviewFeedback picks the excellent-tier message only at rating 5 or below`() {
        val feedback = generateInterviewFeedback(strengths = listOf(OLQ.COURAGE), weaknesses = listOf(OLQ.INITIATIVE), rating = 5)
        assertTrue(feedback.startsWith("Excellent performance!"))
    }

    @Test
    fun `generateInterviewFeedback picks the good-tier message between 6 and 7`() {
        val feedback = generateInterviewFeedback(strengths = listOf(OLQ.COURAGE), weaknesses = listOf(OLQ.INITIATIVE), rating = 7)
        assertTrue(feedback.startsWith("Good performance."))
    }

    @Test
    fun `generateInterviewFeedback picks the needs-improvement message above 7`() {
        val feedback = generateInterviewFeedback(strengths = listOf(OLQ.COURAGE), weaknesses = listOf(OLQ.INITIATIVE), rating = 9)
        assertTrue(feedback.startsWith("Focus on developing:"))
    }
}
