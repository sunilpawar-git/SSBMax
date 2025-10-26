package com.ssbmax.ui.tests.srt

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
 * UI tests for SRTTestScreen
 */
@HiltAndroidTest
class SRTTestScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: SRTTestViewModel
    private lateinit var uiStateFlow: MutableStateFlow<SRTTestUiState>
    private var testCompleteCalled = false
    private lateinit var testSituations: List<SRTSituation>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup test data
        testSituations = listOf(
            TestDataFactory.createTestSRTSituation(
                id = "srt-1",
                situation = "You are lost in a forest. What will you do?",
                sequenceNumber = 1
            ),
            TestDataFactory.createTestSRTSituation(
                id = "srt-2",
                situation = "You see a friend in trouble. What is your action?",
                sequenceNumber = 2
            )
        )
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            SRTTestUiState(
                isLoading = false,
                testId = "test-123",
                situations = testSituations,
                currentSituationIndex = 0,
                phase = SRTPhase.INSTRUCTIONS
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
        
        testCompleteCalled = false
    }

    @Test
    fun instructionsScreen_displaysCorrectly() {
        // Given: Instructions phase
        composeTestRule.setContent {
            SRTTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Verify instructions content
        composeTestRule
            .onNodeWithText("SRT Test", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Situation Reaction Test", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Start Test")
            .assertIsDisplayed()
    }

    @Test
    fun startButton_startsTest() {
        // Given: Instructions phase
        composeTestRule.setContent {
            SRTTestScreen(
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
    fun activePhase_displaysSituationAndResponseInput() {
        // Given: In progress phase with a situation
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 0
        )

        composeTestRule.setContent {
            SRTTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Situation number should be displayed
        composeTestRule
            .onNodeWithText("1/60", substring = true)
            .assertIsDisplayed()
        
        // Timer should be visible
        composeTestRule
            .onNodeWithText("30", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun responseInput_isDisplayedAndInteractable() {
        // Given: In progress phase
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 0,
            currentResponse = ""
        )

        composeTestRule.setContent {
            SRTTestScreen(
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
        // Given: Test in progress (15/60 situations)
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 15,
            responses = List(15) { mockk() }
        )

        composeTestRule.setContent {
            SRTTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Progress should show 15/60
        composeTestRule
            .onNodeWithText("15", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun completedTest_triggersCallback() {
        // Given: Test completed state
        val submissionId = "sub-srt-789"
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = SRTPhase.COMPLETED,
            isSubmitted = true,
            submissionId = submissionId,
            subscriptionType = SubscriptionType.FREE
        )

        composeTestRule.setContent {
            SRTTestScreen(
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
            SRTTestScreen(
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
            error = "Failed to load SRT test"
        )

        composeTestRule.setContent {
            SRTTestScreen(
                testId = "test-123",
                viewModel = mockViewModel
            )
        }

        // Then: Error message should be visible
        composeTestRule
            .onNodeWithText("Failed to load SRT test", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun backButton_showsExitDialog() {
        // Given: Test in progress
        uiStateFlow.value = uiStateFlow.value.copy(
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 20
        )

        composeTestRule.setContent {
            SRTTestScreen(
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

