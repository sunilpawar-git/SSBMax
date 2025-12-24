package com.ssbmax.ui.tests.gpe

import com.ssbmax.core.domain.model.gto.GTOResult
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GPESubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: GPESubmissionResultViewModel
    private val mockGtoRepository = mockk<GTORepository>(relaxed = true)

    private val mockSubmission = GTOSubmission.GPESubmission(
        id = "gpe-sub-123",
        userId = "user-123",
        testId = "gpe-q1",
        imageUrl = "https://example.com/scenario.jpg",
        scenario = "Plan a rescue operation",
        solution = "Detailed solution",
        plan = "My detailed planning response",
        characterCount = 500,
        submittedAt = System.currentTimeMillis(),
        timeSpent = 1800, // 30 minutes
        status = GTOSubmissionStatus.COMPLETED,
        olqScores = emptyMap()
    )

    private val mockResult = GTOResult(
        submissionId = "gpe-sub-123",
        userId = "user-123",
        testType = com.ssbmax.core.domain.model.gto.GTOTestType.GROUP_PLANNING_EXERCISE,
        olqScores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(7, 7, "Good intelligence"),
            OLQ.ORGANIZING_ABILITY to OLQScore(8, 8, "Strong organization"),
            OLQ.REASONING_ABILITY to OLQScore(7, 7, "Solid reasoning")
        ),
        overallScore = 7.5f,
        overallRating = "Good",
        strengths = listOf("Good planning", "Clear communication"),
        weaknesses = listOf("Consider more resources"),
        recommendations = emptyList(),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 85
    )

    @Before
    fun setup() {
        coEvery { mockGtoRepository.observeSubmission(any()) } returns flowOf(mockSubmission)
        coEvery { mockGtoRepository.getTestResult(any()) } returns Result.success(mockResult)

        viewModel = GPESubmissionResultViewModel(mockGtoRepository)
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.result)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission success updates state with submission`() = runTest {
        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertEquals("gpe-sub-123", state.submission?.id)
        assertEquals("My detailed planning response", state.submission?.plan)
        assertEquals(500, state.submission?.characterCount)
        assertNull(state.error)

        coVerify { mockGtoRepository.observeSubmission("gpe-sub-123") }
    }

    @Test
    fun `loadSubmission loads result when status is COMPLETED`() = runTest {
        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNotNull(state.result)
        assertEquals(3, state.result?.olqScores?.size)
        assertEquals(7.5f, state.result?.overallScore)
        assertTrue(state.isCompleted)

        coVerify { mockGtoRepository.getTestResult("gpe-sub-123") }
    }

    @Test
    fun `loadSubmission shows error when submission not found`() = runTest {
        coEvery { mockGtoRepository.observeSubmission(any()) } returns flowOf(null)

        viewModel.loadSubmission("invalid-id")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.submission)
        assertNotNull(state.error)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `isAnalyzing returns true when status is PENDING or ANALYZING`() = runTest {
        val pendingSubmission = mockSubmission.copy(status = GTOSubmissionStatus.PENDING_ANALYSIS)
        coEvery { mockGtoRepository.observeSubmission(any()) } returns flowOf(pendingSubmission)

        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.isAnalyzing)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `isCompleted returns true when status is COMPLETED and result exists`() = runTest {
        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.isCompleted)
        assertFalse(state.isAnalyzing)
        assertFalse(state.isFailed)
    }

    @Test
    fun `isFailed returns true when status is FAILED`() = runTest {
        val failedSubmission = mockSubmission.copy(status = GTOSubmissionStatus.FAILED)
        coEvery { mockGtoRepository.observeSubmission(any()) } returns flowOf(failedSubmission)

        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.isFailed)
        assertFalse(state.isCompleted)
        assertFalse(state.isAnalyzing)
    }

    @Test
    fun `formattedTimeSpent displays correct time format`() = runTest {
        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("30m 0s", state.formattedTimeSpent)
    }

    @Test
    fun `retry reloads submission`() = runTest {
        coEvery { mockGtoRepository.observeSubmission(any()) } returns flowOf(null) andThen flowOf(mockSubmission)

        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        // First load fails
        assertNotNull(viewModel.uiState.value.error)

        viewModel.retry("gpe-sub-123")
        advanceUntilIdle()

        // Retry succeeds
        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNotNull(state.submission)

        coVerify(exactly = 2) { mockGtoRepository.observeSubmission("gpe-sub-123") }
    }

    @Test
    fun `loadSubmission handles repository exception gracefully`() = runTest {
        coEvery { mockGtoRepository.observeSubmission(any()) } throws Exception("Network error")

        viewModel.loadSubmission("gpe-sub-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Failed to load submission"))
    }
}

