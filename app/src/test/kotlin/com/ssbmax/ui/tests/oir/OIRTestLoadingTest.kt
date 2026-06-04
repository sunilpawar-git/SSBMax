package com.ssbmax.ui.tests.oir

import android.util.Log
import com.ssbmax.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for OIRTestViewModel: question loading, validation filtering, and subscription limits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestLoadingTest : OIRViewModelTestBase() {

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

        android.util.Log.d("TEST", "isLoading: ${state.isLoading}, errorResId: ${state.errorResId}, isLimitReached: ${state.isLimitReached}")

        assertFalse("Should not be loading, but was ${state.isLoading}", state.isLoading)
        assertNotNull("Should have error, but was null", state.errorResId)
    }

    // ==================== Validation Tests (loadTest) ====================

    @Test
    fun `loadTest filters out invalid questions with malformed option IDs`() = runTest {
        val corruptedQuestions = listOf(
            OIRQuestion(
                id = "oir_corrupt_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Test question?",
                options = listOf(
                    OIROption("a", "Option A"),  // ❌ Should be "opt_a"
                    OIROption("b", "Option B"),  // ❌ Should be "opt_b"
                    OIROption("c", "Option C"),  // ❌ Should be "opt_c"
                    OIROption("d", "Option D")   // ❌ Should be "opt_d"
                ),
                correctAnswerId = "b",  // ❌ Should be "opt_b"
                explanation = "Test"
            )
        )

        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(corruptedQuestions)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNotNull("Should have error about validation", state.errorResId)
    }

    @Test
    fun `loadTest filters out questions with malformed correctAnswerId`() = runTest {
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
                correctAnswerId = "opt_103_b",  // ❌ Should be "opt_b"
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
                    OIROption("a", "A"),  // ❌ Invalid
                    OIROption("b", "B"),  // ❌ Invalid
                    OIROption("c", "C"),  // ❌ Invalid
                    OIROption("d", "D")   // ❌ Invalid
                ),
                correctAnswerId = "b",  // ❌ Invalid
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
        assertEquals(
            "Should have 2 valid questions (1 invalid filtered out)",
            2,
            state.totalQuestions
        )

        val session = viewModel.uiState.value.currentQuestion
        assertNotNull("Should have current question", session)
        assertTrue(
            "Current question should be valid",
            session?.id?.startsWith("oir_valid") == true
        )
    }

    // ==================== Subscription Limit Tests ====================

    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.LimitReached(
            tier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
            limit = 1,
            usedCount = 1,
            resetsAt = "Dec 1, 2025"
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", com.ssbmax.core.domain.model.SubscriptionTier.FREE, state.subscriptionTier)
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
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 5
        )

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

        coVerify(exactly = 1) {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        }
    }
}
