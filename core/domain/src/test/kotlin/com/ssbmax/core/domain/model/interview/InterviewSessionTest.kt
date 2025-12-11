package com.ssbmax.core.domain.model.interview

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for InterviewSession model
 *
 * Tests:
 * - Session state transitions
 * - Duration calculation
 * - Progress tracking
 * - Consent handling
 */
class InterviewSessionTest {

    @Test
    fun `InterviewSession should initialize with IN_PROGRESS status`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertEquals(InterviewStatus.IN_PROGRESS, session.status)
        assertNull(session.completedAt)
        assertTrue(session.isActive())
    }

    @Test
    fun `InterviewSession should support TEXT_BASED and VOICE_BASED modes`() {
        val textSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        val voiceSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.VOICE_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertEquals(InterviewMode.TEXT_BASED, textSession.mode)
        assertEquals(InterviewMode.VOICE_BASED, voiceSession.mode)
    }

    @Test
    fun `InterviewSession should track current question index`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3", "q4", "q5"),
            currentQuestionIndex = 2,
            estimatedDuration = 30
        )

        assertEquals(2, session.currentQuestionIndex)
        assertEquals("q3", session.questionIds[session.currentQuestionIndex])
    }

    @Test
    fun `InterviewSession should calculate progress percentage`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10"),
            currentQuestionIndex = 5,
            estimatedDuration = 30
        )

        // Progress = (currentQuestionIndex / totalQuestions) * 100
        // At index 5, completed 5 out of 10 = 50%
        val progress = (session.currentQuestionIndex.toFloat() / session.questionIds.size * 100).toInt()
        assertEquals(50, progress)
    }

    @Test
    fun `InterviewSession should calculate duration in seconds`() {
        val startTime = Instant.now().minusSeconds(120) // 2 minutes ago
        val endTime = Instant.now()

        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.COMPLETED,
            startedAt = startTime,
            completedAt = endTime,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2"),
            currentQuestionIndex = 2,
            estimatedDuration = 30
        )

        val durationSeconds = session.getDurationSeconds()
        assertTrue(durationSeconds >= 120L) // At least 120 seconds
        assertTrue(durationSeconds <= 125L) // Allow 5 second buffer for execution time
    }

    @Test
    fun `InterviewSession should handle COMPLETED status`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.COMPLETED,
            startedAt = Instant.now().minusSeconds(600),
            completedAt = Instant.now(),
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 3,
            estimatedDuration = 30
        )

        assertEquals(InterviewStatus.COMPLETED, session.status)
        assertNotNull(session.completedAt)
        assertFalse(session.isActive())
    }

    @Test
    fun `InterviewSession should handle ABANDONED status`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.ABANDONED,
            startedAt = Instant.now().minusSeconds(300),
            completedAt = Instant.now(),
            piqSnapshotId = "piq123",
            consentGiven = false,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 1,
            estimatedDuration = 30
        )

        assertEquals(InterviewStatus.ABANDONED, session.status)
        assertNotNull(session.completedAt)
        assertFalse(session.isActive())
    }

    @Test
    fun `InterviewSession should track consent given`() {
        val consentedSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        val nonConsentedSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = false,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertTrue(consentedSession.consentGiven)
        assertFalse(nonConsentedSession.consentGiven)
    }

    @Test
    fun `InterviewSession should have PIQ snapshot reference`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq_submission_456",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertEquals("piq_submission_456", session.piqSnapshotId)
        assertNotNull(session.piqSnapshotId)
        assertTrue(session.piqSnapshotId.isNotEmpty())
    }

    @Test
    fun `InterviewSession should have estimated duration`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 0,
            estimatedDuration = 30 // 30 minutes
        )

        assertEquals(30, session.estimatedDuration)
    }

    @Test
    fun `InterviewSession should support state transition from IN_PROGRESS to COMPLETED`() {
        val inProgressSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now().minusSeconds(600),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 2,
            estimatedDuration = 30
        )

        val completedSession = inProgressSession.copy(
            status = InterviewStatus.COMPLETED,
            completedAt = Instant.now(),
            currentQuestionIndex = 3
        )

        assertEquals(InterviewStatus.IN_PROGRESS, inProgressSession.status)
        assertNull(inProgressSession.completedAt)

        assertEquals(InterviewStatus.COMPLETED, completedSession.status)
        assertNotNull(completedSession.completedAt)
        assertEquals(3, completedSession.currentQuestionIndex)
    }

    @Test
    fun `InterviewSession should support state transition from IN_PROGRESS to ABANDONED`() {
        val inProgressSession = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now().minusSeconds(300),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 1,
            estimatedDuration = 30
        )

        val abandonedSession = inProgressSession.copy(
            status = InterviewStatus.ABANDONED,
            completedAt = Instant.now()
        )

        assertEquals(InterviewStatus.IN_PROGRESS, inProgressSession.status)
        assertEquals(InterviewStatus.ABANDONED, abandonedSession.status)
        assertNotNull(abandonedSession.completedAt)
    }

    @Test
    fun `InterviewSession should calculate remaining questions`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10"),
            currentQuestionIndex = 3,
            estimatedDuration = 30
        )

        val remaining = session.questionIds.size - session.currentQuestionIndex
        assertEquals(7, remaining)
    }

    @Test
    fun `InterviewSession isActive should return true for IN_PROGRESS`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null,
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertTrue(session.isActive())
    }

    @Test
    fun `InterviewSession isActive should return false for COMPLETED`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.COMPLETED,
            startedAt = Instant.now().minusSeconds(600),
            completedAt = Instant.now(),
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 1,
            estimatedDuration = 30
        )

        assertFalse(session.isActive())
    }

    @Test
    fun `InterviewSession isActive should return false for ABANDONED`() {
        val session = InterviewSession(
            id = UUID.randomUUID().toString(),
            userId = "user123",
            mode = InterviewMode.TEXT_BASED,
            status = InterviewStatus.ABANDONED,
            startedAt = Instant.now().minusSeconds(300),
            completedAt = Instant.now(),
            piqSnapshotId = "piq123",
            consentGiven = true,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            estimatedDuration = 30
        )

        assertFalse(session.isActive())
    }
}
