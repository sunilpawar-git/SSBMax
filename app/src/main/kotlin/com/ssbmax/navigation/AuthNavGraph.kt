package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.ui.auth.LoginScreen
import com.ssbmax.ui.auth.RoleSelectionScreen
import com.ssbmax.ui.splash.SplashScreen

/**
 * Authentication navigation graph
 * Contains splash, login, and role selection screens
 * 
 * This graph handles the pre-authenticated user flow:
 * - Splash screen (initial app entry)
 * - Login screen (authentication)
 * - Role selection screen (for new users)
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onNavigateToHome: (isStudent: Boolean) -> Unit
) {
    // Splash Screen
    composable(SSBMaxDestinations.Splash.route) {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate(SSBMaxDestinations.Login.route) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            },
            onNavigateToHome = { isStudent ->
                onNavigateToHome(isStudent)
            },
            onNavigateToRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection.route) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            },
            onNavigateToProfileOnboarding = {
                navController.navigate(SSBMaxDestinations.UserProfile.createOnboardingRoute()) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            }
        )
    }
    
    // Login Screen
    composable(SSBMaxDestinations.Login.route) {
        LoginScreen(
            onLoginSuccess = {
                // Navigate to student home by default
                // Role-based navigation handled by onNavigateToHome callback
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.Login.route) { inclusive = true }
                }
            },
            onNeedsRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection.route)
            }
        )
    }
    
    // Role Selection Screen
    composable(SSBMaxDestinations.RoleSelection.route) {
        RoleSelectionScreen(
            onRoleSelected = { role ->
                val destination = when {
                    role.isStudent -> SSBMaxDestinations.StudentHome.route
                    role.isInstructor -> SSBMaxDestinations.InstructorHome.route
                    else -> SSBMaxDestinations.StudentHome.route
                }
                navController.navigate(destination) {
                    popUpTo(SSBMaxDestinations.RoleSelection.route) { inclusive = true }
                }
            }
        )
    }
}

