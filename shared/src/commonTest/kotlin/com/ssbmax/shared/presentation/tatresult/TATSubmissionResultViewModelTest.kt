@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.tatresult

import com.ssbmax.shared.domain.model.TATSubmission
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
import kotlin.test.assertTrue

/**
 * Characterization test, written before converting [TATSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the "GTO pattern" (observe
 * -> on COMPLETED fetch result and stop observing) plus the TAT-specific
 * `usesPartialAssessment` derived property.
 *
 * [FakeSubmissionRepository] leaves `observeTATSubmission`/`getTATResult`
 * `unused()`, so this file wraps it with [TATObservingRepository] to stub
 * just those two TAT-specific methods without touching the shared fake.
 */
class TATSubmissionResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TATObservingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = TATObservingRepository(FakeSubmissionRepository())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission(status: AnalysisStatus) = TATSubmission(
        userId = "u-1", testId = "tat_standard", stories = emptyList(),
        totalTimeTakenMinutes = 10, submittedAt = 0L, analysisStatus = status, olqResult = null
    )

    private fun olqResult(usedPartial: Boolean = false) = OLQAnalysisResult(
        submissionId = "sub-1", testType = TestType.TAT,
        olqScores = mapOf(OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(score = 5, confidence = 70, reasoning = "ok")),
        overallScore = 6f, overallRating = "Average", strengths = emptyList(), weaknesses = emptyList(),
        recommendations = emptyList(), analyzedAt = 0L, aiConfidence = 70, usedPartialAssessment = usedPartial
    )

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(null)
        val viewModel = TATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Submission not found", viewModel.uiState.value.error)
    }

    @Test
    fun `pending analysis surfaces submission without fetching result`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.PENDING_ANALYSIS))
        val viewModel = TATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.submission)
        assertEquals(AnalysisStatus.PENDING_ANALYSIS, state.analysisStatus)
    }

    @Test
    fun `completed analysis fetches OLQ result and exposes usesPartialAssessment`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        repository.resultResult = Result.success(olqResult(usedPartial = true))
        val viewModel = TATSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.submission?.olqResult)
        assertTrue(state.usesPartialAssessment)
        assertNotNull(state.ssbRecommendation)
    }
}

/** Delegates every other member to [base]; stubs only the TAT-specific observe/result pair. */
private class TATObservingRepository(private val base: SubmissionRepository) : SubmissionRepository by base {
    var observeFlow: Flow<TATSubmission?> = flowOf(null)
    var resultResult: Result<OLQAnalysisResult?> = Result.success(null)

    override fun observeTATSubmission(submissionId: String) = observeFlow
    override suspend fun getTATResult(submissionId: String) = resultResult
}
