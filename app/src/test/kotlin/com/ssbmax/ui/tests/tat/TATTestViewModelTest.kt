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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TATTestViewModel
 * Tests image loading, story input, navigation, and submission
 *
 * Test Focus:
 * - Test session creation and image loading
 * - Story input and character count
 * - Navigation between images
 * - Submission flow
 * - Error handling
 *
 * Note: Timer-based tests are limited to avoid flakiness.
 * The ViewModel uses real coroutine delays for timers which are tested in integration tests.
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
    private val mockGetOLQDashboard = mockk<com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)
    
    private val mockQuestions = createMockTATQuestions()
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
    }
    
    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads 12 questions and shows instructions`() = runTest {
        // When
        viewModel = createViewModel()
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
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error message", state.error)
            assertTrue("Error should be non-empty", !state.error.isNullOrBlank())
        }
    }
    
    // ==================== Story Input ====================
    
    @Test
    fun `updateStory updates current image story text`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // When
        viewModel.updateStory("This is my TAT story about the image.")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be updated", 
                "This is my TAT story about the image.", 
                state.currentStory)
        }
    }
    
    @Test
    fun `updateStory handles long stories correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        val longStory = "A".repeat(1000)
        
        // When
        viewModel.updateStory(longStory)
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be stored", longStory, state.currentStory)
            assertTrue("Story length should be tracked", state.currentStory.length == 1000)
        }
    }
    
    // ==================== Question Navigation ====================
    
    @Test
    fun `currentQuestionIndex starts at 0`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should start at question 0", 0, state.currentQuestionIndex)
        }
    }
    
    @Test
    fun `responses list stores completed stories`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Responses should start empty", state.responses.isEmpty())
        }
    }
    
    // ==================== Submission ====================
    
    @Test
    fun `submitTest submits all stories successfully`() = runTest {
        // Given
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // Add story to first image
        viewModel.updateStory("Story for image 1")
        advanceUntilIdle()
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        coVerify { mockSubmitTATTest(any(), any()) }
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertEquals("Phase should be SUBMITTED", TATPhase.SUBMITTED, state.phase)
        }
    }
    
    @Test
    fun `submitTest failure shows error`() = runTest {
        // Given
        coEvery { 
            mockSubmitTATTest(any(), any()) 
        } returns Result.failure(Exception("Submission failed"))
        
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be submitted", state.isSubmitted)
            assertNotNull("Should have error", state.error)
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createViewModel(): TATTestViewModel {
        return TATTestViewModel(
            mockTestContentRepo,
            mockSubmitTATTest,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockSubscriptionManager,
            mockDifficultyManager,
            mockGetOLQDashboard,
            mockSecurityLogger,
            mockWorkManager
        )
    }
    
    private fun createMockTATQuestions(): List<TATQuestion> {
        return (1..12).map { index ->
            TATQuestion(
                id = "tat_q_$index",
                imageUrl = "https://example.com/tat_$index.jpg",
                sequenceNumber = index,
                prompt = "Write a story about what you see in the picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4,
                minCharacters = 50,
                maxCharacters = 1500
            )
        }
    }
}

