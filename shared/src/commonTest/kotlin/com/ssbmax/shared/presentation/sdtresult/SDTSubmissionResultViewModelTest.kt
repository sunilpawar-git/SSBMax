@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.sdtresult

import com.ssbmax.shared.domain.model.SDTSubmission
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import com.ssbmax.shared.presentation.testing.RecordingLogger
import com.ssbmax.shared.presentation.testing.sequentialFlow
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
 * Characterization test, written before converting [SDTSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the "GTO pattern": observe
 * -> on COMPLETED fetch result and stop observing.
 *
 * [FakeSubmissionRepository] leaves `observeSDTSubmission`/`getSDTResult`
 * `unused()`, so this file wraps it with [SDTObservingRepository] to stub
 * just those two SDT-specific methods without touching the shared fake.
 */
class SDTSubmissionResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SDTObservingRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = SDTObservingRepository(FakeSubmissionRepository())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun submission(status: AnalysisStatus) = SDTSubmission(
        userId = "u-1", testId = "sdt_standard", responses = emptyList(),
        totalTimeTakenMinutes = 12, submittedAt = 0L, analysisStatus = status, olqResult = null
    )

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(null)
        val viewModel = SDTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Submission not found", viewModel.uiState.value.error)
    }

    @Test
    fun `pending analysis surfaces submission without fetching result`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.PENDING_ANALYSIS))
        val viewModel = SDTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.runCurrent()

        assertEquals(AnalysisStatus.PENDING_ANALYSIS, viewModel.uiState.value.analysisStatus)
    }

    @Test
    fun `completed analysis fetches OLQ result and derives a recommendation`() = runTest(testDispatcher) {
        repository.observeFlow = MutableStateFlow(submission(AnalysisStatus.COMPLETED))
        val olq = OLQAnalysisResult(
            submissionId = "sub-1", testType = com.ssbmax.shared.domain.model.TestType.SD,
            olqScores = mapOf(OLQ.DETERMINATION to OLQScore(score = 5, confidence = 60, reasoning = "steady")),
            overallScore = 6f, overallRating = "Average", strengths = emptyList(), weaknesses = emptyList(),
            recommendations = emptyList(), analyzedAt = 0L, aiConfidence = 60
        )
        repository.resultResult = Result.success(olq)
        val viewModel = SDTSubmissionResultViewModel(repository, NoOpLogger())

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(olq, state.submission?.olqResult)
        assertNotNull(state.ssbRecommendation)
    }

    @Test
    fun `regression from COMPLETED to an earlier status is logged rather than blocked`() = runTest(testDispatcher) {
        repository.observeFlow = sequentialFlow(
            submission(AnalysisStatus.COMPLETED),
            submission(AnalysisStatus.PENDING_ANALYSIS)
        )
        val logger = RecordingLogger()
        val viewModel = SDTSubmissionResultViewModel(repository, logger)

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(logger.entries.any { it.level == "w" && it.message.contains("regressed") })
        assertEquals(AnalysisStatus.PENDING_ANALYSIS, viewModel.uiState.value.analysisStatus)
    }
}

/** Delegates every other member to [base]; stubs only the SDT-specific observe/result pair. */
private class SDTObservingRepository(private val base: SubmissionRepository) : SubmissionRepository by base {
    var observeFlow: Flow<SDTSubmission?> = flowOf(null)
    var resultResult: Result<OLQAnalysisResult?> = Result.success(null)

    override fun observeSDTSubmission(submissionId: String) = observeFlow
    override suspend fun getSDTResult(submissionId: String) = resultResult
}
