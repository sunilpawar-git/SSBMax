@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.srtresult

import com.ssbmax.shared.domain.model.SRTSubmission
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
 * Characterization test, written before converting [SRTSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the "GTO pattern": observe
 * -> on COMPLETED fetch result and stop observing.
 *
 * [FakeSubmissionRepository] leaves `observeSRTSubmission`/`getSRTResult`
 * `unused()`, so this file wraps it with [SRTObservingRepository] to stub
 * just those two SRT-specific methods without touching the shared fake.
 */
class SRTSubmissionResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SRTObservingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = SRTObservingRepository(FakeSubmissionRepository())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission(status: AnalysisStatus) = SRTSubmission(
        userId = "u-1", testId = "srt_standard", responses = emptyList(),
        totalTimeTakenMinutes = 20, submittedAt = 0L, analysisStatus = status, olqResult = null
    )

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(null)
        val viewModel = SRTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Submission not found", viewModel.uiState.value.error)
    }

    @Test
    fun `pending analysis surfaces submission without fetching result`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.PENDING_ANALYSIS))
        val viewModel = SRTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.runCurrent()

        assertEquals(AnalysisStatus.PENDING_ANALYSIS, viewModel.uiState.value.analysisStatus)
    }

    @Test
    fun `completed analysis fetches OLQ result and derives a recommendation`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        val olq = OLQAnalysisResult(
            submissionId = "sub-1", testType = com.ssbmax.shared.domain.model.TestType.SRT,
            olqScores = mapOf(OLQ.SPEED_OF_DECISION to OLQScore(score = 5, confidence = 65, reasoning = "prompt")),
            overallScore = 6f, overallRating = "Average", strengths = emptyList(), weaknesses = emptyList(),
            recommendations = emptyList(), analyzedAt = 0L, aiConfidence = 65
        )
        repository.resultResult = Result.success(olq)
        val viewModel = SRTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(olq, state.submission?.olqResult)
        assertNotNull(state.ssbRecommendation)
    }
}

/** Delegates every other member to [base]; stubs only the SRT-specific observe/result pair. */
private class SRTObservingRepository(private val base: SubmissionRepository) : SubmissionRepository by base {
    var observeFlow: Flow<SRTSubmission?> = flowOf(null)
    var resultResult: Result<OLQAnalysisResult?> = Result.success(null)

    override fun observeSRTSubmission(submissionId: String) = observeFlow
    override suspend fun getSRTResult(submissionId: String) = resultResult
}
