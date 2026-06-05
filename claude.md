# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Is SSBMax

Android app for SSB (Services Selection Board) preparation — India's military officer selection process. Candidates practice psychology tests (TAT, WAT, SRT, SD), GTO tasks (Group Discussion, Lecturette, GPE), and Interview simulation. The app uses Gemini AI for evaluation and question generation, Firebase for auth/storage, and Room for local caching.

## Build & Test Commands

Always use the Gradle wrapper (`./gradlew`), never bare `gradle`.

```bash
./gradlew assembleDebug                  # Build debug APK
./gradlew testDebugUnitTest              # Run all unit tests
./gradlew check                          # Run lint + tests
./gradlew lintDebug                      # Lint only
./gradlew :app:testDebugUnitTest         # Tests for app module only
./gradlew :core:domain:test              # Tests for domain module only
./gradlew :core:data:testDebugUnitTest   # Tests for data module only
./gradlew :lint:test                     # Tests for custom lint rules
./gradlew clean assembleDebug            # Clean build
```

Run a single test class:
```bash
./gradlew :app:testDebugUnitTest --tests "com.ssbmax.ui.profile.UserProfileViewModelTest"
./gradlew :core:domain:test --tests "*.GetOLQDashboardUseCaseTest"
```

## Project Architecture

**Multi-module Android app** using MVVM + Repository pattern, 100% Jetpack Compose (no XML layouts).

### Modules (settings.gradle.kts)

| Module | Purpose |
|---|---|
| `app` | UI screens (Compose), ViewModels, navigation, DI modules |
| `core:domain` | Use cases, domain models, repository interfaces, service interfaces. **ZERO Android dependencies** |
| `core:data` | Repository implementations, Room DB, Firebase data sources, Gemini AI integration |
| `core:designsystem` | Shared Compose components, theme |
| `core:common` | Shared utilities |
| `lint` | Custom lint rules (see below) |

### Data flow
```
Compose Screen → ViewModel → UseCase → Repository (interface in domain, impl in data) → Firebase/Room
```
- ViewModels expose `StateFlow<UiState>` collected with `collectAsStateWithLifecycle()`
- Domain layer returns `Result<T>` — no Android dependencies, no ErrorLogger
- Presentation layer maps domain errors to string resources

### Key paths
- Navigation routes: `app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt`
- Subscription limits (single source of truth): `core/data/src/main/kotlin/com/ssbmax/core/data/repository/SubscriptionManager.kt`
- AI prompts & Gemini integration: `core/data/src/main/kotlin/com/ssbmax/core/data/ai/`
- TestType enum: `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/SSBPhase.kt`

### Test infrastructure
- **Unit tests**: JUnit 4 + MockK + Turbine (Flow testing) + kotlinx-coroutines-test
- **Compose tests**: `@HiltAndroidTest` with custom `HiltTestRunner`
- **Test utilities**: `app/src/androidTest/kotlin/com/ssbmax/testing/` (BaseComposeTest, TestDataFactory, etc.)
- Tests have a 60-second global timeout

## Mandatory Code Rules

These are enforced by custom lint rules in `lint/` — the build will fail if violated.

### ErrorLogger, not printStackTrace
```kotlin
// ✅ ErrorLogger.log(e, "context message")
// ❌ e.printStackTrace()  — lint error: PrintStackTraceDetector
```
**Exception**: `core:domain` must NOT use ErrorLogger (Android dependency). Use `Result<T>` instead.

### String resources for all user-facing text
```kotlin
// ✅ stringResource(R.string.key)
// ❌ "Hardcoded text"  — lint error: HardcodedTextDetector
```
**Exception**: Domain layer business logic messages.

### Thread-safe StateFlow updates
```kotlin
// ✅ _uiState.update { it.copy(isLoading = true) }
// ❌ _uiState.value = _uiState.value.copy(...)  — lint error: StateFlowValueAssignmentDetector
```
ViewModels must expose `StateFlow<UiState>`, never `MutableStateFlow`.

### No singleton mutable state
```kotlin
// ❌ object FooHolder { var state: X? = null }  — lint error: SingletonMutableStateDetector
```
Never create `*Holder.kt` files.

### No Firebase in UI layer
Direct Firebase calls in Compose screens or ViewModels are flagged by `FirebaseInUILayerDetector`. Always go through repositories.

### ID-based navigation only
Pass only IDs (strings) between screens via navigation. Never pass complex objects — result screens fetch data from Firestore/Room via their own ViewModel.

## Quality Limits

- **Max 300 lines per file** — split if exceeded
- **Max 50 lines per Composable** — extract sub-components
- No hardcoded colors (use `MaterialTheme.colorScheme.*`)
- No hardcoded dimensions (use `dp`/`sp` values)
- Dependencies injected via Hilt — never manually constructed

## Testing Protocol

- If you change ViewModel or Repository logic, you **must** update or add tests
- If `./gradlew check` fails, fix it before considering the task done

## Content-Ingestion Principle (question banks from PDFs/sources)

When importing question/test content from an external source (PDF, scan, doc) into Firestore, **keep the LLM out of the correctness path.** A vision-LLM doing extraction + image-matching + answer/explanation generation in one pass produces jumbled, hallucinated data (see the `scripts/*fix-*-questions.js` graveyard — the cost of the old approach).

Instead:
1. **Parse the source-of-truth deterministically.** Question text, options, the answer key, and explanations almost always live in the PDF *text layer* — extract them with `pymupdf`, not an LLM. Map figures to questions by *geometry* (vertical position), and rebuild figures by compositing the clean embedded images onto a canvas (never a page-render, which bakes in watermarks).
2. **LLM only for non-critical enrichment** (tags, subtype, difficulty) — never for `questionText`, `correctAnswerId`, or `explanation`.
3. **Always gate writes with a human-reviewable HTML preview** before uploading to Firebase.

Reference implementation: `scripts/oir-extraction/` (`oir_extract_v2.py` + `upload-oir-batch.js`). Apply the same shape to future WAT/SRT/GPE content pipelines. Full pipeline + serving architecture: `docs/architecture/OIR_Architecture.md`.

## Tech Stack Quick Reference

Kotlin 2.1.0, AGP 8.7.3, compileSdk/targetSdk 35, minSdk 26, JVM 21, Compose BOM 2024.05.00, Hilt 2.54, Room 2.6.1, Coroutines 1.9.0, Firebase BOM 33.7.0, Ktor 3.0.2, Gemini AI 0.9.0. Versions managed in `gradle/libs.versions.toml`.
