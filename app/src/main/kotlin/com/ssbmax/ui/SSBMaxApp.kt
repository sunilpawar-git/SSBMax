package com.ssbmax.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.navigation.SSBMaxNavGraph
import com.ssbmax.ui.components.SSBMaxScaffold

/**
 * Main app composable
 * Manages global app state and navigation
 */
@Composable
fun SSBMaxApp() {
    val navController = rememberNavController()
    
    // TODO: Get current user from ViewModel/Repository
    // For now, use mock user
    val currentUser by remember {
        mutableStateOf(
            SSBMaxUser(
                id = "mock-user-id",
                email = "user@example.com",
                displayName = "SSB Aspirant",
                role = UserRole.STUDENT,
                isPremium = false
            )
        )
    }
    
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
            user = currentUser
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

