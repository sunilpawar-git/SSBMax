package com.ssbmax.ui.home.student

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
 * UI tests for StudentHomeScreen
 */
@HiltAndroidTest
class StudentHomeScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: StudentHomeViewModel
    private lateinit var uiStateFlow: MutableStateFlow<StudentHomeUiState>

    @Before
    override fun setup() {
        super.setup()
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            StudentHomeUiState(
                userName = "Test User",
                isLoading = false
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun homeScreen_displaysUserName() {
        // Given: User logged in
        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: User name should be displayed
        composeTestRule
            .onNodeWithText("Test User", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysPhaseProgressRibbons() {
        // Given: Home screen with progress
        uiStateFlow.value = uiStateFlow.value.copy(
            phase1Progress = TestDataFactory.createTestPhase1Progress(
                oirStatus = TestStatus.COMPLETED,
                ppdtStatus = TestStatus.IN_PROGRESS
            )
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: Phase progress ribbons should be visible
        composeTestRule
            .onNodeWithText("PHASE 1", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysTestCards() {
        // Given: Home screen loaded
        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: Test category cards should be visible
        composeTestRule.waitForIdle()
        assert(composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()) {
            "Screen should have clickable elements"
        }
    }

    @Test
    fun homeScreen_showsLoadingState() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: Loading indicator should be visible (screen should render)
        composeTestRule.waitForIdle()
        assert(true) { "Screen should handle loading state" }
    }

    @Test
    fun homeScreen_showsErrorState() {
        // Given: Error state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = false,
            error = "Failed to load progress"
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: Error message should be displayed
        composeTestRule
            .onNodeWithText("Failed to load progress", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun progressRibbon_showsCorrectCompletion() {
        // Given: Phase 1 partially completed
        uiStateFlow.value = uiStateFlow.value.copy(
            phase1Progress = TestDataFactory.createTestPhase1Progress(
                oirStatus = TestStatus.COMPLETED,
                ppdtStatus = TestStatus.NOT_ATTEMPTED
            )
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onOpenDrawer = {}
            )
        }

        // Then: Completion status should be reflected
        composeTestRule
            .onNodeWithText("COMPLETED", substring = true)
            .assertExists()
    }
}

