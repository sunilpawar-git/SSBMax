package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.GradingStatus
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestResponse
import com.ssbmax.shared.domain.model.TestSubmission
import com.ssbmax.shared.domain.model.TestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TestSubmissionDto]'s round-trip mapping, mirroring the Android original's
 * `toMap()`/`toTestSubmission()` behavior — including its documented simplifications, since a
 * KMP port must reproduce them, not silently improve on them mid-slice.
 */
class TestSubmissionDtoTest {

    private fun sampleSubmission() = TestSubmission(
        id = "sub-1",
        testId = "test-1",
        userId = "user-1",
        testType = TestType.OIR,
        phase = TestPhase.PHASE_1,
        submittedAt = 1_000L,
        responses = listOf(
            TestResponse.MultipleChoice(questionId = "q1", timestamp = 500L, selectedOption = 2, isCorrect = true)
        ),
        aiPreliminaryScore = 7.5f,
        gradingStatus = GradingStatus.PENDING,
        timeSpent = 60_000L
    )

    @Test
    fun toDto_thenToDomain_roundTripsCoreFields() {
        val original = sampleSubmission()
        val restored = original.toDto().toDomain()

        assertEquals(original.id, restored?.id)
        assertEquals(original.testId, restored?.testId)
        assertEquals(original.userId, restored?.userId)
        assertEquals(original.testType, restored?.testType)
        assertEquals(original.phase, restored?.phase)
        assertEquals(original.submittedAt, restored?.submittedAt)
        assertEquals(original.aiPreliminaryScore, restored?.aiPreliminaryScore)
    }

    @Test
    fun toDomain_dropsResponseSubtypeDetail_sameAsAndroidOriginal() {
        // Documented simplification: the DTO only ever persists questionId/timestamp/type, so a
        // MultipleChoice response with selectedOption=2 comes back as selectedOption=0.
        val restored = sampleSubmission().toDto().toDomain()
        val response = restored?.responses?.single() as TestResponse.MultipleChoice
        assertEquals(0, response.selectedOption)
        assertNull(response.isCorrect)
    }

    @Test
    fun toDomain_returnsNull_whenTestTypeUnparseable() {
        val dto = sampleSubmission().toDto().copy(testType = "NOT_A_REAL_TYPE")
        assertNull(dto.toDomain())
    }

    @Test
    fun toDomain_defaultsToPending_whenGradingStatusUnparseable() {
        val dto = sampleSubmission().toDto().copy(gradingStatus = "NOT_A_REAL_STATUS")
        val restored = dto.toDomain()
        assertTrue(restored != null)
        assertEquals(GradingStatus.PENDING, restored?.gradingStatus)
    }
}
