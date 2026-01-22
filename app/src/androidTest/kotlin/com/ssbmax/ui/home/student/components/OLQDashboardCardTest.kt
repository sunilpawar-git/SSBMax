package com.ssbmax.ui.home.student.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.ssbmax.core.domain.model.dashboard.OLQDashboardData
import com.ssbmax.core.domain.usecase.dashboard.CacheMetadata
import com.ssbmax.core.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.testing.BaseComposeTest
import org.junit.Test

class OLQDashboardCardTest : BaseComposeTest() {

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

        // Verify section titles (using resource strings or substrings if possible)
        // Note: These should match the strings in the sections
        composeTestRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
        composeTestRule.onNodeWithText("Psychology").assertIsDisplayed()
        composeTestRule.onNodeWithText("GTO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interview").assertIsDisplayed()
    }
}
