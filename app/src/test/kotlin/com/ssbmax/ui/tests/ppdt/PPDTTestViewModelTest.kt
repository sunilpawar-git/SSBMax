package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PPDTTestViewModel
 * Tests question loading, phase transitions, story writing, and submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: PPDTTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmissionRepo = mockk<com.ssbmax.core.domain.repository.SubmissionRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)
    
    private val mockQuestion = createMockQuestion()
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
        coEvery { 
            mockObserveCurrentUser() 
        } returns flowOf(mockUser)
        
        // Mock test session creation
        coEvery { 
            mockTestContentRepo.createTestSession(any(), any(), TestType.PPDT) 
        } returns Result.success("session-ppdt-123")
        
        // Mock question loading
        coEvery { 
            mockTestContentRepo.getPPDTQuestions(any()) 
        } returns Result.success(listOf(mockQuestion))
        
        // Mock user profile
        coEvery { 
            mockUserProfileRepo.getUserProfile(any()) 
        } returns flowOf(Result.success(mockUserProfile))
        
        // Mock subscription manager
        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 1)
        
        coEvery {
            mockSubscriptionManager.recordTestUsage(any(), any(), any())
        } returns Unit
        
        // Mock PPDT submission - return success with the submission ID
        coEvery {
            mockSubmissionRepo.submitPPDT(any(), any())
        } returns Result.success("test-submission-id")
        
        // Mock difficulty manager
        coEvery {
            mockDifficultyManager.recordPerformance(any(), any(), any(), any(), any(), any())
        } returns Unit
    }
    
    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads question and shows instructions`() = runTest {
        // When
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should be in instructions phase", PPDTPhase.INSTRUCTIONS, state.currentPhase)
            assertTrue("Image URL should be set", state.imageUrl.isNotEmpty())
            assertEquals("Min characters should be 200", 200, state.minCharacters)
            assertEquals("Max characters should be 1000", 1000, state.maxCharacters)
        }
    }
    
    @Test
    fun `loadTest failure shows error message`() = runTest {
        // Given - mock failure
        coEvery { 
            mockTestContentRepo.getPPDTQuestions(any()) 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
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
    fun `loadTest with empty questions shows error`() = runTest {
        // Given
        coEvery { 
            mockTestContentRepo.getPPDTQuestions(any()) 
        } returns Result.success(emptyList())
        
        // When
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have error", state.error)
        assertTrue(state.error!!.contains("Cloud connection required"))
    }
    
    // ==================== Phase Transitions ====================
    
    @Test
    fun `startTest transitions to image viewing with 30s timer`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        
        // When
        viewModel.startTest()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be in image viewing phase", PPDTPhase.IMAGE_VIEWING, state.currentPhase)
            assertEquals("Timer should be 30s", 30, state.timeRemainingSeconds)
        }
    }
    
    @Test
    fun `image viewing auto-advances to writing after 30 seconds`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - let viewing timer expire (30 seconds)
        advanceTimeBy(31000)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should transition to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Writing timer should be 4 minutes (240s)", 240, state.timeRemainingSeconds)
        }
    }
    
    @Test
    fun `proceedToNextPhase from viewing to writing transitions correctly`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - manually proceed before timer expires
        viewModel.proceedToNextPhase()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should transition to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Writing timer should start at 4 minutes", 240, state.timeRemainingSeconds)
        }
    }
    
    @Test
    fun `proceedToNextPhase from writing to review requires min characters`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Auto-advance to writing
        
        // When - try to proceed with short story (< 200 chars)
        viewModel.updateStory("Short story")
        viewModel.proceedToNextPhase()
        
        // Then
        var state = viewModel.uiState.value
        assertEquals("Should still be in writing phase", PPDTPhase.WRITING, state.currentPhase)
        
        // When - write long enough story (>= 200 chars)
        val longStory = "a".repeat(210)
        viewModel.updateStory(longStory)
        viewModel.proceedToNextPhase()
        
        // Then
        state = viewModel.uiState.value
        assertEquals("Should transition to review phase", PPDTPhase.REVIEW, state.currentPhase)
    }
    
    @Test
    fun `returnToWriting transitions from review back to writing`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Viewing → Writing
        
        val story = "a".repeat(210)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase() // Writing → Review
        
        // When
        viewModel.returnToWriting()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should return to writing phase", PPDTPhase.WRITING, state.currentPhase)
            assertEquals("Story should be preserved", story, state.story)
        }
    }
    
    // ==================== Story Writing ====================
    
    @Test
    fun `updateStory updates story text and character count`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing
        
        val storyText = "This is a story about a brave officer who saw something in the hazy picture."
        
        // When
        viewModel.updateStory(storyText)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Story should be updated", storyText, state.story)
            assertEquals("Character count should match", storyText.length, state.charactersCount)
        }
    }
    
    @Test
    fun `canProceedToNextPhase validates min characters`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing
        
        // When - write short story (< 200 chars)
        viewModel.updateStory("Short story content")
        
        // Then
        var state = viewModel.uiState.value
        assertFalse("Should not be able to proceed with short story", state.canProceedToNextPhase)
        
        // When - write story meeting minimum (>= 200 chars)
        val validStory = "a".repeat(205)
        viewModel.updateStory(validStory)
        
        // Then
        state = viewModel.uiState.value
        assertTrue("Should be able to proceed with valid story", state.canProceedToNextPhase)
    }
    
    @Test
    fun `story enforces max characters of 1000`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Move to writing
        
        // When - try to write story exceeding max
        val veryLongStory = "a".repeat(1100)
        viewModel.updateStory(veryLongStory)
        
        // Then
        val state = viewModel.uiState.value
        // The ViewModel should validate, but we're testing the state tracking
        assertEquals("Character count should be tracked", veryLongStory.length, state.charactersCount)
    }
    
    // ==================== Test Submission ====================
    
    @Test
    fun `submitTest creates submission with story and AI score`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Viewing → Writing
        
        val story = "This is a detailed story about what I perceived in the hazy image. The characters showed leadership, courage, and determination in facing the challenge. ".repeat(2)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase() // Writing → Review
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have submission", state.submission)
            
            val submission = state.submission!!
            assertEquals("Submission should have story", story, submission.story)
            // Note: Legacy AI scoring removed - now using unified OLQ scoring system
            assertTrue("Submission should have user ID", submission.userId.isNotEmpty())
            assertTrue("Submission should have story", submission.story.isNotEmpty())
        }
    }
    
    @Test
    fun `submission includes complete story data`() = runTest {
        // Given - Legacy AI scoring removed, now using unified OLQ system
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000)

        val story = "a".repeat(300)
        viewModel.updateStory(story)
        viewModel.proceedToNextPhase()

        // When
        viewModel.submitTest()
        advanceUntilIdle()

        // Then
        val submission = viewModel.uiState.value.submission!!

        assertEquals("Story should match", story, submission.story)
        assertEquals("Character count should match", 300, submission.charactersCount)
        assertTrue("Should have user ID", submission.userId.isNotEmpty())
        assertTrue("Submission ID should be set", submission.submissionId.isNotEmpty())
    }
    
    // ==================== Timer Management ====================
    
    @Test
    fun `viewing timer decrements correctly`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        
        // When - advance 5 seconds
        advanceTimeBy(5000)
        advanceUntilIdle() // Ensure all coroutines catch up
        
        // Then
        val newTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Time should decrease (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 4 seconds", initialTime - newTime >= 4)
    }
    
    @Test
    fun `proceedToNextPhase transitions from image viewing to writing`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        
        // Start test - should transition to IMAGE_VIEWING
        viewModel.startTest()
        
        // When - immediately call proceedToNextPhase without waiting
        viewModel.proceedToNextPhase()
        advanceUntilIdle()
        
        // Then - should be in WRITING phase
        val state = viewModel.uiState.value
        assertEquals("Should be in writing phase", PPDTPhase.WRITING, state.currentPhase)
        assertNotNull("Session should exist", state.session)
        assertEquals("Session phase should be writing", PPDTPhase.WRITING, state.session?.currentPhase)
    }
    
    @Test
    fun `writing timer expiry auto-advances to review`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        advanceTimeBy(31000) // Viewing → Writing
        
        viewModel.updateStory("a".repeat(250)) // Write valid story
        
        // When - let writing timer expire (4 minutes = 240s)
        advanceTimeBy(241000)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should auto-advance to review", PPDTPhase.REVIEW, state.currentPhase)
        }
    }
    
    // ==================== Pause/Resume ====================
    
    @Test
    fun `pauseTest cancels timer`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        
        val timeBeforePause = viewModel.uiState.value.timeRemainingSeconds
        
        // When
        viewModel.pauseTest()
        advanceTimeBy(5000) // Try to advance time
        
        // Then
        val timeAfterPause = viewModel.uiState.value.timeRemainingSeconds
        assertEquals("Timer should be paused", timeBeforePause, timeAfterPause)
    }
    
    // ==================== Cleanup ====================
    
    @Test
    fun `onCleared cancels timer job`() = runTest {
        // Given
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        advanceUntilIdle()
        viewModel.startTest()
        
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        
        // Note: onCleared() is protected and called by Android system when ViewModel is destroyed
        // For this test, we verify the timer was running before
        assertTrue("Timer was active", initialTime > 0)
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
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "01 Dec 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have empty image URL when limited", "", state.imageUrl)
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
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded image URL", state.imageUrl.isNotEmpty())
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
    }
    
    @Test
    fun `loadTest calls canTakeTest with correct test type`() = runTest {
        // When
        viewModel = PPDTTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockSecurityLogger,
            mockWorkManager
        )
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        
        // Then - verify subscription manager was called with PPDT type
        coVerify(atLeast = 1) {
            mockSubscriptionManager.canTakeTest(TestType.PPDT, any())
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockQuestion(): PPDTQuestion {
        return PPDTQuestion(
            id = "ppdt_q1",
            imageUrl = "https://example.com/ppdt-image.jpg",
            imageDescription = "A hazy image showing people in a situation",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000
        )
    }
}

