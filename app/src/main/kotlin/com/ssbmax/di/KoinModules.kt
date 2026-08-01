package com.ssbmax.di

import com.ssbmax.core.data.di.aiModule
import com.ssbmax.core.data.di.contentRepositoryModule
import com.ssbmax.core.data.di.coreDataInjectablesModule
import com.ssbmax.core.data.di.coroutineScopeModule
import com.ssbmax.core.data.di.databaseModule
import com.ssbmax.core.data.di.firebaseModule
import com.ssbmax.core.data.di.loggerModule
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
 * `testUseCaseModule`/`debugModule`/`workManagerModule`/`appInjectablesModule`
 * stay — each binds at least one class `app/workers` genuinely still needs
 * (`GetOLQDashboardUseCase`, `DebugConfig` transitively via
 * `SubscriptionManager`, `SubmissionAnalysisTrigger`/`BackgroundTaskScheduler`,
 * and `TATAnalysisPipelineOrchestrator`/`TATAnalysisWorkPlanner`/
 * `NotificationHelper` respectively — `appInjectablesModule` itself was
 * trimmed of its other, now-dead bindings, see its own file).
 */
val appModules = listOf(
    // :shared (KMP)
    sharedModule,

    // :core:data
    databaseModule,
    repositoryModule,
    contentRepositoryModule,
    firebaseModule,
    loggerModule,
    coroutineScopeModule,
    aiModule,
    coreDataInjectablesModule,

    // :app
    testUseCaseModule,
    debugModule,
    workManagerModule,
    appInjectablesModule
)
