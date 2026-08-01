package com.ssbmax.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ssbmax.navigation.DeepLinkGateway
import com.ssbmax.navigation.SSBMaxNavHost
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.presentation.root.AppRootViewModel
import com.ssbmax.shared.ui.components.SSBMaxAppScaffold
import com.ssbmax.shared.ui.theme.LocalThemeState
import com.ssbmax.shared.ui.theme.SSBMaxTheme
import com.ssbmax.shared.ui.theme.ThemeState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Phase 5 cutover target: `shared`'s full themed, chrome-wrapped,
 * nav-graphed root. As of Phase 2 of the KMP-convergence plan this renders
 * behind `app`'s debug-only `CanaryActivity` and iOS's `MainViewController`
 * only — Android's production `MainActivity` still
 * renders `app`'s own `SSBMaxApp`/nav graph until Phase 5's atomic swap, so
 * canary and the real cutover target are, by construction, literally the
 * same code being validated ahead of time.
 *
 * The current [com.ssbmax.shared.domain.model.AppTheme] is collected live
 * from [AppRootViewModel.themeFlow] (`collectAsStateWithLifecycle`, not a
 * one-time read) — the fix for "theme switching broken on both platforms,
 * iOS has no theming at all" this phase closes. [LocalThemeState] is
 * provided as a derived read cache of that same flow for any composable
 * that needs synchronous theme access without its own ViewModel; nothing
 * should write back into it — the flow (via [AppRootViewModel]) is the
 * single source of truth.
 *
 * Caller is responsible for providing platform CompositionLocals
 * (`LocalGoogleSignInLauncher`, `LocalNotificationPermissionController`)
 * before this, matching `MainActivity`'s existing pattern — those depend on
 * `ActivityResultLauncher`/`UNUserNotificationCenter` registration that must
 * happen before this composable's first composition.
 *
 * [DeepLinkEffect] (Phase 4) closes iOS's total deep-link gap here: both
 * platforms' entry points submit a raw notification payload into the
 * [DeepLinkGateway] Koin singleton, and this composable is the one place
 * that drains it once the user is authenticated and past the auth screens.
 */
@Composable
fun SSBMaxRoot() {
    val viewModel: AppRootViewModel = koinViewModel()
    val currentTheme by viewModel.themeFlow.collectAsStateWithLifecycle()
    val themeState = remember(currentTheme) { ThemeState(currentTheme) }

    val deepLinkGateway: DeepLinkGateway = koinInject()
    val authRepository: AuthRepository = koinInject()
    val logger: DomainLogger = koinInject()

    CompositionLocalProvider(LocalThemeState provides themeState) {
        SSBMaxTheme(appTheme = currentTheme) {
            val navController = rememberNavController()
            val pendingRoute by deepLinkGateway.pendingRoute.collectAsStateWithLifecycle()
            val currentUser by authRepository.currentUser.collectAsStateWithLifecycle()

            DeepLinkEffect(
                navController = navController,
                pendingRoute = pendingRoute,
                isAuthenticated = currentUser != null,
                onConsume = deepLinkGateway::consume,
                logger = logger
            )

            SSBMaxAppScaffold(navController = navController) { onOpenDrawer ->
                SSBMaxNavHost(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }
    }
}
