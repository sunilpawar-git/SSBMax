package com.ssbmax.ui.tests.gto.gd

import app.cash.turbine.test
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
class GDResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: GDResultViewModel
    private val mockGtoRepository = mockk<GTORepository>(relaxed = true)
    
    private val mockGTOResult = GTOResult(
        submissionId = "submission-gd-123",
        userId = "user-123",
        testType = GTOTestType.GROUP_DISCUSSION,
        olqScores = emptyMap(), // Simplified for basic testing
        overallScore = 7.5f,
        overallRating = "Good",
        strengths = listOf("Leadership", "Communication"),
        weaknesses = listOf("Decision Speed"),
        recommendations = listOf("Practice under pressure"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 85
    )

    private val mockGDSubmission = GTOSubmission.GDSubmission(
        id = "submission-gd-123",
        userId = "user-123",
        testId = "gd-test-1",
        topic = "Leadership in difficult situations",
        response = "My detailed response about leadership in difficult situations...",
        charCount = 200,
        submittedAt = System.currentTimeMillis(),
        status = GTOSubmissionStatus.COMPLETED,
        timeSpent = 600 // 10 minutes
    )

    @Before
    fun setup() {
        viewModel = GDResultViewModel(mockGtoRepository)
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
        coEvery { mockGtoRepository.observeSubmission("submission-gd-123") } returns 
            flowOf(mockGDSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-gd-123") } returns 
            Result.success(mockGTOResult)

        viewModel.loadSubmission("submission-gd-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertEquals("submission-gd-123", state.submission?.id)
        assertEquals("Leadership in difficult situations", state.submission?.topic)
        assertEquals(200, state.submission?.charCount)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-gd-123") } returns 
            flowOf(null)

        viewModel.loadSubmission("submission-gd-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Submission not found", state.error)
    }

    @Test
    fun `loadSubmission with pending status shows analyzing state`() = runTest {
        val pendingSubmission = mockGDSubmission.copy(
            status = GTOSubmissionStatus.PENDING_ANALYSIS
        )
        coEvery { mockGtoRepository.observeSubmission("submission-gd-123") } returns 
            flowOf(pendingSubmission)

        viewModel.loadSubmission("submission-gd-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertTrue(state.isAnalyzing)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `loadSubmission with completed status loads result`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-gd-123") } returns 
            flowOf(mockGDSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-gd-123") } returns 
            Result.success(mockGTOResult)

        viewModel.loadSubmission("submission-gd-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertNotNull(state.result)
        assertEquals(7.5f, state.result?.overallScore)
        assertTrue(state.isCompleted)
    }

    @Test
    fun `retry calls loadSubmission again`() = runTest {
        coEvery { mockGtoRepository.observeSubmission("submission-gd-123") } returns 
            flowOf(mockGDSubmission)
        coEvery { mockGtoRepository.getTestResult("submission-gd-123") } returns 
            Result.success(mockGTOResult)

        viewModel.retry("submission-gd-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.submission)
    }
}

