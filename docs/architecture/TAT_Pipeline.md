# TAT Pipeline Architecture

**Last updated:** June 17, 2026 (Phase 4 prompt upgrade — R1–R14 rubric + OLQ correlation + notRecommended + R15 display order)
**Status:** Living document — update when fixing bugs, improving flow, or adding features.

Thematic Apperception Test (TAT) is a Phase 1 psychology test in SSBMax. The candidate is shown 11 picture cards (one at a time, 30 s each) and a blank card (card 12), then writes a 4-minute story for each. All 12 stories are evaluated by Gemini AI — each story individually via a per-story multimodal worker, then synthesized holistically — against 15 Officer-Like Qualities (OLQs), feeding the unified OLQ dashboard.

**TAT vs PPDT:**

| Aspect | PPDT | TAT |
|--------|------|-----|
| Images | 1 image per test | 11 picture cards + 1 blank card |
| Stories | 1 story | 12 stories |
| Image clarity | Deliberately hazy/blurred | Clear B&W photo-realistic |
| Image pool | 64 images, gender-tagged | 167 images, gender-tagged, position-assigned |
| Analysis | Single multimodal Gemini call | 12 parallel per-story workers → 1 holistic synthesis worker |
| Blank card | Not applicable | Card 12 — candidate projects from imagination |
| Room cache size | 64 images | 167 images |

---

## 1. User Journey Overview

```
StudentHomeScreen
  └─► TopicScreen (Phase 1 → Tests tab)
        └─► TATTestScreen
              ├─ [0] PROFILE GATE (gender check — blocks if no profile)
              ├─ [1] INSTRUCTIONS
              ├─ [2..13] For each card (1–12):
              │    ├─ IMAGE_VIEWING  (30 s, auto-advance)
              │    ├─ WRITING        (4 min, auto-advance)
              │    └─ REVIEW         (confirm → next card)
              └─ [14] SUBMITTED ──► TATSubmissionResultScreen
                                         └─► OLQ results + per-OLQ reasoning (async via WorkManager)
```

Card 12 is the blank card — no image shown, candidate writes a story from imagination.

---

## 2. Navigation & Entry Points

**Route definitions:** `app/src/main/kotlin/com/ssbmax/navigation/SSBMaxDestinations.kt`

| Destination | Route |
|-------------|-------|
| TAT test | `test/tat/{testId}` |
| TAT result | `test/tat/result/{submissionId}` |

**Route registration:** `app/src/main/kotlin/com/ssbmax/navigation/SharedNavGraph.kt`

**Entry paths:**
1. `StudentHomeScreen` Phase 1 ribbon → `TopicScreen(topicId="PHASE_1", selectedTab=2)` (Tests tab)
2. `TopicScreen` test list → TAT route with default `testId = "tat_standard"`

---

## 3. Screen & ViewModel Layer

### Main screens

| Screen | File | ViewModel |
|--------|------|-----------|
| `TATTestScreen` | `app/.../ui/tests/tat/TATTestScreen.kt` | `TATTestViewModel.kt` |
| `TATSubmissionResultScreen` | `app/.../ui/tests/tat/TATSubmissionResultScreen.kt` | `TATSubmissionResultViewModel.kt` |

`TATTestViewModel` extends `BaseTestViewModel` (Phase 3) which provides: `observeCurrentUser`, `subscriptionManager`, `securityLogger`, `workManager`, `sendNavigationEvent()`, and the `timerGeneration` counter used by the generation-token timer pattern.

### Phase composables

| Composable | File | Responsibility |
|------------|------|----------------|
| `TATInstructionsPhase` | `components/phases/TATInstructionsPhase.kt` | Instructions card, "Start Test" button |
| `TATImageViewingPhase` | `components/phases/TATImageViewingPhase.kt` | Image display + 30 s timer bar |
| `TATWritingPhase` | `components/phases/TATWritingPhase.kt` | `OutlinedTextField` + char count (min 150, max 1500) + 4 min timer |
| `TATReviewPhase` | `components/phases/TATReviewPhase.kt` | Read-only story preview, "Edit" / "Confirm" buttons |

### Shared components

- `TATComponents.kt` — `TATTopBar`, `TATBottomBar`, `TimerChip` (MM:SS, red when < 30 s)
- `TATDialogs.kt` — `TATExitDialog`, `TATSubmitDialog`, `TATProfileRequiredDialog`

---

## 4. UiState & Phase State Machine

**`TATTestUiState`** (exposed by `TATTestViewModel` as `StateFlow<TATTestUiState>`):

```kotlin
data class TATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<TATQuestion> = emptyList(),
    val config: TATTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<TATStoryResponse> = emptyList(),
    val currentStory: String = "",
    val phase: TATPhase = TATPhase.INSTRUCTIONS,
    val viewingTimeRemaining: Int = 30,
    val writingTimeRemaining: Int = 240,
    val startTime: Long = System.currentTimeMillis(),
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,          // generation token — see §5
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: TATSubmission? = null,
    val error: String? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isProfileIncomplete: Boolean = false
)
```

**Phase transition (repeated 12 times, once per card):**

```
INSTRUCTIONS
  ──[startTest()]──►
[Per card N = 1..12]
IMAGE_VIEWING  (30 s timer)
  ──[timer = 0, auto]──►
WRITING  (240 s timer)
  ──[timer = 0, auto OR user taps Review]──►
    saveCurrentStoryToResponses()
    ──►
REVIEW
  ──[editCurrentStory()]──► WRITING (resume; timer restarts)
  ──[confirmCurrentStory()]──►
    if N < 12 → IMAGE_VIEWING for card N+1
    if N = 12 → isTimerActive = false
               ──[submitTest()]──► SUBMITTED ──[NavigateToResult event]──► result screen
```

**`TATPhase` enum:** `INSTRUCTIONS`, `IMAGE_VIEWING`, `WRITING`, `REVIEW`, `SUBMITTED`

---

## 5. Timer Logic

### Generation-token pattern (Phase 1 bug fix)

Both timers (viewing 30 s, writing 240 s) share a single `timerJob: Job?`. Each `startViewingTimer()` / `startWritingTimer()` call increments `timerGeneration` (ViewModel-private `var`) and stamps it into `UiState.timerStartTime`. The `finally` block of the expiring coroutine checks `current.timerStartTime == myGeneration` before clearing `isTimerActive`. This prevents the viewing timer's `finally` from flipping `isTimerActive = false` after the writing timer has already set it `true`.

```kotlin
// startViewingTimer / startWritingTimer — same pattern for both:
timerJob?.cancel()
val myGeneration = ++timerGeneration
_uiState.update { it.copy(timerStartTime = myGeneration, isTimerActive = true, ...) }
val endTime = System.currentTimeMillis() + (seconds * 1000L)
timerJob = viewModelScope.launch {
    try {
        while (isActive) {
            val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
            if (remaining <= 0) break
            _uiState.update { it.copy(viewingTimeRemaining = remaining) }   // or writingTimeRemaining
            delay(200)
        }
        if (isActive) { /* phase transition */ }
    } finally {
        _uiState.update { current ->
            if (current.timerStartTime == myGeneration) current.copy(isTimerActive = false)
            else current
        }
    }
}
```

| Phase | Duration | Trigger |
|-------|----------|---------|
| IMAGE_VIEWING | 30 s | `startTest()` / `moveToNextQuestion()` |
| WRITING | 240 s | `startViewingTimer()` coroutine auto-advance |

---

## 6. Image Loading Pipeline

### Source (Firestore)

```
test_content/tat/image_batches/batch_001
  ├── totalImages: 167
  ├── version: "1.0.1"
  └── images: List<{
        id: "tat_001_male",               ← tat_{N}_{genderSuffix}
        sourceFile: "1Men.png",
        imageUrl: String,                  ← Firebase Storage URL
        cardPosition: Int,                 ← 1–11 (fixed sequence position)
        genderTag: "MALE" | "FEMALE" | "MIXED",
        imageContext: { 9 fields },        ← TATImageContext (see §13)
        category: String,
        difficulty: String,
        viewingTimeSeconds: 30,
        writingTimeMinutes: 4,
        minCharacters: 150,
        maxCharacters: 1500
      }>
```

