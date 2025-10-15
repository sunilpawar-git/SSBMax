package com.ssbmax.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.navigation.SSBMaxDestinations
import kotlinx.coroutines.launch

/**
 * Main scaffold wrapper for SSBMax app
 * Integrates drawer and bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSBMaxScaffold(
    navController: NavHostController,
    user: SSBMaxUser,
    content: @Composable (drawerState: DrawerState, onOpenDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    
    val showBottomBar = shouldShowBottomBar(currentRoute)
    val showDrawer = shouldShowDrawer(currentRoute)
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (showDrawer) {
                SSBMaxDrawer(
                    user = user,
                    onNavigateToPhase = { phase ->
                        scope.launch { drawerState.close() }
                        when (phase) {
                            TestPhase.PHASE_1 -> navController.navigate(SSBMaxDestinations.Phase1Detail.route)
                            TestPhase.PHASE_2 -> navController.navigate(SSBMaxDestinations.Phase2Detail.route)
                        }
                    },
                    onNavigateToTest = { testType ->
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to specific test based on type
                    },
                    onNavigateToBatches = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to batches
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        // TODO: Navigate to settings
                    },
                    onNavigateToPendingGrading = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.InstructorGrading.route)
                    },
                    onNavigateToAnalytics = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.InstructorAnalytics.route)
                    },
                    onSwitchRole = {
                        scope.launch { drawerState.close() }
                        // TODO: Implement role switching
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        // TODO: Sign out and navigate to login
                        navController.navigate(SSBMaxDestinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        },
        gesturesEnabled = showDrawer && drawerState.isOpen
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    SSBMaxBottomBar(
                        currentRoute = currentRoute,
                        userRole = user.role,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                // Pop up to the start destination to avoid building up a large stack
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content(drawerState) {
                    scope.launch {
                        drawerState.open()
                    }
                }
            }
        }
    }
}

/**
 * Determines if bottom bar should be shown for the current route
 */
private fun shouldShowBottomBar(route: String): Boolean {
    return route in listOf(
        // Student routes
        SSBMaxDestinations.StudentHome.route,
        SSBMaxDestinations.StudentTests.route,
        SSBMaxDestinations.StudentStudy.route,
        SSBMaxDestinations.StudentProfile.route,
        // Instructor routes
        SSBMaxDestinations.InstructorHome.route,
        SSBMaxDestinations.InstructorStudents.route,
        SSBMaxDestinations.InstructorGrading.route,
        SSBMaxDestinations.InstructorAnalytics.route
    )
}

/**
 * Determines if drawer should be available for the current route
 */
private fun shouldShowDrawer(route: String): Boolean {
    return route !in listOf(
        SSBMaxDestinations.Splash.route,
        SSBMaxDestinations.Login.route,
        SSBMaxDestinations.RoleSelection.route
    )
}

