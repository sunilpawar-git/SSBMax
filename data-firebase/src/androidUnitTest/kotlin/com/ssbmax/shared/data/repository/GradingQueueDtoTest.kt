package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.GradingPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies the priority-escalation rule and nested AI/instructor score DTOs
 * GitLiveGradingQueueRepository depends on. Why it matters: the Android
 * original reads aiPreliminaryScore/instructorScore out of a nested "data"
 * map via raw key lookups (data["data"]["aiPreliminaryScore"]["overallScore"])
 * — a flattened DTO here would silently decode nothing and every submission
 * would look like it has no AI suggestions.
 */
class GradingQueueDtoTest {

    @Test
    fun `priority escalates with hours waiting using the same thresholds as Android`() {
        assertEquals(GradingPriority.LOW, gradingPriorityForHoursWaiting(0))
        assertEquals(GradingPriority.LOW, gradingPriorityForHoursWaiting(24))
        assertEquals(GradingPriority.NORMAL, gradingPriorityForHoursWaiting(25))
        assertEquals(GradingPriority.NORMAL, gradingPriorityForHoursWaiting(48))
        assertEquals(GradingPriority.HIGH, gradingPriorityForHoursWaiting(49))
        assertEquals(GradingPriority.HIGH, gradingPriorityForHoursWaiting(72))
        assertEquals(GradingPriority.URGENT, gradingPriorityForHoursWaiting(73))
    }

    @Test
    fun `nested aiPreliminaryScore overallScore is reachable through the data DTO`() {
        val dto = GradingQueueSubmissionDto(
            id = "sub-1",
            userId = "user-1",
            testType = "PPDT",
            status = "SUBMITTED_PENDING_REVIEW",
            submittedAt = 1_700_000_000_000L,
            data = SubmissionScoresDto(aiPreliminaryScore = ScoreDto(overallScore = 78.5f))
        )
        assertEquals(78.5f, dto.data?.aiPreliminaryScore?.overallScore)
        assertNull(dto.data?.instructorScore)
    }

    @Test
    fun `missing data map defaults to null nested scores instead of throwing`() {
        val dto = GradingQueueSubmissionDto(id = "sub-2", userId = "user-2", testType = "OIR", status = "GRADED")
        assertNull(dto.data)
    }
}