**Flat pool design:** All 167 images in one `batch_001` document — no batch rotation. Images are selected per card position at test-load time using least-used + gender SQL filter.

**Image ID convention:** `tat_{N}_{suffix}` where suffix = `male`, `female`, or `mixed`. Gender derived from source filename: `1Men.png → MALE`, `1Women.png → FEMALE`, `1Mixed.png → MIXED`.

### Local cache (Room)

- **Entity:** `CachedTATImageEntity` → table `cached_tat_images`
  - Key fields: `id`, `batchId`, `cardPosition`, `genderTag = "MIXED"`, `imageUrl`, `imageContextJson = "{}"`, `usageCount = 0`
  - `@Entity(indices = [@Index("cardPosition"), @Index("genderTag"), @Index("usageCount"), @Index("batchId")])`
- **DAO:** `TATImageCacheDao` — primary selection query:
  ```sql
  SELECT * FROM cached_tat_images
  WHERE cardPosition = :cardPosition
  AND (genderTag = :genderTag OR genderTag = 'MIXED')
  ORDER BY usageCount ASC, RANDOM()
  LIMIT 1
  ```
- **Manager:** `core/data/.../repository/TATImageCacheManager.kt`
  - `TARGET_CACHE_SIZE = 167`, `MINIMUM_CACHE_SIZE = 33` (3 per position triggers resync)
  - **24h TTL staleness gate:** `isCacheStale()` reads `lastStalenessCheckAt` from `TATBatchMetadataEntity`; skips Firestore version call within 24 h. After Firestore read, updates `lastStalenessCheckAt`.
  - **Version-based invalidation:** if Firestore `version` ≠ local version → `clearAllImages()` + `downloadBatch()`

### `getImagesForTest(genderTag)` logic

```
For cardPosition in 1..11:
    getLeastUsedImageByPosition(cardPosition, genderTag)
    if null → getLeastUsedImageByPosition(cardPosition, "MIXED")  // fallback
Append blank card at position 12 (programmatic, no Room query)
markImagesAsUsed(selectedIds)
Return 12 TATQuestions in fixed card-position sequence
```

### Gender-based image routing (Phase 2)

```
loadTest()
  └─ LoadTATTestUseCase → UserProfileRepository.getUserProfile(userId)
       ├─ profile == null → ProfileIncompleteException → TATProfileRequiredDialog → stop
       ├─ gender == MALE   → GenderTag.MALE   → MALE+MIXED pool
       ├─ gender == FEMALE → GenderTag.FEMALE → FEMALE+MIXED pool
       └─ gender == OTHER / network error → null → full pool (no filter)
```

### Cache invalidation contract

**Every content pipeline run MUST bump the Firestore `version` field.** The Room cache is served indefinitely until a version change is detected.

```
initialSync()
  ├─ cachedImages < 33?  → download immediately
  └─ cachedImages >= 33? → isCacheStale("batch_001")
        ├─ lastStalenessCheckAt within 24h → fresh (TTL gate, skip Firestore)
        └─ 24h expired → fetch Firestore version
              ├─ version matches → update lastStalenessCheckAt
              ├─ version differs → clearAllImages() + downloadBatch()
              └─ Firestore fetch fails → assume fresh (fail-safe)
```

---

## 7. Subscription Check

Called in `loadTest()` before test content loads:

```
SubscriptionManager.canTakeTest(TestType.TAT, userId)
  → TestEligibility.Eligible     → continue
  → TestEligibility.LimitReached → isLimitReached = true → TestLimitReachedDialog → stop
  → TestEligibility.NetworkError → show error state → stop
```

Usage persisted in Firestore: `users/{userId}/subscription/usage_{month}.tatTestsUsed` (incremented atomically at submission via `recordTestUsage()`).

Debug override: `BuildConfig.BYPASS_SUBSCRIPTION_LIMITS = true` returns 999 remaining.

---

## 8. Submission Flow

`TATTestViewModel.submitTest()` steps (in sequence):

1. Stop timer (`isTimerActive = false`)
2. Get current user (3 s timeout)
3. Get user profile → resolve `subscriptionType`
4. Build `TATSubmission` with `stories = state.responses`, `analysisStatus = PENDING_ANALYSIS`
5. Write to Firestore: `submissions/{submissionId}` via `SubmitTATTestUseCase`
6. Enqueue `enqueueSynthesisChain(submissionId, stories)` via WorkManager
7. Record performance analytics (`DifficultyProgressionManager`)
8. Increment `tatTestsUsed` via `SubscriptionManager.recordTestUsage()`
9. Emit `TestNavigationEvent.NavigateToResult(submissionId)` → result screen

**WorkManager chain (step 6):**

```kotlin
// N = stories.size (typically 11–12)
val storyRequests = stories.mapIndexed { index, story ->
    OneTimeWorkRequestBuilder<TATStoryAnalysisWorker>()
        .setInputData(workDataOf(
            KEY_SUBMISSION_ID to submissionId,
            KEY_QUESTION_ID to story.questionId,
            KEY_STORY_INDEX to index
        ))
        .setConstraints(networkConnected)
        .build()
}
val synthesisRequest = OneTimeWorkRequestBuilder<TATSynthesisWorker>()
    .setInputData(workDataOf(KEY_SUBMISSION_ID to submissionId))
    .setConstraints(networkConnected)
    .build()

WorkContinuation.combine(storyRequests.map { workManager.beginWith(it) })
    .then(synthesisRequest)
    .enqueue()
```

`combine()` runs synthesis only after ALL story workers complete — but story workers now always return `Result.success()` (see §9), so the synthesis always runs.

---

## 9. Background Analysis — TATStoryAnalysisWorker (×N, per story)

**File:** `app/.../workers/TATStoryAnalysisWorker.kt`
**Trigger:** WorkManager, immediately after submission (one worker per story, in parallel)
**Input:** `KEY_SUBMISSION_ID`, `KEY_QUESTION_ID`, `KEY_STORY_INDEX`, `KEY_IMAGE_URL`, `KEY_IMAGE_CONTEXT_JSON`, `KEY_IMAGE_GENDER_TAG`

`KEY_IMAGE_URL` and `KEY_IMAGE_CONTEXT_JSON` are bundled by `TATTestViewModel.enqueueSynthesisChain()` at submission time from `state.questions` — the exact set shown to the user. **The worker never calls `getTATQuestions()`.**

| Step | Action |
|------|--------|
| 1 | Fetch `TATSubmission` from Firestore — if not found, **throw** (WorkManager retries the worker) |
| 2 | Find `TATStoryResponse` by `questionId` — if missing, save `FAILED` placeholder, return `Result.success()` |
| 3 | Read `imageUrl` and `imageContextJson` from `inputData` (passed by ViewModel at enqueue time) |
| 4 | Download image bytes from `imageUrl` (best-effort, 10 s connect / 20 s read); on failure proceeds with `ByteArray(0)` |
| 5 | Parse `TATImageContext` from `imageContextJson` (JSON → domain model) |
| 6 | Resolve `candidateGender` from `UserProfileRepository` |
| 7 | Call `aiService.analyzeTATStoryMultimodal(imageBytes, story, imageContext, candidateGender, storyIndex, totalStories)` → `GeminiTATStoryAnalyzer` → `gemini-2.5-flash`, **60 s timeout, `model` (Tier 1, 8192 output tokens)** |
| 8 | If AI returns < 14 OLQs → retry up to 3 times with exponential backoff |
| 9 | If all retries exhausted → save `FAILED` placeholder to Room, return `Result.success()` (does NOT break synthesis chain) |
| 10 | Parse `olqScores: Map<OLQ, OLQScore>` — each `OLQScore(score coerceIn(5,9), confidence, reasoning)` |
| 11 | Insert `TATStoryAssessmentEntity` into Room |

**Key design decision:** The worker always returns `Result.success()`. Failures are represented as `overallRating = "FAILED"` placeholder records in Room. This ensures `WorkContinuation.combine()` always proceeds to `TATSynthesisWorker`.

**Key implementation files:**
- `TATStoryAnalysisWorker.kt` — orchestration, image download, gender resolution
- `GeminiTATStoryAnalyzer.kt` (`core/data/.../ai/`) — multimodal Gemini call + response parsing
- `TATStoryAnalysisPrompts.kt` (`core/data/.../ai/prompts/`) — `generateTATStoryMultimodalPrompt()`

