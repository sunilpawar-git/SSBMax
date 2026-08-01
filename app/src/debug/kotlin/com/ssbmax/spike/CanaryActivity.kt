package com.ssbmax.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher
import com.ssbmax.shared.platform.permissions.AndroidNotificationPermissionController
import com.ssbmax.shared.ui.SSBMaxRoot
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController

/**
 * Phase 5 canary validation harness (debug builds only). Renders `shared`'s
 * real, full [SSBMaxRoot] (all 54 ported destinations, wrapped in its
 * drawer/bottom-nav chrome and real branded theme) so the ported screens are
 * reachable/testable end-to-end without wiring them into the production
 * app's own nav graph (`NavGraph.kt`/`AuthNavGraph.kt`/`StudentNavGraph.kt`/
 * `InstructorNavGraph.kt`/`SharedNavGraph.kt` are untouched by this class).
 *
 * Phase 2 of the KMP-convergence plan pointed this at [SSBMaxRoot] directly
 * (previously a bare `MaterialTheme { }` + hand-assembled scaffold/nav-host
 * pair) so canary and the real Phase 5 cutover target are literally the
 * same code, validated ahead of time.
 *
 * Launch via:
 *   adb shell am start -n com.ssbmax.debug/com.ssbmax.spike.CanaryActivity
 *
 * Supersedes the Phase 0 `KmpSpikeActivity` (deleted this phase, along with
 * its single-screen `SpikeApp` demo composable) — that activity's own doc
 * comment already called for its removal once Phase 5's real UI port
 * landed; this class is the real replacement, not a second harness kept
 * alongside it.
 *
 * `SSBMaxApplication.onCreate()` already starts Koin with `appModules`
 * (which includes `sharedModule`), so no separate Koin bootstrap is needed
 * here — same reasoning as `MainActivity`.
 *
 * Both `ActivityResultLauncher`s below must be registered before this
 * Activity reaches `STARTED` (Android platform constraint) — same pattern
 * as `MainActivity`, which provides the same two CompositionLocals to the
 * production nav graph. Without them, `LoginScreen`/`StudentHomeScreen`
 * would crash on `LocalGoogleSignInLauncher`/`LocalNotificationPermissionController`'s
 * "no value provided" default.
 */
class CanaryActivity : ComponentActivity() {

    private lateinit var notificationPermissionController: AndroidNotificationPermissionController
    private lateinit var googleSignInLauncher: AndroidGoogleSignInLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> notificationPermissionController.onPermissionResult(granted) }
        notificationPermissionController = AndroidNotificationPermissionController(this, permissionLauncher)

        val signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> googleSignInLauncher.onSignInResult(result) }
        googleSignInLauncher = AndroidGoogleSignInLauncher(this, signInLauncher)

        setContent {
            CompositionLocalProvider(
                LocalNotificationPermissionController provides notificationPermissionController,
                LocalGoogleSignInLauncher provides googleSignInLauncher
            ) {
                SSBMaxRoot()
            }
        }
    }
}
