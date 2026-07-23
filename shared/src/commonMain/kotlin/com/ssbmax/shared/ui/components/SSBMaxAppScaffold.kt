package com.ssbmax.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ssbmax.navigation.BottomNavItem
import com.ssbmax.navigation.SSBMaxDestinations
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.presentation.profile.UserProfileViewModel
import com.ssbmax.shared.ui.components.drawer.SSBMaxDrawer
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * KMP port of the Android `app/.../ui/components/SSBMaxScaffold.kt` — the
 * app-wide chrome (nav drawer + bottom nav bar) wrapping every authenticated
 * screen in [com.ssbmax.navigation.SSBMaxNavHost]. This is the structural
 * gap named at the top of Phase 5's progress notes ("nav drawer + bottom
 * nav bar not ported") -- every screen already existed and was individually
 * reachable, but nothing let a logged-in user navigate BETWEEN them via
 * persistent chrome the way the Android app does. This closes that gap for
 * the routes ported into `shared/commonMain/ui` so far.
 *
 * Deviations from the Android original, named explicitly:
 * - The Android `SSBMaxScaffold` takes a `user: SSBMaxUser` parameter
 *   (sourced from `app`'s own `AppViewModel`, an `androidx.lifecycle.ViewModel`
 *   not available in `commonMain`). This port reads [AuthRepository.currentUser]
 *   directly instead -- one fewer indirection, same data, and consistent with
 *   this phase's existing pattern of injecting repositories/plain-class
 *   ViewModels straight into `commonMain` screens via `koinInject()`.
 * - `shouldShowBottomBar` in the Android original is hardcoded `return false`
 *   (confirmed by reading the source -- the Android bottom bar is wired but
 *   never actually shown). This port makes it real for the routes matching a
 *   registered [BottomNavItem], rather than porting forward the disabled
 *   state -- see [SSBMaxBottomBar]'s own class doc.
 * - Sign-out navigates via `popUpTo(0) { inclusive = true }` to
 *   [SSBMaxDestinations.Login], matching the Android original's own
 *   `onSignOut` callback shape exactly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSBMaxAppScaffold(
    navController: NavHostController,
    content: @Composable (onOpenDrawer: () -> Unit) -> Unit
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""

    val showChrome = currentRoute !in AUTH_ROUTES

    if (!showChrome) {
        // Splash/Login/RoleSelection render full-bleed, no drawer/bottom bar --
        // matches the Android original's `shouldShowDrawer` exclusion list.
        content({})
        return
    }

    val authRepository: AuthRepository = koinInject()
    val profileViewModel: UserProfileViewModel = koinInject()

    DisposableEffect(Unit) {
        onDispose { profileViewModel.close() }
    }

    val currentUser by authRepository.currentUser.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val userRole = currentUser?.role ?: UserRole.STUDENT

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var phase1Expanded by remember { mutableStateOf(false) }
    var phase2Expanded by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SSBMaxDrawer(
                    userProfile = profileUiState.profile,
                    isLoadingProfile = profileUiState.isLoading,
                    currentRoute = currentRoute,
                    phase1Expanded = phase1Expanded,
                    phase2Expanded = phase2Expanded,
                    onNavigateToHome = {
                        scope.launch { drawerState.close() }
                        val home = if (userRole.isInstructor && !userRole.isStudent) {
                            SSBMaxDestinations.InstructorHome.route
                        } else {
                            SSBMaxDestinations.StudentHome.route
                        }
                        navController.navigate(home) {
                            // popUpTo the concrete home route, not
                            // `graph.startDestinationId` (Splash) -- Splash
                            // isn't guaranteed to be in the back stack for an
                            // already-authenticated user, a real bug this
                            // codebase already found and fixed once in
                            // `TestResultHandler.kt` (see
                            // `NavigationArchitectureTest`'s
                            // "should use StudentHome for popUpTo not
                            // startDestinationId" guard) -- not repeating it
                            // here.
                            popUpTo(home) { saveState = true }
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
                        navController.navigate(SSBMaxDestinations.SSBOverview.route)
                    },
                    onNavigateToMyBatches = {
                        scope.launch { drawerState.close() }
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
                    onTogglePhase1 = { phase1Expanded = !phase1Expanded },
                    onTogglePhase2 = { phase2Expanded = !phase2Expanded },
                    onSignOut = {
                        scope.launch {
                            drawerState.close()
                            authRepository.signOut()
                            navController.navigate(SSBMaxDestinations.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    SSBMaxBottomBar(
                        currentRoute = currentRoute,
                        userRole = userRole,
                        onNavigate = { route ->
                            val home = if (userRole.isInstructor && !userRole.isStudent) {
                                SSBMaxDestinations.InstructorHome.route
                            } else {
                                SSBMaxDestinations.StudentHome.route
                            }
                            navController.navigate(route) {
                                // See onNavigateToHome above for why this
                                // pops to the concrete home route rather
                                // than `graph.startDestinationId`.
                                popUpTo(home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                content({ scope.launch { drawerState.open() } })
            }
        }
    }
}

private val AUTH_ROUTES = setOf(
    SSBMaxDestinations.Splash.route,
    SSBMaxDestinations.Login.route,
    SSBMaxDestinations.RoleSelection.route
)

private val BOTTOM_NAV_ROUTES = setOf(
    BottomNavItem.StudentHome.route,
    BottomNavItem.StudentTests.route,
    BottomNavItem.StudentSubmissions.route,
    BottomNavItem.StudentStudy.route,
    BottomNavItem.StudentProfile.route,
    BottomNavItem.InstructorHome.route,
    BottomNavItem.InstructorStudents.route,
    BottomNavItem.InstructorGrading.route,
    BottomNavItem.InstructorAnalytics.route
)
