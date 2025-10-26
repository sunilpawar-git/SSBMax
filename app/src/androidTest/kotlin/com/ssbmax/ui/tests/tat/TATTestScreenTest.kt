package com.ssbmax.ui.tests.tat

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.TATPhase
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for TATTestScreen
 */
@HiltAndroidTest
class TATTestScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: TATTestViewModel
    private lateinit var uiStateFlow: MutableStateFlow<TATTestUiState>
    private var testCompleteCalled = false
    private var navigateBackCalled = false
    private lateinit var testQuestions: List<TATQuestion>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup test data
        testQuestions = listOf(
            TestDataFactory.createTestTATQuestion(id = "tat-1", sequenceNumber = 1),
            TestDataFactory.createTestTATQuestion(id = "tat-2", sequenceNumber = 2),
            TestDataFactory.createTestTATQuestion(id = "tat-3", sequenceNumber = 3)
        )
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            TATTestUiState(
                isLoading = false,
                testId = "test-123",
                questions = testQuestions,
                phase = TATPhase.INSTRUCTIONS
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
        
        // Reset flags
        testCompleteCalled = false
        navigateBackCalled = false
    }

    @Test
    fun instructionsScreen_displaysCorrectly() {
        // Given: Instructions phase
        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Verify instructions content
        composeTestRule
            .onNodeWithText("TAT Test")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("TAT - Thematic Apperception Test", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Start Test")
            .assertIsDisplayed()
    }

    @Test
    fun startButton_startsTest() {
        // Given: Instructions phase
        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // When: Click Start Test button
        composeTestRule
            .onNodeWithText("Start Test")
            .performClick()

        // Then: ViewModel startTest should be called
        verify { mockViewModel.startTest() }
    }

    @Test
    fun imageViewingPhase_displaysImageAndTimer() {
        // Given: Image viewing phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.IMAGE_VIEWING,
            currentQuestionIndex = 0,
            viewingTimeRemaining = 30
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Verify image view elements
        composeTestRule
            .onNodeWithText("Picture 1/12", substring = true)
            .assertIsDisplayed()
        
        // Timer should be visible (30 seconds or counting down)
        composeTestRule
            .onNodeWithText("30", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun writingPhase_displaysStoryInputAndTimer() {
        // Given: Writing phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 0,
            currentStory = "",
            writingTimeRemaining = 240
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Verify writing view elements
        composeTestRule
            .onNodeWithText("Write Your Story", substring = true)
            .assertIsDisplayed()
        
        // Character count should be visible
        composeTestRule
            .onNodeWithText("0", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun writingPhase_storyInputWorks() {
        // Given: Writing phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 0,
            currentStory = ""
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // When: Enter story text
        // Note: We verify the text field exists and is interactable
        // Actual text input is handled by ViewModel
        composeTestRule
            .onAllNodesWithText("Start typing your story...", substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun progressIndicator_showsCurrentProgress() {
        // Given: Writing phase with 3/12 stories completed
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 3,
            responses = listOf(
                mockk(), mockk(), mockk() // 3 completed responses
            )
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Progress should show 3/12
        composeTestRule
            .onNodeWithText("3/12")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Picture 4/12", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun bottomBar_navigationButtonsWork() {
        // Given: Writing phase with valid story
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 1, // Can go back
            currentStory = "A".repeat(200) // Valid length story
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Navigation buttons should be visible
        // Previous button (exists because we're at index 1)
        composeTestRule
            .onAllNodesWithContentDescription("Previous", substring = true)
            .onFirst()
            .assertIsDisplayed()
        
        // Next button
        composeTestRule
            .onAllNodesWithContentDescription("Next", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun completedTest_triggersCallback() {
        // Given: Test completed state
        val submissionId = "sub-123"
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.SUBMITTED,
            isSubmitted = true,
            submissionId = submissionId,
            subscriptionType = SubscriptionType.FREE
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel,
                onTestComplete = { id, type ->
                    testCompleteCalled = true
                    assert(id == submissionId)
                    assert(type == SubscriptionType.FREE)
                }
            )
        }

        // Then: Callback should be triggered
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            testCompleteCalled
        }
        
        assert(testCompleteCalled) { "Test complete callback should be called" }
    }

    @Test
    fun backButton_showsExitDialog() {
        // Given: Any test phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.WRITING,
            currentQuestionIndex = 0
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // When: Click back button
        composeTestRule
            .onNodeWithContentDescription("Exit")
            .performClick()

        // Then: Exit dialog should appear
        composeTestRule
            .onNodeWithText("Exit Test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true,
            loadingMessage = "Loading test..."
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Loading indicator should be visible
        composeTestRule
            .onNodeWithText("Loading", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Given: Error state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = false,
            error = "Failed to load test questions"
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Error message should be visible
        composeTestRule
            .onNodeWithText("Failed to load test questions", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun reviewPhase_displaysStoryReview() {
        // Given: Review phase
        val testStory = "This is a test story with sufficient length to meet the minimum character requirement."
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = TATPhase.REVIEW_CURRENT,
            currentQuestionIndex = 0,
            currentStory = testStory
        )

        composeTestRule.setContent {
            TATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Story should be displayed for review
        composeTestRule
            .onNodeWithText(testStory, substring = true)
            .assertIsDisplayed()
    }
}

