package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SRTCategory
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the shared OLQ DTO and the per-test-type submission DTOs ported for the
 * submission-repository cluster slice. Each case protects a real behavior called out in the
 * ported classes' doc comments — not a trivial getter test.
 */
class SubmissionClusterDtoTest {

    @Test
    fun `OLQAnalysisResultDto round-trips through toDto and toDomain`() {
        val domain = com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult(
            submissionId = "sub-1",
            testType = TestType.TAT,
            olqScores = mapOf(
                com.ssbmax.shared.domain.model.interview.OLQ.EFFECTIVE_INTELLIGENCE to
                    com.ssbmax.shared.domain.model.interview.OLQScore(score = 8, confidence = 90, reasoning = "Strong")
            ),
            overallScore = 7.5f,
            overallRating = "Good",
            strengths = listOf("Clarity"),
            weaknesses = listOf("Pacing"),
            recommendations = listOf("Practice more"),
            analyzedAt = 1_700_000_000_000L,
            aiConfidence = 85
        )

        val dto = domain.toDto(userId = "user-1")
        assertEquals("user-1", dto.userId)
        assertEquals("TAT", dto.testType)

        val roundTripped = dto.toDomain()
        assertEquals(domain.submissionId, roundTripped?.submissionId)
        assertEquals(domain.testType, roundTripped?.testType)
        assertEquals(8, roundTripped?.olqScores?.get(com.ssbmax.shared.domain.model.interview.OLQ.EFFECTIVE_INTELLIGENCE)?.score)
    }

    @Test
    fun `OLQAnalysisResultDto with unparseable testType returns null instead of throwing`() {
        val dto = OLQAnalysisResultDto(submissionId = "sub-2", testType = "NOT_A_REAL_TYPE")
        assertNull(dto.toDomain())
    }

    @Test
    fun `OLQAnalysisResultDto drops unparseable OLQ keys instead of throwing`() {
        val dto = OLQAnalysisResultDto(
            submissionId = "sub-3",
            testType = "WAT",
            olqScores = mapOf(
                "EFFECTIVE_INTELLIGENCE" to OLQScoreDto(score = 6, confidence = 50, reasoning = ""),
                "NOT_A_REAL_OLQ" to OLQScoreDto(score = 1, confidence = 1, reasoning = "")
            )
        )
        val domain = dto.toDomain()
        assertEquals(1, domain?.olqScores?.size)
    }

    @Test
    fun `TATDataDto with unparseable status falls back to SUBMITTED_PENDING_REVIEW`() {
        val dto = TATDataDto(id = "s1", userId = "u1", testId = "t1", status = "GARBAGE")
        assertEquals(SubmissionStatus.SUBMITTED_PENDING_REVIEW, dto.toDomain().status)
    }

    @Test
    fun `SRTInstructorScoreDto drops unparseable category keys same defensive behavior as Android`() {
        val dto = SRTInstructorScoreDto(
            gradedByInstructorId = "i1",
            gradedByInstructorName = "Instructor",
            gradedAt = 0L,
            categoryWiseComments = mapOf(
                "LEADERSHIP" to "Good call under pressure",
                "NOT_A_CATEGORY" to "should be dropped"
            )
        )
        val domain = dto.toDomain()
        assertEquals(1, domain.categoryWiseComments.size)
        assertEquals("Good call under pressure", domain.categoryWiseComments[SRTCategory.LEADERSHIP])
    }

    @Test
    fun `OIR answeredQuestions are never round-tripped same real limitation as the Android original`() {
        val dto = OIRDataDto(
            id = "oir-1",
            userId = "u1",
            testId = "t1",
            testResult = OIRSubmissionTestResultDto(totalQuestions = 10, correctAnswers = 7),
            status = "SUBMITTED_PENDING_REVIEW"
        )
        val domain = dto.toDomain()
        assertTrue(domain.testResult.answeredQuestions.isEmpty())
        assertEquals(7, domain.testResult.correctAnswers)
    }

