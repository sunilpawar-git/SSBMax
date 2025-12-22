package com.ssbmax.ui.tests.sdt

import com.ssbmax.core.domain.model.*
import com.ssbmax.testing.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SDTTestViewModel - Character validation tests
 * 
 * NOTE: These tests validate the state logic without triggering timer coroutines
 * to avoid test hangs. Full integration tests should be run separately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SDTTestViewModelTest : BaseViewModelTest() {

    // ==================== SDTTestUiState Validation Tests ====================
    // Tests the computed properties directly without ViewModel timer issues

    @Test
    fun `canMoveToNext returns false when charCount below minimum`() {
        // Given - state with short answer
        val state = SDTTestUiState(
            currentAnswer = "Short",
            config = SDTTestConfig(minCharsPerQuestion = 50, maxCharsPerQuestion = 1500)
        )

        // Then
        assertFalse("Should not allow move with < 50 chars", state.canMoveToNext)
        assertEquals("Char count should be 5", 5, state.currentCharCount)
    }

    @Test
    fun `canMoveToNext returns true when charCount in valid range`() {
        // Given - state with valid answer
        val validAnswer = "a".repeat(100)
        val state = SDTTestUiState(
            currentAnswer = validAnswer,
            config = SDTTestConfig(minCharsPerQuestion = 50, maxCharsPerQuestion = 1500)
        )

        // Then
        assertTrue("Should allow move with 100 chars", state.canMoveToNext)
        assertEquals("Char count should be 100", 100, state.currentCharCount)
    }

    @Test
    fun `canMoveToNext returns false when charCount above maximum`() {
        // Given - state with too long answer
        val longAnswer = "a".repeat(1600)
        val state = SDTTestUiState(
            currentAnswer = longAnswer,
            config = SDTTestConfig(minCharsPerQuestion = 50, maxCharsPerQuestion = 1500)
        )

        // Then
        assertFalse("Should not allow move with > 1500 chars", state.canMoveToNext)
        assertEquals("Char count should be 1600", 1600, state.currentCharCount)
    }

    @Test
    fun `progress is calculated correctly`() {
        // Given - 4 questions, 2 responses
        val questions = listOf(
            SDTQuestion(id = "q1", question = "Q1", sequenceNumber = 1),
            SDTQuestion(id = "q2", question = "Q2", sequenceNumber = 2),
            SDTQuestion(id = "q3", question = "Q3", sequenceNumber = 3),
            SDTQuestion(id = "q4", question = "Q4", sequenceNumber = 4)
        )
        val responses = listOf(
            SDTQuestionResponse("q1", "Q1", "Answer1", 7, 10, 0L, false),
            SDTQuestionResponse("q2", "Q2", "Answer2", 7, 10, 0L, false)
        )
        val state = SDTTestUiState(questions = questions, responses = responses)

        // Then
        assertEquals("Progress should be 50%", 0.5f, state.progress, 0.01f)
        assertEquals("Completed should be 2", 2, state.completedQuestions)
    }

    @Test
    fun `validResponseCount excludes skipped responses`() {
        // Given - 3 responses, 1 skipped
        val responses = listOf(
            SDTQuestionResponse("q1", "Q1", "Answer1", 7, 10, 0L, isSkipped = false),
            SDTQuestionResponse("q2", "Q2", "", 0, 5, 0L, isSkipped = true),
            SDTQuestionResponse("q3", "Q3", "Answer3", 7, 10, 0L, isSkipped = false)
        )
        val state = SDTTestUiState(responses = responses)

        // Then
        assertEquals("Should have 2 valid responses", 2, state.validResponseCount)
    }

    @Test
    fun `currentQuestion returns correct question by index`() {
        // Given
        val questions = listOf(
            SDTQuestion(id = "q1", question = "First", sequenceNumber = 1),
            SDTQuestion(id = "q2", question = "Second", sequenceNumber = 2)
        )
        val state = SDTTestUiState(questions = questions, currentQuestionIndex = 1)

        // Then
        assertEquals("Current question should be Second", "Second", state.currentQuestion?.question)
    }
}
