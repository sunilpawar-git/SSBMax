@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.interviewresult

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeInterviewRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Characterization test for [InterviewResultViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current one-shot fetch behaviour (no live listener, see the ViewModel's own
 * doc comment for why -- unlike PPDT/TAT/etc's result screens).
 */
class InterviewResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var interviewRepository: FakeInterviewRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        interviewRepository = FakeInterviewRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun result() = InterviewResult(
        id = "result-1",
        sessionId = "session-1",
        userId = testUser().id,
        mode = InterviewMode.VOICE_BASED,
        completedAt = Instant.fromEpochMilliseconds(1_000),
        durationSec = 900,
        totalQuestions = 5,
        totalResponses = 5,
        overallOLQScores = emptyMap(),
        categoryScores = emptyMap(),
        overallConfidence = 75,
        strengths = emptyList(),
        weaknesses = emptyList(),
        feedback = "Solid performance overall.",
        overallRating = 5
    )

    private fun buildViewModel() = InterviewResultViewModel(
        interviewRepository = interviewRepository,
        logger = NoOpLogger()
    )

    @Test
    fun `loadResult populates state on success`() = runTest(testDispatcher) {
        interviewRepository.getResultByIdResult = Result.success(result())
        val viewModel = buildViewModel()

        viewModel.loadResult("result-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.result)
        assertEquals(5, state.result?.overallRating)
        // No OLQ scores in this fixture -> no SSB recommendation is computed (see ViewModel's own branch).
        assertNull(state.ssbRecommendation)
        assertNull(state.error)
    }

    @Test
    fun `loadResult surfaces an error when the repository call fails`() = runTest(testDispatcher) {
        interviewRepository.getResultByIdResult = Result.failure(Exception("network down"))
        val viewModel = buildViewModel()

        viewModel.loadResult("result-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Failed to load results", state.error)
        assertNull(state.result)
    }

    @Test
    fun `refresh reloads the previously requested result`() = runTest(testDispatcher) {
        interviewRepository.getResultByIdResult = Result.success(result())
        val viewModel = buildViewModel()
        viewModel.loadResult("result-1")
        testDispatcher.scheduler.advanceUntilIdle()

        interviewRepository.getResultByIdResult = Result.success(result().copy(overallRating = 3))
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.result?.overallRating)
    }
}
