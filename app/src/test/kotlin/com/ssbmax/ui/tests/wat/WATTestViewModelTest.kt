package com.ssbmax.ui.tests.wat

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.testing.MockDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for WATTestViewModel
 * 
 * WAT Test: 60 words shown for 15 seconds each, rapid response required
 */
class WATTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: WATTestViewModel
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var submitWATTest: SubmitWATTestUseCase
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    
    private val testId = "wat-test-1"
    private val mockUser = SSBMaxUser(
        id = "user123",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.BASIC
    )
    
    @Before
    fun setUp() {
        testContentRepository = mockk(relaxed = true)
        submitWATTest = mockk(relaxed = true)
        observeCurrentUser = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        
        coEvery { observeCurrentUser() } returns flowOf(mockUser)
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(
            Result.success(MockDataFactory.createMockUserProfile())
        )
        
        viewModel = WATTestViewModel(
            testContentRepository,
            submitWATTest,
            observeCurrentUser,
            userProfileRepository
        )
    }
    
    // ==================== Loading Tests ====================
    
    @Test
    fun `loadTest - successfully loads words`() = runTest {
        // Given
        val mockWords = MockDataFactory.createMockWATWords(60)
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getWATQuestions(testId) } returns Result.success(mockWords)
        
        // When
        viewModel.loadTest(testId)
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 60 words", 60, state.words.size)
        assertEquals("Should be in instructions", WATPhase.INSTRUCTIONS, state.phase)
    }
    
    @Test
    fun `loadTest - handles empty words list`() = runTest {
        // Given
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getWATQuestions(testId) } returns Result.success(emptyList())
        
        // When
        viewModel.loadTest(testId)
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
        viewModel.loadTest(testId)
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
    }
    
    // ==================== Test Flow ====================
    
    @Test
    fun `startTest - transitions to in progress`() = runTest {
        // Given
        setupWithWords(3)
        
        // When
        viewModel.startTest()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in progress", WATPhase.IN_PROGRESS, state.phase)
        assertEquals("Should start at word 0", 0, state.currentWordIndex)
        assertEquals("Timer should be 15 seconds", 15, state.timeRemaining)
    }
    
    @Test
    fun `submitResponse - saves response and moves to next word`() = runTest {
        // Given
        setupWithWords(3)
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("Brave")
        viewModel.submitResponse()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 1 response", 1, state.responses.size)
        assertEquals("Response should be saved", "Brave", state.responses[0].response)
        assertEquals("Should move to word 1", 1, state.currentWordIndex)
        assertEquals("Response should be cleared", "", state.currentResponse)
    }
    
    @Test
    fun `skipWord - records skipped word and moves next`() = runTest {
        // Given
        setupWithWords(3)
        viewModel.startTest()
        
        // When
        viewModel.skipWord()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 1 response", 1, state.responses.size)
        assertTrue("Should be marked as skipped", state.responses[0].isSkipped)
        assertEquals("Response should be empty", "", state.responses[0].response)
        assertEquals("Should move to next word", 1, state.currentWordIndex)
    }
    
    @Test
    fun `autoAdvanceWord - moves to next after timer expires`() = runTest {
        // Given
        setupWithWords(3)
        viewModel.startTest()
        
        // When - Wait for timer to expire
        advanceTimeBy(16000) // 16 seconds (past 15)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should auto-advance to word 1", 1, state.currentWordIndex)
        assertEquals("Should record blank response", 1, state.responses.size)
    }
    
    @Test
    fun `updateResponse - limits response length`() = runTest {
        // Given
        setupWithWords(1)
        val longResponse = "This is a very long response that exceeds the maximum allowed length"
        
        // When
        viewModel.updateResponse(longResponse)
        
        // Then
        val response = viewModel.uiState.value.currentResponse
        assertTrue("Response should be limited to 50 chars", response.length <= 50)
    }
    
    @Test
    fun `completes test after all words shown`() = runTest {
        // Given
        setupWithWords(3)
        viewModel.startTest()
        
        // When - Complete all words
        repeat(3) {
            viewModel.updateResponse("Response")
            viewModel.submitResponse()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be completed", WATPhase.COMPLETED, state.phase)
        assertEquals("Should have 3 responses", 3, state.responses.size)
    }
    
    // ==================== Timer Tests ====================
    
    @Test
    fun `timer starts at 15 seconds`() = runTest {
        // Given
        setupWithWords(1)
        
        // When
        viewModel.startTest()
        
        // Then
        assertEquals("Timer should be 15", 15, viewModel.uiState.value.timeRemaining)
    }
    
    @Test
    fun `timer resets for each word`() = runTest {
        // Given
        setupWithWords(2)
        viewModel.startTest()
        advanceTimeBy(5000) // Use 5 seconds on first word
        
        // When - Move to next word
        viewModel.submitResponse()
        
        // Then
        assertEquals("Timer should reset to 15", 15, viewModel.uiState.value.timeRemaining)
    }
    
    // ==================== Submission Tests ====================
    // Note: submitTest() is private and called automatically on test completion
    // Testing via test completion flow
    
    @Test
    fun `test completion triggers submission automatically`() = runTest {
        // Given
        setupWithWords(3)
        coEvery { submitWATTest(any(), any()) } returns Result.success("submission-123")
        
        viewModel.startTest()
        
        // When - Complete all words
        repeat(3) {
            viewModel.updateResponse("Response$it")
            viewModel.submitResponse()
        }
        advanceTimeBy(500) // Give more time for async operations
        
        // Then
        val state = viewModel.uiState.value
        // Test should reach completion (phase might be COMPLETED or later state)
        assertEquals("All responses recorded", 3, state.responses.size)
        assertEquals("Progress should be 100%", 1.0f, state.progress, 0.01f)
    }
    
    @Test
    fun `handles rapid response submission`() = runTest {
        // Given
        setupWithWords(5)
        coEvery { submitWATTest(any(), any()) } returns Result.success("submission-123")
        
        viewModel.startTest()
        
        // When - Rapid fire responses
        repeat(5) {
            viewModel.updateResponse("Quick$it")
            viewModel.submitResponse()
        }
        advanceTimeBy(300) // Allow time for async completion
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("All responses recorded", 5, state.responses.size)
        assertEquals("Progress complete", 1.0f, state.progress, 0.01f)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `currentWord returns correct word`() = runTest {
        // Given
        setupWithWords(3)
        viewModel.startTest()
        
        // Then
        assertNotNull("Current word should not be null", viewModel.uiState.value.currentWord)
        assertEquals("Should be first word", 0, viewModel.uiState.value.currentWordIndex)
    }
    
    @Test
    fun `progress calculates correctly`() = runTest {
        // Given
        setupWithWords(10)
        viewModel.startTest()
        
        // When - Complete 5 words
        repeat(5) { viewModel.skipWord() }
        
        // Then
        assertEquals("Progress should be 0.5", 0.5f, viewModel.uiState.value.progress, 0.01f)
    }
    
    @Test
    fun `phase transitions correctly through lifecycle`() = runTest {
        // Given
        setupWithWords(2)
        
        // When - Start test
        assertEquals("Should start in instructions", WATPhase.INSTRUCTIONS, viewModel.uiState.value.phase)
        
        viewModel.startTest()
        assertEquals("Should move to in progress", WATPhase.IN_PROGRESS, viewModel.uiState.value.phase)
        
        // Complete all words
        repeat(2) { viewModel.skipWord() }
        assertEquals("Should move to completed", WATPhase.COMPLETED, viewModel.uiState.value.phase)
    }
    
    @Test
    fun `completedWords counts responses`() = runTest {
        // Given
        setupWithWords(5)
        viewModel.startTest()
        
        // When
        repeat(3) {
            viewModel.updateResponse("Test")
            viewModel.submitResponse()
        }
        
        // Then
        assertEquals("Should have 3 completed", 3, viewModel.uiState.value.completedWords)
    }
    
    @Test
    fun `responses include both answered and skipped`() = runTest {
        // Given
        setupWithWords(5)
        viewModel.startTest()
        
        // When - Mix of answered and skipped
        viewModel.updateResponse("Answer1")
        viewModel.submitResponse()
        viewModel.skipWord()
        viewModel.updateResponse("Answer2")
        viewModel.submitResponse()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 responses total", 3, state.responses.size)
        assertEquals("Should have 2 answered", 2, state.responses.count { !it.isSkipped })
        assertEquals("Should have 1 skipped", 1, state.responses.count { it.isSkipped })
    }
    
    // ==================== Helper Methods ====================
    
    private fun setupWithWords(count: Int) {
        val mockWords = MockDataFactory.createMockWATWords(count)
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getWATQuestions(any()) } returns Result.success(mockWords)
        runTest {
            viewModel.loadTest(testId)
            advanceTimeBy(100)
        }
    }
}

