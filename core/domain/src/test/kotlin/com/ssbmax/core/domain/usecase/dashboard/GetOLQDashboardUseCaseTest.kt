package com.ssbmax.core.domain.usecase.dashboard

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.gto.GTOResult
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.util.NoOpLogger
import java.time.Instant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetOLQDashboardUseCase
 * Verifies data fetching logic and aggregations, including the Split Collection Strategy wiring.
 */
class GetOLQDashboardUseCaseTest {

    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var gtoRepository: GTORepository
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var getOLQDashboardUseCase: GetOLQDashboardUseCase

    @Before
    fun setup() {
        submissionRepository = mockk(relaxed = true)
        gtoRepository = mockk(relaxed = true)
        interviewRepository = mockk(relaxed = true)
        
        // Use NoOpLogger for testing (domain layer is now platform-independent)
        getOLQDashboardUseCase = GetOLQDashboardUseCase(
            submissionRepository,
            gtoRepository,
            interviewRepository,
            NoOpLogger()
        )
    }

    @Test
    fun `invoke calls getTATResult with correct submission ID`() = runTest {
        // Given
        val userId = "user_123"
        val tatSubmissionId = "tat_sub_456"
        val tatSubmission = mockk<TATSubmission> {
            every { id } returns tatSubmissionId
            every { analysisStatus } returns AnalysisStatus.COMPLETED
        }
        val tatResult = createOLQResult(
            submissionId = tatSubmissionId,
            testType = TestType.TAT,
            scores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to 4f)
        )

        // Mock: Latest submission return to get ID
        coEvery { submissionRepository.getLatestTATSubmission(userId) } returns Result.success(tatSubmission)
        // Mock: Result fetch using that ID
        coEvery { submissionRepository.getTATResult(tatSubmissionId) } returns Result.success(tatResult)
        
