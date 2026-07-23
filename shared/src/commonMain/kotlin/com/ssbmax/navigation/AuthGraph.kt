package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.splash.SplashScreen

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable(SSBMaxDestinations.Splash.route) {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate(SSBMaxDestinations.Login.route) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            },
            onNavigateToHome = { isStudent ->
                val destination = if (isStudent) {
                    SSBMaxDestinations.StudentHome.route
                } else {
                    SSBMaxDestinations.InstructorHome.route
                }
                navController.navigate(destination) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            },
            onNavigateToRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection.route) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            },
            onNavigateToProfileOnboarding = {
                // UserProfileScreen isn't ported yet (Phase 5 continuation) --
                // land on the honest placeholder rather than silently dropping
                // the onboarding step.
                navController.navigate(SSBMaxDestinations.UserProfile.route) {
                    popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                }
            }
        )
    }

    composable(SSBMaxDestinations.Login.route) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.Login.route) { inclusive = true }
                }
            },
            onNeedsRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection.route)
            }
        )
    }

    composable(SSBMaxDestinations.RoleSelection.route) {
        RoleSelectionScreen(
            onRoleSelected = { role ->
                val destination = when {
                    role == UserRole.INSTRUCTOR -> SSBMaxDestinations.InstructorHome.route
                    else -> SSBMaxDestinations.StudentHome.route
                }
                navController.navigate(destination) {
                    popUpTo(SSBMaxDestinations.RoleSelection.route) { inclusive = true }
                }
            }
        )
    }
}
