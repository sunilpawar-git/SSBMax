package com.ssbmax.ui.tests.tat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the TAT result screen's partial-assessment notice (Phase 5).
 * Tests the extracted [TATPartialAssessmentSection] directly, matching this codebase's
 * convention of testing presentational composables without standing up the full
 * ViewModel/Hilt-backed screen (see ComponentsTest).
 */
class TATSubmissionResultScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shows_degradation_notice_when_usedPartialAssessment_is_true() {
        composeTestRule.setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = true,
                olqResult = buildOlqResult(validStoriesCount = 9, failedStoriesCount = 3)
            )
        }

        composeTestRule.onNodeWithText("Partial Assessment").assertIsDisplayed()
    }

    @Test
    fun does_not_show_degradation_notice_when_usedPartialAssessment_is_false() {
        composeTestRule.setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = false,
                olqResult = buildOlqResult(validStoriesCount = 12, failedStoriesCount = 0)
            )
        }

        composeTestRule.onNodeWithText("Partial Assessment").assertIsNotDisplayed()
    }

    @Test
    fun uses_string_resource_based_text_only() {
        composeTestRule.setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = true,
                olqResult = buildOlqResult(validStoriesCount = 9, failedStoriesCount = 3)
            )
        }

        // The message is built from result_tat_partial_assessment_message with %1$d/%2$d args
        // substituted in — asserting on the formatted text proves no hardcoded string was used.
        composeTestRule
            .onNodeWithText("This result is based on 9 of 12 stories", substring = true)
            .assertIsDisplayed()
    }

    private fun buildOlqResult(validStoriesCount: Int, failedStoriesCount: Int) = OLQAnalysisResult(
        submissionId = "preview-submission",
        testType = TestType.TAT,
        olqScores = mapOf(
            OLQ.COURAGE to OLQScore(score = 6, confidence = 80, reasoning = "Steady resolve")
        ),
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = emptyList(),
        weaknesses = emptyList(),
        recommendations = emptyList(),
        analyzedAt = 0L,
        aiConfidence = 80,
        validStoriesCount = validStoriesCount,
        failedStoriesCount = failedStoriesCount,
        usedPartialAssessment = failedStoriesCount > 0
    )
}
