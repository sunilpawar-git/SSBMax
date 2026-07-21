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
import androidx.compose.runtime.setValue
import com.ssbmax.shared.platform.auth.AndroidGoogleSignInLauncher
import com.ssbmax.shared.platform.permissions.AndroidNotificationPermissionController
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import org.koin.compose.viewmodel.koinViewModel
import com.ssbmax.ui.SSBMaxApp
import com.ssbmax.ui.permissions.LocalNotificationPermissionController
import com.ssbmax.ui.theme.LocalThemeState
import com.ssbmax.ui.theme.SSBMaxTheme
import com.ssbmax.utils.DeepLinkParser

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
            val mainViewModel: MainViewModel = koinViewModel()
            val themeState = mainViewModel.themeState

            CompositionLocalProvider(
                LocalThemeState provides themeState,
                LocalNotificationPermissionController provides notificationPermissionController,
                LocalGoogleSignInLauncher provides googleSignInLauncher
            ) {
                SSBMaxTheme(appTheme = themeState.currentTheme) {
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

