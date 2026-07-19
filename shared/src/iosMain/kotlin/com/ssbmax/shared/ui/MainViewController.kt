package com.ssbmax.shared.ui

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.startKoin

/**
 * iOS entry point for the Phase 0 KMP spike. `startKoin` is called lazily on
 * first invocation (guarded) rather than from a platform-specific
 * Application/AppDelegate lifecycle hook, since this spike has none —
 * Phase 4/6 will need a proper iOS lifecycle-integrated Koin bootstrap.
 */
private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(com.ssbmax.shared.di.sharedModule)
        }
        koinStarted = true
    }
    SpikeApp()
}
