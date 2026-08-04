package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQCategory
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trips the fixed-schema interview DTOs ([InterviewSessionDto]/[InterviewResponseDto]/
 * [InterviewResultDto]) added for the `GitLiveInterviewRepository` port, matching the style of
 * every other `*DtoTest` in this file's package (e.g. [GTODtoTest], [SubmissionClusterDtoTest]):
 * one round-trip per DTO, plus the unrecognized-enum-name fallback behavior each DTO's
 * `toDomain()` relies on (Firestore documents can carry stale/renamed enum values written by an
 * older app version).
 */
class InterviewFirestoreDtosTest {

    @Test
    fun `InterviewSessionDto round-trips an in-progress session`() {
        val session = InterviewSession(
            id = "session-1",
            userId = "user-1",
            mode = InterviewMode.VOICE_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            completedAt = null,
            piqSnapshotId = "piq-1",
            consentGiven = true,
            questionIds = listOf("q1", "q2"),
            currentQuestionIndex = 1,
            estimatedDuration = 25
        )

        val dto = session.toDto()
        assertEquals("session-1", dto.id)
        assertEquals("IN_PROGRESS", dto.status)
        assertNull(dto.completedAt)

        val roundTripped = dto.toDomain()
        assertEquals(session, roundTripped)
    }

    @Test
    fun `InterviewSessionDto round-trips a completed session with a completion timestamp`() {
        val session = InterviewSession(
            id = "session-2",
            userId = "user-2",
            mode = InterviewMode.VOICE_BASED,
            status = InterviewStatus.COMPLETED,
            startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            completedAt = Instant.fromEpochMilliseconds(1_700_001_000_000L),
            piqSnapshotId = "piq-2",
            consentGiven = false,
            questionIds = listOf("q1"),
            currentQuestionIndex = 1,
            estimatedDuration = 30
        )

        val roundTripped = session.toDto().toDomain()
        assertEquals(session, roundTripped)
    }

    @Test
    fun `InterviewResponseDto carries userId as a side-channel field not present on the domain model`() {
        val response = InterviewResponse(
            id = "resp-1",
            sessionId = "session-1",
            questionId = "q1",
            responseText = "My answer",
            responseMode = InterviewMode.VOICE_BASED,
            respondedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            thinkingTimeSec = 12,
            audioUrl = null,
            olqScores = mapOf(OLQ.COURAGE to OLQScore(score = 4, confidence = 70, reasoning = "Solid")),
            confidenceScore = 65
        )

        val dto = response.toDto(userId = "user-1")
        assertEquals("user-1", dto.userId)

        val roundTripped = dto.toDomain()
        assertEquals(response, roundTripped)
    }

    @Test
    fun `InterviewResponseDto toDomain drops OLQ score entries with an unrecognized enum name`() {
        val dto = InterviewResponseDto(
            id = "resp-2",
            sessionId = "session-1",
            questionId = "q1",
            userId = "user-1",
            responseText = "Answer",
            responseMode = "VOICE_BASED",
            respondedAt = 1_700_000_000_000L,
            olqScores = mapOf(
                "COURAGE" to OLQScoreDto(score = 5, confidence = 80, reasoning = "ok"),
                "RETIRED_QUALITY_NO_LONGER_SCORED" to OLQScoreDto(score = 9, confidence = 10, reasoning = "stale")
            )
        )

        val response = dto.toDomain()
        assertEquals(setOf(OLQ.COURAGE), response.olqScores.keys)
    }

    @Test
    fun `InterviewResultDto round-trips full aggregation output`() {
        val result = InterviewResult(
            id = "result-1",
            sessionId = "session-1",
            userId = "user-1",
            mode = InterviewMode.VOICE_BASED,
            completedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            durationSec = 900L,
            totalQuestions = 10,
            totalResponses = 8,
            overallOLQScores = mapOf(
                OLQ.COURAGE to OLQScore(score = 4, confidence = 70, reasoning = "Aggregated"),
                OLQ.INITIATIVE to OLQScore(score = 6, confidence = 60, reasoning = "Aggregated")
            ),
            categoryScores = mapOf(OLQCategory.CHARACTER to 4f, OLQCategory.DYNAMIC to 6f),
            overallConfidence = 65,
            strengths = listOf(OLQ.COURAGE),
            weaknesses = listOf(OLQ.INITIATIVE),
            feedback = "Good performance overall.",
            overallRating = 5
        )

        val roundTripped = result.toDto().toDomain()
        assertEquals(result, roundTripped)
    }

    @Test
    fun `InterviewResultDto toDomain drops category and strength OLQ entries with unrecognized names`() {
        val dto = InterviewResultDto(
            id = "result-2",
            sessionId = "session-1",
            userId = "user-1",
            mode = "VOICE_BASED",
            completedAt = 1_700_000_000_000L,
            durationSec = 600L,
            totalQuestions = 5,
            totalResponses = 5,
            overallOLQScores = mapOf("COURAGE" to OLQScoreDto(score = 4, confidence = 70, reasoning = "ok")),
            categoryScores = mapOf("CHARACTER" to 4f, "NOT_A_REAL_CATEGORY" to 9f),
            strengths = listOf("COURAGE", "BOGUS_OLQ"),
            weaknesses = emptyList(),
            feedback = "Feedback",
            overallRating = 5
        )

        val result = dto.toDomain()
        assertEquals(setOf(OLQCategory.CHARACTER), result.categoryScores.keys)
        assertEquals(listOf(OLQ.COURAGE), result.strengths)
    }

    @Test
    fun `InterviewProgressSubmissionDto default-constructs with the documented COMPLETED IO shape`() {
        val dto = InterviewProgressSubmissionDto(
            id = "interview_result-1",
            userId = "user-1",
            testId = "session-1",
            submittedAt = 1_700_000_000_000L,
            score = 60f,
            resultId = "result-1",
            mode = "VOICE_BASED"
        )

        assertEquals("IO", dto.testType)
        assertEquals("COMPLETED", dto.status)
        assertTrue(dto.score in 0f..100f)
    }
}
