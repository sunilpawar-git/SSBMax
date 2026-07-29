package com.ssbmax.shared.platform

import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle

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
    // GEMINI_API_KEY reaches iOS via the GEMINI_API_KEY Xcode build setting -> Info.plist's
    // "GeminiAPIKey" -> here, mirroring Android's local.properties -> BuildConfig path. Android's
    // own production AIService binding (core:data's GeminiAIService) never goes through this
    // property (it reads BuildConfig directly), so this wiring only matters for iOS, which is
    // the platform that actually resolves KtorAIService/KtorGeminiClient from coreInfraModule.
    val geminiApiKey = NSBundle.mainBundle.objectForInfoDictionaryKey("GeminiAPIKey") as? String ?: ""
    koinInstance = startKoin {
        properties(mapOf("GEMINI_API_KEY" to geminiApiKey))
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
