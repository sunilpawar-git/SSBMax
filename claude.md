# CLAUDE.md — AI Agent Instructions for SSBMax

**Purpose:** Guidance for Claude, Cursor, and other LLM-based coding assistants working on this repository. Last updated: June 2026.

## What Is SSBMax

Android app for SSB (Services Selection Board) preparation — India's military officer selection process. Candidates practice psychology tests (TAT, WAT, SRT, SD), GTO tasks (Group Discussion, Lecturette, GPE), and Interview simulation. Tech stack: Gemini AI for evaluation, Firebase for auth/storage, Room DB for caching, Jetpack Compose (100% — no XML layouts).

## Your 12 Core Rules (Always follow)

1. **State assumptions explicitly** — if uncertain, ask rather than guess; present multiple interpretations when ambiguous
2. **Simplicity first** — minimum code solving the problem; no speculative features or premature abstractions
3. **Surgical changes** — touch only what you must; clean up only your own mess; match existing style
4. **Goal-driven execution** — define success criteria before coding; loop until verified
5. **Use LLM for judgment** — classification, drafting, summarization, extraction; NOT for routing, retries, deterministic transforms
6. **Token budgets are mandatory** — 4K per task, 30K per session; surface breaches, don't silently overrun
7. **Surface conflicts, don't average** — if two patterns contradict, pick one and explain why
8. **Read before you write** — understand exports, callers, shared utilities; "looks orthogonal" is dangerous
9. **Tests verify intent** — tests must encode WHY behavior matters, not just WHAT it does
10. **Checkpoint after every step** — summarize what was done, verified, and remains; if you lose track, stop and restate
11. **Match codebase conventions** — conform > taste inside the codebase; flag harmful conventions separately
12. **Fail loud** — surface uncertainty; never silently skip work or hide skipped tests

## Guiding Principles (MVVM + DRY + SOLID + SSOT)

**Single Source of Truth for every domain:**
- `SubscriptionLimits`/`CheckTestEligibilityUseCase` → subscription limits (only place to define/fetch; `core:data`'s `SubscriptionManager` was deleted in the KMP-convergence plan's Phase 9d)
- `SSBMaxDestinations` → navigation routes (only place for route definitions)
- `SSBPhase` → test type enum (only place to define test types)

Architecture: `Compose Screen → ViewModel (StateFlow<UiState>) → UseCase → Repository (interface in domain, impl in data) → Firebase/Room`

## Development Method (TDD-First)

**Before writing code:**
1. **Define success criteria** — what assertions pass? what fail?
2. **Plan test assertions WITH user** — no blind mocks; every assertion must encode WHY it matters (Rule 9)
3. **Write test first** — implementation follows; tests specify contract
4. **Phase ends with tech debt sweep** — phase complete only when task DONE + tech debt REMOVED (non-negotiable)

## Project Architecture

**Multi-module MVVM + Repository pattern:**

