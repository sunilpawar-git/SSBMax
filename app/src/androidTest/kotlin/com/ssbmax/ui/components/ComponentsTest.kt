package com.ssbmax.ui.components

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.*
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import com.ssbmax.ui.home.student.PhaseProgressRibbon
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * UI tests for reusable components
 */
@HiltAndroidTest
class ComponentsTest : BaseComposeTest() {

    @Test
    fun phaseProgressRibbon_displaysPhase1() {
        // Given: Phase 1 progress
        val phase1Progress = TestDataFactory.createTestPhase1Progress(
            oirStatus = TestStatus.COMPLETED,
            ppdtStatus = TestStatus.IN_PROGRESS
        )

        composeTestRule.setContent {
            PhaseProgressRibbon(
                phase1Progress = phase1Progress,
                phase2Progress = null,
                onPhaseClick = {},
                onTopicClick = {}
            )
        }

        // Then: Phase 1 should be displayed
        composeTestRule
            .onNodeWithText("PHASE 1", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun phaseProgressRibbon_displaysPhase2() {
        // Given: Phase 2 progress
        val phase2Progress = TestDataFactory.createTestPhase2Progress(
            psychologyStatus = TestStatus.NOT_ATTEMPTED
        )

        composeTestRule.setContent {
            PhaseProgressRibbon(
                phase1Progress = null,
                phase2Progress = phase2Progress,
                onPhaseClick = {},
                onTopicClick = {}
            )
        }

        // Then: Phase 2 should be displayed
        composeTestRule
            .onNodeWithText("PHASE 2", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testContentLoadingState_displays() {
        // Given: Loading state component
        composeTestRule.setContent {
            TestContentLoadingState(message = "Loading test data...")
        }

        // Then: Loading message should be displayed
        composeTestRule
            .onNodeWithText("Loading test data...")
            .assertIsDisplayed()
    }

    @Test
    fun testContentErrorState_displays() {
        // Given: Error state component
        composeTestRule.setContent {
            TestContentErrorState(
                error = "Failed to load test",
                onRetry = {}
            )
        }

        // Then: Error message and retry button should be displayed
        composeTestRule
            .onNodeWithText("Failed to load test", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Retry", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testContentErrorState_retryButtonWorks() {
        // Given: Error state with retry callback
        var retryClicked = false
        
        composeTestRule.setContent {
            TestContentErrorState(
                error = "Network error",
                onRetry = { retryClicked = true }
            )
        }

        // When: Click retry button
        composeTestRule
            .onNodeWithText("Retry", substring = true)
            .performClick()

        // Then: Retry callback should be called
        composeTestRule.waitForIdle()
        assert(retryClicked) { "Retry callback should be called" }
    }

    @Test
    fun phaseProgressRibbon_clickablePhases() {
        // Given: Phase progress with click handlers
        var phaseClicked = false
        val phase1Progress = TestDataFactory.createTestPhase1Progress()

        composeTestRule.setContent {
            PhaseProgressRibbon(
                phase1Progress = phase1Progress,
                phase2Progress = null,
                onPhaseClick = { phaseClicked = true },
                onTopicClick = {}
            )
        }

        // When: Click on phase card
        composeTestRule
            .onAllNodes(hasClickAction())
            .onFirst()
            .performClick()

        // Then: Phase click callback should be called
        composeTestRule.waitForIdle()
        assert(phaseClicked) { "Phase click callback should be called" }
    }
}

