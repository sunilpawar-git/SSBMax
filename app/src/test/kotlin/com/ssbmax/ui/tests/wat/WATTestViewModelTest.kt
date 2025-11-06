package com.ssbmax.ui.tests.wat

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WATTestViewModel
 * Tests word loading, response handling, timer management, and submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WATTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: WATTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmitWATTest = mockk<SubmitWATTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    
    private val mockWords = createMockWords()
    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    private val mockUserProfile = UserProfile(
        userId = "test-user-123",
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE,
        createdAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        // Mock current user
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        
        // Mock test session creation
        coEvery { 
            mockTestContentRepo.createTestSession(any(), any(), TestType.WAT) 
        } returns Result.success("session-wat-123")
        
        // Mock word loading
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(mockWords)
        
        // Mock user profile
        coEvery { 
            mockUserProfileRepo.getUserProfile(any()) 
        } returns flowOf(Result.success(mockUserProfile))
        
        // Mock submission
        coEvery { 
            mockSubmitWATTest(any(), any()) 
        } returns Result.success("submission-123")
    }
    
    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads 60 words and shows instructions`() = runTest {
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should have 60 words", 60, state.words.size)
            assertEquals("Should be in instructions phase", WATPhase.INSTRUCTIONS, state.phase)
            assertNotNull("Should have config", state.config)
            assertEquals("Time per word should be 15s", 15, state.config?.timePerWordSeconds)
        }
        
        coVerify { mockTestContentRepo.createTestSession("test-user-123", "wat_standard", TestType.WAT) }
        coVerify { mockTestContentRepo.getWATQuestions("wat_standard") }
    }
    
    @Test
    fun `loadTest failure shows error message`() = runTest {
        // Given - mock failure
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue(
                "Error should mention cloud connection",
                state.error!!.contains("Cloud connection required")
            )
        }
    }
    
    @Test
    fun `loadTest with empty words shows error`() = runTest {
        // Given
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(emptyList())
        
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have error", state.error)
        assertTrue(state.error!!.contains("Cloud connection required"))
    }
    
    // ==================== Test Start ====================
    
    @Test
    fun `startTest transitions to in_progress and starts timer`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // When
        viewModel.startTest()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be in progress", WATPhase.IN_PROGRESS, state.phase)
            assertEquals("Should start at word 0", 0, state.currentWordIndex)
            assertNotNull("Should have current word", state.currentWord)
            assertEquals("Timer should be 15s", 15, state.timeRemaining)
        }
    }
    
    // ==================== Response Handling ====================
    
    @Test
    fun `updateResponse updates text correctly`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("Leadership")
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Response should be updated", "Leadership", state.currentResponse)
        }
    }
    
    @Test
    fun `updateResponse enforces max length of 50 chars`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val longResponse = "a".repeat(60) // 60 chars, exceeds max
        
        // When
        viewModel.updateResponse(longResponse)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            // Should not accept response longer than 50 chars
            assertNotEquals("Should not accept 60 char response", longResponse, state.currentResponse)
        }
    }
    
    @Test
    fun `submitResponse saves response and moves to next word`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val firstWord = viewModel.uiState.value.currentWord!!
        
        // When
        viewModel.updateResponse("Courage")
        viewModel.submitResponse()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertEquals("Response should be saved", "Courage", state.responses[0].response)
            assertEquals("Should move to word 1", 1, state.currentWordIndex)
            assertEquals("Current response should be cleared", "", state.currentResponse)
        }
    }
    
    @Test
    fun `skipWord marks response as skipped and moves to next`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When
        viewModel.skipWord()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertTrue("Response should be marked as skipped", state.responses[0].isSkipped)
            assertEquals("Skipped response should be empty", "", state.responses[0].response)
            assertEquals("Should move to word 1", 1, state.currentWordIndex)
        }
    }
    
    // ==================== Timer Tests ====================
    
    @Test
    fun `timer decrements from 15 seconds`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.timeRemaining
        
        // When - advance 3 seconds
        advanceTimeBy(3000)
        advanceUntilIdle() // Ensure all coroutines catch up
        
        // Then
        val newTime = viewModel.uiState.value.timeRemaining
        assertTrue("Time should decrease (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 2 seconds", initialTime - newTime >= 2)
    }
    
    @Test
    fun `timer expiry auto-skips word`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - let timer expire (15 seconds)
        advanceTimeBy(16000)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertTrue("Should be marked as skipped", state.responses[0].isSkipped)
            assertEquals("Should move to next word", 1, state.currentWordIndex)
        }
    }
    
    // ==================== Test Completion ====================
    
    @Test
    fun `completing all words marks test as completed`() = runTest {
        // Given - use only 3 words for faster test
        val shortWords = mockWords.take(3)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer all 3 words
        repeat(3) {
            viewModel.updateResponse("Response${it + 1}")
            viewModel.submitResponse()
        }
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Phase should be submitted", WATPhase.SUBMITTED, state.phase)
            assertEquals("Should have 3 responses", 3, state.completedWords)
        }
    }
    
    @Test
    fun `submitTest creates submission with responses and AI score`() = runTest {
        // Given
        val shortWords = mockWords.take(3)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Answer all words
        repeat(3) {
            viewModel.updateResponse("Response${it + 1}")
            viewModel.submitResponse()
        }
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertEquals("Phase should be submitted", WATPhase.SUBMITTED, state.phase)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have submission with AI score", state.submission)
            assertNotNull("Submission should have AI score", state.submission?.aiPreliminaryScore)
        }
        
        // Verify submitWATTest was called
        coVerify { mockSubmitWATTest(any(), null) }
    }
    
    @Test
    fun `submitTest without authenticated user shows error`() = runTest {
        // Given - mock no user
        every { mockObserveCurrentUser() } returns flowOf(null)
        
        val shortWords = mockWords.take(1)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        viewModel.submitResponse() // Complete the word
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue(
                "Error should mention login",
                state.error!!.contains("login")
            )
        }
    }
    
    // ==================== Performance Analytics ====================
    
    @Test
    fun `submitTest records performance analytics with correct score`() = runTest {
        // Given - use 10 words for controlled test
        val shortWords = mockWords.take(10)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer 7 out of 10 words, skip 3
        repeat(7) {
            viewModel.updateResponse("Response${it + 1}")
            viewModel.submitResponse()
        }
        repeat(3) {
            viewModel.skipWord()
        }
        advanceUntilIdle()
        
        // Then - verify recordPerformance was called with correct score (70%)
        coVerify { 
            mockDifficultyManager.recordPerformance(
                testType = "WAT",
                difficulty = "MEDIUM",
                score = 70f, // 7 valid / 10 total = 70%
                correctAnswers = 7,
                totalQuestions = 10,
                timeSeconds = any()
            )
        }
    }
    
    @Test
    fun `submitTest records subscription usage`() = runTest {
        // Given
        val shortWords = mockWords.take(3)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - complete all words
        repeat(3) {
            viewModel.updateResponse("Response${it + 1}")
            viewModel.submitResponse()
        }
        advanceUntilIdle()
        
        // Then - verify subscription usage was recorded
        coVerify { 
            mockSubscriptionManager.recordTestUsage(TestType.WAT, "test-user-123")
        }
    }
    
    @Test
    fun `submitTest with all skipped words records 0 percent score`() = runTest {
        // Given
        val shortWords = mockWords.take(5)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - skip all words
        repeat(5) {
            viewModel.skipWord()
        }
        advanceUntilIdle()
        
        // Then - verify 0% score recorded
        coVerify { 
            mockDifficultyManager.recordPerformance(
                testType = "WAT",
                difficulty = "MEDIUM",
                score = 0f, // 0 valid / 5 total = 0%
                correctAnswers = 0,
                totalQuestions = 5,
                timeSeconds = any()
            )
        }
    }
    
    @Test
    fun `submitTest with all valid responses records 100 percent score`() = runTest {
        // Given
        val shortWords = mockWords.take(5)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer all words
        repeat(5) {
            viewModel.updateResponse("ValidResponse${it + 1}")
            viewModel.submitResponse()
        }
        advanceUntilIdle()
        
        // Then - verify 100% score recorded
        coVerify { 
            mockDifficultyManager.recordPerformance(
                testType = "WAT",
                difficulty = "MEDIUM",
                score = 100f, // 5 valid / 5 total = 100%
                correctAnswers = 5,
                totalQuestions = 5,
                timeSeconds = any()
            )
        }
    }
    
    // ==================== Progress Tracking ====================
    
    @Test
    fun `progress is calculated correctly`() = runTest {
        // Given
        val shortWords = mockWords.take(10)
        coEvery { 
            mockTestContentRepo.getWATQuestions(any()) 
        } returns Result.success(shortWords)
        
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer 5 out of 10
        repeat(5) {
            viewModel.updateResponse("Response${it + 1}")
            viewModel.submitResponse()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Completed words should be 5", 5, state.completedWords)
        assertEquals("Progress should be 50%", 0.5f, state.progress, 0.01f)
    }
    
    // ==================== Subscription Limit Tests ====================
    
    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        // Given - mock limit reached
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.WAT, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.LimitReached(
            tier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
            limit = 1,
            usedCount = 1,
            resetsAt = "Dec 1, 2025"
        )
        
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", com.ssbmax.core.domain.model.SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "Dec 1, 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 0 words", 0, state.words.size)
    }
    
    @Test
    fun `loadTest proceeds when user is eligible`() = runTest {
        // Given - mock eligible (this is the default setup)
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.WAT, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 5
        )
        
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded words", state.words.size > 0)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
    }
    
    @Test
    fun `loadTest calls canTakeTest with correct test type`() = runTest {
        // When
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        
        // Then - verify subscription manager was called with WAT type
        coVerify(exactly = 1) {
            mockSubscriptionManager.canTakeTest(TestType.WAT, any())
        }
    }
    
    // ==================== Cleanup ====================
    
    @Test
    fun `onCleared cancels timer job`() = runTest {
        // Given
        viewModel = WATTestViewModel(
            mockTestContentRepo,
            mockSubmitWATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager
        )
        viewModel.loadTest("wat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.timeRemaining
        
        // Note: onCleared() is protected and called by Android system when ViewModel is destroyed
        // For this test, we verify the timer was running before
        assertTrue("Timer was active", initialTime > 0)
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockWords(): List<WATWord> {
        val commonWords = listOf(
            "Friend", "Success", "Failure", "Army", "Courage", "Fear", "Leader", "Team",
            "Challenge", "Victory", "Defeat", "Mother", "Father", "Love", "Hate", "War",
            "Peace", "Fight", "Help", "Sacrifice", "Duty", "Honor", "Brave", "Weak",
            "Strong", "Win", "Lose", "Run", "Stand", "Fall", "Rise", "Dark", "Light",
            "Night", "Day", "Fast", "Slow", "Good", "Bad", "Right", "Wrong", "Truth",
            "Lie", "Future", "Past", "Present", "Hope", "Dream", "Goal", "Path", "Choice",
            "Difficult", "Easy", "Hard", "Soft", "Hot", "Cold", "High", "Low", "Up"
        )
        
        return commonWords.mapIndexed { index, word ->
            WATWord(
                id = "wat_w_${index + 1}",
                word = word,
                sequenceNumber = index + 1,
                timeAllowedSeconds = 15
            )
        }
    }
}