| Module | Purpose |
|---|---|
| `app` | Android platform glue only: `MainActivity`, `Application`, notifications, WorkManager workers, Koin bootstrap (`app/ui`/`app/navigation`/app-module ViewModels deleted in the KMP-convergence plan's Phase 6a — `shared` is the UI/ViewModel/nav SSOT on both platforms) |
| `shared` | KMP module: use cases, models, repository/service interfaces, Compose UI, Koin DI (ZERO Android-only dependencies in commonMain) — SSOT target of the KMP-convergence plan; `core:domain` was fully absorbed into it |
| `core:data` | Repository implementations, Room DB, Firebase, Gemini AI (being dissolved into `shared` incrementally, see the KMP-convergence plan) |
| `lint` | Custom lint rules (build fails if violated) |
| `detekt-rules` | Custom Detekt rule set (`shared`'s commonMain-reaching equivalent of `:lint`'s Compose checks — AGP Lint doesn't analyze a KMP module's commonMain) |

**Key paths (SSOT):** Navigation: `shared/.../SSBMaxDestinations.kt` | Subscription limits: `shared/.../data/repository/SubscriptionDtos.kt` (`SubscriptionLimits`) + `shared/.../domain/usecase/subscription/CheckTestEligibilityUseCase.kt` | AI: `shared/.../ai/` | Tests enum: `shared/.../SSBPhase.kt`

## Architecture Guidance Hierarchy (Phase 4)

**Multi-tier guidance system** (12 CLAUDE.md files) enforces patterns at every layer:

```
Root: claude.md (12 core rules, global patterns)
  ├── app/CLAUDE.md (Android platform glue only — UI/ViewModel/navigation
  │   │  moved to `shared` in the KMP-convergence plan's Phase 5/6a)
  │   └── app/di/CLAUDE.md (Koin dependency injection, not Hilt)
  │
  ├── shared/ai/CLAUDE.md (Gemini AI integration — the only AI path,
  │      both platforms; core/data/ai was deleted in Phase 9.0)
  │
  ├── core/data/CLAUDE.md (data repositories, error handling)
  │   ├── core/data/local/CLAUDE.md (Room database patterns)
  │   └── core/data/remote/CLAUDE.md (Firebase/Firestore)
  │
  ├── lint/CLAUDE.md (custom lint detectors)
  │
  ├── functions/CLAUDE.md (Cloud Functions backend)
  │
  └── scripts/CLAUDE.md (data ingestion, deterministic extraction)
```

**Quick Navigation:** See [CLAUDE_HIERARCHY.md](CLAUDE_HIERARCHY.md) for scenarios → right CLAUDE.md file lookup.

**Enforcement:**
- ✅ Lint: 16 custom detectors (Phase 2-4) catch violations at build time
- ✅ Pre-commit hook: Module-aware checks block commits on violations
- ✅ Code review: Pattern references guide PR feedback
- ✅ Documentation: GUIDELINES.md explains team processes

## Mandatory Lint Rules (enforced — build FAILS if violated)

1. **ErrorLogger, not printStackTrace** — `ErrorLogger.log(e, msg)` always; exception: `shared`'s domain layer uses `Result<T>`
2. **NO hardcoded strings** — all UI strings externalized: `app` via `stringResource(R.string.key)`, `shared` via `stringResource(Res.string.key)` (Compose Resources — no `R.string` equivalent in commonMain). Zero exceptions (not even domain layer)
3. **Thread-safe StateFlow updates** — `_uiState.update { it.copy(...) }`, never direct assignment
4. **Expose StateFlow, not MutableStateFlow** — ViewModels expose `StateFlow<UiState>` via `.asStateFlow()`
5. **No singleton mutable state** — never create `*Holder.kt` files
6. **No Firebase in UI layer** — use repositories; no direct Firebase calls in Compose/ViewModels
7. **ID-based navigation only** — pass string IDs between screens; result screens fetch data via their own ViewModel
8. **No Android deps in `shared`'s domain layer** — `shared/commonMain/.../domain` is 100% pure Kotlin — enables JVM/iOS testing, reusability
9. **No Firebase imports in app layer** — except Firebase Auth (Phase 4) — use repositories; no direct Firestore/Database calls
10. **Reusable Composables need @Preview** — required for visual validation. ⚠️ Currently **unenforced**: `ComponentMissingPreviewDetector` only ever targeted `core:designsystem`, deleted in the KMP-convergence plan's Phase 0f once its only two SSOT-worthy objects were confirmed duplicated in `shared`. No replacement detector scoped to `shared/ui`'s reusable components exists yet — a real gap, not a rewritten rule

## Quality Limits

- **Max 300 lines per file** (split if exceeded)
- **Max 50 lines per Composable** (extract sub-components)
- No hardcoded colors/dimensions (use `MaterialTheme.*`, `dp`, `sp`)
- Dependencies injected via Koin (never manual construction) — Hilt was removed; `app`/`shared` both use `module { }`/`singleOf`/`viewModelOf`
- If ViewModel/Repository logic changes → update/add tests; `./gradlew check` must pass

## Content-Ingestion Principle (PDFs → Firestore)

**Keep LLM out of correctness path.** Vision-LLM extraction produces hallucinated data.

**Instead:**
1. Parse deterministically — extract questions, options, answer keys from text layer (use `pymupdf`, NOT LLM)
2. Map figures by geometry — position → question association; composite images onto canvas (no page renders)
3. **LLM only for enrichment** — tags, difficulty, subtype; NEVER for `questionText`, `correctAnswerId`, `explanation`
4. Gate writes with HTML preview before Firebase upload

Reference: `scripts/oir-extraction/` + `docs/architecture/OIR_Architecture.md`

## Security Principles (Gold Standard)

- **Never hardcode secrets** — all API keys from `local.properties` or Firebase Config
- **Firestore rules first** — Firebase security rules are SSOT for data access control
- **Input validation** — sanitize all user inputs; prevent injection/XSS
- **Encrypt PII** — user interview responses, assessment data encrypted at rest (if stored locally)
- **No secrets in logs** — ErrorLogger must strip API keys, auth tokens, user IDs
- **Dependencies audited** — run `./gradlew dependencyCheckAnalyze` monthly
- **Full details:** See `docs/security/SECURITY.md`

## Test Infrastructure & Build Commands

**Setup:** JUnit 4 + MockK + Turbine + kotlinx-coroutines-test. Compose: `@HiltAndroidTest` + `HiltTestRunner`. Timeout: 60 sec/test.

**Commands:**
```bash
./gradlew testDebugUnitTest    # All unit tests
./gradlew check                # Lint + tests
./gradlew :app:testDebugUnitTest  # App module
```
Full list: see `claude.local.md`.

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.3, compileSdk 35, minSdk 26, JVM 21, Compose BOM 2024.05.00, Hilt 2.54, Room 2.6.1, Firebase BOM 33.7.0, Gemini AI 0.9.0. Versions: `gradle/libs.versions.toml`.
