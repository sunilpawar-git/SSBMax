package com.ssbmax.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.ssbmax.navigation.SSBMaxNavHost
import com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher
import com.ssbmax.shared.platform.ensureKoinStarted
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.components.SSBMaxAppScaffold

/**
 * iOS entry point. Koin is started via [ensureKoinStarted] -- as of Phase 6,
 * the real bootstrap happens earlier, from `AppDelegate.swift`'s
 * `application(_:didFinishLaunchingWithOptions:)` (needed before
 * `BGTaskScheduler` registration/APNs token hand-off, both Koin-dependent).
 * The call here is a guarded no-op in that case; kept as a defensive
 * fallback for any future entry point that renders this Composable without
 * going through `AppDelegate` first.
 *
 * Phase 5: renders the real [SSBMaxNavHost] wrapped in [SSBMaxAppScaffold]
 * (this session's nav-chrome addition -- drawer + bottom nav bar) instead of
 * the Phase 0 single-screen demo it replaced — the first real multi-screen,
 * chrome-wrapped flow exercised on iOS. The `NavHostController` is created
 * here (not inside [SSBMaxNavHost]) so the scaffold and the nav graph share
 * the same controller instance, matching the Android original's
 * `SSBMaxApp.kt` -> `SSBMaxScaffold` -> `SSBMaxNavGraph` composition shape.
 * [IosGoogleSignInLauncher] (a STUB — see its class doc) is provided
 * directly here rather than via Koin, matching the Android side's
 * `MainActivity`-constructed, CompositionLocal-provided pattern; iOS has no
 * equivalent `ActivityResultLauncher` lifecycle constraint, so it's
 * constructed inline rather than needing an earlier registration hook.
 */
fun MainViewController() = ComposeUIViewController {
    ensureKoinStarted()
    MaterialTheme {
        CompositionLocalProvider(
            LocalGoogleSignInLauncher provides IosGoogleSignInLauncher()
        ) {
            val navController = rememberNavController()
            SSBMaxAppScaffold(navController = navController) { onOpenDrawer ->
                SSBMaxNavHost(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }
    }
}
