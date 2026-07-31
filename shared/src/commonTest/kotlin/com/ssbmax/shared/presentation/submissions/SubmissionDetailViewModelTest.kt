@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.submissions

import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.submission.ObserveSubmissionUseCase
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization test, written before converting [SubmissionDetailViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Since this is NOT
 * `androidx.lifecycle.ViewModel` pre-conversion, the Android original's
 * `SavedStateHandle`-sourced `submissionId` constructor param is already
 * dropped in favor of `loadSubmission(submissionId)` called from the screen's
 * `LaunchedEffect` -- this test pins that call contract plus the AI/instructor
 * score parsing from the raw map.
 */
class SubmissionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: FakeSubmissionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = FakeSubmissionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SubmissionDetailViewModel(ObserveSubmissionUseCase(submissionRepository))

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        submissionRepository.observeSubmissionFlow = MutableStateFlow(null)
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Submission not found", viewModel.uiState.value.error)
    }

    @Test
    fun `loads submission and parses AI and instructor scores`() = runTest(testDispatcher) {
        submissionRepository.observeSubmissionFlow = MutableStateFlow(
            mapOf(
                "testType" to "TAT", "status" to "GRADED", "submittedAt" to 5000L,
                "data" to mapOf(
                    "aiPreliminaryScore" to mapOf("overallScore" to 70, "feedback" to "Good structure"),
                    "instructorScore" to mapOf("overallScore" to 80, "feedback" to "Well done", "gradedByInstructorName" to "Maj. Rao")
                )
            )
        )
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestType.TAT, state.testType)
        assertEquals(SubmissionStatus.GRADED, state.status)
        assertEquals(70f, state.aiScore?.overallScore)
        assertEquals(80f, state.instructorScore?.overallScore)
        assertEquals(80f, state.finalScore)
    }

    @Test
    fun `retry reloads the last requested submissionId`() = runTest(testDispatcher) {
        submissionRepository.observeSubmissionFlow = MutableStateFlow(mapOf("testType" to "WAT", "status" to "DRAFT", "submittedAt" to 1L))
        val viewModel = buildViewModel()
        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("sub-1", viewModel.uiState.value.submissionId)
    }
}