---

## 10. Background Analysis — TATSynthesisWorker (×1, holistic)

**File:** `app/.../workers/TATSynthesisWorker.kt`
**Trigger:** WorkManager, after ALL `TATStoryAnalysisWorker` instances complete (WorkManager chain)
**Input:** `KEY_SUBMISSION_ID`

| Step | Action |
|------|--------|
| 1 | Read all `TATStoryAssessmentEntity` for `submissionId` from Room |
| 2 | Filter out `FAILED` placeholders → `validAssessments` |
| 3 | If `validAssessments.size < MIN_STORY_THRESHOLD` (6) → `handleFailure()` + `Result.failure()` |
| 4 | Update Firestore: `data.analysisStatus → ANALYZING` |
| 5 | Build cross-story synthesis prompt: `TATSynthesisPrompts.buildPrompt(validAssessments)` — compact format, ~8.4K chars (see §11) |
| 6 | Call `aiService.analyzeTATResponse(prompt)` — **`synthesisModel` (Tier 3, 16384 output tokens, 120 s timeout)**, up to 3 retries |
| 7 | Resolve `entryType` from `UserProfileRepository` → `ScoringUtils.toScoringEntryType()` |
| 8 | SSB validation: `ValidationIntegration.validateScores(olqScores, entryType)` — Factor II critical rules |
| 9 | Build `OLQAnalysisResult`: overallScore (avg), overallRating, top 3 strengths, bottom 3 weaknesses |
| 10 | Firestore write: `psych_results/{submissionId}` with `userId` (required by security rules) |
| 11 | Update `data.analysisStatus → COMPLETED` in `submissions/{submissionId}` |
| 12 | Invalidate OLQ dashboard cache + send push notification |

**Error path:** On failure after all retries → `data.analysisStatus = FAILED`, failure notification sent.

`MIN_STORY_THRESHOLD = 6` — synthesis proceeds if at least 6 of 12 stories have valid AI analysis (handles network failures on a subset of stories gracefully).

---

## 11. Gemini AI Prompt Design

### Per-story prompt (`TATStoryAnalysisPrompts.generateTATStoryMultimodalPrompt`)

Sends **both image bytes and a per-picture rubric** to Gemini (multimodal). Structure mirrors the PPDT prompt:

```
[IMAGE BYTES attached via content { blob("image/jpeg", imageBytes) }]   ← skipped for blank card

=== PICTURE BRIEFING ===
Scene: {imageContext.sceneDescription}
Core elements (MUST acknowledge): ...
Ambiguous elements (creative interpretation acceptable): ...
Penalized story themes: ...
Primary OLQs this picture tests: ...
Deviation tolerance: LOW | MEDIUM | HIGH
Story position: {storyIndex+1} of {totalStories}

=== STORY STRUCTURE RUBRIC ===
[Murray's compact 3-tier need table: POOR / AVERAGE / GOOD need categories]
[R1–R11 scoring rules — one line per rule, ~12 words each]
NOTE: Scores are capped at 9. Multiple rules can stack on same OLQ.
[Hero-gender alignment check (R4) injected ONLY when imageGenderTag == "MIXED"]

=== CANDIDATE STORY ===
{story}

=== CANDIDATE PROFILE ===
Gender: {candidateGender}

=== CRITICAL INSTRUCTIONS ===
1. Return ONLY a single JSON object
2. NO markdown code blocks
3. ALL 15 OLQs MUST be present
4. Use EXACT enum names
5. Response must START with { and END with }

Each OLQ entry: score (int 5-9), confidence (int 0-100), reasoning (string).
Key: olqScores containing all 15 OLQ keys.
```

**`imageGenderTag`:** `"MIXED"` | `"MALE"` | `"FEMALE"` — passed via `KEY_IMAGE_GENDER_TAG` input data from `TATTestViewModel.enqueueSynthesisChain()`. The rubric injects the hero-gender alignment check (R4) only when `imageGenderTag == "MIXED"`. For `MALE`/`FEMALE`-tagged images the hero gender is unambiguous, so R4 is skipped.

**Scoring scale: 5–9, LOWER = BETTER** (SSB convention)

| Rating | Score Range |
|--------|-------------|
| Exceptional | ≤ 5.5 |
| Good | ≤ 6.5 |
| Average | ≤ 7.5 |
| Needs Improvement | > 7.5 |

**Determinism:** `TEMPERATURE = 0.0f` in `GeminiAIService.kt` — identical story → identical score.

### Synthesis prompt (`TATSynthesisPrompts.buildPrompt`)

Text-only. Sends summaries of all valid per-story assessments, asking Gemini to:
- Compute final 15 OLQ scores by considering evidence across all stories
- Identify cross-story narrative patterns (consistency, evolution, contradictions)
- Weight blank card (position 12) more heavily (imagination under no visual stimulus)

**Compact format (critical for token budget):** Per-story OLQ data is stripped to scores only via `compactOlqScores()` — `DETERMINATION:6, COURAGE:7, ...` (~180 chars/story) instead of the full reasoning JSON (~3000 chars/story). Story text is capped at 200 chars. This keeps the total prompt to ~8.4K chars, well within the 16384-token output budget of `synthesisModel`.

**SSB scoring rules (sprint June 2026):** Synthesis prompt now includes:
- `=== SCORING RULES ===` extended: blank card story carries **1.5× weight** (§13 blank card weighting, previously noted in this doc but absent from the actual prompt).
- `=== OLQ CORRELATION CONSTRAINTS ===` (R13): EI score generally ≥ RA; Factor III avg (INI/SC/SOD/AIG/LIV) ≤ Factor I avg (EI/RA/OA/POE); SC anchors INI/SOD/AIG.
- `=== REJECTION FLAG ===` (R14): if any synthesised OLQ score ≥ 8, Gemini sets `"notRecommended": true` in response.
- Format updated: `{"notRecommended": false, "olqScores": {...}, "overallConfidence": 78}`.

**`notRecommended` is an advisory field:** `GeminiResponseParser.parseGTOAnalysisResponse()` extracts it via `json.optBoolean("notRecommended", false)` and propagates it as `ResponseAnalysis.notRecommended`. `ValidationIntegration.validateScores()` is the SSOT for `RecommendationOutcome` — its R14 rule (any `limitationCount > 0` → `NOT_RECOMMENDED`) fires before `SSBScoreValidator` is consulted and is authoritative. Gemini's flag is a signal, not a decision.

**Prompt schema hardening (commit `230ffb1`):** Synthesis prompt now includes an explicit critical instructions section and a full JSON skeleton example. Key constraints enforced in the prompt:
- `olqScores` MUST be a JSON object keyed by OLQ name — NOT an array
- All 15 OLQs are mandatory keys
- Response MUST start with `{` and end with `}`

**Response format fallback (commit `d39bfab`):** Gemini occasionally returns a flat array `[{"olq":"EFFECTIVE_INTELLIGENCE","score":7,...}]` despite the prompt, instead of the canonical `{"olqScores":{...}}` object. `GeminiResponseParser.parseGTOAnalysisResponse()` now detects this (`cleanJson.trimStart().startsWith("[")`) and dispatches to `parseGTOArrayFormat()` (`GeminiResponseParser.kt:69–107`). A secondary bug in `extractJsonFromResponse()` was also fixed: it previously found the first `{` and last `}`, stripping the `[` and `]` from array responses before the array check could fire. Fixed by checking `indexOf('[') < indexOf('{')` first. Two regression tests in `core/data/src/test/.../GeminiResponseParserTest.kt` guard both parsing paths.

**Token budget — Gemini model tiers** (`GeminiAIService.kt`):

| Tier | Model instance | `maxOutputTokens` | Timeout | Used for |
|------|---------------|-------------------|---------|----------|
| 1 | `model` | 8192 | 60 s | Per-story TAT, PPDT, WAT, SD, Interview response analysis, GTO |
| 2 | `largeContextModel` | 12288 | 90 s | SRT (60 situations), Interview Q-gen (large PIQ), Adaptive Q-gen (growing transcript) |
| 3 | `synthesisModel` | 16384 | 120 s | TAT synthesis, Interview feedback (full Q&A transcript) |

