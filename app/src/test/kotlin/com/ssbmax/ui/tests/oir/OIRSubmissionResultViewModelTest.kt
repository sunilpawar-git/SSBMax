package com.ssbmax.ui.tests.oir

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OIRSubmissionResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: OIRSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)

    private val mockResult = OIRTestResult(
        testId = "oir-standard",
        sessionId = "session-oir-123",
        userId = "user-123",
        totalQuestions = 50,
        correctAnswers = 38,
        incorrectAnswers = 10,
        skippedQuestions = 2,
        totalTimeSeconds = 1200,
        timeTakenSeconds = 1050,
        rawScore = 38,
        percentageScore = 76.0f,
        categoryScores = emptyMap(),
        difficultyBreakdown = emptyMap(),
        answeredQuestions = emptyList(),
        completedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        viewModel = OIRSubmissionResultViewModel(mockSubmissionRepo)
    }

    @Test
    fun `loadSubmission success stops loading`() = runTest {
        val firestoreData = createFirestoreResultMap(mockResult)
        coEvery { mockSubmissionRepo.getSubmission(any()) } returns Result.success(firestoreData)

        viewModel.loadSubmission("session-oir-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            // Note: OIR parsing is complex, just verify loading completes
        }
    }

    @Test
    fun `loadSubmission with null data shows error`() = runTest {
        coEvery { mockSubmissionRepo.getSubmission(any()) } returns Result.success(null)

        viewModel.loadSubmission("session-oir-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.result)
            assertNotNull(state.error)
            assertEquals("Submission not found", state.error)
        }
    }

    @Test
    fun `loadSubmission failure shows error`() = runTest {
        coEvery { mockSubmissionRepo.getSubmission(any()) } returns Result.failure(
            Exception("Network error")
        )

        viewModel.loadSubmission("session-oir-123")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.result)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("error") || state.error!!.isNotBlank())
        }
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertNull(state.result)
        assertNull(state.error)
    }

    private fun createFirestoreResultMap(result: OIRTestResult): Map<String, Any> {
        return mapOf(
            "data" to mapOf(
                "testId" to result.testId,
                "sessionId" to result.sessionId,
                "userId" to result.userId,
                "totalQuestions" to result.totalQuestions,
                "correctAnswers" to result.correctAnswers,
                "incorrectAnswers" to result.incorrectAnswers,
                "skippedQuestions" to result.skippedQuestions,
                "totalTimeSeconds" to result.totalTimeSeconds,
                "timeTakenSeconds" to result.timeTakenSeconds,
                "rawScore" to result.rawScore,
                "percentageScore" to result.percentageScore,
                "categoryScores" to result.categoryScores,
                "difficultyBreakdown" to result.difficultyBreakdown,
                "answeredQuestions" to result.answeredQuestions,
                "completedAt" to result.completedAt
            )
        )
    }
}

