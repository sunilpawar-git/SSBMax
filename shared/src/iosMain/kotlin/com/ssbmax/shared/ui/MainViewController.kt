package com.ssbmax.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.ssbmax.navigation.SSBMaxNavHost
import com.ssbmax.shared.platform.auth.IosGoogleSignInLauncher
import com.ssbmax.shared.ui.auth.LocalGoogleSignInLauncher
import org.koin.core.context.startKoin

/**
 * iOS entry point. `startKoin` is called lazily on first invocation
 * (guarded) rather than from a platform-specific Application/AppDelegate
 * lifecycle hook, since this app has none yet — Phase 6 will need a proper
 * iOS lifecycle-integrated Koin bootstrap.
 *
 * Phase 5: now renders the real [SSBMaxNavHost] (Splash -> Login ->
 * RoleSelection -> Home placeholders) instead of the Phase 0 [SpikeApp] demo
 * screen — this is the first real multi-screen flow exercised on iOS.
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
            SSBMaxNavHost()
        }
    }
}
