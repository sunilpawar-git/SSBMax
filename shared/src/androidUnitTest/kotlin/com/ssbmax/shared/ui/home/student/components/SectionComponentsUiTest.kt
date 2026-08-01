package com.ssbmax.shared.ui.home.student.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/home/student/components/SectionComponentsTest.kt`
 * onto shared's [Phase1Section]/[PsychologySection] (Phase 6a of the
 * KMP-convergence plan). Pure presentational composables (data in, no
 * ViewModel/Koin).
 *
 * Two real string-value drifts from the pre-cutover original, both verified
 * against the current `strings.xml` rather than assumed:
 * - `dashboard_phase_1` is "PHASE 1", not the old hardcoded "Phase 1 - Screening".
 * - `dashboard_test_self_desc` is "Self Desc", not the old hardcoded "Self Description".
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class SectionComponentsUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun phase1Section_displaysOirAndPpdt() = runComposeUiTest {
        setContent {
            Phase1Section(
                results = OLQDashboardData.Phase1Results(
                    oirResult = null,
                    ppdtResult = null,
                    ppdtOLQResult = null
                ),
                onNavigateToResult = { _, _ -> }
            )
        }

        onNodeWithText("PHASE 1", substring = true).assertIsDisplayed()
        onNodeWithText("OIR").assertIsDisplayed()
        onNodeWithText("PPDT").assertIsDisplayed()
    }

    @Test
    fun psychologySection_displaysPsychTests() = runComposeUiTest {
        setContent {
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

        onNodeWithText("Psychology").assertIsDisplayed()
        onNodeWithText("TAT").assertIsDisplayed()
        onNodeWithText("WAT").assertIsDisplayed()
        onNodeWithText("SRT").assertIsDisplayed()
        onNodeWithText("Self Desc").assertIsDisplayed()
    }
}
