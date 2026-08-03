# app/CLAUDE.md — Android Platform Glue

**Scope:** `app` is Android platform glue only — `MainActivity`, `SSBMaxApplication`, Firebase Cloud Messaging, and `WorkManager` workers. This file specializes the root [claude.md](../claude.md).

**KMP-convergence Phase 5/6a:** `app/ui`, `app/navigation`, and every app-module ViewModel were deleted — `MainActivity` renders `shared`'s `SSBMaxRoot()` directly, and `shared` is the single production UI/ViewModel/navigation graph on both Android and iOS. **For Compose screens, ViewModels, navigation, and DI patterns, see `shared`'s own conventions (established across Phases 1–4 of the KMP-convergence plan), not this file.** There is no `shared/CLAUDE.md` yet; read `shared`'s existing `presentation/`, `ui/`, `navigation/`, and `di/` packages directly for the live pattern.

---

## What lives in `app` today

| Area | Files | Responsibility |
|---|---|---|
| Entry point | `MainActivity.kt` | Registers `ActivityResultLauncher`s (must happen before STARTED), submits deep links to `DeepLinkGateway`, renders `SSBMaxRoot()` |
| DI bootstrap | `SSBMaxApplication.kt`, `di/KoinModules.kt` | Starts Koin with `appModules` (`sharedModule` + `core:data` modules + the handful of `app`-local modules `app/workers`/`app/notifications` still need); schedules background tasks via `BackgroundTaskScheduler` |
| Push | `notifications/{SSBMaxFirebaseMessagingService,NotificationHelper}.kt` | FCM token handling, notification channel/display |
| Background analysis | `workers/` (13 files) | WorkManager workers for AI grading (GTO/Interview/PPDT/TAT/SRT/WAT/SDT). **Duplication closed (KMP-convergence Phase 8):** GTO/WAT/SRT/SD/PPDT/Interview workers are thin shells delegating to `shared/analysis/*Orchestrator.kt`; TAT's per-story/synthesis workers keep their WorkManager-chain topology (real process-death resilience the orchestrator's single-coroutine design doesn't need) but converge onto the same `shared/analysis/AnalysisRetry` retry/prompt logic. `InterviewQuestionGenerationWorker` is a shell over `shared`'s `InterviewQuestionGenerator` |
| DI modules | `di/{AppInjectablesModule,TestUseCaseModule,WorkManagerModule}.kt` | Koin bindings for the above — **not Hilt**; see [app/di/CLAUDE.md](di/CLAUDE.md) (`DebugModule` deleted in the KMP-convergence plan's Phase 9d — its `DebugConfig`/`BYPASS_SUBSCRIPTION_LIMITS` bypass is now a Koin property `SSBMaxApplication` supplies into `shared`'s `CheckTestEligibilityUseCase`) |

## Anti-patterns (still enforced here)

- ❌ Adding a new screen, ViewModel, or nav route under `app` — those belong in `shared/commonMain` now; a second copy is exactly the SSOT violation the KMP-convergence plan closed
- ❌ `@HiltViewModel` / `@Inject` / any Hilt annotation — this project uses **Koin** (`viewModelOf`/`singleOf`/`module {}`), Hilt was removed
- ❌ Direct Firebase/Firestore calls from `app/workers` — go through `shared`'s repository interfaces where one exists

## References

- **Root guidance:** [claude.md](../claude.md)
- **DI patterns:** [app/di/CLAUDE.md](di/CLAUDE.md)
- **KMP-convergence plan:** tracks the app→shared migration this file reflects

---

**Last Updated:** 2026-08-01 | **Maintainer:** Sunil Pawar
