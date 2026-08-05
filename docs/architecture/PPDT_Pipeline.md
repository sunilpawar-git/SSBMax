# PPDT Pipeline Architecture

**Last updated:** June 2026 (Phase 8 complete; bug-fix + cache improvement pass complete)
**Status:** Living document — update when fixing bugs, improving flow, or adding features.

Picture Perception and Description Test (PPDT) is a Phase 1 psychology test in SSBMax. The candidate views a blurry image for 30 seconds, then writes a story based on what they perceived. The story is evaluated by Gemini AI against 15 Officer-Like Qualities (OLQs) using multimodal analysis (image + story + per-picture rubric), and the result feeds the unified OLQ dashboard.

---

## 1. User Journey Overview

```
StudentHomeScreen
  └─► TopicScreen (Phase 1 → Tests tab)
        └─► PPDTTestScreen
              ├─ [0] PROFILE GATE (gender check — blocks if no profile)
              ├─ [1] INSTRUCTIONS
              ├─ [2] IMAGE_VIEWING  (30 s, auto-advance)
              ├─ [3] WRITING        (4 min, auto-advance)
              ├─ [4] REVIEW
              └─ [5] SUBMITTED ──► PPDTSubmissionResultScreen
                                        └─► OLQ results + per-OLQ reasoning (async via WorkManager)
```

---

## 2. Navigation & Entry Points

**Route definitions:** `app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt`

| Destination | Route |
|-------------|-------|
| PPDT test | `test/ppdt/{testId}` |
| PPDT result | `test/ppdt/result/{submissionId}` |

**Route registration:** `app/src/main/kotlin/com/ssbmax/navigation/SharedNavGraph.kt` (lines 109–146)

**Entry paths:**
1. `StudentHomeScreen` Phase 1 ribbon → `TopicScreen(topicId="PHASE_1", selectedTab=2)` (Tests tab)
2. `TopicScreen` test list → `PPDTTest.createRoute("ppdt_standard")`
3. `StudentTests` screen → same route

Default `testId` is `ppdt_standard`.

---

## 3. Screen & ViewModel Layer

### Main screens

| Screen | File | ViewModel |
|--------|------|-----------|
| `PPDTTestScreen` | `app/.../ui/tests/ppdt/PPDTTestScreen.kt` | `PPDTTestViewModel.kt` |
| `PPDTSubmissionResultScreen` | `app/.../ui/tests/ppdt/PPDTSubmissionResultScreen.kt` | `PPDTSubmissionResultViewModel.kt` |

### Phase composables

| Composable | File | Responsibility |
|------------|------|----------------|
| `PPDTInstructionsPhase` | `components/phases/PPDTInstructionsPhase.kt` | 5-point instruction card, "Start Test" button |
| `PPDTImageViewingPhase` | `components/phases/PPDTImageViewingPhase.kt` | Layout root: instruction card + image card + timer bar |
| `PPDTImageCard` | `components/phases/PPDTImageViewingPhase.kt` | Private sub-composable: `AspectRatio(4:3)` image card with `ContentScale.Crop`; extracted to isolate stable content from per-second recomposition |
| `PPDTTimerProgressBar` | `components/phases/PPDTImageViewingPhase.kt` | Private sub-composable: 30 s linear progress bar; only this recomposes on each second tick |
| `PPDTWritingPhase` | `components/phases/PPDTWritingPhase.kt` | `OutlinedTextField` with char count (min 200, max 1000), keyboard padding |
| `PPDTReviewPhase` | `components/phases/PPDTReviewPhase.kt` | Read-only story preview, "Edit Story" returns to WRITING |

### Shared components

- `PPDTComponents.kt` — `PPDTTopBar` (phase name, timer, exit), `PPDTBottomBar` (Next/Review/Submit), `TimerChip` (MM:SS, red when < 30 s)
- `PPDTDialogs.kt` — `PPDTExitDialog`, `PPDTSubmitDialog`, `PPDTProfileRequiredDialog` (Phase 3 — shown when profile has no gender)
- `PPDTOLQReasoningCard.kt` — expandable per-OLQ reasoning card shown in result screen (Phase 4)

---

## 4. UiState & Phase State Machine

**`PPDTTestUiState`** (exposed by `PPDTTestViewModel` as `StateFlow<PPDTTestUiState>`):

```kotlin
data class PPDTTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val error: String? = null,

    val currentPhase: PPDTPhase = PPDTPhase.INSTRUCTIONS,
    val session: PPDTTestSession? = null,

    val imageUrl: String = "",
    val story: String = "",
    val charactersCount: Int = 0,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,

    val timeRemainingSeconds: Int = 0,
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,

    val canProceedToNextPhase: Boolean = false,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: PPDTSubmission? = null,

    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",

    val isProfileIncomplete: Boolean = false   // Phase 3: true when profile has no gender → shows PPDTProfileRequiredDialog
)
```

**Phase transitions:**

```
INSTRUCTIONS
  ──[startTest()]──►
IMAGE_VIEWING  (30 s timer)
  ──[timer = 0, auto]──►
WRITING  (240 s timer)
  ──[story ≥ 200 chars, proceedToNextPhase()]──►
REVIEW
  ──[returnToWriting()]──► WRITING
  ──[submitTest()]──►
SUBMITTED  ──[NavigateToResult event]──► result screen
```

**`PPDTPhase` enum:** `INSTRUCTIONS`, `IMAGE_VIEWING`, `WRITING`, `REVIEW`, `SUBMITTED`

---

## 5. Timer Logic

### Timers

| Phase | Duration | Trigger |
|-------|----------|---------|
| IMAGE_VIEWING | 30 s | `startTest()` → `startTimer(30)` |
| WRITING | 240 s | `proceedToNextPhase()` → `startTimer(240)` |

Both timers auto-advance the phase when they reach 0. `TimerChip` shows MM:SS and turns error-red when < 30 s remaining.

### Configuration-change recovery + generation-token race guard

`timerStartTime: Long` in `PPDTTestUiState` serves a **dual purpose**:

1. **Config-change recovery:** `timeRemainingSeconds` is stored in UiState. On ViewModel recreation, `restoreTimerIfNeeded()` (via `shouldRestoreTimer()` helper) resumes the timer with the saved remaining seconds — no timer loss on device rotation.

