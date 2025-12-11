package com.ssbmax.ui.interview.result

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for InterviewResultViewModel
 *
 * Tests cover:
 * - Result loading
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterviewResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: InterviewResultViewModel
    private lateinit var interviewRepository: InterviewRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testResultId = "result-123"
    private val testSessionId = "session-123"
    private val testUserId = "user-123"

    private val testResult = InterviewResult(
        id = testResultId,
        sessionId = testSessionId,
        userId = testUserId,
        mode = InterviewMode.TEXT_BASED,
        completedAt = Instant.now(),
        durationSec = 1800,
        totalQuestions = 16,
        totalResponses = 16,
        overallOLQScores = mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(5, 80, "Good reasoning"),
            OLQ.DETERMINATION to OLQScore(4, 75, "Strong determination")
        ),
        categoryScores = mapOf(
            OLQCategory.INTELLECTUAL to 5f,
            OLQCategory.SOCIAL to 5f
        ),
        overallConfidence = 78,
        strengths = listOf(OLQ.DETERMINATION, OLQ.SELF_CONFIDENCE),
        weaknesses = listOf(OLQ.POWER_OF_EXPRESSION),
        feedback = "Good performance overall",
        overallRating = 5
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        interviewRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("resultId" to testResultId))
    }

    private fun createViewModel() {
        viewModel = InterviewResultViewModel(
            interviewRepository = interviewRepository,
            savedStateHandle = savedStateHandle
        )
    }

    // ============================================
    // LOADING TESTS
    // ============================================

    @Test
    fun `loadResult fetches and displays result`() = runTest {
        // Given
        coEvery { interviewRepository.getResultById(testResultId) } returns Result.success(testResult)

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.result)
        assertEquals(testResultId, state.result?.id)
        assertEquals(5, state.result?.overallRating)
        assertNull(state.error)
    }

    @Test
    fun `loadResult handles result not found`() = runTest {
        // Given
        coEvery { interviewRepository.getResultById(testResultId) } returns
                Result.failure(Exception("Not found"))

        // When
        createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.result)
        assertEquals("Failed to load results", state.error)
    }

    @Test
    fun `refresh reloads the result`() = runTest {
        // Given
        coEvery { interviewRepository.getResultById(testResultId) } returns Result.success(testResult)
        createViewModel()
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.result)
    }
}
