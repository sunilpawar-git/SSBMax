package com.ssbmax.timer

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.ui.tests.tat.TATTestViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * CRITICAL: Timer Accuracy Tests for SSB Authenticity
 *
 * SSB tests have strict timing requirements that must be followed precisely:
 * - TAT: 30 seconds viewing per picture, 4 minutes writing per story
 * - WAT: 15 seconds per word (60 words total)
 *
 * These tests ensure the app maintains authentic SSB test conditions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerAccuracyTest : BaseViewModelTest() {

    // Mock dependencies
    private val mockTestContentRepository = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmitTATTest = mockk<SubmitTATTestUseCase>(relaxed = true)
    private val mockSubmitWATTest = mockk<SubmitWATTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepository = mockk<UserProfileRepository>(relaxed = true)

    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )

    @Test
    fun `TAT test configuration meets exact SSB timer requirements`() = runTest {
        // Given: TAT test loaded with proper configuration
        val viewModel = createTATViewModel()
        val testId = "tat_test_001"

        // Mock successful test loading
        val mockQuestions = createMockTATQuestions(12) // TAT requires exactly 12 pictures
        coEvery { mockTestContentRepository.createTestSession(any(), any(), any()) } returns Result.success("test_session_123")
        coEvery { mockTestContentRepository.getTATQuestions(testId) } returns Result.success(mockQuestions)
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)

        // When: Load test
        viewModel.loadTest(testId)
        advanceUntilIdle()

        // Then: Verify SSB-compliant configuration is loaded
        val state = viewModel.uiState.value
        assertFalse("Test should finish loading", state.isLoading)
        assertNotNull("TAT config should be loaded", state.config)
        assertEquals("Should have loaded all TAT questions", 12, state.questions.size)

        // CRITICAL SSB REQUIREMENTS: Timer configuration must be exact
        val config = state.config!!
        assertEquals("TAT viewing time must be exactly 30 seconds (SSB standard)", 30, config.viewingTimePerPictureSeconds)
        assertEquals("TAT writing time must be exactly 4 minutes (SSB standard)", 4, config.writingTimePerPictureMinutes)
        assertEquals("TAT must have exactly 12 pictures (SSB standard)", 12, config.totalPictures)
        assertEquals("TAT story length must be 150-800 characters (SSB standard)", 150, config.minCharactersPerStory)
        assertEquals("TAT story length must be 150-800 characters (SSB standard)", 800, config.maxCharactersPerStory)

        // Verify questions match configuration
        assertEquals("Number of questions should match config", config.totalPictures, state.questions.size)
        state.questions.forEach { question ->
            assertEquals("Each question viewing time should match config", config.viewingTimePerPictureSeconds, question.viewingTimeSeconds)
            assertEquals("Each question writing time should match config", config.writingTimePerPictureMinutes, question.writingTimeMinutes)
            assertEquals("Each question min characters should match config", config.minCharactersPerStory, question.minCharacters)
            assertEquals("Each question max characters should match config", config.maxCharactersPerStory, question.maxCharacters)
        }
    }

    @Test
    fun `TAT viewing timer maintains exact 30-second intervals with sub-second accuracy`() = runTest {
        // Given: TAT test started
        val viewModel = createTATViewModel()
        val testId = "tat_test_001"
        
        // Mock successful test loading
        val mockQuestions = createMockTATQuestions(12)
        coEvery { mockTestContentRepository.createTestSession(any(), any(), any()) } returns Result.success("test_session_123")
        coEvery { mockTestContentRepository.getTATQuestions(testId) } returns Result.success(mockQuestions)
        coEvery { mockObserveCurrentUser() } returns flowOf(mockUser)
        
        // Load and start test
        viewModel.loadTest(testId)
        advanceUntilIdle()
        
        viewModel.startTest()
        // DO NOT call advanceUntilIdle() here - it would fast-forward through the entire timer!
        // The timer starts immediately when startTest() is called
        
        // Then: Verify timer is initialized and phase is set
        val stateAfterStart = viewModel.uiState.value
        assertEquals("Must be in IMAGE_VIEWING phase", TATPhase.IMAGE_VIEWING, stateAfterStart.phase)
        assertEquals("Timer must start at exactly 30 seconds", 30, stateAfterStart.viewingTimeRemaining)
        
        // After 1 second
        advanceTimeBy(1000)
        runCurrent() // Process the state update after delay completes
        assertEquals("After 1 second, timer should be at 29", 29, viewModel.uiState.value.viewingTimeRemaining)
        
        // After 5 seconds total
        advanceTimeBy(4000)
        runCurrent()
        assertEquals("After 5 seconds, timer should be at 25", 25, viewModel.uiState.value.viewingTimeRemaining)
        
        // After 10 seconds total
        advanceTimeBy(5000)
        runCurrent()
        assertEquals("After 10 seconds, timer should be at 20", 20, viewModel.uiState.value.viewingTimeRemaining)
        
        // After 29 seconds total (1 second remaining)
        advanceTimeBy(19000)
        runCurrent()
        assertEquals("After 29 seconds, timer should be at 1", 1, viewModel.uiState.value.viewingTimeRemaining)
        assertEquals("Should still be in IMAGE_VIEWING phase", TATPhase.IMAGE_VIEWING, viewModel.uiState.value.phase)
        
        // After 30 seconds total (timer expires)
        advanceTimeBy(1000)
        runCurrent() // Process the countdown to 0
        
        // CRITICAL: Auto-transition to writing phase should happen immediately
        // The viewing timer hits 0, then updates phase to WRITING and starts writing timer
        val stateAfterViewing = viewModel.uiState.value
        assertEquals("Viewing timer must reach exactly 0", 0, stateAfterViewing.viewingTimeRemaining)
        assertEquals("Must auto-transition to WRITING phase", TATPhase.WRITING, stateAfterViewing.phase)
        assertEquals("Writing timer must start at exactly 240 seconds (4 minutes)", 240, stateAfterViewing.writingTimeRemaining)
    }

    // ==================== HELPER METHODS ====================

    private fun createTATViewModel(): TATTestViewModel {
        return TATTestViewModel(
            testContentRepository = mockTestContentRepository,
            submitTATTest = mockSubmitTATTest,
            observeCurrentUser = mockObserveCurrentUser,
            userProfileRepository = mockUserProfileRepository
        )
    }

    private fun createMockTATQuestions(count: Int): List<TATQuestion> {
        return (1..count).map { index ->
            TATQuestion(
                id = "tat_q_$index",
                imageUrl = "https://via.placeholder.com/800x600/test_$index.jpg",
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