2. **Generation token:** Each `startTimer()` call increments `timerGeneration` (ViewModel-private `var`) and writes it into `timerStartTime`. The `finally` block of the dying timer coroutine checks `current.timerStartTime == myGeneration` before clearing `isTimerActive`. This prevents the 30 s IMAGE_VIEWING timer's `finally` block from flipping `isTimerActive = false` after the 240 s WRITING timer has already set it `true`.

`shouldRestoreTimer(s)` encapsulates the three-condition guard: test is in progress, phase is timerable (IMAGE_VIEWING or WRITING), and no timer is already running. This keeps `restoreTimerIfNeeded()` readable and detekt-compliant (ComplexCondition limit = 4).

---

## 6. Image Loading Pipeline

### Source (Firestore)

```
test_content/ppdt/image_batches/{batchId}
  ├── totalImages: Int
  └── images: List<{
        id: String,
        imageUrl: String,           ← Firebase Storage URL
        imageDescription: String,
        imageContext: {             ← structured PPDTImageContext (Phase 6)
          sceneDescription, coreElements[], ambiguousElements[],
          expectedThemes[], penalizedThemes[], primaryOLQs[],
          deviationTolerance, exemplarGoodHints[], exemplarBadHints[]
        },
        genderTag: String,          ← "MALE" | "FEMALE" | "MIXED" (Phase 6)
        viewingTimeSeconds: 30,
        writingTimeMinutes: 4,
        minCharacters: 200,
        maxCharacters: 1000,
        category, difficulty
      }>
```

Active batch: `batch_001` (64 images — replaced in Phase 5).

### Local cache (Room)

> **KMP-convergence Phase 9a (stale below, not rewritten):** this Room-backed cache (`CachedPPDTImageEntity`/`PPDTImageCacheDao`/`PPDTImageCacheManager`) and the repository that wired it (`TestContentRepositoryImpl`) are deleted; `shared`'s SQLDelight-backed `GitLivePPDTImageCacheManager` (`shared/src/commonMain/kotlin/com/ssbmax/shared/data/repository/`) is the sole implementation on both platforms now, ported to the same target/minimum-cache-size, least-used-selection, and 24h TTL-staleness-gate design this section describes. File paths below are historical; the algorithm they document is intended to still apply 1:1 to the SQLDelight port — not independently re-verified line-by-line here.

- **Entity:** `CachedPPDTImageEntity` → table `cached_ppdt_images`
  - Fields: `id`, `imageUrl`, `imageDescription`, `imageContextJson` (JSON-serialized `PPDTImageContext`), `genderTag` (default `MIXED`)
  - DB version: **23** (22→23 migration added `lastStalenessCheckAt INTEGER NOT NULL DEFAULT 0` to `ppdt_batch_metadata`)
- **DAO:** `PPDTImageCacheDao` — queries include least-used selection, batch fetch, usage tracking
  - `getLeastUsedImagesByGender(tag, count)` — SQL-level gender filter (`WHERE genderTag = :tag OR genderTag = 'MIXED'`); eliminates the former in-memory `selectRandomImage()` call
- **Manager:** `core/data/.../repository/PPDTImageCacheManager.kt`
  - Target cache: 15 images; minimum: 5 (triggers `initialSync()`)
  - Selection: `getLeastUsedImagesByGender(genderTag, 1)` — SQL rotates + gender-filters in one query
  - **24h TTL staleness gate:** `isCacheStale()` reads `lastStalenessCheckAt` from `PPDTBatchMetadataEntity`; if < 24 h since last check, skips the Firestore version call entirely. After a Firestore read, updates `lastStalenessCheckAt`. Eliminates the cold-start Firestore round-trip on every warm launch.

### Gender-based image routing (Phase 3 + 6)

```
loadTest()
  └─ resolveGenderTag(userId)
       └─ UserProfileRepository.getUserProfile(userId)
            ├─ profile == null (server confirms no profile) → isProfileIncomplete=true, stop
            ├─ gender == MALE   → GenderTag.MALE   → MALE+MIXED pool
            ├─ gender == FEMALE → GenderTag.FEMALE → FEMALE+MIXED pool
            └─ gender == OTHER / network error → null → full pool (no filter)
```

### Display (UI)

```kotlin
// PPDTImageViewingPhase.kt
val imageRequest = remember(imageUrl) {
    ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
}
// Card is sized proportionally (4:3 landscape) — no whitespace bands above/below image
Card(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
    AsyncImage(model = imageRequest, contentScale = ContentScale.Crop)
}
```

`remember(imageUrl)` keeps the request stable across recompositions; Coil handles HTTP disk cache. `aspectRatio(4f / 3f)` replaced `weight(1f)` to eliminate whitespace bands — PPDT images are landscape 4:3. `ContentScale.Crop` replaced `ContentScale.Fit` so the image fills the card without letter-boxing.

---

## 7. Subscription Check

Called before test content loads in `PPDTTestViewModel`:

```
SubscriptionManager.canTakeTest(TestType.PPDT, userId)
  → TestEligibility { canProceed, testsUsed, testsLimit, resetsAt }
```

- If limit reached: `isLimitReached = true` → `TestLimitReachedDialog` shown; test does not load.
- Usage persisted in Firestore: `users/{userId}/subscription/usage_{month}.ppdtTestsUsed` (incremented atomically on submission).
- Debug override: Settings → Developer section → `SubscriptionOverride.FORCE_PREMIUM` (dev-subscription-override plan Phase 6; replaces the retired `BuildConfig.BYPASS_SUBSCRIPTION_LIMITS`) routes through the real eligibility lookup with tier forced to PREMIUM, rather than a hardcoded remaining count.

---

## 8. Submission Flow

`PPDTTestViewModel.submitTest()` → `SubmissionRepository.submitPPDT(submission, batchId)` → Firestore `submissions` collection.

Steps in sequence:
1. Stop timer (`isTimerActive = false`)
2. Build `PPDTSubmission` with story, timings, userId, userName, userEmail
3. Write to Firestore `submissions/{submissionId}` (see Section 11 for document shape)
4. Increment `ppdtTestsUsed` via `SubscriptionManager`
5. Enqueue `PPDTAnalysisWorker(submissionId)` via WorkManager
6. Emit `TestNavigationEvent.NavigateToResult(submissionId)` → `TestResultHandler` pops test screen and navigates to result

---

## 9. Background Analysis — PPDTAnalysisWorker

**File:** `app/.../workers/PPDTAnalysisWorker.kt`
**Trigger:** WorkManager, immediately after submission
**Input:** `KEY_SUBMISSION_ID`

