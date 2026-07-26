package com.ssbmax.shared.platform

import org.koin.core.Koin
import org.koin.core.context.startKoin

private var koinInstance: Koin? = null

/**
 * Idempotent Koin bootstrap for iOS.
 *
 * Phase 6: called from `iosApp/iosApp/AppDelegate.swift`'s
 * `application(_:didFinishLaunchingWithOptions:)`, i.e. real app-launch
 * lifecycle, so Koin is ready before `BGTaskScheduler` registration and
 * APNs device-token hand-off (both of which need Koin-provided
 * repositories, see [com.ssbmax.shared.platform.notifications]) run.
 *
 * [com.ssbmax.shared.ui.MainViewController] also calls this (kept as a
 * defensive fallback from Phase 5, when this app had no `AppDelegate` yet)
 * -- guarded so calling it twice is a no-op, not a crash.
 */
fun ensureKoinStarted() {
    if (koinInstance != null) return
    koinInstance = startKoin {
        modules(com.ssbmax.shared.di.sharedModule)
    }.koin
}

/**
 * The started [Koin] instance. Callers must call [ensureKoinStarted] first
 * (both real bridge entry points -- [com.ssbmax.shared.platform.notifications.onApnsDeviceTokenReceived]
 * and [com.ssbmax.shared.platform.worker.registerAndScheduleBackgroundTasks]
 * -- already do this themselves before calling this getter).
 */
fun sharedKoin(): Koin =
    koinInstance ?: error("ensureKoinStarted() must be called before sharedKoin()")
