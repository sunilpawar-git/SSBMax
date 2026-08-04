package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Verifies the submission-status -> TestStatus mapping GitLiveTestProgressRepository
 * depends on. Why it matters: this is the seam the Android original's own `when`
 * fallback protected against malformed/legacy status strings — a regression here
 * would silently show "not attempted" for tests a candidate actually completed.
 */
class TestProgressDtoTest {

    @Test
    fun `known status strings map to their TestStatus`() {
        assertEquals(TestStatus.SUBMITTED_PENDING_REVIEW, submissionStatusToTestStatus("SUBMITTED_PENDING_REVIEW"))
        assertEquals(TestStatus.GRADED, submissionStatusToTestStatus("GRADED"))
        assertEquals(TestStatus.COMPLETED, submissionStatusToTestStatus("COMPLETED"))
    }

    @Test
    fun `unknown or legacy status strings default to NOT_ATTEMPTED instead of throwing`() {
        assertEquals(TestStatus.NOT_ATTEMPTED, submissionStatusToTestStatus("SOME_LEGACY_STATUS"))
        assertEquals(TestStatus.NOT_ATTEMPTED, submissionStatusToTestStatus(""))
    }

    @Test
    fun `SubmissionProgressDto defaults are safe for a malformed or partial document`() {
        val dto = SubmissionProgressDto()
        assertEquals("", dto.testType)
        assertEquals("", dto.status)
        assertEquals(0L, dto.submittedAt)
        assertFalse(dto.score != null)
    }
}