| Step | Action |
|------|--------|
| 1 | Fetch submission; verify `analysisStatus == PENDING_ANALYSIS` (idempotency guard) |
| 2 | Update Firestore: `analysisStatus → ANALYZING` |
| 3 | Resolve `candidateGender`: `UserProfileRepository.getUserProfile(userId).gender.displayName` — falls back to `"Unknown"` if fetch fails |
| 4 | Fetch `PPDTQuestion` via `TestContentRepository.getPPDTQuestion(questionId)` — Room cache → Firestore fallback; includes `imageUrl` and structured `imageContext` |
| 5 | Download image bytes from `imageUrl` (best-effort, `Dispatchers.IO`); on failure logs error and proceeds with `ByteArray(0)` |
| 6 | Build multimodal Gemini prompt: `PPDTPrompts.generatePPDTMultimodalPrompt(story, imageContext, candidateGender)` — injects per-picture rubric (coreElements, penalizedThemes, primaryOLQs, deviationTolerance) |
| 7 | Call Gemini AI: `KtorAIService.analyzePPDTMultimodal(imageBytes, story, imageContext, candidateGender)` — delegated to `KtorPPDTAnalyzer`; model `gemini-2.5-flash`, 60 s timeout, max 3 retries with exponential backoff |
| 8 | Parse JSON response → 15 `OLQScore` objects (each with `score`, `confidence`, `reasoning`) |
| 9 | SSB validation: `ValidationIntegration.validateScores(olqScores, EntryType.NDA)` — checks Factor II critical rules |
| 10 | Build `OLQAnalysisResult`: overallScore (avg of 15), overallRating, top 3 strengths, bottom 3 weaknesses |
| 11 | Atomic Firestore batch write: `ppdt_results/{submissionId}` (full result) + `submissions/{submissionId}` (status → COMPLETED) |
| 12 | Invalidate dashboard cache + push notification |

**Error path:** On failure after retries → `analysisStatus = FAILED`, failure notification sent, `Result.failure()` returned to WorkManager.

**Key implementation files:**
- `PPDTAnalysisWorker.kt` — orchestration, image download, gender resolution
- `KtorPPDTAnalyzer.kt` (`shared/.../shared/ai/`) — multimodal Gemini call + response parsing
- `PPDTPrompts.kt` (`shared/.../shared/ai/prompts/`) — `generatePPDTMultimodalPrompt()`

---

## 10. Gemini AI Prompt Design

**Prompt builder:** `shared/.../shared/ai/prompts/PPDTPrompts.kt` → `generatePPDTMultimodalPrompt(story, imageContext, candidateGender)`

The prompt sends **both image bytes and a per-picture rubric** to Gemini (multimodal). Gemini directly verifies scene accuracy from the image; the rubric controls what to reward and penalize.

**Prompt structure:**
```
[IMAGE BYTES attached via content { inlineData(imageBytes, "image/jpeg") }]

=== PICTURE BRIEFING ===
Scene: {imageContext.sceneDescription}
Core elements (MUST acknowledge — EFFECTIVE_INTELLIGENCE penalty if missed): ...
Ambiguous elements (creative interpretation acceptable — picture is hazy): ...
Penalized story themes (heavy penalty): ...
Primary OLQs this picture tests: ...
Deviation tolerance: LOW | MEDIUM | HIGH

=== CANDIDATE STORY ===
{story}  (Length: N chars | Writing time: 4 min)

=== CANDIDATE PROFILE ===
Gender: {candidateGender}

=== SSB SCORING SCALE (LOWER = BETTER) ===
[scoring rules block]

=== OUTPUT FORMAT (JSON only) ===
{ "olqScores": { "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 85, "reasoning": "..."}, ... } }
```

**Scoring scale:** 1–10, **LOWER = BETTER** (SSB convention)

| Rating | Score Range |
|--------|-------------|
| Exceptional | ≤ 3 |
| Good | ≤ 5 |
| Average | ≤ 7 |
| Needs Improvement | > 7 |

**Determinism:** `temperature = 0.0` default parameter of `KtorGeminiClient.generateContent` — identical story → identical score always.

---

## 11. Firestore Data Model

### `submissions/{submissionId}`

```
submissions/{submissionId}
  ├── id: String
  ├── userId: String
  ├── testType: "PPDT"
  ├── testId: String              ← mapped from questionId
  ├── status: String              ← "SUBMITTED_PENDING_REVIEW" | "COMPLETED"
  ├── submittedAt: Long
  ├── batchId: String?
  ├── gradedByInstructorId: String?
  ├── gradingTimestamp: Long?
  └── data: Map
      ├── submissionId, questionId, userId, userName, userEmail, batchId
      ├── story: String
      ├── charactersCount: Int
      ├── viewingTimeTakenSeconds: Int
      ├── writingTimeTakenMinutes: Int
      ├── submittedAt: Long
      ├── status: String
      ├── analysisStatus: String   ← PENDING_ANALYSIS | ANALYZING | COMPLETED | FAILED
      ├── olqResult: Map?          ← OLQAnalysisResult when COMPLETED
      └── instructorReview: Map?
```

### `ppdt_results/{submissionId}`

```
ppdt_results/{submissionId}
  ├── submissionId: String
  ├── userId: String
  ├── testType: "PPDT"
  ├── olqScores: Map<OLQ, { score: Int, confidence: Int, reasoning: String }>
  ├── overallScore: Float          ← average of 15 scores (1–10 SSB scale)
  ├── overallRating: String
  ├── strengths: List<String>      ← top 3 lowest-score OLQs
  ├── weaknesses: List<String>     ← top 3 highest-score OLQs
  ├── recommendations: List<String>
  ├── aiConfidence: Int            ← 0–100
  └── analyzedAt: Long
```

### `test_content/ppdt/image_batches/{batchId}`

```
test_content/ppdt/image_batches/{batchId}
  ├── totalImages: Int
  └── images: List<{
        id: String,
        imageUrl: String,
        imageDescription: String,
        imageContext: {
          sceneDescription: String,
          coreElements: List<String>,
          ambiguousElements: List<String>,
          expectedThemes: List<String>,
          penalizedThemes: List<String>,
          primaryOLQs: List<String>,
          deviationTolerance: "LOW" | "MEDIUM" | "HIGH",
          exemplarGoodHints: List<String>,
          exemplarBadHints: List<String>
        },
        genderTag: "MALE" | "FEMALE" | "MIXED",
        category: String,
        difficulty: String,
        viewingTimeSeconds: Int,
        writingTimeMinutes: Int,
        minCharacters: Int,
        maxCharacters: Int
      }>
```

