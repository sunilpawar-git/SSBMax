package com.ssbmax.ui.components.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel
import org.junit.Rule
import org.junit.Test

/**
 * Regression coverage for a bug where the shared OLQ result template silently discarded the
 * real analysis-failure message and never offered a retry action, leaving every OLQ-scored
 * test's failure screen (TAT/WAT/SRT/SD/PPDT/GD/GPE/Lecturette) with only a generic message
 * and a "back to home" button.
 */
class UnifiedOLQResultTemplateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_real_error_message_instead_of_generic_default_when_error_is_set() {
        composeTestRule.setContent {
            UnifiedOLQResultTemplate(
                uiState = fakeUiState(error = "Network timeout while fetching results"),
                testTitle = "Test Result",
                submissionConfirmationContent = {},
                onNavigateHome = {}
            )
        }

        composeTestRule.onNodeWithText("Network timeout while fetching results").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "AI analysis could not be completed. Please contact support or retake the test."
        ).assertIsNotDisplayed()
    }

    @Test
    fun shows_retry_action_when_onRetry_provided_and_analysis_failed() {
        var retried = false
        composeTestRule.setContent {
            UnifiedOLQResultTemplate(
                uiState = fakeUiState(error = "Analysis failed"),
                testTitle = "Test Result",
                submissionConfirmationContent = {},
                onNavigateHome = {},
                onRetry = { retried = true }
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assert(retried) { "Expected onRetry callback to be invoked when Retry is tapped" }
    }

    @Test
    fun does_not_show_retry_action_when_onRetry_is_null() {
        composeTestRule.setContent {
            UnifiedOLQResultTemplate(
                uiState = fakeUiState(error = "Analysis failed"),
                testTitle = "Test Result",
                submissionConfirmationContent = {},
                onNavigateHome = {},
                onRetry = null
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsNotDisplayed()
    }

    private fun fakeUiState(error: String?) = object : UnifiedResultUiState {
        override val isLoading: Boolean = false
        override val error: String? = error
        override val analysisStatus: AnalysisStatus = AnalysisStatus.FAILED
        override val olqResult: OLQAnalysisResult? = null
        override val ssbRecommendation: SSBRecommendationUIModel? = null
    }
}
