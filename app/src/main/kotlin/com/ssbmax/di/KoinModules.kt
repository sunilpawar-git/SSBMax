package com.ssbmax.di

import com.ssbmax.core.data.di.coreDataInjectablesModule
import com.ssbmax.core.data.di.coroutineScopeModule
import com.ssbmax.core.data.di.databaseModule
import com.ssbmax.core.data.di.firebaseModule
import com.ssbmax.core.data.di.repositoryModule
import com.ssbmax.shared.di.sharedModule

/**
 * All Koin modules the app needs, in one place for `startKoin()`
 * (see [com.ssbmax.SSBMaxApplication]). Mirrors the module set previously
 * wired via Hilt's `@InstallIn(SingletonComponent::class)` across
 * `core:data`'s 6 modules and `app`'s 11 modules — split here into
 * `core:data` modules (repositories/Room/Firebase/AI) and `app` modules
 * (use cases bridging :shared, platform singletons, ViewModels).
 *
 * `sharedModule` (the KMP `:shared` module's own Koin graph) is included
 * too — per this phase's scope, `app` still depends on both `:shared` and
 * `:core:data` side by side (not yet rewired to consume only `:shared`,
 * that's Phase 5 territory), so both graphs must be live at once.
 *
 * KMP-convergence Phase 6a: `viewModelModule` removed (its file, and every
 * ViewModel it bound, deleted along with `app/ui` — nothing reaches them at
 * runtime post-cutover). `authUseCaseModule`/`studyUseCaseModule`/
 * `submissionUseCaseModule`/`subscriptionUseCaseModule`/`clockModule`/
 * `gtoTestModule`/`imageModule` removed too — each bound only classes that
 * were, in turn, only ever consumed by the deleted `app/ui` ViewModels
 * (confirmed by grep against the surviving `app/workers`/`app/notifications`/
 * `MainActivity`/`SSBMaxApplication` before removing each one; none matched).
 * `testUseCaseModule`/`workManagerModule`/`appInjectablesModule` stay — each
 * binds at least one class `app/workers` genuinely still needs
 * (`GetOLQDashboardUseCase`, `SubmissionAnalysisTrigger`/
 * `BackgroundTaskScheduler`, and `TATAnalysisPipelineOrchestrator`/
 * `TATAnalysisWorkPlanner`/`NotificationHelper` respectively —
 * `appInjectablesModule` itself was trimmed of its other, now-dead bindings,
 * see its own file).
 *
 * KMP-convergence Phase 9.0: `aiModule` removed — `AIService` was the last
 * binding `core:data` shadowed on the AI path, and `shared`'s `KtorAIService`
 * (bound in `coreInfraModule`) is now the only implementation on both
 * platforms. The Gemini key it needs arrives as a Koin *property* supplied by
 * [com.ssbmax.SSBMaxApplication], not by reading `core:data`'s `BuildConfig`
 * inside the module.
 *
 * KMP-convergence Phase 9d: `debugModule` removed — its sole binding,
 * `DebugConfig`, existed only for `core:data`'s `SubscriptionManager` (now
 * deleted, along with `DebugConfig` itself). The `BYPASS_SUBSCRIPTION_LIMITS`
 * local-dev convenience it gated is restored, but as a Koin *property*
 * supplied by [com.ssbmax.SSBMaxApplication] into `shared`'s
 * `CheckTestEligibilityUseCase` — same shape as the Gemini key above, not a
 * ported `DebugConfig` interface.
 */
val appModules = listOf(
    // :shared (KMP)
    sharedModule,

    // :core:data
    databaseModule,
    repositoryModule,
    firebaseModule,
    coroutineScopeModule,
    coreDataInjectablesModule,

    // :app
    testUseCaseModule,
    workManagerModule,
    appInjectablesModule
)
