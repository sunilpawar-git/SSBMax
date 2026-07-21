package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.splash.SplashScreen

/**
 * Authentication navigation graph
 * Contains splash, login, and role selection screens
 *
 * This graph handles the pre-authenticated user flow:
 * - Splash screen (initial app entry)
 * - Login screen (authentication)
 * - Role selection screen (for new users)
 *
 * Phase 5: these three composables now come from `:shared`'s
 * `commonMain/ui` (Compose Multiplatform), not the old Android-only
 * `app/.../ui/{splash,auth}` versions (both deleted — grep-confirmed this
 * was their last real caller). The Android-only `NavGraphBuilder` extension
 * function shape stays as-is (no KMP equivalent needed here — only the
 * screens inside needed porting, not this file's own plumbing).
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

