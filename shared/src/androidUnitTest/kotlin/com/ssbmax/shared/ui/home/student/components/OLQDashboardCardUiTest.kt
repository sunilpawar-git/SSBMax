package com.ssbmax.shared.ui.home.student.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.dashboard.OLQDashboardData
import com.ssbmax.shared.domain.usecase.dashboard.CacheMetadata
import com.ssbmax.shared.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/home/student/components/OLQDashboardCardTest.kt`
 * onto shared's [OLQDashboardCard] (Phase 6a of the KMP-convergence plan).
 * A pure presentational composable (data in, no ViewModel/Koin), so this is
 * a near-verbatim port -- only the import package moved.
 *
 * Section title assertions use "Phase 1 - Screening" from the original test
 * -- replaced with "PHASE 1" here: [OLQDashboardCard]'s `Phase1Section` now
 * uses the `dashboard_phase_1` resource string, whose current value is
 * "PHASE 1" (verified in `strings.xml`), not the old hardcoded
 * "Phase 1 - Screening" Android copy.
 *
 * Lives in `androidUnitTest`, not `commonTest` -- same Robolectric-needing
 * precedent as every other Phase 6a screen/component test.
 */
// The card's 4 sections + optional strengths/focus banners lay out taller
// than Robolectric's default ~320dp-tall window (same class of issue as
// StudentHomeScreenUiTest, confirmed empirically) -- a tall qualifier avoids
// depending on that default.
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp")
class OLQDashboardCardUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun olqDashboardCard_displaysAllSections() = runComposeUiTest {
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

        setContent { OLQDashboardCard(processedData = dummyData) }

        onNodeWithText("PHASE 1", substring = true).assertIsDisplayed()
        onNodeWithText("Psychology").assertIsDisplayed()
        onNodeWithText("GTO").assertIsDisplayed()
        // "Interview" matches twice -- the section title and the chip label
        // both use it ("dashboard_interview"/"dashboard_test_interview" are
        // both literally "Interview") -- so assert existence, not a single
        // unique displayed node.
        onAllNodesWithText("Interview").onFirst().assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_showsLinearProgressIndicator_whenIsLoadingTrue() = runComposeUiTest {
        setContent {
            OLQDashboardCard(processedData = ProcessedDashboardData.empty(), isLoading = true)
        }

        onNodeWithTag("dashboard_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_allSectionsVisibleWhileLoading() = runComposeUiTest {
        setContent {
            OLQDashboardCard(processedData = ProcessedDashboardData.empty(), isLoading = true)
        }

        // Card structure must be intact even during loading -- no layout jump.
        onNodeWithText("PHASE 1", substring = true).assertIsDisplayed()
        onNodeWithText("Psychology").assertIsDisplayed()
        onNodeWithText("GTO").assertIsDisplayed()
        // "Interview" matches twice -- the section title and the chip label
        // both use it ("dashboard_interview"/"dashboard_test_interview" are
        // both literally "Interview") -- so assert existence, not a single
        // unique displayed node.
        onAllNodesWithText("Interview").onFirst().assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_doesNotShowLinearProgressIndicator_whenIsLoadingFalse_withData() = runComposeUiTest {
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

        setContent { OLQDashboardCard(processedData = dummyData, isLoading = false) }

        onNodeWithTag("dashboard_loading_indicator").assertDoesNotExist()
    }

    @Test
    fun olqDashboardCard_doesNotShowLinearProgressIndicator_withEmptyDataAndNotLoading() = runComposeUiTest {
        setContent {
            OLQDashboardCard(processedData = ProcessedDashboardData.empty(), isLoading = false)
        }

        onNodeWithTag("dashboard_loading_indicator").assertDoesNotExist()
    }

    @Test
    fun olqDashboardCard_rendersWithEmptyData_withoutCrash() = runComposeUiTest {
        setContent { OLQDashboardCard(processedData = ProcessedDashboardData.empty()) }

        onNodeWithText("PHASE 1", substring = true).assertIsDisplayed()
        onNodeWithText("Psychology").assertIsDisplayed()
        onNodeWithText("GTO").assertIsDisplayed()
        // "Interview" matches twice -- the section title and the chip label
        // both use it ("dashboard_interview"/"dashboard_test_interview" are
        // both literally "Interview") -- so assert existence, not a single
        // unique displayed node.
        onAllNodesWithText("Interview").onFirst().assertIsDisplayed()
    }

    @Test
    fun olqDashboardCard_showsZeroOfFifteenProgress_withEmptyData() = runComposeUiTest {
        setContent { OLQDashboardCard(processedData = ProcessedDashboardData.empty()) }

        // Progress badge must show "0/15" when no tests are completed
        // (OLQDashboard.totalTests defaults to 15, ProcessedDashboardData.empty()'s
        // completedTestsCount is 0).
        onNodeWithText("0/15", substring = true).assertIsDisplayed()
    }
}
