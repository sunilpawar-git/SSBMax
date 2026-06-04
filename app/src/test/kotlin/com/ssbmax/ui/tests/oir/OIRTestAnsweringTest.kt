package com.ssbmax.ui.tests.oir

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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

            assertEquals("Selected option should match", correctOptionId, state.selectedOptionId)
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

            assertEquals("Selected option should match", incorrectOptionId, state.selectedOptionId)
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
            assertNull("Selected option should be reset", state.selectedOptionId)
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
}