---

## 12. OLQ Scoring System & Dashboard

### 15 Officer-Like Qualities (4 SSB Factors)

| Factor | Category | OLQs | Variance |
|--------|----------|------|---------|
| I — Planning & Organizing | INTELLECTUAL | EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION | ±1 tick |
| II — Social Adjustment ⚠️ CRITICAL | SOCIAL | SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY | ±1 tick |
| III — Social Effectiveness | DYNAMIC | INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS | ±2 ticks |
| IV — Character | CHARACTER | DETERMINATION, COURAGE, STAMINA | ±2 ticks |

**Auto-reject rule:** Factor II overall score ≥ 8 → automatic rejection flag.
**Critical OLQs:** REASONING_ABILITY, all Factor II, LIVELINESS, COURAGE — score ≥ 8 triggers review.

### Dashboard integration

- **`GetOLQDashboardUseCase`** (`core/domain/.../usecase/dashboard/`) fetches all test results in parallel.
- PPDT result arrives via `OLQDashboardData.Phase1Results.ppdtOLQResult`.
- Each test type has a **6-second timeout** — slow Firestore doesn't block the whole dashboard.
- **5-minute in-memory cache** reduces Firestore reads; invalidated by `PPDTAnalysisWorker` after analysis completes.
- Pre-computed once in the use-case: top 3 strengths, bottom 3 weaknesses, overall average — not on every UI recomposition.

---

## 13. Result Screen Flow

`PPDTSubmissionResultViewModel.loadSubmission(submissionId)`:

1. Opens real-time Firestore listener via `SubmissionRepository.observePPDTSubmission(submissionId)`
2. When `analysisStatus == COMPLETED` detected → calls `SubmissionRepository.getPPDTResult(submissionId)` (reads `ppdt_results` collection), then immediately calls `currentCoroutineContext().cancel()` to stop the listener — Firestore re-fires the COMPLETED snapshot on any subsequent document update, and without this cancel, each re-fire would call `getPPDTResult()` again (duplicate Firestore reads + duplicate UI updates)
3. Builds `SSBRecommendationUIModel` from OLQ scores
4. Exposes `PPDTSubmissionResultUiState` (implements `UnifiedResultUiState` — shared interface across all test result ViewModels)

**`CancellationException` contract:** The `catch (e: Exception)` block always checks `if (e is CancellationException) throw e` first. Without this, a navigate-away (which cancels the scope) would be incorrectly treated as an error and set `uiState.error` — showing a false error to the user.

```kotlin
data class PPDTSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: PPDTSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus get() = submission?.analysisStatus ?: PENDING_ANALYSIS
    override val olqResult get() = submission?.olqResult
}
```

The result screen polls by observing the Flow — no manual refresh required.

**Per-OLQ reasoning (Phase 4):** `PPDTSubmissionResultScreen` renders `PPDTOLQReasoningCard(olqResult)` when `analysisStatus == COMPLETED`. Each OLQ row is expandable and shows `OLQScore.reasoning` from the Gemini response — giving candidates specific feedback on why they scored low.

---

## 14. Domain Models Quick Reference

| Class | File | Purpose |
|-------|------|---------|
| `PPDTQuestion` | `core/domain/.../model/PPDTTest.kt` | Image URL, `imageContext` (structured), `genderTag`, viewing/writing time config |
| `PPDTImageContext` | same | Structured per-picture rubric: sceneDescription, coreElements, ambiguousElements, expectedThemes, penalizedThemes, primaryOLQs, deviationTolerance, exemplarHints |
| `GenderTag` | same | `MALE`, `FEMALE`, `MIXED` — used for image routing and Room entity filtering |
| `DeviationTolerance` | same | `LOW`, `MEDIUM`, `HIGH` — controls how strictly scene accuracy is enforced in prompt |
| `PPDTSubmission` | same | Full submission including story, timings, `analysisStatus`, `olqResult` |
| `PPDTPhase` | same | Enum: INSTRUCTIONS → IMAGE_VIEWING → WRITING → REVIEW → SUBMITTED |
| `PPDTTestSession` | same | Active session tracking (sessionId, phaseId, startTimes) |
| `PPDTTestConfig` | same | Defaults for timing & character limits |
| `PPDTDetailedScores` | same | Instructor grading breakdown (perception, imagination, narration, characterDepiction, positivity) |
| `PPDTInstructorReview` | same | Manual instructor review (instructorId, finalScore 0–100, agreedWithAI) |
| `OLQAnalysisResult` | `core/domain/.../model/scoring/UnifiedOLQResult.kt` | Unified AI result (15 OLQ scores with reasoning, overallScore, rating, strengths, weaknesses) |
| `OLQ` | `core/domain/.../model/interview/OLQ.kt` | Enum of all 15 qualities with Factor grouping & critical-flag metadata |
| `AnalysisStatus` | `UnifiedOLQResult.kt` | PENDING_ANALYSIS, ANALYZING, COMPLETED, FAILED |

---

## 15. Test Coverage

