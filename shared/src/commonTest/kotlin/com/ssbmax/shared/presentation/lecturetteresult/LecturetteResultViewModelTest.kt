@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.lecturetteresult

import com.ssbmax.shared.domain.model.gto.GTOResult
import com.ssbmax.shared.domain.model.gto.GTOSubmission
import com.ssbmax.shared.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.shared.domain.model.gto.GTOTestType
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeGTORepository
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertNull

/**
 * Characterization test, written before converting [LecturetteResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1c). Structurally identical to
 * `GDResultViewModelTest` (see that class's doc comment).
 */
class LecturetteResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var gtoRepository: FakeGTORepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        gtoRepository = FakeGTORepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = LecturetteResultViewModel(gtoRepository, NoOpLogger())

    private fun submission(status: GTOSubmissionStatus) = GTOSubmission.LecturetteSubmission(
        id = "sub-1", userId = "user-1", testId = "gto_lecturette_standard",
        topicChoices = listOf("A", "B"), selectedTopic = "A", speechTranscript = "transcript", charCount = 10,
        timeSpent = 120, status = status
    )

    @Test
    fun `submission not found surfaces an error`() = runTest(testDispatcher) {
        gtoRepository.observeSubmissionFlow = flowOf(null)
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `pending submission does not fetch the result yet`() = runTest(testDispatcher) {
        gtoRepository.observeSubmissionFlow = flowOf(submission(GTOSubmissionStatus.PENDING_ANALYSIS))
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.submission)
        assertNull(state.result)
    }

    @Test
    fun `completed submission fetches and surfaces the result`() = runTest(testDispatcher) {
        gtoRepository.observeSubmissionFlow = flowOf(submission(GTOSubmissionStatus.COMPLETED))
        gtoRepository.getTestResultResult = Result.success(
            GTOResult(
                submissionId = "sub-1", userId = "user-1", testType = GTOTestType.LECTURETTE,
                olqScores = emptyMap(), overallScore = 6f, overallRating = "Good", aiConfidence = 75
            )
        )
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.result)
        assertEquals("Good", state.result?.overallRating)
    }
}
