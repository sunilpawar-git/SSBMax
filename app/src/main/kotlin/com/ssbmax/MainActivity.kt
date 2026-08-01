package com.ssbmax

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import com.ssbmax.navigation.DeepLinkGateway
import com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher
import com.ssbmax.shared.platform.permissions.AndroidNotificationPermissionController
import com.ssbmax.shared.ui.SSBMaxRoot
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Deep-link seam (KMP-convergence Phase 4): both `onCreate` and
    // `onNewIntent` just forward the raw intent extra here -- parsing and
    // pending-state ownership live in `DeepLinkGateway`/`DeepLinkParser`,
    // shared with iOS. `SSBMaxRoot`'s `DeepLinkEffect` (Phase 5 cutover
    // target, now this Activity's production graph) is the one place that
    // drains it.
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
            CompositionLocalProvider(
                LocalNotificationPermissionController provides notificationPermissionController,
                LocalGoogleSignInLauncher provides googleSignInLauncher
            ) {
                SSBMaxRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link from notification (app was in background)
        deepLinkGateway.submit(intent.getStringExtra("deepLink"))
    }
}

