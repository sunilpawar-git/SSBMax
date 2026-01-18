package com.ssbmax.ui.home.student.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.ssbmax.core.domain.model.dashboard.OLQDashboardData
import com.ssbmax.testing.BaseComposeTest
import org.junit.Test

class SectionComponentsTest : BaseComposeTest() {

    @Test
    fun phase1Section_displaysOirAndPpdt() {
        composeTestRule.setContent {
            Phase1Section(
                results = OLQDashboardData.Phase1Results(
                    oirResult = null,
                    ppdtResult = null,
                    ppdtOLQResult = null
                ),
                onNavigateToResult = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
        composeTestRule.onNodeWithText("OIR").assertIsDisplayed()
        composeTestRule.onNodeWithText("PPDT").assertIsDisplayed()
    }

    @Test
    fun psychologySection_displaysPsychTests() {
        composeTestRule.setContent {
            PsychologySection(
                results = OLQDashboardData.Phase2Results(
                    tatResult = null,
                    watResult = null,
                    srtResult = null,
                    sdResult = null,
                    gtoResults = emptyMap(),
                    interviewResult = null
                ),
                onNavigateToResult = { _, _ -> }
            )
        }
        
        composeTestRule.onNodeWithText("Psychology").assertIsDisplayed()
        composeTestRule.onNodeWithText("TAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("WAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("SRT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Self Description").assertIsDisplayed()
    }
}
