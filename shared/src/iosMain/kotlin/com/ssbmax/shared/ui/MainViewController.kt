package com.ssbmax.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.ssbmax.navigation.SSBMaxNavHost
import com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import com.ssbmax.shared.ui.components.SSBMaxAppScaffold
import org.koin.core.context.startKoin

/**
 * iOS entry point. `startKoin` is called lazily on first invocation
 * (guarded) rather than from a platform-specific Application/AppDelegate
 * lifecycle hook, since this app has none yet — Phase 6 will need a proper
 * iOS lifecycle-integrated Koin bootstrap.
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
private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(com.ssbmax.shared.di.sharedModule)
        }
        koinStarted = true
    }
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