        // Mock others to return empty/null to isolate test
        coEvery { submissionRepository.getLatestWATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestOIRSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestPPDTSubmission(any()) } returns Result.success(null)
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())
        every { interviewRepository.getUserResults(any()) } returns flowOf(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        
        // Verify TAT result is present
        assertNotNull(data?.dashboard?.phase2Results?.tatResult)
        assertEquals(tatSubmissionId, data?.dashboard?.phase2Results?.tatResult?.submissionId)
        assertEquals(4f, data?.averageOLQScores?.get(OLQ.EFFECTIVE_INTELLIGENCE))
    }

    @Test
    fun `invoke calls getLatestPPDTSubmission and getPPDTResult with correct IDs`() = runTest {
        // Given
        val userId = "user_123"
        val ppdtSubmissionId = "ppdt_sub_789"
        val ppdtSubmission = mockk<PPDTSubmission> {
            every { submissionId } returns ppdtSubmissionId
            // PPDT usually stores analysisStatus in data map, but repo handles it.
            // Mocking the object itself is enough here.
        }
        val ppdtResult = createOLQResult(
            submissionId = ppdtSubmissionId,
            testType = TestType.PPDT,
            scores = mapOf(OLQ.INITIATIVE to 5f)
        )

        // Mock: Latest submission return
        coEvery { submissionRepository.getLatestPPDTSubmission(userId) } returns Result.success(ppdtSubmission)
        // Mock: Result fetch using that ID
        coEvery { submissionRepository.getPPDTResult(ppdtSubmissionId) } returns Result.success(ppdtResult)

        // Mock others empty
        coEvery { submissionRepository.getLatestTATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestWATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestOIRSubmission(any()) } returns Result.success(null)
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())
        every { interviewRepository.getUserResults(any()) } returns flowOf(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        
        // Verify PPDT result
        assertNotNull(data?.dashboard?.phase1Results?.ppdtOLQResult)
        assertEquals(ppdtSubmissionId, data?.dashboard?.phase1Results?.ppdtOLQResult?.submissionId)
        assertEquals(5f, data?.averageOLQScores?.get(OLQ.INITIATIVE))
    }
    
    @Test
    fun `invoke calls gtoRepository and aggregates GTO scores correctly`() = runTest {
        // Given
        val userId = "user_123"
        val gdResult = GTOResult(
            submissionId = "gd_1",
            userId = userId,
            testType = GTOTestType.GROUP_DISCUSSION,
            olqScores = mapOf(
                OLQ.SOCIAL_ADJUSTMENT to OLQScore(4, 80, ""),
                OLQ.COOPERATION to OLQScore(5, 80, "")
            ),
            overallScore = 4.5f,
            overallRating = "Average",
            strengths = emptyList(),
            weaknesses = emptyList(),
            recommendations = emptyList(),
            analyzedAt = System.currentTimeMillis(),
            aiConfidence = 80
        )
        
        // Mock GTO repo to return this result for GD
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())
        coEvery { gtoRepository.getUserResults(userId, GTOTestType.GROUP_DISCUSSION) } returns Result.success(listOf(gdResult))
        
        // Mock others empty
        coEvery { submissionRepository.getLatestTATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestWATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestOIRSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestPPDTSubmission(any()) } returns Result.success(null)
        every { interviewRepository.getUserResults(any()) } returns flowOf(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        
        // Verify GTO result
        val gdDashboardResult = data?.dashboard?.phase2Results?.gtoResults?.get(GTOTestType.GROUP_DISCUSSION)
        assertNotNull(gdDashboardResult)
        assertEquals("gd_1", gdDashboardResult?.submissionId)
        // Avg Social Adjustment: 4.0
        assertEquals(4.0f, data?.averageOLQScores?.get(OLQ.SOCIAL_ADJUSTMENT))
    }

    @Test
    fun `computeAverageOLQScores calculates correct averages`() = runTest {
         // Given
        val userId = "user_123"
        val tatResult = createOLQResult(
            submissionId = "tat_1",
            testType = TestType.TAT,
            scores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to 3f, OLQ.REASONING_ABILITY to 4f)
        )
        val watResult = createOLQResult(
            submissionId = "wat_1",
            testType = TestType.WAT,
            scores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to 5f, OLQ.REASONING_ABILITY to 6f)
        )
        
        // Mock TAT
        val tatSub = mockk<TATSubmission> { every { id } returns "tat_1" }
        coEvery { submissionRepository.getLatestTATSubmission(userId) } returns Result.success(tatSub)
        coEvery { submissionRepository.getTATResult("tat_1") } returns Result.success(tatResult)
        
        // Mock WAT
        val watSub = mockk<WATSubmission> { every { id } returns "wat_1" }
        coEvery { submissionRepository.getLatestWATSubmission(userId) } returns Result.success(watSub)
        coEvery { submissionRepository.getWATResult("wat_1") } returns Result.success(watResult)
        
        // Mock others empty
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestOIRSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestPPDTSubmission(any()) } returns Result.success(null)
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())
        every { interviewRepository.getUserResults(any()) } returns flowOf(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)
        val averages = result.getOrNull()?.averageOLQScores

        // Then
        assertNotNull(averages)
        // Avg Effective Intelligence: (3 + 5) / 2 = 4.0
        assertEquals(4.0f, averages?.get(OLQ.EFFECTIVE_INTELLIGENCE) ?: 0f, 0.01f)
        // Avg Reasoning Ability: (4 + 6) / 2 = 5.0
        assertEquals(5.0f, averages?.get(OLQ.REASONING_ABILITY) ?: 0f, 0.01f)
    }

    @Test
    fun `invoke calls interviewRepository and aggregates Interview scores correctly`() = runTest {
        // Given
        val userId = "user_123"
        val interviewResult = InterviewResult(
            id = "res_1",
            sessionId = "session_1",
            userId = userId,
            mode = InterviewMode.VOICE_BASED,
            completedAt = Instant.now(),
            durationSec = 1800,
            totalQuestions = 10,
            totalResponses = 10,
            overallOLQScores = mapOf(
                OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(6, 80, "Good") 
            ), 
            categoryScores = mapOf(OLQCategory.INTELLECTUAL to 6f),
            overallConfidence = 80,
            strengths = emptyList(),
            weaknesses = emptyList(),
            feedback = "Good",
            overallRating = 6
        )
        
        // Mock Interview repo
        // Returns Flow<List<InterviewResult>>
        every { interviewRepository.getUserResults(userId) } returns flowOf(listOf(interviewResult))
        
        // Mock others empty
        coEvery { submissionRepository.getLatestTATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestWATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestOIRSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestPPDTSubmission(any()) } returns Result.success(null)
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        
        // Verify Interview result
        assertNotNull(data?.dashboard?.phase2Results?.interviewResult)
        assertEquals("session_1", data?.dashboard?.phase2Results?.interviewResult?.sessionId)
        // Avg for that OLQ
        assertEquals(6.0f, data?.averageOLQScores?.get(OLQ.EFFECTIVE_INTELLIGENCE))
    }

    @Test
    fun `invoke reconciles OIR sessionId with submission document ID`() = runTest {
        // Given
        val userId = "user_123"
        val submissionId = "oir_doc_ID"
        val sessionIdInResult = "internal_session_ID" // Mismatched ID in old records
        
        val oirResult = OIRTestResult(
            testId = "test_1",
            sessionId = sessionIdInResult,
            userId = userId,
            totalQuestions = 10,
            correctAnswers = 5,
            incorrectAnswers = 5,
            skippedQuestions = 0,
            totalTimeSeconds = 600,
            timeTakenSeconds = 300,
            rawScore = 5,
            percentageScore = 50f,
            categoryScores = emptyMap(),
            difficultyBreakdown = emptyMap(),
            answeredQuestions = emptyList(),
            completedAt = System.currentTimeMillis()
        )
        
        val oirSubmission = OIRSubmission(
            id = submissionId,
            userId = userId,
            testId = "test_1",
            testResult = oirResult,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW
        )

        // Mock: Return submission with mismatched internal ID
        coEvery { submissionRepository.getLatestOIRSubmission(userId) } returns Result.success(oirSubmission)
        
        // Mock others empty
        coEvery { submissionRepository.getLatestTATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestWATSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSRTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestSDTSubmission(any()) } returns Result.success(null)
        coEvery { submissionRepository.getLatestPPDTSubmission(any()) } returns Result.success(null)
        coEvery { gtoRepository.getUserResults(any(), any()) } returns Result.success(emptyList())
        every { interviewRepository.getUserResults(any()) } returns flowOf(emptyList())

        // When
        val result = getOLQDashboardUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        
        // CRITICAL VERIFICATION: The sessionId in the processed data MUST match the submission document ID
        // even if it was different in the raw data (regression prevention)
        val finalOirResult = data?.dashboard?.phase1Results?.oirResult
        assertNotNull(finalOirResult)
        assertEquals("The sessionId must be reconciled with the document ID for navigation consistency", 
            submissionId, finalOirResult?.sessionId)
    }

    // Helper to create test OLQ results
    private fun createOLQResult(
        submissionId: String,
        testType: TestType,
        scores: Map<OLQ, Float>
    ): OLQAnalysisResult {
        return OLQAnalysisResult(
            submissionId = submissionId,
            testType = testType,
            olqScores = scores.mapValues { (_, score) ->
                OLQScore(score = score.toInt(), confidence = 80, reasoning = "Test")
            },
            overallScore = scores.values.average().toFloat(),
            overallRating = "Test",
            strengths = emptyList(),
            weaknesses = emptyList(),
            recommendations = emptyList(),
            analyzedAt = System.currentTimeMillis(),
            aiConfidence = 80
        )
    }
}
