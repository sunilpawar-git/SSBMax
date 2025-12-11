package com.ssbmax

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.ui.SSBMaxApp
import com.ssbmax.ui.theme.LocalThemeState
import com.ssbmax.ui.theme.SSBMaxTheme
import com.ssbmax.utils.DeepLinkParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Deep link state that can be observed by Compose
    private var pendingDeepLink by mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link from notification (app was closed)
        pendingDeepLink = extractDeepLinkFromIntent(intent)
        
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeState = mainViewModel.themeState
            
            CompositionLocalProvider(LocalThemeState provides themeState) {
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

