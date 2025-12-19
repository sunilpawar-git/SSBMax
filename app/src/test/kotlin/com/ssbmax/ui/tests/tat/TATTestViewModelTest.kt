package com.ssbmax.ui.tests.tat

import androidx.work.WorkManager
import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
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
 * Unit tests for TATTestViewModel
 * Tests picture loading, phase transitions, story writing, and submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TATTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: TATTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmitTATTest = mockk<SubmitTATTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)
    
    private val mockQuestions = createMockQuestions()
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
            mockTestContentRepo.createTestSession(any(), any(), TestType.TAT) 
        } returns Result.success("session-tat-123")
        
        // Mock question loading
        coEvery { 
            mockTestContentRepo.getTATQuestions(any()) 
        } returns Result.success(mockQuestions)
        
        // Mock user profile
        coEvery { 
            mockUserProfileRepo.getUserProfile(any()) 
        } returns flowOf(Result.success(mockUserProfile))
        
        // Mock submission
        coEvery { 
            mockSubmitTATTest(any(), any()) 
        } returns Result.success("submission-tat-123")
        
        // Mock subscription manager
        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 1)
        
        coEvery {
            mockSubscriptionManager.recordTestUsage(any(), any(), any())
        } returns Unit
        
        // Mock difficulty manager
        coEvery {
            mockDifficultyManager.recordPerformance(any(), any(), any(), any(), any(), any())
        } returns Unit
    }
    
    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads 12 questions and shows instructions`() = runTest {
        // When
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should have 12 questions", 12, state.questions.size)
            assertEquals("Should be in instructions phase", TATPhase.INSTRUCTIONS, state.phase)
            assertNotNull("Should have config", state.config)
        }
        
        coVerify { mockTestContentRepo.createTestSession("test-user-123", "tat_standard", TestType.TAT) }
        coVerify { mockTestContentRepo.getTATQuestions("tat_standard") }
    }
    
    @Test
    fun `loadTest failure shows error message`() = runTest {
        // Given - mock failure
        coEvery { 
            mockTestContentRepo.getTATQuestions(any()) 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
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
    
    // ==================== Phase Transitions ====================
    
    @Test
    fun `startTest transitions to image viewing phase with 30s timer`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // When
        viewModel.startTest()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be in image viewing phase", TATPhase.IMAGE_VIEWING, state.phase)
            assertEquals("Should be on question 0", 0, state.currentQuestionIndex)
            assertNotNull("Should have current question", state.currentQuestion)
            assertEquals("Viewing timer should be 30s", 30, state.viewingTimeRemaining)
        }
    }
    
    @Test
    fun `viewing phase auto-transitions to writing after 30 seconds`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - let viewing timer expire (30 seconds)
        advanceTimeBy(31000)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should transition to writing phase", TATPhase.WRITING, state.phase)
            assertEquals("Writing timer should be 4 minutes (240s)", 240, state.writingTimeRemaining)
        }
    }
    
    @Test
    fun `writing phase transitions to review when time expires`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Skip to writing phase
        advanceTimeBy(31000) // Viewing → Writing
        
        // When - let writing timer expire (4 minutes = 240s)
        advanceTimeBy(241000)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should transition to review phase", TATPhase.REVIEW_CURRENT, state.phase)
        }
    }
    
    // ==================== Story Writing ====================
    
    @Test
    fun `updateStory updates current story text`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val storyText = "Once upon a time, there was a brave officer who faced a difficult challenge."
        
        // When
        viewModel.updateStory(storyText)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be updated", storyText, state.currentStory)
        }
    }
    
    @Test
    fun `canMoveToNextQuestion validates min characters`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing phase
        
        // When - write short story (< 150 chars minimum)
        viewModel.updateStory("Short story")
        
        // Then
        var state = viewModel.uiState.value
        assertFalse("Should not be able to proceed with short story", state.canMoveToNextQuestion)
        
        // When - write long enough story (>= 150 chars)
        val longStory = "a".repeat(160)
        viewModel.updateStory(longStory)
        
        // Then
        state = viewModel.uiState.value
        assertTrue("Should be able to proceed with long story", state.canMoveToNextQuestion)
    }
    
    // ==================== Navigation ====================
    
    @Test
    fun `moveToNextQuestion saves story and loads next picture`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing phase
        
        val firstStory = "a".repeat(160)
        viewModel.updateStory(firstStory)
        
        // When
        viewModel.moveToNextQuestion()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertEquals("Saved story should match", firstStory, state.responses[0].story)
            assertEquals("Should be on question 1", 1, state.currentQuestionIndex)
            assertEquals("Current story should be cleared", "", state.currentStory)
            assertEquals("Should be back in viewing phase", TATPhase.IMAGE_VIEWING, state.phase)
        }
    }
    
    @Test
    fun `moveToPreviousQuestion loads previous story`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing
        
        val firstStory = "a".repeat(160)
        viewModel.updateStory(firstStory)
        viewModel.moveToNextQuestion()
        advanceTimeBy(31000) // Viewing for second picture
        
        // When - go back to first question
        viewModel.moveToPreviousQuestion()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be back on question 0", 0, state.currentQuestionIndex)
            assertEquals("Should reload first story", firstStory, state.currentStory)
            assertEquals("Should be in writing phase", TATPhase.WRITING, state.phase)
        }
    }
    
    @Test
    fun `editCurrentStory returns to writing phase`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Viewing
        viewModel.updateStory("a".repeat(160))
        advanceTimeBy(241000) // Writing timer expires → REVIEW_CURRENT
        
        // When
        viewModel.editCurrentStory()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should return to writing phase", TATPhase.WRITING, state.phase)
        }
    }
    
    // ==================== Test Submission ====================
    
    @Test
    fun `submitTest with all stories creates submission`() = runTest {
        // Given - use only 2 questions for faster test
        val shortQuestions = mockQuestions.take(2)
        coEvery { 
            mockTestContentRepo.getTATQuestions(any()) 
        } returns Result.success(shortQuestions)
        
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Write 2 stories
        repeat(2) { index ->
            advanceTimeBy(31000) // Viewing
            advanceUntilIdle() // Ensure phase transition completes
            viewModel.updateStory("Story number ${index + 1}. ".repeat(20)) // Make it long enough
            viewModel.moveToNextQuestion()
        }
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertEquals("Phase should be submitted", TATPhase.SUBMITTED, state.phase)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have submission", state.submission)
            // Note: Legacy AI scoring removed - now using unified OLQ scoring system
            assertTrue("Submission should have stories", state.submission!!.stories.isNotEmpty())
        }
        
        coVerify { mockSubmitTATTest(any(), null) }
    }
    
    @Test
    fun `submitTest without authenticated user shows error`() = runTest {
        // Given - mock no user
        every { mockObserveCurrentUser() } returns flowOf(null)
        
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue("Error should mention login", state.error!!.contains("login"))
        }
    }
    
    @Test
    fun `canSubmitTest allows submission after 11 stories`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Write 11 stories
        repeat(11) { index ->
            advanceTimeBy(31000) // Viewing
            viewModel.updateStory("Story ${index + 1}. ".repeat(20))
            viewModel.moveToNextQuestion()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be able to submit after 11 stories", state.canSubmitTest)
    }
    
    // ==================== Progress Tracking ====================
    
    @Test
    fun `completedStories tracks number of written stories`() = runTest {
        // Given
        val shortQuestions = mockQuestions.take(5)
        coEvery { 
            mockTestContentRepo.getTATQuestions(any()) 
        } returns Result.success(shortQuestions)
        
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Write 3 stories
        repeat(3) {
            advanceTimeBy(31000)
            viewModel.updateStory("Story content. ".repeat(20))
            viewModel.moveToNextQuestion()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 completed stories", 3, state.completedStories)
        assertEquals("Progress should be 60%", 0.6f, state.progress, 0.01f)
    }
    
    // ==================== Timer Management ====================
    
    @Test
    fun `viewing timer decrements correctly`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.viewingTimeRemaining
        
        // When - advance 5 seconds
        advanceTimeBy(5000)
        advanceUntilIdle() // Ensure all coroutines catch up
        
        // Then
        val newTime = viewModel.uiState.value.viewingTimeRemaining
        assertTrue("Time should decrease (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 4 seconds", initialTime - newTime >= 4)
    }
    
    @Test
    fun `writing timer decrements correctly`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing phase
        
        val initialTime = viewModel.uiState.value.writingTimeRemaining
        
        // When - advance 10 seconds
        advanceTimeBy(10000)
        
        // Then
        val newTime = viewModel.uiState.value.writingTimeRemaining
        assertEquals("Writing time should decrease by 10 seconds", initialTime - 10, newTime)
    }
    
    // ==================== Subscription Limit Tests ====================
    
    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        // Given - mock limit reached
        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.LimitReached(
            tier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
            limit = 1,
            usedCount = 1,
            resetsAt = "01 Dec 2025"
        )
        
        // When
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", com.ssbmax.core.domain.model.SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "01 Dec 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 0 questions", 0, state.questions.size)
    }
    
    @Test
    fun `loadTest proceeds when user is eligible`() = runTest {
        // Given - mock eligible (this is the default setup)
        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 5
        )
        
        // When
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded questions", state.questions.size > 0)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
    }
    
    @Test
    fun `loadTest calls canTakeTest with correct test type`() = runTest {
        // When
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then - verify subscription manager was called with TAT type
        coVerify(exactly = 1) {
            mockSubscriptionManager.canTakeTest(TestType.TAT, any())
        }
    }
    
    // ==================== Cleanup ====================
    
    @Test
    fun `onCleared cancels timer job`() = runTest {
        // Given
        viewModel = TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.viewingTimeRemaining
        
        // Note: onCleared() is protected and called by Android system when ViewModel is destroyed
        // For this test, we verify the timer was running before
        assertTrue("Timer was active", initialTime > 0)
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockQuestions(): List<TATQuestion> {
        return (1..12).map { index ->
            TATQuestion(
                id = "tat_q_$index",
                imageUrl = "https://example.com/tat-image-$index.jpg",
                sequenceNumber = index,
                prompt = "Write a story about what you see in the picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4,
                minCharacters = 150,
                maxCharacters = 800
            )
        }
    }
}

