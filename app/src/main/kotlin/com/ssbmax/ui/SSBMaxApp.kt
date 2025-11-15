package com.ssbmax.ui

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

/**
 * Main app composable
 * Manages global app state and navigation
 */
@Composable
fun SSBMaxApp(
    viewModel: AppViewModel = hiltViewModel()
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
    
    val needsScaffold = currentRoute !in listOf(
        "splash",
        "login",
        "role_selection"
    )
    
    if (needsScaffold) {
        SSBMaxScaffold(
            navController = navController,
            user = user
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

