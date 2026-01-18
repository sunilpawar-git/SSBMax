package com.ssbmax.ui.home.student.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.ssbmax.testing.BaseComposeTest
import org.junit.Test

class DashboardSectionTest : BaseComposeTest() {

    @Test
    fun dashboardSection_displaysTitleAndContent() {
        val testTitle = "Test Section"
        val testContent = "Test Content"

        composeTestRule.setContent {
            DashboardSection(
                title = testTitle
            ) {
                Text(text = testContent)
            }
        }

        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(testContent).assertIsDisplayed()
    }
}
