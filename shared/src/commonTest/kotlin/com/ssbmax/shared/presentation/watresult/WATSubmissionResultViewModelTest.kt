@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.watresult

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.WATSubmission
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Characterization test, written before converting [WATSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the "GTO pattern": observe
 * -> on COMPLETED fetch result and stop observing.
 *
 * [FakeSubmissionRepository] leaves `observeWATSubmission`/`getWATResult`
 * `unused()`, so this file wraps it with [WATObservingRepository].
 */
class WATSubmissionResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: WATObservingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = WATObservingRepository(FakeSubmissionRepository())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission(status: AnalysisStatus) = WATSubmission(
        userId = "u-1", testId = "wat_standard", responses = emptyList(),
        totalTimeTakenMinutes = 5, submittedAt = 0L, analysisStatus = status, olqResult = null
    )

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(null)
        val viewModel = WATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Submission not found", viewModel.uiState.value.error)
    }

    @Test
    fun `pending analysis surfaces submission without fetching result`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.PENDING_ANALYSIS))
        val viewModel = WATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.runCurrent()

        assertEquals(AnalysisStatus.PENDING_ANALYSIS, viewModel.uiState.value.analysisStatus)
    }

    @Test
    fun `completed analysis fetches OLQ result and derives a recommendation`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        val olq = OLQAnalysisResult(
            submissionId = "sub-1", testType = TestType.WAT,
            olqScores = mapOf(OLQ.SELF_CONFIDENCE to OLQScore(score = 6, confidence = 75, reasoning = "steady")),
            overallScore = 6f, overallRating = "Average", strengths = emptyList(), weaknesses = emptyList(),
            recommendations = emptyList(), analyzedAt = 0L, aiConfidence = 75
        )
        repository.resultResult = Result.success(olq)
        val viewModel = WATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(olq, state.submission?.olqResult)
        assertNotNull(state.ssbRecommendation)
    }
}

/** Delegates every other member to [base]; stubs only the WAT-specific observe/result pair. */
private class WATObservingRepository(private val base: SubmissionRepository) : SubmissionRepository by base {
    var observeFlow: Flow<WATSubmission?> = flowOf(null)
    var resultResult: Result<OLQAnalysisResult?> = Result.success(null)

    override fun observeWATSubmission(submissionId: String) = observeFlow
    override suspend fun getWATResult(submissionId: String) = resultResult
}
