package com.ssbmax.di

import com.ssbmax.shared.di.firebaseDataModule
import com.ssbmax.shared.di.sharedModule

/**
 * All Koin modules the app needs, in one place for `startKoin()`
 * (see [com.ssbmax.SSBMaxApplication]). Originally mirrored the module set
 * wired via Hilt's `@InstallIn(SingletonComponent::class)` across
 * `core:data`'s 6 modules and `app`'s 11 modules; `core:data` itself was
 * deleted in the KMP-convergence plan's Phase 9f (see [databaseModule]'s
 * class doc for where its one surviving live piece — the TAT Room cache —
 * landed).
 *
 * `sharedModule` (the KMP `:shared` module's own Koin graph) is the SSOT for
 * every screen/ViewModel/use-case/repository binding on both platforms;
 * `app`'s own modules below only bind the handful of classes
 * `app/workers`/`app/notifications` need directly.
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
 * local-dev convenience it gated was restored as a Koin property (same shape
 * as the Gemini key above), then retired again by the dev-subscription-override
 * plan's Phase 6 once `shared`'s `DeveloperSettings` + `SubscriptionOverride.FORCE_PREMIUM`
 * gave both platforms an equivalent, non-`BuildConfig` debug toggle.
 *
 * KMP-convergence Phase 9f (module retirement): `repositoryModule` (already
 * empty since 9e), `firebaseModule`, `coreDataInjectablesModule` removed —
 * every binding they carried (`AnalyticsManager`, `DifficultyProgressionManager`,
 * `FirebaseAuthService`, `FirebaseInitializer`, `FirestoreUserRepository`, the
 * 5 raw Firebase SDK singles) had zero production callers left, confirmed by
 * grep before deleting (`shared`'s `GitLive*` equivalents or `AnalyticsTracker`
 * already covered every real call site). `coroutineScopeModule` folded into
 * `databaseModule` (this module now binds `app`'s Room database directly,
 * having moved out of the deleted `core:data`, plus the application-scoped
 * `CoroutineScope` — see [databaseModule]'s own class doc).
 */
val appModules = listOf(
    // :shared (KMP)
    sharedModule,

    // :data-firebase (KMP) — the Firebase-backed repository bindings, split
    // out of `sharedModule` by Move 2 of the iOS CocoaPods->SPM convergence.
    // `sharedModule` alone is an INCOMPLETE graph: it declares the repository
    // interfaces but no longer binds them. See FirebaseDataModule.kt.
    firebaseDataModule,

    // :app
    databaseModule,
    testUseCaseModule,
    workManagerModule,
    appInjectablesModule
)
