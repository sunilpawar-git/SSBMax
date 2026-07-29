package com.ssbmax.ui.tests.oir

import app.cash.turbine.test
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for OIRTestViewModel: answer selection, question navigation, and image prefetching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestAnsweringTest : OIRViewModelTestBase() {

    // ==================== Answer Selection ====================

    @Test
    fun `selectOption records correct answer and shows feedback`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val firstQuestion = mockQuestions[0]
        val correctOptionId = firstQuestion.correctAnswerId

        viewModel.selectOption(correctOptionId)

        viewModel.uiState.test {
            val state = awaitItem()

            assertTrue("Selected option should match", correctOptionId in state.selectedOptionIds)
            assertTrue("Should show feedback", state.showFeedback)
            assertTrue("Answer should be marked correct", state.isCurrentAnswerCorrect)
            assertTrue("Question should be marked as answered", state.currentQuestionAnswered)
        }
    }

    @Test
    fun `selectOption records incorrect answer`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val firstQuestion = mockQuestions[0]
        val incorrectOptionId = firstQuestion.options.first { it.id != firstQuestion.correctAnswerId }.id

        viewModel.selectOption(incorrectOptionId)

        viewModel.uiState.test {
            val state = awaitItem()

            assertTrue("Selected option should match", incorrectOptionId in state.selectedOptionIds)
            assertTrue("Should show feedback", state.showFeedback)
            assertFalse("Answer should be marked incorrect", state.isCurrentAnswerCorrect)
            assertTrue("Question should be marked as answered", state.currentQuestionAnswered)
        }
    }

    @Test
    fun `selectOption validates question before scoring`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val firstQuestion = mockQuestions[0]
        viewModel.selectOption(firstQuestion.correctAnswerId)

        val state = viewModel.uiState.value
        assertTrue("Answer should be recorded", state.currentQuestionAnswered)
        assertTrue("Feedback should be shown", state.showFeedback)
    }

    // ==================== Navigation ====================

    @Test
    fun `nextQuestion moves to next question`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption(mockQuestions[0].correctAnswerId)

        viewModel.nextQuestion()

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Should be on question 1", 1, state.currentQuestionIndex)
            assertNotNull("Should have current question", state.currentQuestion)
            assertEquals("Should be second question", mockQuestions[1].id, state.currentQuestion?.id)
            assertTrue("Selected options should be empty after navigation", state.selectedOptionIds.isEmpty())
            assertFalse("Feedback should be hidden", state.showFeedback)
        }
    }

    @Test
    fun `previousQuestion moves to previous question`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.nextQuestion()

        viewModel.previousQuestion()

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals("Should be back on question 0", 0, state.currentQuestionIndex)
            assertEquals("Should be first question", mockQuestions[0].id, state.currentQuestion?.id)
        }
    }

    @Test
    fun `previousQuestion at start does nothing`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialIndex = viewModel.uiState.value.currentQuestionIndex

        viewModel.previousQuestion()

        assertEquals("Should stay at index 0", initialIndex, viewModel.uiState.value.currentQuestionIndex)
    }

    // ==================== Image Prefetch Tests ====================

    @Test
    fun `prefetchNextImage enqueues next question imageUrl when advancing`() = runTest {
        val questionsWithImages = createMockQuestions().mapIndexed { idx, q ->
            q.copy(questionImageUrl = "https://cdn.example.com/oir_q${idx + 1}.png")
        }
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(questionsWithImages)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.nextQuestion()
        advanceUntilIdle()

        verify(atLeast = 1) { mockImageLoader.enqueue(any()) }
    }

    @Test
    fun `prefetchNextImage does NOT enqueue when on last question`() = runTest {
        val singleQuestion = createMockQuestions().take(1).map { q ->
            q.copy(questionImageUrl = "https://cdn.example.com/oir_only.png")
        }
        coEvery {
            mockTestContentRepo.getOIRTestQuestions(any(), any())
        } returns Result.success(singleQuestion)

        viewModel = createViewModel()
        advanceUntilIdle()

        clearMocks(mockImageLoader)

        viewModel.nextQuestion()
        advanceUntilIdle()

        verify(exactly = 0) { mockImageLoader.enqueue(any()) }
    }

    // ==================== Per-question timeTakenSeconds (Bug 1) ====================

    @Test
    fun `selectOption timeTakenSeconds equals elapsed time since question start`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        fakeClock.advanceBy(3_000L) // 3 seconds pass while user reads question
        viewModel.selectOption(mockQuestions[0].correctAnswerId)

        val answer = viewModel.uiState.value.session?.answers?.get(mockQuestions[0].id)
        assertNotNull("Answer should be recorded", answer)
        assertEquals("timeTakenSeconds should reflect elapsed time", 3, answer!!.timeTakenSeconds)
    }

    @Test
    fun `nextQuestion resets per-question timer so second answer tracks its own elapsed time`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Answer Q1 at t+2s
        fakeClock.advanceBy(2_000L)
        viewModel.selectOption(mockQuestions[0].correctAnswerId)

        // Navigate to Q2 — timer should reset here
        viewModel.nextQuestion()

        // 5 more seconds pass while user reads Q2
        fakeClock.advanceBy(5_000L)
        viewModel.selectOption(mockQuestions[1].correctAnswerId)

        val answerQ2 = viewModel.uiState.value.session?.answers?.get(mockQuestions[1].id)
        assertNotNull("Q2 answer should be recorded", answerQ2)
        assertEquals("Q2 timeTakenSeconds should be 5, not 7", 5, answerQ2!!.timeTakenSeconds)
    }

    @Test
    fun `selectOption timeTakenSeconds is never negative`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Call selectOption immediately — no time has passed since question displayed
        viewModel.selectOption(mockQuestions[0].correctAnswerId)

        val answer = viewModel.uiState.value.session?.answers?.get(mockQuestions[0].id)
        assertNotNull(answer)
        assertTrue("timeTakenSeconds should be >= 0", answer!!.timeTakenSeconds >= 0)
    }

    // ==================== Multi-Select Toggle Tests (Phase 6) ====================

    private fun multiSelectQuestion() = com.ssbmax.shared.domain.model.OIRQuestion(
        id = "oir_multi_1",
        questionNumber = 99,
        type = com.ssbmax.shared.domain.model.OIRQuestionType.NON_VERBAL_REASONING,
        difficulty = com.ssbmax.shared.domain.model.QuestionDifficulty.MEDIUM,
        questionText = "Which two figures belong to Class A?",
        options = listOf(
            com.ssbmax.shared.domain.model.OIROption("opt_a", "Figure 1"),
            com.ssbmax.shared.domain.model.OIROption("opt_b", "Figure 2"),
            com.ssbmax.shared.domain.model.OIROption("opt_c", "Figure 3"),
            com.ssbmax.shared.domain.model.OIROption("opt_d", "Figure 4"),
            com.ssbmax.shared.domain.model.OIROption("opt_e", "Figure 5"),
        ),
        correctAnswerId = "",
        correctAnswerIds = listOf("opt_b", "opt_d"),
        explanation = "Answer: 2 and 4."
    )

    @Test
    fun `selectOption multiSelect togglesCorrectly — second tap on same option deselects it`() = runTest {
        // WHY: multi-select questions require exactly 2 answers; tapping a selected option
        // must remove it from the set so the user can correct their choice.
        val question = multiSelectQuestion()
        coEvery { mockTestContentRepo.getOIRTestQuestions(any(), any()) } returns Result.success(listOf(question))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption("opt_b")
        assertTrue("opt_b should be selected", "opt_b" in viewModel.uiState.value.selectedOptionIds)

        viewModel.selectOption("opt_b") // deselect
        assertTrue("opt_b should be removed on re-tap", viewModel.uiState.value.selectedOptionIds.isEmpty())
    }

    @Test
    fun `selectOption multiSelect capsAtTwo — third tap on a new option is ignored`() = runTest {
        // WHY: these questions ask for exactly 2; allowing >2 selections would corrupt scoring.
        val question = multiSelectQuestion()
        coEvery { mockTestContentRepo.getOIRTestQuestions(any(), any()) } returns Result.success(listOf(question))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption("opt_b")
        viewModel.selectOption("opt_d")
        viewModel.selectOption("opt_c") // should be ignored — cap reached

        val selected = viewModel.uiState.value.selectedOptionIds
        assertEquals("Only 2 options should be selected", 2, selected.size)
        assertTrue("opt_b should be selected", "opt_b" in selected)
        assertTrue("opt_d should be selected", "opt_d" in selected)
        assertFalse("opt_c tap should have been ignored", "opt_c" in selected)
    }

    @Test
    fun `selectOption singleSelect replacesPreviousSelection`() = runTest {
        // WHY: single-select questions must replace, not accumulate; otherwise the answer
        // set would grow unboundedly and scoring would break.
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption("opt1")
        viewModel.selectOption("opt2")

        val selected = viewModel.uiState.value.selectedOptionIds
        assertEquals("Only 1 option for single-select", 1, selected.size)
        assertTrue("Latest selection should win", "opt2" in selected)
        assertFalse("Previous selection should be replaced", "opt1" in selected)
    }

    @Test
    fun `selectOption multiSelect isCurrentAnswerCorrect — true only when exact correct pair selected`() = runTest {
        // WHY: isCurrentAnswerCorrect drives immediate visual feedback; it must be false
        // for partial or wrong selections and true only for the exact correct pair.
        val question = multiSelectQuestion()
        coEvery { mockTestContentRepo.getOIRTestQuestions(any(), any()) } returns Result.success(listOf(question))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectOption("opt_b") // 1 of 2 correct — partial
        assertFalse("Partial selection should not be correct", viewModel.uiState.value.isCurrentAnswerCorrect)

        viewModel.selectOption("opt_d") // both correct now
        assertTrue("Complete correct pair should be correct", viewModel.uiState.value.isCurrentAnswerCorrect)

        viewModel.selectOption("opt_b") // deselect one — incomplete again
        assertFalse("Deselecting from correct pair should no longer be correct", viewModel.uiState.value.isCurrentAnswerCorrect)
    }

    // ==================== Phase 5-D RED: pauseTest session leak ====================

    @Test
    fun `pauseTest callsEndTestSession preventingSessionLeak`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.session?.sessionId ?: "unknown"

        // Act
        viewModel.pauseTest()
        advanceUntilIdle()

        // Assert — endTestSession must be called to prevent orphaned Firestore sessions
        coVerify(exactly = 1) {
            mockTestSessionRepo.endTestSession(sessionId)
        }
    }
}

