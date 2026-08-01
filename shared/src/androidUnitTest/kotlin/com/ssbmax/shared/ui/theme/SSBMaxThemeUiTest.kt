package com.ssbmax.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.AppTheme
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * CMP UI test for Phase 2 of the KMP-convergence plan's stated gate:
 * "toggle theme in Settings on both platforms, confirm the running app
 * re-themes." [SSBMaxRoot] drives this in production by collecting
 * [com.ssbmax.shared.presentation.root.AppRootViewModel.themeFlow] into a
 * `appTheme` parameter passed to [SSBMaxTheme]; this test isolates
 * [SSBMaxTheme] itself and asserts that changing that parameter actually
 * recomposes with a different resolved [MaterialTheme.colorScheme].
 *
 * Lives in `androidUnitTest`, not `commonTest`: `runComposeUiTest`'s Android
 * host-side implementation needs Robolectric's shadow of `android.os.Build`
 * (confirmed by a real `NullPointerException` on `Build.FINGERPRINT` before
 * this was added), and `@RunWith(RobolectricTestRunner::class)` is
 * JUnit4-only with no Kotlin/Native equivalent, so it can't live in a
 * source set iOS also compiles. Porting a full commonTest UI-test suite
 * (Robolectric on Android, the Native test harness on iOS) is Phase 6a's
 * larger undertaking, not this one test's job.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class SSBMaxThemeUiTest {

    @Test
    fun `theme change recomposes with a different color scheme`() = runComposeUiTest {
        var appTheme by mutableStateOf(AppTheme.LIGHT)
        var observedPrimary: Color? = null

        setContent {
            SSBMaxTheme(appTheme = appTheme) {
                observedPrimary = MaterialTheme.colorScheme.primary
            }
        }

        waitForIdle()
        val lightPrimary = observedPrimary

        appTheme = AppTheme.DARK
        waitForIdle()
        val darkPrimary = observedPrimary

        assertNotNull(lightPrimary)
        assertNotNull(darkPrimary)
        assertNotEquals(lightPrimary, darkPrimary)
    }
}
