package com.ssbmax.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ssbmax.navigation.SSBMaxDestinations
import com.ssbmax.navigation.isAuthScreen
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.presentation.profile.UserProfileViewModel
import com.ssbmax.shared.ui.components.drawer.SSBMaxDrawer
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * KMP port of the Android app scaffold — the app-wide navigation drawer
 * wrapping every authenticated screen in [com.ssbmax.navigation.SSBMaxNavHost].
 * The drawer is the product's persistent navigation chrome; test-specific tabs
 * remain owned by their individual screens.
 *
 * Deviations from the pre-KMP Android scaffold, named explicitly:
 * - The Android scaffold takes a `user: SSBMaxUser` parameter
 *   (sourced from `app`'s own `AppViewModel`, an `androidx.lifecycle.ViewModel`
 *   not available in `commonMain`). This port reads [AuthRepository.currentUser]
 *   directly instead -- one fewer indirection, same data, and consistent with
 *   this phase's existing pattern of injecting repositories/plain-class
 *   ViewModels straight into `commonMain` screens via `koinInject()`.
 * - There is no app-level bottom navigation bar. This is a product decision:
 *   the drawer is the primary navigation, while `TopicScreen`'s tab row and
 *   test-specific bottom controls remain local to their owning screens.
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
    val currentDestination = currentBackStackEntry?.destination

    // Type-safe routes (KMP-convergence Phase 3d) no longer expose the old
    // "student/home"-shaped strings via `destination.route` -- that now holds
    // the pattern generated from the destination's KClass. `currentRoute`
    // here is reconstructed from the legacy `SSBMaxDestinations.*.route`
    // constants purely for `SSBMaxDrawer`/`DrawerContent`'s existing
    // `.contains(...)` highlighting checks, which don't touch the nav graph.
    val currentRoute = when {
        currentDestination?.hasRoute<SSBMaxDestinations.StudentHome>() == true -> SSBMaxDestinations.StudentHome.route
        currentDestination?.hasRoute<SSBMaxDestinations.InstructorHome>() == true -> SSBMaxDestinations.InstructorHome.route
        currentDestination?.hasRoute<SSBMaxDestinations.Settings>() == true -> SSBMaxDestinations.Settings.route
        else -> ""
    }

    if (currentDestination.isAuthScreen()) {
        // Splash/Login/RoleSelection render full-bleed, no drawer/bottom bar --
        // matches the Android original's `shouldShowDrawer` exclusion list.
        content({})
        return
    }

    val authRepository: AuthRepository = koinInject()
    val profileViewModel: UserProfileViewModel = koinViewModel()

    val currentUser by authRepository.currentUser.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val userRole = currentUser?.role ?: UserRole.STUDENT

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var phase1Expanded by remember { mutableStateOf(false) }
    var phase2Expanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SSBMaxDrawer(
                    userProfile = profileUiState.profile,
                    subscriptionTier = profileUiState.subscriptionTier,
                    isLoadingProfile = profileUiState.isLoading,
                    currentRoute = currentRoute,
                    phase1Expanded = phase1Expanded,
                    phase2Expanded = phase2Expanded,
                    onNavigateToHome = {
                        scope.launch { drawerState.close() }
                        val home = if (userRole.isInstructor && !userRole.isStudent) {
                            SSBMaxDestinations.InstructorHome
                        } else {
                            SSBMaxDestinations.StudentHome
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
                        navController.navigate(SSBMaxDestinations.TopicScreen(topicId))
                    },
                    onNavigateToSSBOverview = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.SSBOverview)
                    },
                    onNavigateToMyBatches = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.JoinBatch)
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.Settings)
                    },
                    onEditProfile = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SSBMaxDestinations.UserProfile())
                    },
                    onTogglePhase1 = { phase1Expanded = !phase1Expanded },
                    onTogglePhase2 = { phase2Expanded = !phase2Expanded },
                    onSignOut = {
                        scope.launch {
                            drawerState.close()
                            authRepository.signOut()
                            navController.navigate(SSBMaxDestinations.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        // Leaves the status-bar inset unconsumed here (only navigation-bar/
        // horizontal insets are reserved) so each routed screen's own
        // Scaffold+TopAppBar -- every one of them has one: StudentHomeScreen,
        // InstructorHomeScreen, SettingsScreen, SSBOverviewScreen,
        // TopicScreen, UserProfileScreen -- receives the real top inset and
        // can paint its TopAppBar's background behind the status bar instead
        // of this outer Scaffold's default background showing through as a
        // gap above it. Affects Android and iOS identically since this is
        // shared/commonMain.
        // Routed screens use their own Material3 Scaffold, whose default safe-drawing
        // insets include navigation bars. Marking this outer inset as consumed prevents
        // those descendants from reserving the same bottom space a second time.
        Scaffold(contentWindowInsets = WindowInsets.navigationBars) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
                content({ scope.launch { drawerState.open() } })
            }
        }
    }
}
