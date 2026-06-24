package com.ssbmax.ui.tests.tat

import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.model.TATStoryResponse
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TATSubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: TATSubmissionResultViewModel
    private val submissionRepository = mockk<SubmissionRepository>(relaxed = true)

    private val submissionId = "submission-tat-123"
    private val baseOlqResult = OLQAnalysisResult(
        submissionId = submissionId,
        testType = TestType.TAT,
        olqScores = OLQ.entries.associateWith { olq ->
            OLQScore(
                score = 6,
                confidence = 85,
                reasoning = "Consistent evidence for ${olq.displayName}"
            )
        },
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = listOf("Leadership", "Courage"),
        weaknesses = listOf("Decision Speed"),
        recommendations = listOf("Practice more scenarios"),
        analyzedAt = 1_717_171_717L,
        aiConfidence = 85
    )

    @Before
    fun setUp() {
        viewModel = TATSubmissionResultViewModel(submissionRepository)
    }

    @Test
    fun `initial state is loading`() {
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission success updates state with submission data`() = runTest {
        val submission = buildSubmission(
            analysisStatus = AnalysisStatus.COMPLETED,
            olqResult = baseOlqResult
        )
        every { submissionRepository.observeSubmission(submissionId) } returns
            flowOf(createFirestoreSubmissionMap(submission, includeOlqResult = true))

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(submissionId, state.submission?.id)
        assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
        assertEquals(baseOlqResult.overallScore, state.submission?.olqResult?.overallScore)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        every { submissionRepository.observeSubmission(submissionId) } returns flowOf(null)

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.submission)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `exposes partial assessment state when result is degraded`() = runTest {
        val degradedResult = baseOlqResult.copy(
            validStoriesCount = 9,
            failedStoriesCount = 3,
            usedPartialAssessment = true
        )
        every { submissionRepository.observeSubmission(submissionId) } returns
            flowOf(
                createFirestoreSubmissionMap(
                    buildSubmission(analysisStatus = AnalysisStatus.COMPLETED, olqResult = degradedResult),
                    includeOlqResult = true
                )
            )

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.usesPartialAssessment)
    }

    @Test
    fun `does not expose warning state for full valid analysis`() = runTest {
        every { submissionRepository.observeSubmission(submissionId) } returns
            flowOf(
                createFirestoreSubmissionMap(
                    buildSubmission(analysisStatus = AnalysisStatus.COMPLETED, olqResult = baseOlqResult),
                    includeOlqResult = true
                )
            )

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.usesPartialAssessment)
    }

    @Test
    fun `fetches result separately when status is completed but snapshot has no result`() = runTest {
        every { submissionRepository.observeSubmission(submissionId) } returns
            flowOf(createFirestoreSubmissionMap(buildSubmission(analysisStatus = AnalysisStatus.COMPLETED), includeOlqResult = false))
        coEvery { submissionRepository.getTATResult(submissionId) } returns Result.success(baseOlqResult)

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
        assertNotNull(state.submission?.olqResult)
        assertEquals(baseOlqResult.overallRating, state.submission?.olqResult?.overallRating)
        coVerify(exactly = 1) { submissionRepository.getTATResult(submissionId) }
    }

    @Test
    fun `does not regress when analysis status is pending or analyzing`() = runTest {
        val completedWithoutEmbeddedResult = createFirestoreSubmissionMap(
            buildSubmission(analysisStatus = AnalysisStatus.COMPLETED),
            includeOlqResult = false
        )
        val pendingWithoutResult = createFirestoreSubmissionMap(
            buildSubmission(analysisStatus = AnalysisStatus.PENDING_ANALYSIS),
            includeOlqResult = false
        )
        val analyzingWithoutResult = createFirestoreSubmissionMap(
            buildSubmission(analysisStatus = AnalysisStatus.ANALYZING),
            includeOlqResult = false
        )
        every { submissionRepository.observeSubmission(submissionId) } returns flow {
            emit(completedWithoutEmbeddedResult)
            emit(pendingWithoutResult)
            emit(analyzingWithoutResult)
        }
        coEvery { submissionRepository.getTATResult(submissionId) } returns Result.success(baseOlqResult)

        viewModel.loadSubmission(submissionId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalysisStatus.COMPLETED, state.submission?.analysisStatus)
        assertNotNull(state.submission?.olqResult)
        assertEquals(baseOlqResult.overallScore, state.submission?.olqResult?.overallScore)
        coVerify(exactly = 1) { submissionRepository.getTATResult(submissionId) }
    }

    private fun buildSubmission(
        analysisStatus: AnalysisStatus,
        olqResult: OLQAnalysisResult? = null
    ) = TATSubmission(
        id = submissionId,
        userId = "user-123",
        testId = "tat-standard",
        stories = listOf(
            TATStoryResponse(
                questionId = "tat_001",
                story = "A cadet organizes a rescue operation.",
                charactersCount = 40,
                viewingTimeTakenSeconds = 30,
                writingTimeTakenSeconds = 180,
                submittedAt = 1_717_171_000L
            )
        ),
        totalTimeTakenMinutes = 48,
        submittedAt = 1_717_171_100L,
        analysisStatus = analysisStatus,
        olqResult = olqResult
    )

    private fun createFirestoreSubmissionMap(
        submission: TATSubmission,
        includeOlqResult: Boolean
    ): Map<String, Any> {
        val submissionData = mutableMapOf<String, Any>(
            "id" to submission.id,
            "userId" to submission.userId,
            "testId" to submission.testId,
            "stories" to submission.stories.map {
                mapOf(
                    "questionId" to it.questionId,
                    "story" to it.story,
                    "charactersCount" to it.charactersCount,
                    "viewingTimeTakenSeconds" to it.viewingTimeTakenSeconds,
                    "writingTimeTakenSeconds" to it.writingTimeTakenSeconds,
                    "submittedAt" to it.submittedAt
                )
            },
            "totalTimeTakenMinutes" to submission.totalTimeTakenMinutes,
            "submittedAt" to submission.submittedAt,
            "analysisStatus" to submission.analysisStatus.name
        )

        if (includeOlqResult) {
            val olqResult = submission.olqResult ?: baseOlqResult
            submissionData["olqResult"] = mapOf(
                "submissionId" to olqResult.submissionId,
                "testType" to olqResult.testType.name,
                "olqScores" to olqResult.olqScores.entries.associate { (olq, score) ->
                    olq.name to mapOf(
                        "score" to score.score,
                        "confidence" to score.confidence,
                        "reasoning" to score.reasoning
                    )
                },
                "overallScore" to olqResult.overallScore,
                "overallRating" to olqResult.overallRating,
                "strengths" to olqResult.strengths,
                "weaknesses" to olqResult.weaknesses,
                "recommendations" to olqResult.recommendations,
                "analyzedAt" to olqResult.analyzedAt,
                "aiConfidence" to olqResult.aiConfidence,
                "validStoriesCount" to olqResult.validStoriesCount,
                "failedStoriesCount" to olqResult.failedStoriesCount,
                "usedPartialAssessment" to olqResult.usedPartialAssessment
            )
        }

        return mapOf("data" to submissionData)
    }
}
