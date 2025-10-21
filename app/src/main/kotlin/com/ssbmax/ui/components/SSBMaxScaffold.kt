package com.ssbmax.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.navigation.SSBMaxDestinations
import com.ssbmax.ui.components.drawer.SSBMaxDrawer
import com.ssbmax.ui.profile.UserProfileViewModel
import kotlinx.coroutines.flow.first
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
    
    // Drawer UI state
    var phase1Expanded by remember { mutableStateOf(false) }
    var phase2Expanded by remember { mutableStateOf(false) }
    
    // Load user profile using ViewModel
    val profileViewModel: UserProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val userProfile = profileUiState.profile
    val isLoadingProfile = profileUiState.isLoading
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (showDrawer) {
                ModalDrawerSheet {
                    SSBMaxDrawer(
                        userProfile = userProfile,
                        isLoadingProfile = isLoadingProfile,
                        currentRoute = currentRoute,
                        phase1Expanded = phase1Expanded,
                        phase2Expanded = phase2Expanded,
                        onNavigateToHome = {
                            scope.launch { drawerState.close() }
                            navController.navigate(SSBMaxDestinations.StudentHome.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToTopic = { topicId ->
                            scope.launch { drawerState.close() }
                            navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
                        },
                        onNavigateToSSBOverview = {
                            scope.launch { drawerState.close() }
                            // Navigate to SSB Overview (will be created in Phase 6)
                            navController.navigate("ssb_overview")
                        },
                        onNavigateToMyBatches = {
                            scope.launch { drawerState.close() }
                            // Navigate to My Batches
                            navController.navigate(SSBMaxDestinations.JoinBatch.route)
                        },
                        onNavigateToSettings = {
                            scope.launch { drawerState.close() }
                            navController.navigate(SSBMaxDestinations.Settings.route)
                        },
                        onEditProfile = {
                            scope.launch { drawerState.close() }
                            navController.navigate(SSBMaxDestinations.UserProfile.route)
                        },
                        onTogglePhase1 = {
                            phase1Expanded = !phase1Expanded
                        },
                        onTogglePhase2 = {
                            phase2Expanded = !phase2Expanded
                        },
                        onSignOut = {
                            scope.launch { drawerState.close() }
                            // TODO: Sign out via AuthRepository
                            navController.navigate(SSBMaxDestinations.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        },
        gesturesEnabled = showDrawer
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
 * Topic screens have their own internal bottom navigation
 * Home screens and other screens don't use bottom nav
 */
private fun shouldShowBottomBar(route: String): Boolean {
    return false
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

