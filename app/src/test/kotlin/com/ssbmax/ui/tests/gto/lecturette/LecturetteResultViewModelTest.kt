package com.ssbmax.ui.tests.gto.lecturette

import com.ssbmax.core.domain.model.gto.GTOResult
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LecturetteResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: LecturetteResultViewModel
    private val mockGtoRepository = mockk<GTORepository>(relaxed = true)

    private val mockGTOResult = GTOResult(
        submissionId = "submission-lec-123",
        userId = "user-123",
        testType = GTOTestType.LECTURETTE,
        olqScores = emptyMap(),
        overallScore = 8.0f,
        overallRating = "Excellent",
        strengths = listOf("Public Speaking", "Clarity"),
        weaknesses = listOf("Nervousness"),
        recommendations = listOf("Practice more speeches"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 90
    )

    private val mockLecturetteSubmission = GTOSubmission.LecturetteSubmission(
        id = "submission-lec-123",
        userId = "user-123",
        testId = "lec-test-1",
        topicChoices = listOf("Topic 1", "Topic 2", "Topic 3", "Topic 4"),
        selectedTopic = "Leadership in the Indian Armed Forces",
        speechTranscript = "My detailed speech about leadership...",
        charCount = 250,
        submittedAt = System.currentTimeMillis(),
        status = GTOSubmissionStatus.COMPLETED,
        timeSpent = 180 // 3 minutes
    )

    @Before
    fun setup() {
        viewModel = LecturetteResultViewModel(mockGtoRepository)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.submission)
        assertNull(state.result)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission success updates state with submission data`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-lec-123") } returns 
            flowOf(mockLecturetteSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-lec-123") } returns 
            Result.success(mockGTOResult)

        viewModel.loadSubmission("submission-lec-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertEquals("submission-lec-123", state.submission?.id)
        assertEquals("Leadership in the Indian Armed Forces", state.submission?.selectedTopic)
        assertEquals(250, state.submission?.charCount)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-lec-123") } returns 
            flowOf(null)

        viewModel.loadSubmission("submission-lec-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `loadSubmission with pending status shows analyzing state`() = runTest {
        val pendingSubmission = mockLecturetteSubmission.copy(
            status = GTOSubmissionStatus.PENDING_ANALYSIS
        )
        coEvery { mockGtoRepository.observeSubmission("submission-lec-123") } returns 
            flowOf(pendingSubmission)

        viewModel.loadSubmission("submission-lec-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertTrue(state.isAnalyzing)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `loadSubmission with completed status loads result`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-lec-123") } returns 
            flowOf(mockLecturetteSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-lec-123") } returns 
            Result.success(mockGTOResult)

        viewModel.loadSubmission("submission-lec-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertNotNull(state.result)
        assertEquals(8.0f, state.result?.overallScore)
        assertTrue(state.isCompleted)
    }

    @Test
    fun `retry calls loadSubmission again`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-lec-123") } returns 
            flowOf(mockLecturetteSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-lec-123") } returns 
            Result.success(mockGTOResult)

        viewModel.retry("submission-lec-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
    }
}

