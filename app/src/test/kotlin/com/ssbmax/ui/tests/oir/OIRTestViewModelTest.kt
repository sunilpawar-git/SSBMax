package com.ssbmax.ui.tests.oir

import com.ssbmax.shared.domain.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OIRTestViewModel: question loading, validation filtering, and subscription gates.
 * Answer selection + navigation tests live in OIRTestAnsweringTest.
 * Submission + timer tests live in OIRTestSubmissionTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestViewModelTest : OIRViewModelTestBase() {

    // ==================== Test Loading ====================

    @Test
    fun `loadTest success loads questions and starts timer`() = runTest {
        viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertFalse("Should not be loading: ${state.isLoading}", state.isLoading)
        assertNull("Should not have error: ${state.errorResId}", state.errorResId)
        assertEquals("Should have 5 questions", 5, state.totalQuestions)
        assertEquals("Should start at question 0", 0, state.currentQuestionIndex)
        assertNotNull("Should have current question", state.currentQuestion)
        assertEquals("Timer should be 40 minutes (2400s)", 2400, state.timeRemainingSeconds)

        coVerify { mockTestContentRepo.getOIRTestQuestions(50, any()) }
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.failure(Exception("Network error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.errorResId)
    }

    @Test
    fun `loadTest with empty questions shows error`() = runTest {
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading, but was ${state.isLoading}", state.isLoading)
        assertNotNull("Should have error, but was null", state.errorResId)
    }

    // ==================== Validation Filtering ====================

    @Test
    fun `loadTest filters out invalid questions with malformed option IDs`() = runTest {
        // WHY: single-letter option IDs (e.g. "a") indicate a corrupted extraction;
        // they must be rejected so the user never sees an un-scorable question.
        val corruptedQuestions = listOf(
            OIRQuestion(
                id = "oir_corrupt_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Test question?",
                options = listOf(
                    OIROption("a", "Option A"),
                    OIROption("b", "Option B"),
                    OIROption("c", "Option C"),
                    OIROption("d", "Option D")
                ),
                correctAnswerId = "b",
                explanation = "Test"
            )
        )
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(corruptedQuestions)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull("Should have error about validation", viewModel.uiState.value.errorResId)
    }

    @Test
    fun `loadTest filters out questions with malformed correctAnswerId`() = runTest {
        // WHY: opt_103_b is an extraction artifact where the question number leaked into
        // the answer field; it cannot be scored and must be dropped.
        val corruptedQuestions = listOf(
            OIRQuestion(
                id = "oir_103",
                questionNumber = 103,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "What is 2+2?",
                options = listOf(
                    OIROption("opt_a", "3"),
                    OIROption("opt_b", "4"),
                    OIROption("opt_c", "5"),
                    OIROption("opt_d", "6")
                ),
                correctAnswerId = "opt_103_b",
                explanation = "2+2=4"
            )
        )
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(corruptedQuestions)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Should have error or filter out question",
            state.errorResId != null || state.totalQuestions == 0
        )
    }

    @Test
    fun `loadTest handles mix of valid and invalid questions`() = runTest {
        val mixedQuestions = listOf(
            OIRQuestion(
                id = "oir_valid_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Valid question?",
                options = listOf(
                    OIROption("opt_a", "A"),
                    OIROption("opt_b", "B"),
                    OIROption("opt_c", "C"),
                    OIROption("opt_d", "D")
                ),
                correctAnswerId = "opt_b",
                explanation = "Test"
            ),
            OIRQuestion(
                id = "oir_invalid_1",
                questionNumber = 2,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Invalid question?",
                options = listOf(
                    OIROption("a", "A"),
                    OIROption("b", "B"),
                    OIROption("c", "C"),
                    OIROption("d", "D")
                ),
                correctAnswerId = "b",
                explanation = "Test"
            ),
            OIRQuestion(
                id = "oir_valid_2",
                questionNumber = 3,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "Another valid question?",
                options = listOf(
                    OIROption("opt_a", "1"),
                    OIROption("opt_b", "2"),
                    OIROption("opt_c", "3"),
                    OIROption("opt_d", "4")
                ),
                correctAnswerId = "opt_c",
                explanation = "Test"
            )
        )
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(mixedQuestions)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.errorResId)
        assertEquals("Should have 2 valid questions (1 invalid filtered out)", 2, state.totalQuestions)
        assertTrue(
            "Current question should be valid",
            state.currentQuestion?.id?.startsWith("oir_valid") == true
        )
    }

    // ==================== Subscription Limit Tests ====================

    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.LimitReached(
            tier = com.ssbmax.shared.domain.model.SubscriptionTier.FREE,
            limit = 1,
            usedCount = 1,
            resetsAt = "Dec 1, 2025"
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", com.ssbmax.shared.domain.model.SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "Dec 1, 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 0 questions", 0, state.totalQuestions)
    }

    @Test
    fun `loadTest proceeds when user is eligible`() = runTest {
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 5)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded questions", state.totalQuestions > 0)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.errorResId)
    }

    @Test
    fun `loadTest calls canTakeTest with correct parameters`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSubscriptionManager.canTakeTest(TestType.OIR, any()) }
    }

}
