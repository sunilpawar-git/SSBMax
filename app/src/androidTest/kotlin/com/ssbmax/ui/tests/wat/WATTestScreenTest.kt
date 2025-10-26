package com.ssbmax.ui.tests.wat

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.WATPhase
import com.ssbmax.core.domain.model.WATWord
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for WATTestScreen
 */
@HiltAndroidTest
class WATTestScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: WATTestViewModel
    private lateinit var uiStateFlow: MutableStateFlow<WATTestUiState>
    private var testCompleteCalled = false
    private lateinit var testWords: List<WATWord>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup test data
        testWords = listOf(
            TestDataFactory.createTestWATWord(id = "wat-1", word = "BRAVE", sequenceNumber = 1),
            TestDataFactory.createTestWATWord(id = "wat-2", word = "QUICK", sequenceNumber = 2),
            TestDataFactory.createTestWATWord(id = "wat-3", word = "LEADER", sequenceNumber = 3)
        )
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            WATTestUiState(
                isLoading = false,
            testId = "test-123",
            words = testWords,
            phase = WATPhase.INSTRUCTIONS
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
        
        testCompleteCalled = false
    }

    @Test
    fun instructionsScreen_displaysCorrectly() {
        // Given: Instructions phase
        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Verify instructions content
        composeTestRule
            .onNodeWithText("WAT Test")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Word Association Test", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Start Test")
            .assertIsDisplayed()
    }

    @Test
    fun startButton_startsTest() {
        // Given: Instructions phase
        composeTestRule.setContent {
            WATTestScreen(
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
    fun activePhase_displaysWordAndTimer() {
        // Given: In progress phase showing a word
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 0,
            timeRemaining = 15
        )

        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Word should be displayed
        composeTestRule
            .onNodeWithText("BRAVE")
            .assertIsDisplayed()
        
        // Timer should be visible
        composeTestRule
            .onNodeWithText("15", substring = true)
            .assertIsDisplayed()
        
        // Progress indicator
        composeTestRule
            .onNodeWithText("1/60", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun responseInput_isDisplayedAndInteractable() {
        // Given: In progress phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 0,
            currentResponse = ""
        )

        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Response input field should exist
        composeTestRule
            .onAllNodesWithText("Type your response...", substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun progressIndicator_showsCorrectProgress() {
        // Given: Test in progress (10/60 words)
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 10,
            responses = List(10) { mockk() }
        )

        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Progress should show 10/60
        composeTestRule
            .onNodeWithText("10", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun completedTest_triggersCallback() {
        // Given: Test completed state
        val submissionId = "sub-456"
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = WATPhase.COMPLETED,
            isSubmitted = true,
            submissionId = submissionId,
            subscriptionType = SubscriptionType.FREE
        )

        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel,
                onTestComplete = { id, type ->
                    testCompleteCalled = true
                    assert(id == submissionId)
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
    fun loadingState_showsLoadingIndicator() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true
        )

        composeTestRule.setContent {
            WATTestScreen(
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
            error = "Failed to load WAT test"
        )

        composeTestRule.setContent {
            WATTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Error message should be visible
        composeTestRule
            .onNodeWithText("Failed to load WAT test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun backButton_showsExitDialog() {
        // Given: Test in progress
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 5
        )

        composeTestRule.setContent {
            WATTestScreen(
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
}