**Prompt input token estimates (June 2026 sprint):**
- Per-story prompt: ~450 tokens input (+150 from `=== STORY STRUCTURE RUBRIC ===`, R1–R11 rules). Tier 1 (8192 output) comfortably handles this.
- Synthesis prompt: ~420 tokens input (+120 from OLQ correlation constraints, rejection flag, updated format). Tier 3 (16384 output) comfortably handles this.

Rationale: `gemini-2.5-flash` is a thinking model. `maxOutputTokens` covers thinking + response combined. Cross-story synthesis consumes ~5–8K thinking tokens before writing the JSON response — 8192 is insufficient (confirmed by `MAX_TOKENS` failures in production on June 17, 2026).

---

## 12. Firestore Data Model

### `submissions/{submissionId}`

```
submissions/{submissionId}
  ├── id: String
  ├── userId: String
  ├── testType: "TAT"
  ├── testId: String
  ├── status: "SUBMITTED_PENDING_REVIEW" | "COMPLETED"
  ├── submittedAt: Long
  └── data: Map
      ├── id, userId, testId, stories: List<storyMap>
      ├── totalTimeTakenMinutes: Int
      ├── submittedAt: Long
      ├── status: String
      ├── analysisStatus: String   ← PENDING_ANALYSIS | ANALYZING | COMPLETED | FAILED
      └── olqResult: Map?          ← populated only in legacy/transitional path
```

