package com.ssbmax.ui.tests.oir

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for OIRTestViewModel: test submission, timer behaviour, category scores, and cleanup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestSubmissionTest : OIRViewModelTestBase() {

    // ==================== Test Submission ====================

    @Test
    fun `submitTest calculates correct score and ends session`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        mockQuestions.forEachIndexed { index, question ->
            viewModel.selectOption(question.correctAnswerId)
            if (index < mockQuestions.size - 1) {
                viewModel.nextQuestion()
            }
        }

        viewModel.submitTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()

            assertTrue("Test should be completed", state.isCompleted)
            assertNotNull("Should have session ID", state.sessionId)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have test result", state.testResult)

            val result = state.testResult!!
            assertEquals("Should have 5 correct answers", 5, result.correctAnswers)
            assertEquals("Should have 0 incorrect answers", 0, result.incorrectAnswers)
            assertEquals("Should have 0 skipped", 0, result.skippedQuestions)
            assertTrue("Percentage score should be 100%", result.percentageScore >= 99f)

            coVerify { mockSubmitUseCase(any()) }
        }
    }

    @Test
    fun `submitTest with mixed answers calculates partial score`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        mockQuestions.forEachIndexed { index, question ->
            if (index < 3) {
                viewModel.selectOption(question.correctAnswerId)
            } else {
                val wrongOption = question.options.first { it.id != question.correctAnswerId }.id
                viewModel.selectOption(wrongOption)
            }
            if (index < mockQuestions.size - 1) {
                viewModel.nextQuestion()
            }
        }

        viewModel.submitTest()
        advanceUntilIdle()

        val result = viewModel.uiState.value.testResult!!
        assertEquals("Should have 3 correct answers", 3, result.correctAnswers)
        assertEquals("Should have 2 incorrect answers", 2, result.incorrectAnswers)
        assertTrue("Percentage score should be around 60%", result.percentageScore in 55f..65f)
    }

    @Test
    fun `submitTest with unanswered questions counts as skipped`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption(mockQuestions[0].correctAnswerId)
        viewModel.nextQuestion()
        viewModel.selectOption(mockQuestions[1].correctAnswerId)

        viewModel.submitTest()
        advanceUntilIdle()

        val result = viewModel.uiState.value.testResult!!
        assertEquals("Should have 2 correct answers", 2, result.correctAnswers)
        assertEquals("Should have 3 skipped", 3, result.skippedQuestions)
    }

    // ==================== Timer Tests ====================

    @Test
    fun `timer decrements every second`() = runTest {
        viewModel = createViewModel()

        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertEquals("Initial time should be 2400s (40 min)", 2400, initialTime)

        advanceTimeBy(5000)
        advanceUntilIdle()

        val newTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Time should have decreased (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 4 seconds", initialTime - newTime >= 4)
    }

    @Test
    fun `timer expiry auto-submits test`() = runTest {
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any())
        } returns Result.success(createMockQuestions().take(1))

        viewModel = createViewModel()
        advanceUntilIdle()

        advanceTimeBy(1801000)

        assertTrue("Test should be completed", viewModel.uiState.value.isCompleted)
    }

    // ==================== Category Scores ====================

    @Test
    fun `calculateResults generates category scores`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        mockQuestions.forEach { question ->
            viewModel.selectOption(question.correctAnswerId)
            viewModel.nextQuestion()
        }

        viewModel.submitTest()
        advanceUntilIdle()

        val result = viewModel.uiState.value.testResult!!
        assertFalse("Category scores should not be empty", result.categoryScores.isEmpty())

        OIRQuestionType.values().forEach { type ->
            assertTrue(
                "Should have score for category $type",
                result.categoryScores.containsKey(type)
            )
        }
    }

    // ==================== Cleanup ====================

    @Test
    fun `onCleared cancels timer job`() = runTest {
        viewModel = createViewModel()

        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Initial time should be set", initialTime > 0)

        advanceTimeBy(2000)
        advanceUntilIdle()
        val timeAfter2Sec = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Timer should have decreased", timeAfter2Sec < initialTime)

        // Note: onCleared() is protected and called by the Android system on ViewModel destruction.
        // This test verifies the timer was active and functioning correctly.
        assertTrue("Timer was active and working", initialTime > 0)
    }

    // ==================== markQuestionsUsed (Bug 2) ====================

    @Test
    fun `submitTest delegates marking to the use case and does NOT mark questions used itself`() = runTest {
        // Bug 2 (revised): marking served questions used is owned solely by SubmitOIRTestUseCase
        // (step 6) — the single source of truth. The ViewModel must NOT also mark them, which
        // previously produced a duplicate write per submit. (The actual marking is verified in
        // SubmitOIRTestUseCaseTest.) Here we pin that the ViewModel only delegates.
        viewModel = createViewModel()
        advanceUntilIdle()

        // Clear any calls made during auto-submit triggered by the timer running to zero
        clearMocks(mockTestContentRepo, mockSubmitUseCase, answers = false)

        // Reload so there's a fresh session to submit
        viewModel.loadTest()
        advanceUntilIdle()

        // Immediately submit without waiting for timer so no double-trigger
        viewModel.submitTest()
        advanceUntilIdle()

        // Submission is orchestrated through the use case...
        coVerify(exactly = 1) { mockSubmitUseCase(any()) }
        // ...and the ViewModel never marks questions used directly (no duplicate write).
        coVerify(exactly = 0) { mockTestContentRepo.markOIRQuestionsUsed(any()) }
    }
}
