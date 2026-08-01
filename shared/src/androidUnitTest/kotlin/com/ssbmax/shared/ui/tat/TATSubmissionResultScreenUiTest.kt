package com.ssbmax.shared.ui.tat

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/tests/tat/TATSubmissionResultScreenTest.kt`
 * onto shared's `TATSubmissionResultScreen.kt` (Phase 6a of the
 * KMP-convergence plan) -- the result-side half of the "one full
 * test-taking flow" the plan calls out as priority, alongside
 * [TATTestScreenUiTest].
 *
 * Tests the extracted `TATPartialAssessmentSection` directly (same
 * pre-cutover convention this test already followed: presentational
 * composables tested without standing up the full ViewModel-backed screen).
 * `TATPartialAssessmentSection` is `internal`, same module as this test, so
 * no visibility change was needed to port it. Field-for-field identical
 * [OLQAnalysisResult] shape to the pre-cutover original -- verified by
 * reading the shared domain model, no adaptation needed here.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class TATSubmissionResultScreenUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun shows_degradation_notice_when_usedPartialAssessment_is_true() = runComposeUiTest {
        setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = true,
                olqResult = buildOlqResult(validStoriesCount = 9, failedStoriesCount = 3)
            )
        }

        onNodeWithText("Partial Assessment").assertIsDisplayed()
    }

    @Test
    fun does_not_show_degradation_notice_when_usedPartialAssessment_is_false() = runComposeUiTest {
        setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = false,
                olqResult = buildOlqResult(validStoriesCount = 12, failedStoriesCount = 0)
            )
        }

        onNodeWithText("Partial Assessment").assertIsNotDisplayed()
    }

    @Test
    fun uses_string_resource_based_text_only() = runComposeUiTest {
        setContent {
            TATPartialAssessmentSection(
                usesPartialAssessment = true,
                olqResult = buildOlqResult(validStoriesCount = 9, failedStoriesCount = 3)
            )
        }

        // The message is built from result_tat_partial_assessment_message with %1$d/%2$d args
        // substituted in -- asserting on the formatted text proves no hardcoded string was used.
        onNodeWithText("This result is based on 9 of 12 stories", substring = true).assertIsDisplayed()
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