**Note:** `analysisStatus` is NOT written in the initial `submitTAT()` call (it's absent from `toFirestoreMap()`). It is created by `updateTATAnalysisStatus()` when the synthesis worker fires. Result screen handles this correctly by defaulting to `PENDING_ANALYSIS` when the field is absent.

### `psych_results/{submissionId}` (written by TATSynthesisWorker)

```
psych_results/{submissionId}
  ├── submissionId: String
  ├── userId: String              ← required by Firestore security rules
  ├── testType: "TAT"
  ├── olqScores: Map<OLQ, { score: Int, confidence: Int, reasoning: String }>
  ├── overallScore: Float          ← average of 15 scores (5–9 SSB scale)
  ├── overallRating: String
  ├── strengths: List<String>      ← top 3 lowest-score OLQs
  ├── weaknesses: List<String>     ← top 3 highest-score OLQs
  ├── recommendations: List<String>
  ├── aiConfidence: Int
  └── analyzedAt: Long
```

### `test_content/tat/image_batches/batch_001`

```
test_content/tat/image_batches/batch_001
  ├── totalImages: 167
  ├── version: "1.0.1"
  └── images: List<{ id, sourceFile, imageUrl, cardPosition, genderTag, imageContext{9 fields},
                      category, difficulty, viewingTimeSeconds, writingTimeMinutes,
                      minCharacters, maxCharacters }>
```

### `tat_story_assessments` (Room — bridge between per-story and synthesis workers)

```kotlin
@Entity(tableName = "tat_story_assessments",
        indices = [Index("submissionId"), Index("questionId"),
                   Index(value = ["submissionId", "storyIndex"], unique = true)])
data class TATStoryAssessmentEntity(
    @PrimaryKey val id: String,
    val submissionId: String,
    val questionId: String,
    val storyIndex: Int,
    val story: String,
    val imageUrl: String,
    val olqScoresJson: String,      // JSON array of {olq, score, confidence, reasoning}
    val overallScore: Float,
    val overallRating: String,      // "FAILED" = placeholder for failed story analysis
    val aiConfidence: Int,
    val analyzedAt: Long
)
```

`overallRating = "FAILED"` is the sentinel that marks a placeholder inserted when per-story AI analysis exhausts all retries. `TATSynthesisWorker` filters these out before synthesis.

---

## 13. OLQ Scoring System & Dashboard

### 15 Officer-Like Qualities (4 SSB Factors)

| Factor | Category | OLQs | Variance |
|--------|----------|------|---------|
| I — Planning & Organizing | INTELLECTUAL | EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION | ±1 tick |
| II — Social Adjustment ⚠️ CRITICAL | SOCIAL | SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY | ±1 tick |
| III — Social Effectiveness | DYNAMIC | INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS | ±2 ticks |
| IV — Character | CHARACTER | DETERMINATION, COURAGE, STAMINA | ±2 ticks |

**R14 — Any limitation → NOT_RECOMMENDED (absolute):** Any single OLQ score ≥ 8 → `RecommendationOutcome.NOT_RECOMMENDED`, enforced by `ValidationIntegration.validateScores()`. R14 fires before `SSBScoreValidator.determineRecommendation()` is consulted and is unconditional — it subsumes the old Factor II auto-reject and per-entry-type limitation-count logic. Factor II auto-reject (`factorIIAutoReject` flag) is still computed and stored in `OLQScoreValidationResult` for display purposes.

### TATImageContext — per-picture rubric

Identical structure to `PPDTImageContext`:

```kotlin
data class TATImageContext(
    val sceneDescription: String = "",
    val coreElements: List<String> = emptyList(),
    val ambiguousElements: List<String> = emptyList(),
    val expectedThemes: List<String> = emptyList(),
    val penalizedThemes: List<String> = emptyList(),
    val primaryOLQs: List<String> = emptyList(),
    val deviationTolerance: String = "MEDIUM",   // "LOW" | "MEDIUM" | "HIGH"
    val exemplarGoodHints: List<String> = emptyList(),
    val exemplarBadHints: List<String> = emptyList()
)
```

### Card position → OLQ focus mapping

| Position | Primary OLQs | Scene Intent |
|---|---|---|
| 1 | INITIATIVE, SELF_CONFIDENCE | Solo challenge, individual start |
| 2 | COOPERATION, SOCIAL_ADJUSTMENT | Group/social scenario |
| 3 | DETERMINATION, STAMINA | Persistence under pressure |
| 4 | SPEED_OF_DECISION, COURAGE | Emergency / urgency |
| 5 | ORGANIZING_ABILITY, EFFECTIVE_INTELLIGENCE | Planning, coordination |
| 6 | SENSE_OF_RESPONSIBILITY, COOPERATION | Duty, community, service |
| 7 | INFLUENCE_GROUP, POWER_OF_EXPRESSION | Communication, persuasion |
| 8 | LIVELINESS, SOCIAL_ADJUSTMENT | Energy, positive engagement |
| 9 | REASONING_ABILITY, EFFECTIVE_INTELLIGENCE | Problem solving, analysis |
| 10 | DETERMINATION, COURAGE | High-risk adversity |
| 11 | SELF_CONFIDENCE, INITIATIVE | Individual agency under scrutiny |
| 12 (blank) | EFFECTIVE_INTELLIGENCE, INITIATIVE, ORGANIZING_ABILITY | Pure imagination — heavily weighted |

### OLQ display order (R15)

`ResultSections.olqCategorySection()` now iterates `OLQ.entries.filter { it.category == category }` instead of `olqScores.entries`. This guarantees within-factor display order follows the `OLQ.kt` enum declaration (SSOT):

| Factor | Display order |
|--------|--------------|
| I — INTELLECTUAL | EI → RA → OA → POE |
| II — SOCIAL | SA → COOP → SOR |
| III — DYNAMIC | INI → SC → SOD → AIG → LIV |
| IV — CHARACTER | DET → COU → STA |

### Dashboard integration

- **`GetOLQDashboardUseCase`** fetches all test results in parallel with 6-second per-type timeouts.
- TAT result arrives via `OLQDashboardData.Phase1Results.tatOLQResult` from `psych_results/{submissionId}`.
- **5-minute in-memory cache** reduces Firestore reads; invalidated by `TATSynthesisWorker` after synthesis completes.

---

## 14. Result Screen Flow

`TATSubmissionResultViewModel.loadSubmission(submissionId)`:

1. Opens real-time Firestore listener: `submissionRepository.observeSubmission(submissionId)` (generic listener returning raw map)
2. Parses `data.analysisStatus` from each snapshot
3. When `analysisStatus == COMPLETED` but `olqResult` absent from submission doc → calls `submissionRepository.getTATResult(submissionId)` → reads `psych_results/{submissionId}`
4. Tracks `hasSeenCompleteWithOLQ` flag — once COMPLETED+OLQ is seen, regresses to incomplete state are ignored (prevents Firestore re-fires from wiping the result from UI)
5. Parses `TATSubmission` from snapshot map; attaches fetched `OLQAnalysisResult`
6. Builds `SSBRecommendationUIModel` from `ValidationIntegration.validateScores(scores, EntryType.NDA)`

```kotlin
data class TATSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: TATSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus get() = submission?.analysisStatus ?: PENDING_ANALYSIS
    override val olqResult get() = submission?.olqResult
}
```

The result screen polls by observing the Flow — no manual refresh required. While `analysisStatus == PENDING_ANALYSIS | ANALYZING`, a loading indicator is shown.

---

## 15. Domain Models Quick Reference

| Class | File | Purpose |
|-------|------|---------|
| `TATQuestion` | `core/domain/.../model/TATTest.kt` | Image URL, `imageContextJson`, `cardPosition`, `genderTag`, timing config |
| `TATImageContext` | same | Per-picture rubric: sceneDescription, coreElements, ambiguousElements, expectedThemes, penalizedThemes, primaryOLQs, deviationTolerance, exemplarHints |
| `GenderTag` | same | `MALE`, `FEMALE`, `MIXED` |
| `TATStoryResponse` | same | Per-story: questionId, story text, char count, timing |
| `TATSubmission` | same | Full submission: stories list, timings, `analysisStatus`, `olqResult` |
| `TATPhase` | same | `INSTRUCTIONS` → `IMAGE_VIEWING` → `WRITING` → `REVIEW` → `SUBMITTED` |
| `TATTestConfig` | same | Defaults: viewingTime=30s, writingTime=4min, minCharacters=150, maxCharacters=1500 |
| `OLQAnalysisResult` | `core/domain/.../model/scoring/UnifiedOLQResult.kt` | Unified AI result (15 OLQ scores with reasoning, overallScore, rating, strengths, weaknesses) |
| `AnalysisStatus` | same | `PENDING_ANALYSIS`, `ANALYZING`, `COMPLETED`, `FAILED` |
| `TATStoryAssessmentEntity` | `core/data/.../local/entity/` | Room bridge between per-story workers and synthesis worker |

---

## 16. Test Coverage

| Test File | Module | What It Covers |
|-----------|--------|----------------|
| `TATTestViewModelTest.kt` | `app` | Question loading, phase transitions, story writing, submission |
| `TATSubmissionResultViewModelTest.kt` | `app` | Loading/ANALYZING/COMPLETED states, OLQ parsing |
| `TATAnalysisWorkerTest.kt` | `app` | Gender resolution, missing submission, skip on wrong status |
| `TATStoryAnalysisWorkerTest.kt` | `app` | Per-story multimodal call, FAILED placeholder on AI failure, story-not-found path |
| `TATSynthesisWorkerTest.kt` | `app` | Threshold check, FAILED placeholder filtering, synthesis flow |
| `TATImageCacheManagerTest.kt` | `core:data` | Cache sync, TTL gate, position-based selection |
| `TATImageCacheDaoTest.kt` | `core:data` | `getLeastUsedImageByPosition()` SQL |

Run all TAT-relevant tests:

```bash
./gradlew :app:testDebugUnitTest --tests "*TAT*"
./gradlew :core:data:testDebugUnitTest --tests "*TAT*"
./gradlew :core:domain:testDebugUnitTest --tests "*TAT*"
```

---

## 17. Content Pipeline (Phase 0 — Complete)

**Python script directory:** `scripts/tat-picture-pipeline/`

| Step | Script | Purpose |
|------|--------|---------|
| — | _(Claude multimodal session)_ | Read all 167 PNGs, extract embedded description text, generate `TATImageContext` per image |
| — | _(HTML preview gate)_ | `preview.html` — each image + full context + cardPosition + genderTag; red flags on invalid OLQ names or uneven position distribution |
| 1 | `step1_upload.py` | Reads 167 PNGs + `tat_image_contexts.json`; derives gender from filename; uploads to Firebase Storage + writes Firestore `batch_001` (idempotent, bumps `version`) |

**Phase 0 results:**

| Item | Value |
|------|-------|
| Images uploaded | 167/167 (0 errors) |
| Storage path | `tat_images/batch_001/tat_NNN_gender.jpg` |
| Firestore | `test_content/tat/image_batches/batch_001` — 167 images, v1.0.1 |
| Card positions | 14–16 per position, ≥ 9 FEMALE+MIXED per position |
| Gender pool | MALE: 65, FEMALE: 39, MIXED: 63 |

**Gender ID convention:**

| Filename suffix | GenderTag | Notes |
|----------------|-----------|-------|
| `{N}Men.png` | MALE | Male-specific scenario |
| `{N}Women.png` | FEMALE | Female-specific scenario |
| `{N}Mixed.png` | MIXED | Gender-neutral — available to all |

**Checklist before re-running `step1_upload.py`:**
- [ ] Bump `version` field in script: e.g., `"1.0.1"` → `"1.0.2"`
- [ ] Run with `--dry-run` first, verify log output
- [ ] After upload, confirm new version is live in Firestore console
- [ ] App will auto-invalidate Room cache within 24 h of TTL expiry (no app release needed)

---

## 18. Data Flow Diagram (End-to-End)

```
[StudentHomeScreen]
     │ tap TAT
     ▼
[TATTestViewModel.loadTest()]
     ├─ [1] checkSubscriptionEligibility()
     ├─ [2] LoadTATTestUseCase → resolveGenderTag(userId)
     │         ├─ profile null → isProfileIncomplete=true → TATProfileRequiredDialog → stop
     │         ├─ MALE/FEMALE → gender-filtered image pool
     │         └─ OTHER / error → full pool
     └─ [3] TATImageCacheManager.getImagesForTest(genderTag)
                 → 11 position-based images (least-used, gender-filtered) + 1 blank card
     │
     │ 12 TATQuestions in UiState
     ▼
[Per-card loop: cards 1–12]
[TATImageViewingPhase] ──Coil AsyncImage──► Firebase Storage
     │ 30 s timer
     ▼
[TATWritingPhase] ──story text──► UiState
     │ 4 min timer / user Review
     ▼
[TATReviewPhase]
     │ confirmCurrentStory() → saveCurrentStoryToResponses()
     │                       → if card 12: allow submitTest()
     ▼
[TATTestViewModel.submitTest()]
     │
     ├──► Firestore: submissions/{id} (data.stories + analysisStatus absent initially)
     ├──► SubscriptionManager: increment tatTestsUsed
     └──► WorkManager: WorkContinuation.combine(N story workers).then(synthesis worker)
                                   │
     │                             │ (background, parallel)
     │                  [TATStoryAnalysisWorker × N]
     │                        ├─ Fetch submission from Firestore
     │                        ├─ Find story by questionId
     │                        ├─ Read imageUrl + imageContextJson from inputData (bundled by ViewModel at enqueue)
     │                        ├─ Download image bytes (best-effort)
     │                        ├─ generateTATStoryMultimodalPrompt(story, imageContext, gender, index)
     │                        ├─ GeminiAIService.analyzeTATStoryMultimodal → gemini-2.5-flash (model, Tier 1, 8192 tokens, 60s)
     │                        ├─ On AI success: insert TATStoryAssessmentEntity into Room
     │                        └─ On AI failure: insert FAILED placeholder → return Result.success()
     │                             (NEVER returns Result.failure() — ensures synthesis always runs)
     │
     │                  [TATSynthesisWorker × 1, after ALL story workers]
     │                        ├─ Read all TATStoryAssessmentEntity from Room
     │                        ├─ Filter out FAILED placeholders → validAssessments
     │                        ├─ If validAssessments < 6 → FAILED
     │                        ├─ Update Firestore: data.analysisStatus → ANALYZING
     │                        ├─ TATSynthesisPrompts.buildPrompt(validAssessments)  ← compact format, ~8.4K chars
     │                        ├─ GeminiAIService.analyzeTATResponse(prompt)  ← synthesisModel, Tier 3, 16384 tokens, 120s
     │                        ├─ ValidationIntegration.validateScores(olqScores, entryType)
     │                        ├─ Firestore batch write:
     │                        │     psych_results/{id}     ← OLQAnalysisResult (with userId)
     │                        │     data.analysisStatus    ← COMPLETED
     │                        └─ Invalidate dashboard cache + push notification
     │
     ▼
[TATSubmissionResultScreen]
     │ observeSubmission() real-time listener
     ├─ analysisStatus == PENDING/ANALYZING: show loading
     └─ analysisStatus == COMPLETED:
           getTATResult(submissionId) ──► psych_results/{id}
           Display OLQ scores, rating, strengths, weaknesses, per-OLQ reasoning

[StudentHomeScreen → OLQDashboardCard]
     │ GetOLQDashboardUseCase (5-min cache, 6-s per-type timeout)
     └── Phase1Results.tatOLQResult ──► unified OLQ dashboard
```

---

## 19. Known Issues & Improvement Areas

| # | Area | Issue / Improvement | Status |
|---|------|---------------------|--------|
| 1 | Image download in story worker | `URL(imageUrl).openConnection()` with manual timeout. Should use OkHttp/Ktor for proper timeout + retry. Analysis proceeds with `ByteArray(0)` if download fails (graceful degradation). | Deferred |
| 2 | `getTATQuestions()` ignores `testId` | ~~`TestContentRepository.getTATQuestions(testId)` ignores `testId` and draws a fresh random 12 from the pool. Workers calling this get a DIFFERENT random 12 than what the user saw.~~ Fixed June 2026: `TATTestViewModel.enqueueSynthesisChain()` now bundles `KEY_IMAGE_URL` + `KEY_IMAGE_CONTEXT_JSON` into each worker's `inputData` from `state.questions`. Worker reads them directly — no repository call needed. | ✅ Fixed June 2026 |
| 3 | OLQ Reasoning in result screen | `TATSubmissionResultScreen` doesn't yet render expandable per-OLQ reasoning cards (unlike PPDT which has `PPDTOLQReasoningCard`). When added, move the pattern into `UnifiedOLQResultTemplate` as an optional slot. | Deferred to future phase |
| 4 | `analysisStatus` absent on fresh submission | `TATSubmission.toFirestoreMap()` deliberately omits `analysisStatus`. It is created by `updateTATAnalysisStatus()` on first worker write. The result screen handles this correctly (defaults to `PENDING_ANALYSIS`). Intentional but worth documenting. | Intentional design |
| 5 | `MIN_STORY_THRESHOLD = 6` hardcoded | Synthesis proceeds if ≥ 6 of 12 stories succeed. If the user wrote only 6 stories total and all fail AI analysis, synthesis is skipped. Consider making the threshold dynamic (e.g., `max(6, stories.size / 2)`). | Deferred |

---

## 20. Phase Implementation History

| Phase | What | Commit | Status |
|-------|------|--------|--------|
| 0 | Content pipeline — 167 images extracted, gender-tagged, TATImageContext generated, uploaded to Firebase | `95c6e12` | ✅ Done |
| 1 | 8 bug fixes (B1-B7) + LoadTATTestUseCase extraction + TATTestUiState to own file | `86c78bf` | ✅ Done |
| 2 | Progressive architecture + image infrastructure (flat pool, cardPosition, genderTag, Room migration, 24h TTL) | `c5b0281` | ✅ Done |
| 3 | Profile gate + TATSynthesisPrompts + TATSynthesisWorker + TATStoryAnalysisWorker + BaseTestViewModel | `eeb7105` | ✅ Done |
| 4 | Gold-standard assessment: R1–R11 per-story rubric + imageGenderTag wiring; R12–R14 synthesis OLQ correlations + notRecommended field + ValidationIntegration R14 absolute rule; R15 OLQ display order | `012955d`–`267baa8` | ✅ Done |

**Bug fixes (post-Phase 3):**

| Fix | What | Status |
|-----|------|--------|
| Synthesis chain never ran | `TATStoryAnalysisWorker` returned `Result.failure()` on AI failure → `WorkContinuation.combine()` blocked synthesis. Fix: save FAILED placeholder (`overallRating="FAILED"`) to Room, return `Result.success()`. `TATSynthesisWorker` filters placeholders before synthesis. | ✅ Fixed June 2026 |
| Synthesis always failed (`MAX_TOKENS`) | `TATSynthesisPrompts.buildPrompt()` embedded full `olqScoresJson` per story (~3000 chars/story × 12 = ~36K chars). `gemini-2.5-flash` uses 5–8K thinking tokens before writing the response; combined output exceeded `MAX_TOKENS=8192`. Fix: (1) `compactOlqScores()` strips reasoning, sends `OLQ:score` pairs only (~180 chars/story); (2) story text capped at 200 chars. Total prompt: ~8.4K chars. (3) `synthesisModel` uses `maxOutputTokens=16384`, 120s timeout (Tier 3). | ✅ Fixed June 2026 |
| Token budget — all Gemini calls | Added three-tier model config in `GeminiAIService`: Tier 1 (8192), Tier 2 (12288), Tier 3 (16384). Prevents `MAX_TOKENS` failures for SRT (60 situations), Interview Q-gen (large PIQ), Adaptive Q-gen (growing transcript), and Interview feedback (full transcript). | ✅ Fixed June 2026 |
| imageBytes=0 — all 12 stories got text-only analysis | `TATStoryAnalysisWorker` called `TestContentRepository.getTATQuestions()` to recover `imageUrl`. That method ignores `testId` and returns a fresh random 12, so `questions.find { it.id == questionId }` always returned `null` → `imageUrl=""` → `ByteArray(0)`. Fix: `TATTestViewModel.enqueueSynthesisChain()` now bundles `KEY_IMAGE_URL` + `KEY_IMAGE_CONTEXT_JSON` per worker from `state.questions` (the exact set the user saw). Worker removed `TestContentRepository` dependency entirely. | ✅ Fixed June 2026 |
| Synthesis failed all 3 retries — `JSONException: Value [...] cannot be converted to JSONObject` | `GeminiResponseParser.parseGTOAnalysisResponse()` called `JSONObject(cleanJson)` and only handled the `{"olqScores":{...}}` object format. Gemini returned a flat array `[{olq, score, confidence, reasoning}]`. Fix: (1) prompt hardened with explicit JSON schema example and "NO arrays" constraint; (2) parser now detects array format and dispatches to `parseGTOArrayFormat()`; (3) `extractJsonFromResponse()` fixed to preserve `[` `]` brackets when array starts before `{`. | ✅ Fixed June 17, 2026 (`230ffb1`, `d39bfab`) |
| `POST_NOTIFICATIONS` permission not requested for psychology tests | Permission was only requested in `StartInterviewScreen`. TAT/WAT/SRT/SDT/PPDT workers post local notifications on completion, but Android 13+ silently drops them without the runtime permission. Users who never visited Interview setup never got prompted. Fix: centralized permission request in `StudentHomeScreen` `LaunchedEffect(Unit)` — fires once on home load, covers all test types. | ✅ Fixed June 17, 2026 (`cf785a2`) |

---

## 21. File Map (TAT pipeline — all relevant files)

```
app/src/main/kotlin/com/ssbmax/
├── ui/tests/tat/
│   ├── TATTestScreen.kt
│   ├── TATTestViewModel.kt
│   ├── TATTestUiState.kt
│   ├── TATSubmissionResultScreen.kt
│   └── TATSubmissionResultViewModel.kt
│   └── components/
│       ├── TATComponents.kt
│       ├── TATDialogs.kt
│       └── phases/
│           ├── TATInstructionsPhase.kt
│           ├── TATImageViewingPhase.kt
│           ├── TATWritingPhase.kt
│           └── TATReviewPhase.kt
└── workers/
    ├── TATStoryAnalysisWorker.kt   ← per-story multimodal (parallel)
    └── TATSynthesisWorker.kt       ← holistic synthesis (chained after all story workers)

core/domain/src/main/kotlin/com/ssbmax/core/domain/
├── model/
│   └── TATTest.kt                  ← TATQuestion, TATSubmission, TATPhase, TATTestConfig
├── repository/
│   └── SubmissionRepository.kt     ← getTATSubmission(), updateTATAnalysisStatus(), updateTATOLQResult()
└── usecase/
    ├── tat/LoadTATTestUseCase.kt
    └── submission/SubmitTATTestUseCase.kt

core/data/src/main/kotlin/com/ssbmax/core/data/
├── repository/TATImageCacheManager.kt
├── remote/TATSubmissionRepository.kt
├── local/
│   ├── entity/
│   │   ├── CachedTATImageEntity.kt
│   │   ├── TATBatchMetadataEntity.kt
│   │   └── TATStoryAssessmentEntity.kt   ← Room bridge: per-story → synthesis
│   └── dao/
│       ├── TATImageCacheDao.kt
│       └── TATStoryAssessmentDao.kt
└── ai/
    ├── GeminiTATStoryAnalyzer.kt
    └── prompts/
        ├── TATStoryAnalysisPrompts.kt
        └── TATSynthesisPrompts.kt

scripts/tat-picture-pipeline/
├── step0_generate_context.py        ← Gemini Vision extraction (generates tat_image_contexts.json)
├── step1_upload.py                  ← Firebase Storage + Firestore batch upload
├── tat_image_contexts.json          ← 167 TATImageContext records (production-final)
└── output/
    ├── tat_context_draft.json       ← working draft (step0 writes here, then promoted)
    └── preview.html                 ← HTML review gate (visual inspection before upload)

docs/architecture/
└── TAT_Pipeline.md                  ← this file
```

---

## 22. Upgrade Guide — Gold Standard

> **Purpose:** Every TAT image-pool change starts here. Choose your scenario, follow the exact steps, and refer back for field names, constraints, and safety checks. No other file should be needed.

---

### Quick-Reference: Which Procedure?

| What you want to do | Procedure |
|---|---|
| Fix wording in a few image contexts (no image file change) | **A — Context-only patch** |
| Remove one or more images from the pool permanently | **B — Nuke images** |
| Replace a source PNG with a better version | **C — Replace image** |
| Add brand-new images to the pool | **D — Add images** |
| Change viewing / writing timers globally | **E — Tune timing** |
| Change synthesis / analysis AI parameters | **F — Tune AI parameters** |

---

### Pool Balance Constraints (check before any removal or addition)

These constraints keep the test assembly SQL working correctly. Violating them causes `getLeastUsedImageByPosition()` to return null for a position, which silently breaks test construction.

| Constraint | Minimum | Current | Buffer |
|---|---|---|---|
| Images per position (1–11) | 10 | 14–16 | ~4–6 |
| FEMALE+MIXED images per position | 3 | varies | check after change |
| Total images | 33 (emergency minimum) | 167 | — |

Run this check after any structural change before uploading:

```bash
python step0_generate_context.py --preview-only   # regenerates preview.html from current draft
```

Open `output/preview.html` — it shows position counts and highlights positions below threshold in red.

---

### Procedure A — Context-Only Patch (no image file change)

**When:** Fix a scene description, change primaryOLQs, update penalizedThemes, adjust deviationTolerance — for one or a few images. No PNG files change.

**Speed:** Fastest path. No Gemini API calls, no Storage re-upload needed.

**Steps:**

1. Edit `tat_image_contexts.json` directly. Locate the entry by `"id"` (e.g., `"tat_027_male"`).

2. Fix the relevant `imageContext` field(s). Valid field names and values:

   | Field | Type | Constraints |
   |---|---|---|
   | `sceneDescription` | string | 1 sentence, objective |
   | `coreElements` | list\<string\> | 3–5 items |
   | `ambiguousElements` | list\<string\> | 2–3 items |
   | `expectedThemes` | list\<string\> | 3–4 items |
   | `penalizedThemes` | list\<string\> | 2–3 items |
   | `primaryOLQs` | list\<string\> | 2–4, exact OLQ names from §22 constants |
   | `deviationTolerance` | string | `"LOW"` \| `"MEDIUM"` \| `"HIGH"` |
   | `exemplarGoodHints` | list\<string\> | 1–2 items |
   | `exemplarBadHints` | list\<string\> | 1–2 items |

3. Dry-run validate:
   ```bash
   cd scripts/tat-picture-pipeline
   python step1_upload.py --dry-run --skip-storage-upload
   ```
   Fix any validation errors reported.

4. Upload (Firestore only, skip Storage re-upload since no image files changed):
   ```bash
   python step1_upload.py --skip-delete
   ```
   This re-uploads all 167 JPEGs (idempotent — same files) and writes the updated Firestore doc with the bumped version. Total time: ~5 min.

5. **Verify:** Open Firebase Console → Firestore → `test_content/tat/image_batches/batch_001` → confirm `version` incremented and the patched image entry has correct context.

6. **Android pick-up:** Version change detected on next app launch (24h TTL gate — force immediate by clearing app data in Settings or waiting 24h). Room cache cleared + re-synced automatically.

> **Faster alternative for single-image emergency fix:** Directly edit the Firestore document in Firebase Console — find the image in the `images` array, edit `imageContext` fields in place, then manually increment `version` (e.g., `"1.0.1"` → `"1.0.2"`). No scripts needed. Android will pick up the change within 24h.

---

### Procedure B — Nuke Images (remove from pool)

**When:** An image is inappropriate, causes recurring AI failures, is too ambiguous for reliable scoring, or needs to be retired.

**Critical check first:** Verify pool balance won't fall below minimums after removal (see constraints table above). If removing all images at a given card position, you must add replacements in the same run.

**Steps:**

1. Note the `id`(s) to remove (e.g., `"tat_027_male"`).

2. Remove entries from `tat_image_contexts.json`. Delete the entire JSON object for each image to nuke.

3. Check pool balance:
   ```bash
   python step0_generate_context.py --preview-only
   ```
   Open `output/preview.html`. Any position showing red (< 10 images total or < 3 FEMALE+MIXED) means you must add replacement images before proceeding.

4. If balance is healthy, dry-run:
   ```bash
   python step1_upload.py --dry-run
   ```
   Confirm `totalImages` in output equals your new count (167 − N).

5. Upload:
   ```bash
   python step1_upload.py
   ```
   - Deletes ALL old Storage files under `tat_images/batch_001/`
   - Re-uploads remaining images (nuked image JPEGs are not uploaded)
   - Writes Firestore doc with updated `totalImages` and bumped version

6. **Verify Firestore:** `totalImages` matches, nuked image IDs absent from `images` array.

7. **Note:** Nuked image JPEGs are gone from Firebase Storage permanently. If you need them back, they must be re-uploaded from the original PNG source.

> **The nuked image ID must NOT appear in Room.** Room re-syncs from the Firestore batch — since it's not in the new batch, it won't be cached. Existing Room rows for that ID are cleared as part of `clearAllImages()` + full re-download.

---

### Procedure C — Replace an Image (better source PNG)

**When:** A source PNG was cropped poorly, has print artifacts, is too dark/light, or you have a higher-quality version of the same scene.

**Steps:**

1. Replace the PNG in your source directory (`/Users/sunil/Downloads/TAT Work/Cropped_images/`). Use the **exact same filename** (e.g., `27Men.png`). This preserves the `id` (`tat_027_male`) and its position assignment.

2. Regenerate context for just this scene number:
   ```bash
   python step0_generate_context.py --ids 27 --resume
   ```
   Gemini re-analyses only scene 27 (all gender variants). The `output/tat_context_draft.json` is updated.

3. Review output for scene 27 in `output/preview.html` (open in browser). Confirm scene description, OLQs, and hints look correct.

4. If context needs manual adjustment, edit `output/tat_context_draft.json` directly for the affected entry.

5. Promote draft to production:
   ```bash
   cp output/tat_context_draft.json tat_image_contexts.json
   ```

6. Upload:
   ```bash
   python step1_upload.py
   ```
   The new JPEG for scene 27 replaces the old one in Storage. All 167 entries written to Firestore. Version bumped.

7. **Verify:** Public URL for the replaced image loads the new version. Check in browser:
   `https://storage.googleapis.com/ssbmax-49e68.firebasestorage.app/tat_images/batch_001/tat_027_male.jpg`

---

### Procedure D — Add New Images

**When:** Expanding the pool, adding a new gender variant for an under-represented position, or adding entirely new scenes.

**File-naming convention (must follow exactly):**

```
{sceneNumber}{Gender}.png
  sceneNumber : integer (use next available, e.g., 66 if current max is 65)
  Gender      : "Men" | "Women" | "Mixed"

Examples: 66Men.png, 66Women.png, 66Mixed.png
```

**Steps:**

1. Add new PNG(s) to source directory (`/Users/sunil/Downloads/TAT Work/Cropped_images/`).

2. Generate context for new images only:
   ```bash
   python step0_generate_context.py --resume
   ```
   `--resume` skips all 167 existing IDs; only processes new files. Appends to `output/tat_context_draft.json`.

3. Check position assignment in `output/preview.html`. The greedy algorithm assigns position based on `primaryOLQs` — verify placement makes sense.

   If you want to override position for a specific image, edit `output/tat_context_draft.json` directly and change `cardPosition`.

4. Verify pool balance still healthy (all positions ≥ 10, ≥ 3 FEMALE+MIXED).

5. Promote and upload:
   ```bash
   cp output/tat_context_draft.json tat_image_contexts.json
   python step1_upload.py --dry-run   # verify totalImages = 167 + N
   python step1_upload.py
   ```

6. **Verify Firestore:** `totalImages` = 167 + N. New image IDs present. Version bumped.

---

### Procedure E — Tune Timing (viewing/writing durations)

**Two separate systems control timing.** Both must be updated for a change to take effect.

| Where | Controls | File |
|---|---|---|
| Firestore (`viewingTimeSeconds`, `writingTimeMinutes` per image in batch_001) | Image download → `TATQuestion` fields | `step1_upload.py` defaults, lines ~180–185 |
| `TATTestConfig` defaults in domain model | Fallback when no config loaded | `core/domain/.../model/TATTest.kt` |
| `TATTestViewModel` timer launch | Actual countdown | reads from `TATTestConfig` via `LoadTATTestUseCase` |

**To change globally (e.g., writing time 4 min → 5 min):**

1. Edit `step1_upload.py` — find the image builder block:
   ```python
   "viewingTimeSeconds": 30,      # change here
   "writingTimeMinutes": 4,       # change here
   "minCharacters": 150,
   "maxCharacters": 1500,
   ```

2. Edit `TATTestConfig` defaults in `core/domain/src/main/kotlin/com/ssbmax/core/domain/model/TATTest.kt`:
   ```kotlin
   data class TATTestConfig(
       val viewingTimeSeconds: Int = 30,   // change here
       val writingTimeMinutes: Int = 4,    // change here
       val minCharacters: Int = 150,
       val maxCharacters: Int = 1500
   )
   ```

3. Re-run step1 to push the new Firestore values:
   ```bash
   python step1_upload.py
   ```

4. Build and release a new app version (the `TATTestConfig` change requires a code release).

> **Note:** Changing only step1 (Firestore) without a code release means existing app installs still use the old `TATTestConfig` defaults. Both must be in sync.

---

### Procedure F — Tune AI Parameters

All AI parameters are in source code. No Firebase writes needed. Requires code change + app build.

| Parameter | File | Line (approx) | Current Value | Notes |
|---|---|---|---|---|
| `MIN_STORY_THRESHOLD` | `app/.../workers/TATSynthesisWorker.kt` | ~53 | `6` | Min valid stories for synthesis to proceed |
| `MAX_AI_RETRIES` (synthesis) | same | ~52 | `3` | Retries for holistic synthesis call |
| `MAX_AI_RETRIES` (per-story) | `app/.../workers/TATStoryAnalysisWorker.kt` | ~30 | `3` | Retries per per-story worker |
| Per-story model tier | `core/data/.../ai/GeminiAIService.kt` | Tier 1 | `model` (8192 tokens, 60 s) | Upgrade to `largeContextModel` if stories are very long |
| Synthesis model tier | same | Tier 3 | `synthesisModel` (16384 tokens, 120 s) | Do not downgrade — 8192 consistently hits MAX_TOKENS |
| Story prompt structure | `core/data/.../ai/prompts/TATStoryAnalysisPrompts.kt` | full file | — | Edit `generateTATStoryMultimodalPrompt()` |
| Synthesis prompt structure | `core/data/.../ai/prompts/TATSynthesisPrompts.kt` | full file | — | Edit `buildPrompt()` |

**After any prompt change:** Run the full prompt test suite to verify parsers and rubric contract:
```bash
./gradlew :core:data:testDebugUnitTest --tests "*TATStoryPromptRules*"
./gradlew :core:data:testDebugUnitTest --tests "*TATSynthesisPromptRules*"
./gradlew :core:data:testDebugUnitTest --tests "*GeminiResponseParser*"
./gradlew :core:domain:testDebugUnitTest --tests "*ValidationIntegration*"
```

---

### §22 Constants — Copy-Paste Reference

**Valid OLQ names (exact, case-sensitive):**
```
EFFECTIVE_INTELLIGENCE   REASONING_ABILITY   ORGANIZING_ABILITY
POWER_OF_EXPRESSION      SOCIAL_ADJUSTMENT   COOPERATION
SENSE_OF_RESPONSIBILITY  INITIATIVE          SELF_CONFIDENCE
SPEED_OF_DECISION        INFLUENCE_GROUP     LIVELINESS
DETERMINATION            COURAGE             STAMINA
```

**Card positions (1–11; 12 = blank card, programmatic):**
```
1  Individual Adversity / Initiative
2  Social / Group Cooperation
3  Persistence / Stamina
4  Crisis / Quick Decision
5  Planning / Organizing
6  Duty / Responsibility
7  Leadership / Influence
8  Energy / Social Engagement
9  Problem Solving / Analysis
10 Courage / Determination
11 Self-Reliance / Confidence
```

**Firebase paths:**
```
Firestore doc  : test_content/tat/image_batches/batch_001
Storage prefix : gs://ssbmax-49e68.firebasestorage.app/tat_images/batch_001/
Public URL     : https://storage.googleapis.com/ssbmax-49e68.firebasestorage.app/tat_images/batch_001/{imageId}.jpg
```

**Gender tags:** `"MALE"` | `"FEMALE"` | `"MIXED"`

**Deviation tolerance:** `"LOW"` | `"MEDIUM"` | `"HIGH"`

**Image ID format:** `tat_{NNN}_{gender}` where NNN = zero-padded scene number, gender = `male` | `female` | `mixed`
Example: scene 27, male variant → `tat_027_male`

---

### Pre-Upload Safety Checklist (run before every `step1_upload.py`)

```
[ ] Firestore backup: export batch_001 JSON from Firebase Console (takes 30 s)
[ ] preview.html reviewed: all positions ≥ 10 images, ≥ 3 FEMALE+MIXED per position
[ ] No red warnings in preview.html
[ ] dry-run passed: python step1_upload.py --dry-run (zero errors)
[ ] totalImages count is correct in dry-run output
[ ] Git commit of tat_image_contexts.json before upload (so you can roll back context)
```

**Roll-back procedure** (if upload produces wrong results):
1. Restore `tat_image_contexts.json` from git: `git checkout -- scripts/tat-picture-pipeline/tat_image_contexts.json`
2. Re-run `step1_upload.py` — this bumps version again and re-uploads the previous set
3. Android clients pick up the restored version within 24h (or immediately after app relaunch)

---

### Step 0 Script Quick Reference

```bash
# Full re-generation (all 167 images) — ~15 min, costs Gemini API quota
python step0_generate_context.py

# Resume interrupted run (skips already-processed IDs)
python step0_generate_context.py --resume

# Re-process specific scenes only (e.g., scenes 12, 27, 43)
python step0_generate_context.py --ids 12 27 43

# Rebuild preview.html from existing draft (no API calls)
python step0_generate_context.py --preview-only

# Promote draft to production after review
cp output/tat_context_draft.json tat_image_contexts.json
```

### Step 1 Script Quick Reference

```bash
# Validate only — no Firebase writes
python step1_upload.py --dry-run

# Full upload (delete old Storage + re-upload all + write Firestore)
python step1_upload.py

# Re-upload all images + write Firestore, but skip Storage deletion
python step1_upload.py --skip-delete

# Skip input validation (not recommended — use only if validation script is wrong)
python step1_upload.py --skip-validate

# Use explicit service account (instead of Application Default Credentials)
python step1_upload.py --service-account /path/to/key.json
```
