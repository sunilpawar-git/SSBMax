# app/di/CLAUDE.md — Koin Dependency Injection

**Scope:** `app`'s Koin modules. Inherits [app/CLAUDE.md](../CLAUDE.md).

**This project uses Koin, not Hilt.** `shared`'s modules (`shared/src/commonMain/.../di/*Module.kt`) are the SSOT for ViewModel and use-case bindings — everything reachable from a Compose screen lives there. `app`'s own modules exist only to bind the handful of classes `app/workers` and `app/notifications` need that `shared`/`core:data` don't already provide.

---

## Structure

`SSBMaxApplication.onCreate()` calls `startKoin { modules(appModules) }`, where `appModules` (`KoinModules.kt`) is `sharedModule` + `core:data`'s repository/Firebase/Room modules + `app`'s own small set:

```kotlin
val appModules = listOf(
    sharedModule,                 // :shared's full Koin graph (screens, ViewModels, use cases)
    databaseModule, repositoryModule, contentRepositoryModule,
    firebaseModule, coroutineScopeModule,
    coreDataInjectablesModule,    // :core:data
    testUseCaseModule, debugModule, workManagerModule, appInjectablesModule  // :app
)
```

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
- ❌ A module that duplicates a binding `sharedModule` or `core:data`'s modules already provide

---

**Last Updated:** 2026-08-01 | **Maintainer:** Sunil Pawar
