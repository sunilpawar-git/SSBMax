package com.ssbmax

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.navigation.DeepLinkGateway
import com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher
import com.ssbmax.shared.platform.permissions.AndroidNotificationPermissionController
import com.ssbmax.shared.presentation.root.AppRootViewModel
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.theme.LocalThemeState
import com.ssbmax.shared.ui.theme.SSBMaxTheme
import com.ssbmax.shared.ui.theme.ThemeState
import com.ssbmax.ui.SSBMaxApp
import com.ssbmax.ui.permissions.LocalNotificationPermissionController
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController as SharedLocalNotificationPermissionController

class MainActivity : ComponentActivity() {

    // Deep-link seam (KMP-convergence Phase 4): both `onCreate` and
    // `onNewIntent` just forward the raw intent extra here -- parsing and
    // pending-state ownership moved to `DeepLinkGateway`/`DeepLinkParser`,
    // shared with iOS. `app`'s own `SSBMaxApp`/`SSBMaxNavGraph` (still this
    // Activity's production graph pre-Phase-5-cutover) does not consume this
    // gateway, so a real notification tap is a no-op on Android's
    // production graph until Phase 5's swap onto `SSBMaxRoot` -- an accepted
    // gap per this plan's "no production users" decision, not a regression
    // anyone can hit today. `CanaryActivity` (which does render `SSBMaxRoot`)
    // is where this seam is exercised pre-cutover.
    private val deepLinkGateway: DeepLinkGateway by inject()

    // Must be registered before this Activity reaches STARTED -- registering
    // lazily from a Composable would crash. See
    // AndroidNotificationPermissionController's class doc.
    private lateinit var notificationPermissionController: AndroidNotificationPermissionController

    // Same STARTED-lifecycle constraint as notificationPermissionController --
    // see GoogleSignInLauncher's class doc.
    private lateinit var googleSignInLauncher: AndroidGoogleSignInLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> notificationPermissionController.onPermissionResult(granted) }
        notificationPermissionController = AndroidNotificationPermissionController(this, permissionLauncher)

        val signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> googleSignInLauncher.onSignInResult(result) }
        googleSignInLauncher = AndroidGoogleSignInLauncher(this, signInLauncher)

        // Handle deep link from notification (app was closed)
        deepLinkGateway.submit(intent.getStringExtra("deepLink"))

        setContent {
            val rootViewModel: AppRootViewModel = koinViewModel()
            val currentTheme by rootViewModel.themeFlow.collectAsStateWithLifecycle()
            val themeState = remember(currentTheme) { ThemeState(currentTheme) }

            CompositionLocalProvider(
                LocalThemeState provides themeState,
                LocalNotificationPermissionController provides notificationPermissionController,
                // Same underlying controller provided under `shared`'s own CompositionLocal
                // too (Phase 5) -- ported `shared/commonMain/ui` screens (StudentHomeScreen)
                // read this one instead of the `app`-only original above; both must be live
                // since old and new screens currently coexist on the same nav graph.
                SharedLocalNotificationPermissionController provides notificationPermissionController,
                LocalGoogleSignInLauncher provides googleSignInLauncher
            ) {
                SSBMaxTheme(appTheme = currentTheme) {
                    SSBMaxApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link from notification (app was in background)
        deepLinkGateway.submit(intent.getStringExtra("deepLink"))
    }
}

