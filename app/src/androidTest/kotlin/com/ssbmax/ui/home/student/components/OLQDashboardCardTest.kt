package com.ssbmax.ui.home.student.components

import androidx.compose.ui.test.*
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import com.ssbmax.shared.domain.usecase.dashboard.CacheMetadata
import com.ssbmax.shared.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.testing.BaseComposeTest
import org.junit.Test

class OLQDashboardCardTest : BaseComposeTest() {

    // ── Existing regression test ─────────────────────────────────────────────

    @Test
    fun olqDashboardCard_displaysAllSections() {
        val dummyData = ProcessedDashboardData(
            dashboard = OLQDashboardData(
                userId = "123",
                phase1Results = OLQDashboardData.Phase1Results(null, null, null),
                phase2Results = OLQDashboardData.Phase2Results(null, null, null, null, emptyMap(), null)
            ),
            averageOLQScores = emptyMap(),
            topOLQs = emptyList(),
            improvementOLQs = emptyList(),
            overallAverageScore = 8.0f,
            cacheMetadata = CacheMetadata(true, 0, false)
        )

        composeTestRule.setContent {
            OLQDashboardCard(processedData = dummyData)
        }

        composeTestRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
        composeTestRule.onNodeWithText("Psychology").assertIsDisplayed()
        composeTestRule.onNodeWithText("GTO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interview").assertIsDisplayed()
    }

    // ── isLoading = true ─────────────────────────────────────────────────────

    @Test
    fun olqDashboardCard_showsLinearProgressIndicator_whenIsLoadingTrue() {
        composeTestRule.setContent {
            OLQDashboardCard(
                processedData = ProcessedDashboardData.empty(),
                isLoading = true
            )
        }

        composeTestRule
            .onNodeWithTag("dashboard_loading_indicator")
            .assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_allSectionsVisibleWhileLoading() {
        composeTestRule.setContent {
            OLQDashboardCard(
                processedData = ProcessedDashboardData.empty(),
                isLoading = true
            )
        }

        // Card structure must be intact even during loading — no layout jump
        composeTestRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
        composeTestRule.onNodeWithText("Psychology").assertIsDisplayed()
        composeTestRule.onNodeWithText("GTO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interview").assertIsDisplayed()
    }

    // ── isLoading = false ────────────────────────────────────────────────────

    @Test
    fun olqDashboardCard_doesNotShowLinearProgressIndicator_whenIsLoadingFalse_withData() {
        val dummyData = ProcessedDashboardData(
            dashboard = OLQDashboardData(
                userId = "123",
                phase1Results = OLQDashboardData.Phase1Results(null, null, null),
                phase2Results = OLQDashboardData.Phase2Results(null, null, null, null, emptyMap(), null)
            ),
            averageOLQScores = emptyMap(),
            topOLQs = emptyList(),
            improvementOLQs = emptyList(),
            overallAverageScore = null,
            cacheMetadata = CacheMetadata(false, 100, false)
        )

        composeTestRule.setContent {
            OLQDashboardCard(processedData = dummyData, isLoading = false)
        }

        composeTestRule
            .onNodeWithTag("dashboard_loading_indicator")
            .assertDoesNotExist()
    }

    @Test
    fun olqDashboardCard_doesNotShowLinearProgressIndicator_withEmptyDataAndNotLoading() {
        composeTestRule.setContent {
            OLQDashboardCard(
                processedData = ProcessedDashboardData.empty(),
                isLoading = false
            )
        }

        composeTestRule
            .onNodeWithTag("dashboard_loading_indicator")
            .assertDoesNotExist()
    }

    // ── ProcessedDashboardData.empty() renders correctly ────────────────────

    @Test
    fun olqDashboardCard_rendersWithEmptyData_withoutCrash() {
        composeTestRule.setContent {
            OLQDashboardCard(processedData = ProcessedDashboardData.empty())
        }

        // Card structure must be present with all 4 sections
        composeTestRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
        composeTestRule.onNodeWithText("Psychology").assertIsDisplayed()
        composeTestRule.onNodeWithText("GTO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interview").assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_showsZeroOfFifteenProgress_withEmptyData() {
        composeTestRule.setContent {
            OLQDashboardCard(processedData = ProcessedDashboardData.empty())
        }

        // Progress badge must show "0/15" when no tests are completed
        composeTestRule
            .onNodeWithText("0/15", substring = true)
            .assertIsDisplayed()
    }
}
