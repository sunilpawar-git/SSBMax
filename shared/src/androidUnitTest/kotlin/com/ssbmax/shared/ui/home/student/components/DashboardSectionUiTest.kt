package com.ssbmax.shared.ui.home.student.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/home/student/components/DashboardSectionTest.kt`
 * onto shared's [DashboardSection] (Phase 6a of the KMP-convergence plan).
 * A pure presentational composable (title + slot content, no ViewModel, no
 * Koin) -- the simplest possible port, kept 1:1 with the original.
 *
 * Lives in `androidUnitTest`, not `commonTest` -- same Robolectric-needing
 * precedent as every other Phase 6a screen/component test (`runComposeUiTest`'s
 * Android host needs Robolectric; Kotlin/Native has no equivalent runner).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardSectionUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun dashboardSection_displaysTitleAndContent() = runComposeUiTest {
        val testTitle = "Test Section"
        val testContent = "Test Content"

        setContent {
            DashboardSection(title = testTitle) {
                Text(text = testContent)
            }
        }

        onNodeWithText(testTitle).assertIsDisplayed()
        onNodeWithText(testContent).assertIsDisplayed()
    }
}
