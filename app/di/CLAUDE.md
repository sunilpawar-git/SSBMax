# app/di/CLAUDE.md — Koin Dependency Injection

**Scope:** `app`'s Koin modules. Inherits [app/CLAUDE.md](../CLAUDE.md).

**This project uses Koin, not Hilt.** `shared`'s modules (`shared/src/commonMain/.../di/*Module.kt`) are the SSOT for ViewModel, use-case, and repository bindings — everything reachable from a Compose screen (and every repository) lives there. `app`'s own modules exist only to bind the handful of classes `app/workers` and `app/notifications` need directly.

---

## Structure

`SSBMaxApplication.onCreate()` calls `startKoin { modules(appModules) }`, where `appModules` (`KoinModules.kt`) is `sharedModule` + `app`'s own small set:

```kotlin
val appModules = listOf(
    sharedModule,                 // :shared's full Koin graph (screens, ViewModels, use cases, repositories)
    databaseModule,                                                        // :app
    testUseCaseModule, workManagerModule, appInjectablesModule             // :app
)
```

**KMP-convergence Phase 9a-9e:** every repository binding `core:data` used to shadow (19 at plan-authoring time — `contentRepositoryModule`, `repositoryModule`, `coreDataInjectablesModule`) moved to `shared`'s own `RepositoryModule.kt` (`GitLive*` implementations) one sub-phase at a time.

**KMP-convergence Phase 9f (module retirement):** `core:data` itself deleted — `repositoryModule` (already empty since 9e), `firebaseModule`, `coreDataInjectablesModule`, `coroutineScopeModule` removed with it; every binding they carried had either moved to `shared` already or turned out to have zero production callers left (confirmed by grep before deleting: `AnalyticsManager`, `DifficultyProgressionManager`, `FirebaseAuthService`, `FirebaseInitializer`, `FirestoreUserRepository`, `AuthRepositoryImpl`, and the 5 raw Firebase SDK singles were all superseded by `shared` equivalents or simply unused). The one genuinely live survivor — a Room cache (`TATStoryAssessmentDao`) local to two Android-only WorkManager workers, with no iOS equivalent — moved into `app`'s own `databaseModule` (`di/DatabaseModule.kt`), alongside the application-scoped `CoroutineScope` that used to live in `coroutineScopeModule`.

**Before adding a module here**, check whether it belongs in `shared` instead — if the class it binds is reachable from a Compose screen, it almost certainly does. `app`'s modules should only ever bind things `app/workers`/`app/notifications`/`MainActivity`/`SSBMaxApplication` need directly.

## Binding pattern

```kotlin
val appInjectablesModule = module {
    singleOf(::NotificationHelper)
    singleOf(::TATAnalysisPipelineOrchestrator)
}
```

`singleOf(::Ctor)` resolves constructor params from the graph via reflection — no `@Inject`/`@Provides` annotations needed. Use `factoryOf` for non-singleton use cases. There are no ViewModels bound in `app` — `viewModelOf` bindings live entirely in `shared/*Module.kt`.

## Testing

`PlatformModuleCheckTest` (`app/src/test/kotlin/com/ssbmax/di/PlatformModuleCheckTest.kt`) constructs `appModules` for real via `koinApplication { modules(appModules) }.checkModules { ... }` — this is the safety net against a dangling dependency after adding or removing a binding. It mocks the Android statics (`FirebaseApp`/`WorkManager`/etc.) that can't run in a plain JVM test; read its class doc before adding a new binding that needs another Android singleton, so you extend the same pattern rather than reaching for Robolectric (every Robolectric test in this module is currently `@Ignore`d for an SDK 35 shadow mismatch).

**Only stub what a real binding actually reads.** A stub kept "just in case" after its consumer is deleted is dead weight the next person has to re-investigate — trim it when you remove the module that needed it (see git history around the KMP-convergence Phase 6a `app/ui` deletion for the shape of this: an `imageModule`-only `ConnectivityManager`/`cacheDir` stub was left behind for a while after `imageModule` itself was deleted).

## Anti-patterns

- ❌ `@Module`/`@InstallIn`/`@Provides`/`@HiltViewModel` — this is Koin, not Hilt; none of these annotations exist in this codebase
- ❌ Binding a ViewModel here — `app` has no ViewModels; they live in `shared`
- ❌ A module that duplicates a binding `sharedModule` already provides

---

**Last Updated:** 2026-08-03 | **Maintainer:** Sunil Pawar
