package com.ssbmax.ui.tests.oir

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.*
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for OIRTestScreen
 */
@HiltAndroidTest
class OIRTestScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: OIRTestViewModel
    private lateinit var uiStateFlow: MutableStateFlow<OIRTestUiState>
    private var testCompleteCalled = false
    private lateinit var testQuestions: List<OIRQuestion>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup test data
        testQuestions = listOf(
            TestDataFactory.createTestOIRQuestion(
                id = "oir-1",
                questionNumber = 1,
                questionText = "What is 2+2?",
                options = listOf(
                    OIROption("opt1", "3"),
                    OIROption("opt2", "4"),
                    OIROption("opt3", "5"),
                    OIROption("opt4", "6")
                ),
                correctAnswerId = "opt2"
            ),
            TestDataFactory.createTestOIRQuestion(
                id = "oir-2",
                questionNumber = 2,
                questionText = "What is the capital of India?",
                options = listOf(
                    OIROption("opt1", "Mumbai"),
                    OIROption("opt2", "Delhi"),
                    OIROption("opt3", "Kolkata"),
                    OIROption("opt4", "Chennai")
                ),
                correctAnswerId = "opt2"
            )
        )
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            OIRTestUiState(
                isLoading = false,
                totalQuestions = testQuestions.size,
                currentQuestionIndex = 0
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
        
        testCompleteCalled = false
    }

    @Test
    fun instructionsScreen_displaysCorrectly() {
        // Given: Initial state (instructions would be shown before starting)
        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
            )
        }

        // Then: Verify OIR test title is displayed
        composeTestRule
            .onNodeWithText("OIR Test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun questionScreen_displaysQuestionAndOptions() {
        // Given: Active test with a question
        uiStateFlow.value = uiStateFlow.value.copy(
            currentQuestionIndex = 0,
            totalQuestions = 2
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
            )
        }

        // Then: Question number should be displayed
        composeTestRule
            .onNodeWithText("1/2", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun progressIndicator_showsCorrectProgress() {
        // Given: Test in progress (Question 5 of 50)
        uiStateFlow.value = uiStateFlow.value.copy(
            currentQuestionIndex = 4,
            totalQuestions = 50
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
            )
        }

        // Then: Progress should show 5/50
        composeTestRule
            .onNodeWithText("5/50", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun timerDisplay_showsTimeRemaining() {
        // Given: Test with time limit
        uiStateFlow.value = uiStateFlow.value.copy(
            timeRemainingSeconds = 300 // 5 minutes
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
            )
        }

        // Then: Timer should be visible
        composeTestRule
            .onAllNodesWithText("5:00", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
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
            error = "Failed to load OIR test"
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
            )
        }

        // Then: Error message should be visible
        composeTestRule
            .onNodeWithText("Failed to load OIR test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun completedTest_triggersCallback() {
        // Given: Test completed state
        val sessionId = "session-oir-123"
        uiStateFlow.value = uiStateFlow.value.copy(
            isCompleted = true,
            sessionId = sessionId,
            subscriptionType = SubscriptionType.FREE
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { id, type ->
                    testCompleteCalled = true
                    assert(id == sessionId)
                },
                onNavigateBack = {}
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
        // Given: Test in progress
        uiStateFlow.value = uiStateFlow.value.copy(
            currentQuestionIndex = 9,
            totalQuestions = 50
        )

        composeTestRule.setContent {
            OIRTestScreen(
                viewModel = mockViewModel,
                onTestComplete = { _, _ -> },
                onNavigateBack = {}
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
}

