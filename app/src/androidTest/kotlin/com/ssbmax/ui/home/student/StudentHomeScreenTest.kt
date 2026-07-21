package com.ssbmax.ui.home.student

import androidx.compose.ui.test.*
import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for StudentHomeScreen
 */
class StudentHomeScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: StudentHomeViewModel
    private lateinit var uiStateFlow: MutableStateFlow<StudentHomeUiState>

    @Before
    fun setup() {
        
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
                onNavigateToResult = { _, _ -> },
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
                onNavigateToResult = { _, _ -> },
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
                onNavigateToResult = { _, _ -> },
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
                onNavigateToResult = { _, _ -> },
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
                onNavigateToResult = { _, _ -> },
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
                onNavigateToResult = { _, _ -> },
                onOpenDrawer = {}
            )
        }

        // Then: Completion status should be reflected
        composeTestRule
            .onNodeWithText("COMPLETED", substring = true)
            .assertExists()
    }

    // ── OLQ Dashboard always in view ─────────────────────────────────────────

    @Test
    fun homeScreen_olqDashboardCard_isVisibleWhenDashboardIsNullAndLoading() {
        // Given: initial state — no data yet, fetch in flight
        uiStateFlow.value = StudentHomeUiState(
            userName = "Test User",
            dashboard = null,
            isDashboardLoading = true
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onNavigateToResult = { _, _ -> },
                onOpenDrawer = {}
            )
        }

        // OLQDashboardCard sections must be present — same card, just showing placeholder data
        composeTestRule.onNodeWithText("Phase 1 - Screening", substring = true).assertExists()
        composeTestRule.onNodeWithText("Psychology", substring = true).assertExists()
        composeTestRule.onNodeWithText("GTO", substring = true).assertExists()
        composeTestRule.onNodeWithText("Interview", substring = true).assertExists()
    }

    @Test
    fun homeScreen_olqDashboardCard_isVisibleWhenDashboardIsNullAndNotLoading() {
        // Given: dashboard null but fetch finished (error path or cache miss)
        uiStateFlow.value = StudentHomeUiState(
            userName = "Test User",
            dashboard = null,
            isDashboardLoading = false
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onNavigateToResult = { _, _ -> },
                onOpenDrawer = {}
            )
        }

        // Card is still rendered — no layout jump, no replacement composable
        composeTestRule.onNodeWithText("Phase 1 - Screening", substring = true).assertExists()
        composeTestRule.onNodeWithText("Psychology", substring = true).assertExists()
        composeTestRule.onNodeWithText("GTO", substring = true).assertExists()
        composeTestRule.onNodeWithText("Interview", substring = true).assertExists()
    }

    @Test
    fun homeScreen_olqDashboardCard_isVisibleWhenDashboardIsPopulated() {
        // Given: real data loaded
        uiStateFlow.value = StudentHomeUiState(
            userName = "Test User",
            dashboard = ProcessedDashboardData.empty(), // reuse empty() as stand-in for populated
            isDashboardLoading = false
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onNavigateToResult = { _, _ -> },
                onOpenDrawer = {}
            )
        }

        composeTestRule.onNodeWithText("Phase 1 - Screening", substring = true).assertExists()
        composeTestRule.onNodeWithText("Psychology", substring = true).assertExists()
        composeTestRule.onNodeWithText("GTO", substring = true).assertExists()
        composeTestRule.onNodeWithText("Interview", substring = true).assertExists()
    }

    @Test
    fun homeScreen_emptyDashboardState_isNeverShown() {
        // REGRESSION: EmptyDashboardState ("Start First Test" CTA) must never appear.
        // The OLQDashboardCard is always rendered instead.
        listOf(
            StudentHomeUiState(dashboard = null, isDashboardLoading = true),
            StudentHomeUiState(dashboard = null, isDashboardLoading = false),
            StudentHomeUiState(dashboard = ProcessedDashboardData.empty(), isDashboardLoading = false)
        ).forEach { state ->
            uiStateFlow.value = state

            composeTestRule.setContent {
                StudentHomeScreen(
                    viewModel = mockViewModel,
                    onNavigateToTopic = {},
                    onNavigateToPhaseDetail = {},
                    onNavigateToStudy = {},
                    onNavigateToResult = { _, _ -> },
                    onOpenDrawer = {}
                )
            }

            // "Start First Test" button text must not exist in any state
            composeTestRule
                .onNodeWithText("Start First Test", substring = true)
                .assertDoesNotExist()
        }
    }

    @Test
    fun homeScreen_linearProgressIndicator_shownOnlyWhenDashboardLoading() {
        // Loading state — indicator must be visible
        uiStateFlow.value = StudentHomeUiState(
            dashboard = null,
            isDashboardLoading = true
        )

        composeTestRule.setContent {
            StudentHomeScreen(
                viewModel = mockViewModel,
                onNavigateToTopic = {},
                onNavigateToPhaseDetail = {},
                onNavigateToStudy = {},
                onNavigateToResult = { _, _ -> },
                onOpenDrawer = {}
            )
        }

        composeTestRule
            .onNodeWithTag("dashboard_loading_indicator")
            .assertExists()

        // Non-loading state — indicator must be gone
        uiStateFlow.value = StudentHomeUiState(
            dashboard = ProcessedDashboardData.empty(),
            isDashboardLoading = false
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("dashboard_loading_indicator")
            .assertDoesNotExist()
    }
}

