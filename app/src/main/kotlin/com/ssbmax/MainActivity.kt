package com.ssbmax

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher
import com.ssbmax.shared.platform.permissions.AndroidNotificationPermissionController
import com.ssbmax.shared.presentation.root.AppRootViewModel
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.theme.LocalThemeState
import com.ssbmax.shared.ui.theme.SSBMaxTheme
import com.ssbmax.shared.ui.theme.ThemeState
import com.ssbmax.ui.SSBMaxApp
import com.ssbmax.ui.permissions.LocalNotificationPermissionController
import com.ssbmax.utils.DeepLinkParser
import org.koin.compose.viewmodel.koinViewModel
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController as SharedLocalNotificationPermissionController

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Deep link state that can be observed by Compose
    private var pendingDeepLink by mutableStateOf<String?>(null)

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
        pendingDeepLink = extractDeepLinkFromIntent(intent)

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
                    SSBMaxApp(
                        pendingDeepLink = pendingDeepLink,
                        onDeepLinkHandled = { pendingDeepLink = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link from notification (app was in background)
        extractDeepLinkFromIntent(intent)?.let { route ->
            Log.d(TAG, "onNewIntent received deep link route: $route")
            pendingDeepLink = route
        }
    }
    
    /**
     * Extract deep link from notification intent
     * Returns the navigation route (without ssbmax:// prefix)
     */
    private fun extractDeepLinkFromIntent(intent: Intent?): String? {
        val deepLink = intent?.getStringExtra("deepLink")
        Log.d(TAG, "Intent extras: ${intent?.extras}")
        Log.d(TAG, "Deep link from intent: $deepLink")
        
        val route = DeepLinkParser.parseToRoute(deepLink)
        Log.d(TAG, "Parsed navigation route: $route")
        return route
    }
}

