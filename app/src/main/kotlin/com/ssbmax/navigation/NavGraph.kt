package com.ssbmax.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Main navigation graph for SSBMax app
 * Orchestrates all sub-navigation graphs
 * 
 * Navigation Architecture:
 * - AuthNavGraph: Splash, Login, Role Selection
 * - StudentNavGraph: Student-specific screens
 * - InstructorNavGraph: Instructor-specific screens
 * - SharedNavGraph: Common screens for all users
 */
@Composable
fun SSBMaxNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDestination: String = SSBMaxDestinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ========================
        // AUTHENTICATION FLOW
        // ========================
        authNavGraph(
            navController = navController,
            onNavigateToHome = { isStudent ->
                val destination = if (isStudent) {
                    SSBMaxDestinations.StudentHome.route
                } else {
                    SSBMaxDestinations.InstructorHome.route
                }
                navController.navigate(destination) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            }
        )
        
        // ========================
        // STUDENT FLOW
        // ========================
        studentNavGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )
        
        // ========================
        // INSTRUCTOR FLOW
        // ========================
        instructorNavGraph(
            navController = navController,
            onOpenDrawer = onOpenDrawer
        )
        
        // ========================
        // SHARED SCREENS
        // ========================
        sharedNavGraph(
            navController = navController
        )
    }
}

// Note: All route definitions have been extracted to separate navigation graph files:
// - AuthNavGraph.kt: Authentication flow (79 lines)
// - StudentNavGraph.kt: Student-specific screens (110 lines)
// - InstructorNavGraph.kt: Instructor-specific screens (122 lines)
// - SharedNavGraph.kt: Common screens (484 lines)
//
// Total: 795 lines distributed across 4 focused files
// Previous: 764 lines in single monolithic file
