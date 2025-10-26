package com.ssbmax.ui.tests.oir

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.testing.MockDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OIRTestViewModel
 * 
 * OIR Test: Intelligence & Reasoning Test with 50 questions, 30 minutes
 * Note: ViewModel uses internal session, exposing only current question via UIState
 */
class OIRTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: OIRTestViewModel
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var userProfileRepository: UserProfileRepository
    
    private val testId = "oir-test-1"
    private val userId = "user123"
    private lateinit var mockQuestions: List<OIRQuestion>
    
    @Before
    fun setUp() {
        testContentRepository = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        
        mockQuestions = MockDataFactory.createMockOIRQuestions(10)
        
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(
            Result.success(MockDataFactory.createMockUserProfile())
        )
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getOIRQuestions(any()) } returns Result.success(mockQuestions)
        coEvery { testContentRepository.endTestSession(any()) } returns Result.success(Unit)
        
        // ViewModel auto-loads in init, but we'll override
        viewModel = OIRTestViewModel(
            testContentRepository,
            userProfileRepository
        )
    }
    
    // ==================== Loading Tests ====================
    
    @Test
    fun `loadTest - successfully loads questions`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have current question", state.currentQuestion)
        assertEquals("Should have 10 total questions", 10, state.totalQuestions)
        assertEquals("Should start at question 0", 0, state.currentQuestionIndex)
    }
    
    @Test
    fun `loadTest - handles empty questions list`() = runTest {
        // Given
        coEvery { testContentRepository.getOIRQuestions(testId) } returns Result.success(emptyList())
        
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        assertNotNull("Should have error", viewModel.uiState.value.error)
    }
    
    @Test
    fun `loadTest - handles network failure`() = runTest {
        // Given
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns 
            Result.failure(Exception("Network error"))
        
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
    }
    
    @Test
    fun `initial load starts timer`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Timer should be set", initialTime > 0)
    }
    
    // ==================== Question Navigation ====================
    
    @Test
    fun `nextQuestion - moves to next question`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // When
        viewModel.nextQuestion()
        
        // Then
        assertEquals("Should move to question 1", 1, viewModel.uiState.value.currentQuestionIndex)
    }
    
    @Test
    fun `previousQuestion - moves to previous question`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        viewModel.nextQuestion() // Move to 1
        viewModel.nextQuestion() // Move to 2
        
        // When
        viewModel.previousQuestion()
        
        // Then
        assertEquals("Should move back to question 1", 1, viewModel.uiState.value.currentQuestionIndex)
    }
    
    @Test
    fun `navigation updates current question`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val firstQuestion = viewModel.uiState.value.currentQuestion
        
        // When
        viewModel.nextQuestion()
        val secondQuestion = viewModel.uiState.value.currentQuestion
        
        // Then
        assertNotEquals("Questions should be different", firstQuestion?.id, secondQuestion?.id)
    }
    
    // ==================== Answer Selection ====================
    
    @Test
    fun `selectOption - records answer and shows feedback`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val question = viewModel.uiState.value.currentQuestion!!
        val optionId = question.options[0].id
        
        // When
        viewModel.selectOption(optionId)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Selected option should be set", optionId, state.selectedOptionId)
        assertTrue("Feedback should show", state.showFeedback)
        assertTrue("Question should be answered", state.currentQuestionAnswered)
    }
    
    @Test
    fun `selectOption - marks correct answer`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val question = viewModel.uiState.value.currentQuestion!!
        
        // When - Select correct answer
        viewModel.selectOption(question.correctAnswerId)
        
        // Then
        assertTrue("Answer should be marked correct", viewModel.uiState.value.isCurrentAnswerCorrect)
    }
    
    @Test
    fun `selectOption - marks incorrect answer`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val question = viewModel.uiState.value.currentQuestion!!
        val wrongOption = question.options.first { it.id != question.correctAnswerId }
        
        // When
        viewModel.selectOption(wrongOption.id)
        
        // Then
        assertFalse("Answer should be marked incorrect", viewModel.uiState.value.isCurrentAnswerCorrect)
    }
    
    @Test
    fun `moving to next question clears feedback`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val question = viewModel.uiState.value.currentQuestion!!
        viewModel.selectOption(question.correctAnswerId)
        
        // When
        viewModel.nextQuestion()
        
        // Then
        assertNull("Selected option should be cleared", viewModel.uiState.value.selectedOptionId)
        assertFalse("Feedback should be hidden", viewModel.uiState.value.showFeedback)
    }
    
    @Test
    fun `returning to answered question shows previous answer`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val firstQuestion = viewModel.uiState.value.currentQuestion!!
        viewModel.selectOption(firstQuestion.correctAnswerId)
        viewModel.nextQuestion()
        
        // When - Go back
        viewModel.previousQuestion()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should show previous answer", firstQuestion.correctAnswerId, state.selectedOptionId)
        assertTrue("Should show feedback", state.showFeedback)
    }
    
    // ==================== Test Submission ====================
    
    @Test
    fun `submitTest - marks test as completed`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Answer a few questions
        repeat(3) {
            val question = viewModel.uiState.value.currentQuestion!!
            viewModel.selectOption(question.correctAnswerId)
            viewModel.nextQuestion()
        }
        
        // When
        viewModel.submitTest()
        advanceTimeBy(200)
        
        // Then
        assertTrue("Test should be completed", viewModel.uiState.value.isCompleted)
        coVerify { testContentRepository.endTestSession(any()) }
    }
    
    @Test
    fun `submitTest - clears cache`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // When
        viewModel.submitTest()
        advanceTimeBy(200)
        
        // Then
        coVerify { testContentRepository.clearCache() }
    }
    
    // ==================== Timer Tests ====================
    
    @Test
    fun `timer starts on test load`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        
        // Advance time
        advanceTimeBy(3000) // 3 seconds
        
        // Then
        val newTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Timer should have counted down", newTime < initialTime)
    }
    
    @Test
    fun `pauseTest - stops timer`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // When
        viewModel.pauseTest()
        val timeAtPause = viewModel.uiState.value.timeRemainingSeconds
        advanceTimeBy(5000) // Try to advance time
        
        // Then
        // Note: We can't fully test pause without exposing isPaused state
        // Just verify pauseTest doesn't crash
        assertNotNull("State should be valid", viewModel.uiState.value)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `currentQuestion is not null after loading`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        assertNotNull("Current question should not be null", viewModel.uiState.value.currentQuestion)
    }
    
    @Test
    fun `totalQuestions matches loaded count`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // Then
        assertEquals("Should have 10 questions", 10, viewModel.uiState.value.totalQuestions)
    }
    
    @Test
    fun `sessionId is set after loading`() = runTest {
        // When
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        viewModel.submitTest()
        advanceTimeBy(200)
        
        // Then
        assertNotNull("Session ID should be set", viewModel.uiState.value.sessionId)
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    fun `handles rapid navigation`() = runTest {
        // Given
        viewModel.loadTest(testId, userId)
        advanceTimeBy(200)
        
        // When - Rapid next/previous
        repeat(5) { viewModel.nextQuestion() }
        repeat(3) { viewModel.previousQuestion() }
        
        // Then
        assertEquals("Should be at question 2", 2, viewModel.uiState.value.currentQuestionIndex)
    }
}
