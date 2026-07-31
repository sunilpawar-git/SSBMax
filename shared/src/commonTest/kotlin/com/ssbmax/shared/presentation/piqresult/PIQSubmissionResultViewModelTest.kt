@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.piqresult

import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import kotlinx.coroutines.Dispatchers
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
 * Characterization test, written before converting [PIQSubmissionResultViewModel]
 * to `androidx.lifecycle.ViewModel` (Phase 1). Pins the ID-based fetch +
 * hand-rolled map parsing behaviour (see [parsePIQSubmission]'s own doc
 * comment for why it doesn't delegate to the shared data layer's mapper).
 */
class PIQSubmissionResultViewModelTest {

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

    private fun buildViewModel() = PIQSubmissionResultViewModel(submissionRepository, NoOpLogger())

    @Test
    fun `submission not found surfaces error`() = runTest(testDispatcher) {
        submissionRepository.getSubmissionResult = Result.success(null)
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.submission)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `successful fetch parses the submission`() = runTest(testDispatcher) {
        submissionRepository.getSubmissionResult = Result.success(
            mapOf("id" to "sub-1", "userId" to "u-1", "fullName" to "Cadet Singh")
        )
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.submission)
        assertEquals("Cadet Singh", state.submission?.fullName)
        assertEquals(null, state.error)
    }

    @Test
    fun `repository failure surfaces the error message`() = runTest(testDispatcher) {
        submissionRepository.getSubmissionResult = Result.failure(Exception("network down"))
        val viewModel = buildViewModel()

        viewModel.loadSubmission("sub-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("network down", state.error)
    }
}