| Test File | Module | What It Covers |
|-----------|--------|----------------|
| `PPDTTestViewModelTest.kt` | `app` | Question loading (21 tests), phase transitions, story writing, submission, limit check |
| `PPDTProfileGateTest.kt` | `app` | Profile gate (6 tests): isProfileIncomplete when profile missing, proceeds when gender set, no gate on network error, MALE/FEMALE/OTHER gender routing |
| `PPDTAnalysisWorkerTest.kt` | `app` | Worker (7 tests): MALE/FEMALE/Unknown gender resolution, failure on missing submission, skip on non-PENDING status, multimodal call verified |
| `PPDTSubmissionResultViewModelTest.kt` | `app` | Result screen (7 tests): OLQ reasoning in state, empty reasoning safe default, loading/completed flow; + `CancellationException` must not set `error` (navigate-away safety); + `loadResult` called exactly once on COMPLETED (no duplicate Firestore reads) |
| `KtorAIServiceTest.kt` (was `GeminiAIServiceTest.kt`) | `shared` | AI service (6 tests): analyzePPDTMultimodal success/failure, Content vs String call, empty-bytes edge case, temperature=0 |
| `PPDTPromptsTest.kt` (was `PPDTPromptTest.kt`) | `shared` | Prompt (4 tests): coreElements present, penalizedThemes present, candidateGender present, no `{placeholder}` or `null` in output |
| `PPDTImageContextTest.kt` | `shared` | Domain model (3 tests): empty coreElements detectable, DeviationTolerance has exactly 3 values, PPDTQuestion defaults to empty context |
| ~~`PPDTImageContextMappingTest.kt`~~ | `core:data` (deleted, Phase 9a/9b) | JSON (2 tests): PPDTImageContext serializes/deserializes losslessly, CachedPPDTImageEntity defaults genderTag to MIXED |
| `PPDTQuestionDefaultsTest.kt` | `shared` | Model defaults (2 tests): minCharacters=200 matches UI, maxCharacters=1000 |
| ~~`PPDTImageCacheManagerTest.kt`~~ | `core:data` (deleted, Phase 9a/9b) | Cache sync, eviction, status tracking; + 24h TTL gate (skips Firestore within 24h; calls Firestore after TTL expires); + gender SQL filter verified at DAO level (no in-memory filter) |
| ~~`PPDTImageCacheDaoTest.kt`~~ | `core:data` (deleted, Phase 9a/9b) | Room DAO queries |
| `GetOLQDashboardUseCaseTest.kt` | `shared` | Dashboard aggregation logic, timeout/cache behaviour |

**Total PPDT-specific tests (Phases 1–8 + bug-fix pass): 58**

