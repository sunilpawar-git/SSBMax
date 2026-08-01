package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.splash.SplashScreen

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<SSBMaxDestinations.Splash> {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate(SSBMaxDestinations.Login) {
                    popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
                }
            },
            onNavigateToHome = { isStudent ->
                val destination = if (isStudent) {
                    SSBMaxDestinations.StudentHome
                } else {
                    SSBMaxDestinations.InstructorHome
                }
                navController.navigate(destination) {
                    popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
                }
            },
            onNavigateToRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection) {
                    popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
                }
            },
            onNavigateToProfileOnboarding = {
                // UserProfileScreen isn't ported yet (Phase 5 continuation) --
                // land on the honest placeholder rather than silently dropping
                // the onboarding step.
                navController.navigate(SSBMaxDestinations.UserProfile()) {
                    popUpTo<SSBMaxDestinations.Splash> { inclusive = true }
                }
            }
        )
    }

    composable<SSBMaxDestinations.Login> {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.Login> { inclusive = true }
                }
            },
            onNeedsRoleSelection = {
                navController.navigate(SSBMaxDestinations.RoleSelection)
            }
        )
    }

    composable<SSBMaxDestinations.RoleSelection> {
        RoleSelectionScreen(
            onRoleSelected = { role ->
                val destination = when {
                    role == UserRole.INSTRUCTOR -> SSBMaxDestinations.InstructorHome
                    else -> SSBMaxDestinations.StudentHome
                }
                navController.navigate(destination) {
                    popUpTo<SSBMaxDestinations.RoleSelection> { inclusive = true }
                }
            }
        )
    }
}
