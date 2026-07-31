@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.ppdtresult

import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Characterization test, written before converting [PPDTSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1 of the KMP-convergence plan).
 * Pins the "GTO pattern" behaviour (observe -> on COMPLETED fetch result and
 * stop observing) so the conversion is provably behaviour-preserving.
 *
 * [FakeSubmissionRepository] leaves `observePPDTSubmission`/`getPPDTResult`
 * `unused()` (they're not needed by any other Phase 1 ViewModel), so this
 * file wraps it with [PPDTObservingRepository] to stub just those two
 * PPDT-specific methods without touching the shared fake.
 */
class PPDTSubmissionResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var base: FakeSubmissionRepository
    private lateinit var repository: PPDTObservingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        base = FakeSubmissionRepository()
        repository = PPDTObservingRepository(base)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission(status: AnalysisStatus) = PPDTSubmission(
        submissionId = "sub-1", questionId = "q-1", userId = "u-1", userName = "Test", userEmail = "",
        batchId = null, story = "story", charactersCount = 5, viewingTimeTakenSeconds = 30,
        writingTimeTakenMinutes = 4, submittedAt = 0L, status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
        instructorReview = null, analysisStatus = status, olqResult = null
    )

    private fun buildViewModel() = PPDTSubmissionResultViewModel(repository, NoOpLogger())

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(null)
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `pending analysis surfaces submission without fetching result`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.PENDING_ANALYSIS))
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.submission)
        assertEquals(AnalysisStatus.PENDING_ANALYSIS, state.analysisStatus)
    }

    @Test
    fun `completed analysis fetches OLQ result and derives a recommendation`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        val olq = OLQAnalysisResult(
            submissionId = "sub-1", testType = TestType.PPDT,
            olqScores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(score = 4, confidence = 80, reasoning = "clear reasoning")),
            overallScore = 7f, overallRating = "Above Average", strengths = listOf("clarity"),
            weaknesses = emptyList(), recommendations = emptyList(), analyzedAt = 0L, aiConfidence = 80
        )
        repository.resultResult = Result.success(olq)
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(olq, state.submission?.olqResult)
        assertNotNull(state.ssbRecommendation)
    }

    @Test
    fun `result fetch failure is logged without surfacing a fatal error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        repository.resultResult = Result.failure(Exception("not ready"))
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNotNull(state.submission)
    }
}

/** Delegates every other member to [base]; stubs only the PPDT-specific observe/result pair. */
private class PPDTObservingRepository(private val base: SubmissionRepository) : SubmissionRepository by base {
    var observeFlow: Flow<PPDTSubmission?> = kotlinx.coroutines.flow.flowOf(null)
    var resultResult: Result<OLQAnalysisResult?> = Result.success(null)

    override fun observePPDTSubmission(submissionId: String) = observeFlow
    override suspend fun getPPDTResult(submissionId: String) = resultResult
}