Run all PPDT-relevant tests (`core:data` itself was deleted in the KMP-convergence plan's Phase 9f):

```bash
./gradlew :app:testDebugUnitTest --tests "*PPDT*"
./gradlew :shared:testDebugUnitTest --tests "*PPDT*"
```

---

## 16. Content Ingestion Scripts

### Python pipeline (active — Phase 5)

**Directory:** `scripts/ppdt-picture-pipeline/`

| Script | Purpose |
|--------|---------|
| `step1_extract_cards.py` | Crop 2×2 grid PNGs → 64 individual JPEGs, strip caption bars (bottom 18% of each quadrant) |
| `step2_generate_context.py` | Gemini-expanded `PPDTImageContext` JSON for each image; checkpoint-based resumable; 1 req/s rate limit; HTML preview gate |
| `step3_upload.py` | Delete old Storage images, upload 64 new JPEGs, overwrite Firestore `batch_001` (idempotent, `--dry-run` flag available) |
| `gender_map.json` | Hardcoded gender classification for all 64 image IDs (MALE / FEMALE / MIXED) |
| `preview_template.html` | Jinja2 template: image + context fields + genderTag badge side-by-side; red flag on invalid OLQ names |

**Ingestion principle (from `CLAUDE.md`):** LLM is NOT used for `imageUrl` or `imageDescription` — these are set deterministically. LLM (Gemini) is only used for enriching `PPDTImageContext` fields (themes, OLQs, hints). Human review of `preview.html` is a mandatory gate before any Firestore write.

### Legacy Node.js scripts (superseded)

| Script | Status |
|--------|--------|
| `upload_ppdt_images.js` | Superseded by Python pipeline |
| `upload_ppdt_images_simple.js` | Superseded |
| `update_ppdt_urls_smart.js` | Superseded |
| `update_ppdt_image_urls_fixed.js` | Superseded |
| `make_ppdt_images_public.js` | Superseded |
| `add_remaining_ppdt_images.js` | Superseded |

---

## 17. Known Issues & Improvement Areas

_Update this section as bugs are found and improvements are made._

| # | Area | Issue / Improvement | Status |
|---|------|---------------------|--------|
| 1 | Image download in worker | `URL(imageUrl).readBytes()` is a one-shot HTTP call with no timeout or retry. Should be replaced with OkHttp/Ktor for proper timeout + retry. Analysis proceeds with `ByteArray(0)` if download fails (graceful degradation). | Deferred |
| 2 | `PPDTTestViewModel` file size | File is ~620 lines (300-line limit). Pre-existing tech debt; not introduced in Phases 1–8. Requires split into `PPDTTestViewModel.kt` + `PPDTSubmitViewModel.kt` or similar. | Deferred |
| 3 | OLQ Reasoning in other tests | `PPDTOLQReasoningCard` is PPDT-only (Phase 4). TAT/WAT/SRT result screens do not yet show per-OLQ reasoning. When those screens need it, move the card into `UnifiedOLQResultTemplate` as an optional slot. | Deferred to future phase |
| 4 | Profile gate condition | Gate triggers only when `profile == null` (server confirms no profile). If profile exists but gender field is null, the user is NOT gated — they get the full image pool. This is intentional (lenient). If stricter gating is required, update `resolveGenderTag()` condition. | Intentional design choice |
| 5 | `analyzePPDTResponse` legacy | Text-only method fully removed from `AIService` interface and `KtorAIService` (formerly `GeminiAIService`). No callers remain. | ✅ Cleaned up in Phase 8 |
| 6 | **Cache staleness after batch update** | `initialSync()` used a count-only guard (`if cachedImages >= 15 → skip`). After Phase 5 replaced all 64 images in Firestore, the app kept serving the old placeholder sketches indefinitely because the guard never checked the batch version. **Root cause:** no version comparison in `initialSync()`. **Fix:** `isCacheStale()` now fetches the Firestore `version` field and triggers `clearAllImages()` + re-download when local version differs. On network failure, stale cache is preserved (fail-safe). | ✅ Fixed June 2026 |
| 7 | **Room migration schema mismatch** | `MIGRATION_21_22` created `cached_ppdt_images` with `maxCharacters DEFAULT 1000`, but `CachedPPDTImageEntity` had `maxCharacters = 1500`. Also, the entity lacked the 6 `@Index` annotations the migration created, and used `Boolean` for `imageDownloaded` instead of `Int`. Room validates entity schema against the live DB on every open and throws `IllegalStateException: Migration didn't properly handle cached_ppdt_images` when they diverge. **Fix:** entity defaults aligned to match migration SQL exactly; `@Index` annotations added; `imageDownloaded` changed to `Int`. | ✅ Fixed June 2026 |
| 8 | **`loadTest()` called twice on startup** | `PPDTTestViewModel.init {}` called `loadTest()` directly AND `PPDTTestScreen`'s `LaunchedEffect(testId)` called it again. Result: two Firestore reads on every test open, double subscription-eligibility check, potential double-decrement of `ppdtTestsUsed`. **Fix:** removed `loadTest()` from `init {}`; `LaunchedEffect` is now the single call site (SSOT). | ✅ Fixed June 2026 |
| 9 | **IMAGE_VIEWING timer flip race** | When the 30 s IMAGE_VIEWING timer expired and immediately launched the 240 s WRITING timer, the dying coroutine's `finally` block ran after the new timer set `isTimerActive = true` and flipped it back to `false`. Writing phase UI showed timer as stopped. **Fix:** `timerGeneration` counter (monotonically increasing); `finally` block only clears `isTimerActive` if `current.timerStartTime == myGeneration` (generation-token pattern). | ✅ Fixed June 2026 |
| 10 | **Navigate-away shows error dialog** | `PPDTSubmissionResultViewModel.loadSubmission()` caught all `Exception` in one block. When the user navigated away, the scope was cancelled with `CancellationException`, which was caught and set `uiState.error = "Failed to load"` — showing a false error. **Fix:** re-throw `CancellationException` before the error-handling path. | ✅ Fixed June 2026 |
| 11 | **Duplicate `getPPDTResult` calls** | Firestore real-time listeners re-fire the `COMPLETED` snapshot on any update to the document (e.g., when `PPDTAnalysisWorker` writes the result). Without cancellation, each re-fire called `getPPDTResult()` again. **Fix:** after handling `COMPLETED`, call `currentCoroutineContext().cancel()` to stop the listener — one call per result, guaranteed. | ✅ Fixed June 2026 |
| 12 | **In-memory gender filter (performance)** | `getImageForTest()` fetched 15 images from Room and then filtered in-memory by `genderTag` via `selectRandomImage()`. On devices with many cached images, this scanned unnecessary rows. **Fix:** `getLeastUsedImagesByGender(tag, 1)` pushes the filter to SQL (`WHERE genderTag = :tag OR genderTag = 'MIXED'`); `selectRandomImage()` deleted. | ✅ Fixed June 2026 |
| 13 | **Cold-start Firestore version check on every launch** | `isCacheStale()` called Firestore on every `initialSync()` when cache was full. On a device with good cache, this was a redundant network call. **Fix:** `lastStalenessCheckAt` column added to `ppdt_batch_metadata` (migration 22→23); if < 24 h since last check, skip Firestore entirely. | ✅ Fixed June 2026 |
| 14 | **Image whitespace bands in IMAGE_VIEWING phase** | `Card(modifier = Modifier.weight(1f))` stretched the image to full screen height with empty bands above/below the landscape image. **Fix:** `Modifier.aspectRatio(4f / 3f)` sizes the card proportionally; `ContentScale.Crop` fills it without letter-boxing. | ✅ Fixed June 2026 |

---

## 23. Cache Invalidation Contract

**Rule: every batch content update MUST bump the Firestore `version` field.**

The Room cache is keyed by `batchId` (currently `batch_001`). The app will serve cached images forever unless a version change is detected. The version check in `initialSync()` is the only invalidation mechanism.

### How version checking works

```
initialSync()
  ├─ cachedImages < 15?  → download immediately (no version check needed)
  └─ cachedImages >= 15? → isCacheStale("batch_001")
        ├─ dao.getBatchMetadata("batch_001") == null         → stale (no local record)
        ├─ lastStalenessCheckAt within 24h                   → fresh, skip Firestore entirely (TTL gate)
        └─ 24h TTL expired → fetch Firestore version field
              ├─ Firestore version == local version           → fresh, update lastStalenessCheckAt
              ├─ Firestore version != local version           → stale → clearAllImages() + downloadBatch()
              └─ Firestore fetch fails (network error)        → assume fresh (fail-safe, don't wipe)
```

### Checklist before running step3_upload.py

When re-running the content pipeline to update images:

- [ ] **Bump the version** in `step3_upload.py`: change `"version": "2.0.0"` → `"3.0.0"` (or next)
- [ ] Confirm the new version propagates into the Firestore document after upload
- [ ] The app will auto-invalidate its cache on next test open — no app release needed

Failure to bump the version = app continues serving the old images regardless of what was uploaded.

### Entity ↔ Migration contract

`CachedPPDTImageEntity` and `MIGRATION_21_22` in `DatabaseMigrations.kt` must stay in sync. Room validates every column name, type, and default against the live database on each open. A mismatch causes an immediate `IllegalStateException` crash.

**Invariants that must match exactly:**

| Field | Entity default | Migration SQL default |
|-------|---------------|----------------------|
| `maxCharacters` | `= 1000` | `DEFAULT 1000` |
| `minCharacters` | `= 200` | `DEFAULT 200` |
| `viewingTimeSeconds` | `= 30` | `DEFAULT 30` |
| `writingTimeMinutes` | `= 4` | `DEFAULT 4` |
| `imageDownloaded` | `Int = 0` | `INTEGER NOT NULL DEFAULT 0` |
| `imageDescription` | `= "Picture showing an ambiguous scene"` | `DEFAULT 'Picture showing an ambiguous scene'` |
| `imageContextJson` | `= "{}"` | `DEFAULT '{}'` |
| `genderTag` | `GenderTag.MIXED` | `DEFAULT 'MIXED'` |

**Indices** — entity must declare all 6 with `@Entity(indices = [...])`:
`genderTag`, `imageDownloaded`, `usageCount`, `batchId`, `difficulty`, `category`

When adding a new column: always add a `MIGRATION_N_(N+1)` **and** update the entity. Never rely on `fallbackToDestructiveMigration()` in production — it wipes all user cache silently.

### Tests that enforce this contract

`PPDTImageCacheManagerTest.kt` contains regression tests (June 2026) that enforce both invariants:

| Test | What it catches |
|------|----------------|
| `initialSync re-downloads when Firestore batch version is newer` | Stale cache after content pipeline re-run |
| `initialSync skips download when version matches Firestore` | Unnecessary re-downloads |
| `initialSync re-downloads when no local batch metadata exists` | First install / cleared DB |
| `initialSync does NOT wipe cache when Firestore version fetch fails` | Network error safety |
| `CachedPPDTImageEntity default maxCharacters matches migration DEFAULT 1000` | Entity/migration drift |
| `CachedPPDTImageEntity imageDownloaded default is Int 0 not Boolean` | Type mismatch with SQLite |
| `CachedPPDTImageEntity annotation has all 6 indices required by migration` | Missing index annotations |
| `initialSync skips Firestore version check when last staleness check was within 24h TTL` | Warm-start redundant Firestore call |
| `initialSync calls Firestore version check when 24h TTL has expired` | TTL expiry triggers check correctly |

---

## 18. Data Flow Diagram (End-to-End)

```
[StudentHomeScreen]
     │ tap PPDT
     ▼
[PPDTTestViewModel.loadTest()]
     ├─ [1] checkSubscriptionEligibility()
     ├─ [2] resolveGenderTag()              ← UserProfileRepository (Phase 3)
     │         ├─ profile null → isProfileIncomplete=true → PPDTProfileRequiredDialog → stop
     │         ├─ MALE/FEMALE → gender-filtered image pool
     │         └─ OTHER / error → full pool
     └─ [3] TestContentRepository.getPPDTQuestion(genderTag)
                 │
         Room cache hit?
         ├─ YES: CachedPPDTImageEntity (imageContextJson + genderTag)
         └─ NO : Firestore test_content/ppdt/image_batches/batch_001
                   └─► Room insert
     │
     │ imageUrl in UiState
     ▼
[PPDTImageViewingPhase] ──Coil AsyncImage──► Firebase Storage (HTTP cache)
     │ 30 s timer expires
     ▼
[PPDTWritingPhase] ──story text──► UiState
     │ submitTest()
     ▼
[SubmissionRepository.submitPPDT()]
     │
     ├──► Firestore: submissions/{id}  (status: SUBMITTED_PENDING_REVIEW)
     ├──► SubscriptionManager: increment ppdtTestsUsed
     └──► WorkManager: enqueue PPDTAnalysisWorker(submissionId)
                                   │
     │                             │ (background)
     │                        [PPDTAnalysisWorker]
     │                             ├─ [1] Fetch submission + verify PENDING_ANALYSIS
     │                             ├─ [2] Resolve candidateGender from UserProfile
     │                             ├─ [3] Fetch PPDTQuestion (imageUrl + imageContext)
     │                             ├─ [4] Download image bytes (best-effort)
     │                             ├─ [5] generatePPDTMultimodalPrompt(story, imageContext, gender)
     │                             ├─ [6] KtorAIService.analyzePPDTMultimodal(imageBytes, story, context, gender)
     │                             │       └─ KtorPPDTAnalyzer → gemini-2.5-flash, temp=0, 60s, 3 retries
     │                             ├─ [7] Validate 15 OLQ scores (SSB Factor II rules)
     │                             ├─ [8] Firestore batch write:
     │                             │       ppdt_results/{id}   ← full OLQAnalysisResult (with reasoning)
     │                             │       submissions/{id}    ← status COMPLETED
     │                             └─ [9] Invalidate dashboard cache + push notification
     │
     ▼
[PPDTSubmissionResultScreen]
     │ observePPDTSubmission() real-time listener
     ├─ analysisStatus == PENDING/ANALYZING: show loading
     └─ analysisStatus == COMPLETED:
           getPPDTResult(submissionId) ──► ppdt_results/{id}
           Display OLQ scores, rating, strengths, weaknesses
           PPDTOLQReasoningCard: expandable per-OLQ reasoning text   ← Phase 4

[StudentHomeScreen → OLQDashboardCard]
     │ GetOLQDashboardUseCase (5-min cache, 6-s per-type timeout)
     └── Phase1Results.ppdtOLQResult ──► unified OLQ dashboard
```

---

## 19. Gaps Fixed in Phases 1–8

All gaps listed below were confirmed by code analysis before the improvement phases and are now resolved.

| # | Gap | Fix | Phase |
|---|-----|-----|-------|
| 1 | `PPDTQuestion.context` was empty string; no per-picture rubric | Replaced with structured `PPDTImageContext`; 64 images uploaded with full context | 5 + 6 |
| 2 | Generic prompt — no per-picture rubric | `PPDTPrompts.generatePPDTMultimodalPrompt()` injects coreElements, penalizedThemes, primaryOLQs, deviationTolerance | 8 |
| 3 | `candidateGender` hardcoded to `"male"` | `PPDTAnalysisWorker` resolves from `UserProfileRepository.getUserProfile(userId).gender` | 2 |
| 4 | Gemini temperature not set (default ~0.7–1.0) → score drift | `temperature = 0.0` default in `KtorGeminiClient.generateContent` (was `TEMPERATURE = 0.0f` in `GeminiAIService`) | 1 |
| 5 | `batch_001` used 57 placeholder images | 64 Gemini-generated images extracted, gender-classified, context-enriched, and uploaded | 5 |
| 6 | `minCharacters` inconsistency (domain=50, UI=200) | `PPDTQuestion.minCharacters` updated to 200 | 1 |
| 7 | `OLQScore.reasoning` never shown in UI | `PPDTOLQReasoningCard` renders per-OLQ reasoning in result screen | 4 |
| 8 | No gender-based image routing | Profile gate + `GenderTag` filter on image pool + Room migration | 3 + 6 |

**Remaining (not part of Phases 1–8):** Repeat-picture deduplication beyond least-used count rotation — no anti-gaming logic for frequent users.

---

## 20. Phase Implementation History

| Phase | What | Commit | Status |
|-------|------|--------|--------|
| 1 | Deterministic scoring (TEMPERATURE=0) + minCharacters=200 | `42a6c97` | ✅ Done |
| 2 | Gender from UserProfile in PPDTAnalysisWorker | `308ca41` | ✅ Done |
| 3 | Profile gate + gender-based image routing | `b778275` | ✅ Done |
| 4 | OLQ reasoning in result screen (PPDTOLQReasoningCard) | `e9fc1e6` | ✅ Done |
| 5 | Image replacement pipeline (Python: extract → context → upload) | `768c3d6` | ✅ Done |
| 6 | PPDTImageContext + GenderTag domain model + Room migration 21→22 | `2abf352` | ✅ Done |
| 7 | Multimodal GeminiAIService (analyzePPDTMultimodal + GeminiPPDTAnalyzer) (now `KtorAIService` + `KtorPPDTAnalyzer`) | `1f49069` | ✅ Done |
| 8 | Multimodal prompt rubric + worker image-aware analysis | `68c18f1` | ✅ Done |

**Bug-fix + cache improvement pass (June 2026)** — surfaced from logcat analysis after Phase 8:

| Fix | What | Commits | Status |
|-----|------|---------|--------|
| Bug 1 | Remove `loadTest()` from `init {}` — double-fetch on startup | `f4c8808` | ✅ Done |
| Bug 2 | Timer generation token — prevents `finally` race on phase transition | `07ac86a` | ✅ Done |
| Bug 3 | Re-throw `CancellationException` in result ViewModel — no false error on navigate-away | `b6f3b97` | ✅ Done |
| Bug 4 | Cancel Firestore listener after `COMPLETED` — no duplicate `getPPDTResult` calls | `b6f3b97` | ✅ Done |
| Cache A | `getLeastUsedImagesByGender()` at SQL layer — eliminates in-memory `selectRandomImage()` | `f4c8808` | ✅ Done |
| Cache B | 24h TTL in `isCacheStale()` + `lastStalenessCheckAt` column (migration 22→23) | `9828980` | ✅ Done |
| UI | `aspectRatio(4f/3f)` + `ContentScale.Crop` — no whitespace bands | `07ac86a` | ✅ Done |
| UI | Extract `PPDTImageCard` + `PPDTTimerProgressBar` — isolate per-second recomposition | `07ac86a`, `4cb57c5` | ✅ Done |
| Refactor | `LoadPPDTTestUseCase` + `SubmitPPDTTestUseCase` — extract domain use cases from ViewModel | `197a949` | ✅ Done |

---

## 21. OLQ Coverage Matrix — 50-Picture Bank Design

**Rule:** Design the matrix BEFORE generating or commissioning pictures. Each OLQ must appear as a "primary OLQ" in at least 4–6 pictures.

| Category | Count | Primary OLQs |
|----------|-------|-------------|
| Rescue / Emergency | 8 | COURAGE, INITIATIVE, SPEED_OF_DECISION |
| Leadership / Group Planning | 10 | INFLUENCE_GROUP, ORGANIZING_ABILITY, SPEED_OF_DECISION |
| Community / Social Service | 8 | COOPERATION, SENSE_OF_RESPONSIBILITY, SOCIAL_ADJUSTMENT |
| Individual Adversity / Persistence | 7 | DETERMINATION, STAMINA, SELF_CONFIDENCE |
| Conflict / Communication | 7 | POWER_OF_EXPRESSION, SOCIAL_ADJUSTMENT, COOPERATION |
| Problem Solving / Analysis | 6 | EFFECTIVE_INTELLIGENCE, REASONING_ABILITY |
| Team Energy / Motivation | 4 | LIVELINESS, COOPERATION, INITIATIVE |
| **Total** | **50** | All 15 OLQs covered |

**OLQ frequency check (minimum 4 pictures each):**

| OLQ | Appears in categories | Min pictures |
|-----|-----------------------|-------------|
| EFFECTIVE_INTELLIGENCE | Problem Solving | 6 |
| REASONING_ABILITY | Problem Solving | 6 |
| ORGANIZING_ABILITY | Leadership | 10 |
| POWER_OF_EXPRESSION | Conflict/Comm | 7 |
| SOCIAL_ADJUSTMENT | Community, Conflict | 15 |
| COOPERATION | Community, Team | 12 |
| SENSE_OF_RESPONSIBILITY | Community | 8 |
| INITIATIVE | Rescue, Team | 12 |
| SELF_CONFIDENCE | Adversity | 7 |
| SPEED_OF_DECISION | Rescue, Leadership | 18 |
| INFLUENCE_GROUP | Leadership | 10 |
| LIVELINESS | Team | 4 |
| DETERMINATION | Adversity | 7 |
| COURAGE | Rescue | 8 |
| STAMINA | Adversity | 7 |

---

## 22. Picture Creation Pipeline

### Design principles
- **B&W photograph style** — photo-realistic images of real human figures in real settings (NOT pencil sketches). Think staged black-and-white photographs.
- **PPDT vs TAT difference:** PPDT pictures are made hazy/blurry intentionally; TAT pictures are relatively clear. Same base image type, different post-processing.
- **Low resolution + grainy** — Gaussian noise + slight blur applied over the base image. Not HD; deliberately hard to see clearly at a glance.
- **Human figures present** — 1 to 6 people in realistic settings (office, outdoor, group meeting, etc.); no abstract or single-object images
- **No text or labels** in the image
- **Ambiguous but recognizable** — candidate can identify the core scene and characters but fine details are debatable

### Image style reference
The SSB PPDT picture style (from actual SSB centres):
- Black and white base image (monochrome)
- Photo-realistic human figures — real people, real environments
- Grainy texture overlaid (film grain / noise)
- Slightly reduced contrast — mid-tones flattened
- PPDT: additional haze/blur layer (Gaussian blur ~2–4px radius)
- TAT: same style but WITHOUT the haze layer — figures are clearly visible

### Image generation / sourcing approach
Option A (recommended): Commission or photograph staged B&W scenes matching each OLQ category intent, then post-process (desaturate + add grain + blur for PPDT).

Option B: Use AI image generation (Midjourney/DALL-E) with realistic style, then apply B&W + grain + blur in post-processing:
```
Prompt template: "black and white photograph, realistic human figures, [SCENE INTENT],
[NUMBER] people, indoor/outdoor setting, 1960s documentary style, grainy film texture,
no text, monochrome"
```
Then apply in ImageMagick/Pillow:
```bash
# PPDT post-processing (hazy)
convert input.jpg -colorspace Gray -noise 8 -blur 0x2 -contrast-stretch 2%x2% output_ppdt.jpg

# TAT post-processing (clear)
convert input.jpg -colorspace Gray -noise 4 output_tat.jpg
```

### Validation checklist (per picture, before upload)
- [ ] Core scene is recognizable even at 30% opacity
- [ ] At least 1–2 core elements are clearly visible
- [ ] Does NOT suggest a single "correct" story — multiple valid interpretations exist
- [ ] Ambiguous peripheral elements (good for creative deviation)
- [ ] No culturally insensitive, violent, or inappropriate content
- [ ] Matches the intended OLQ category from the matrix (Section 21)
- [ ] Aspect ratio consistent across the batch (~4:3)
- [ ] `genderTag` assigned and verified in `gender_map.json` before upload
- [ ] `PPDTImageContext` reviewed in `preview.html` before `step3_upload.py` runs
