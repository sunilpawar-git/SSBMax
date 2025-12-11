package com.ssbmax.ui

import android.util.Log
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.navigation.SSBMaxNavGraph
import com.ssbmax.ui.components.SSBMaxScaffold
import com.ssbmax.utils.ErrorLogger

private const val TAG = "SSBMaxApp"

/** Auth screens that don't show scaffold and must be passed before deep link navigation */
private val AUTH_SCREENS = setOf("splash", "login", "role_selection")

/**
 * Main app composable
 * Manages global app state and navigation
 */
@Composable
fun SSBMaxApp(
    viewModel: AppViewModel = hiltViewModel(),
    pendingDeepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Get current authenticated user from ViewModel
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Fallback to mock user if not authenticated (for preview/development)
    val user = currentUser ?: SSBMaxUser(
        id = "mock-user-id",
        email = "user@example.com",
        displayName = "SSB Aspirant",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        subscription = null
    )
    
    // Check if we're on a route that needs the scaffold
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Handle deep link navigation after user is authenticated and past auth screens
    LaunchedEffect(pendingDeepLink, currentRoute, currentUser) {
        Log.d(TAG, "Deep link check - pendingDeepLink: $pendingDeepLink, " +
                "currentRoute: $currentRoute, currentUser: ${currentUser?.email}")
        
        if (pendingDeepLink != null && currentUser != null && currentRoute != null) {
            // Only navigate if we're past the auth screens
            val isOnAuthScreen = currentRoute in AUTH_SCREENS
            
            if (!isOnAuthScreen) {
                Log.d(TAG, "✅ Navigating to deep link: $pendingDeepLink (from route: $currentRoute)")
                try {
                    navController.navigate(pendingDeepLink) {
                        // Don't pop the home screen, just add on top
                        launchSingleTop = true
                    }
                    Log.d(TAG, "✅ Deep link navigation successful!")
                    onDeepLinkHandled()
                } catch (e: Exception) {
                    ErrorLogger.log(e, "Failed to navigate to deep link: $pendingDeepLink")
                    onDeepLinkHandled()
                }
            } else {
                Log.d(TAG, "⏳ Waiting to pass auth screen (current: $currentRoute)")
            }
        }
    }
    
    val needsScaffold = currentRoute !in AUTH_SCREENS
    
    if (needsScaffold) {
        SSBMaxScaffold(
            navController = navController,
            user = user,
            onSignOut = viewModel::signOut
        ) { drawerState, onOpenDrawer ->
            SSBMaxNavGraph(
                navController = navController,
                onOpenDrawer = onOpenDrawer
            )
        }
    } else {
        SSBMaxNavGraph(
            navController = navController,
            onOpenDrawer = {}
        )
    }
}