    @Test
    fun `PPDT data DTO defaults analysisStatus to PENDING_ANALYSIS when never written`() {
        val dto = PPDTDataDto(submissionId = "p1", questionId = "q1", userId = "u1", status = "SUBMITTED_PENDING_REVIEW")
        assertEquals(com.ssbmax.shared.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS, dto.toDomain().analysisStatus)
        assertNull(dto.toDomain().olqResult)
    }

    @Test
    fun `PIQDataDto round-trips every top-level and nested section field`() {
        val domain = com.ssbmax.shared.domain.model.PIQSubmission(
            id = "piq-1",
            userId = "u1",
            fullName = "Cadet Test",
            dateOfBirth = "2000-01-01",
            siblings = listOf(com.ssbmax.shared.domain.model.Sibling(id = "sib-1", name = "Sibling One")),
            education10th = com.ssbmax.shared.domain.model.Education(level = "10th", institution = "School A", percentage = "92"),
            workExperience = listOf(com.ssbmax.shared.domain.model.WorkExperience(id = "we-1", company = "Acme")),
            nccTraining = com.ssbmax.shared.domain.model.NCCTraining(hasTraining = true, wing = "Army"),
            previousInterviews = listOf(com.ssbmax.shared.domain.model.PreviousInterview(id = "pi-1", ssbNumber = "SSB-1")),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW
        )

        val dto = domain.toDataDto()
        val roundTripped = dto.toDomain()

        assertEquals(domain.id, roundTripped.id)
        assertEquals(domain.fullName, roundTripped.fullName)
        assertEquals(1, roundTripped.siblings.size)
        assertEquals("Sibling One", roundTripped.siblings.first().name)
        assertEquals("School A", roundTripped.education10th.institution)
        assertEquals("Acme", roundTripped.workExperience.first().company)
        assertTrue(roundTripped.nccTraining.hasTraining)
        assertEquals("SSB-1", roundTripped.previousInterviews.first().ssbNumber)
        assertEquals(SubmissionStatus.SUBMITTED_PENDING_REVIEW, roundTripped.status)
    }

    @Test
    fun `PIQDataDto with unparseable status falls back to SUBMITTED_PENDING_REVIEW`() {
        val dto = PIQDataDto(id = "p1", userId = "u1", status = "GARBAGE")
        assertEquals(SubmissionStatus.SUBMITTED_PENDING_REVIEW, dto.toDomain().status)
    }

    @Test
    fun `PIQ aiPreliminaryScore is written but never round-tripped same real limitation as the Android original`() {
        val domain = com.ssbmax.shared.domain.model.PIQSubmission(
            id = "piq-2",
            userId = "u1",
            aiPreliminaryScore = com.ssbmax.shared.domain.model.PIQAIScore(
                overallScore = 80f,
                personalInfoScore = 20f,
                familyInfoScore = 20f,
                motivationScore = 20f,
                selfAssessmentScore = 20f,
                feedback = "Good",
                strengths = listOf("Clarity"),
                areasForImprovement = emptyList(),
                completenessPercentage = 100,
                clarityScore = 9f,
                consistencyScore = 9f
            )
        )

        val dto = domain.toDataDto()
        assertEquals(80f, dto.aiPreliminaryScore?.overallScore)

        val roundTripped = dto.toDomain()
        assertNull(roundTripped.aiPreliminaryScore)
    }

    @Test
    fun `GTO submission envelope keeps GTOSubmissionStatus in the shared status field same quirk as Android`() {
        val doc = SubmissionDocDto(
            id = "gd-1",
            userId = "u1",
            testId = "t1",
            testType = TestType.GTO_GD.name,
            status = "PENDING_ANALYSIS", // GTOSubmissionStatus name, not SubmissionStatus
            data = GDSubmissionDataDto(topic = "Leadership", response = "My answer")
        )
        assertEquals("PENDING_ANALYSIS", doc.status)
        assertEquals("Leadership", doc.data.topic)
    }
}
