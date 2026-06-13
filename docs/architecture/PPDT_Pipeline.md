# PPDT Pipeline Architecture

**Last updated:** June 2026  
**Status:** Living document — update when fixing bugs, improving flow, or adding features.

Picture Perception and Description Test (PPDT) is a Phase 1 psychology test in SSBMax. The candidate views a blurry image for 30 seconds, then writes a story based on what they perceived. The story is evaluated by Gemini AI against 15 Officer-Like Qualities (OLQs), and the result feeds the unified OLQ dashboard.

---

## 1. User Journey Overview

```
StudentHomeScreen
  └─► TopicScreen (Phase 1 → Tests tab)
        └─► PPDTTestScreen
              ├─ [1] INSTRUCTIONS
              ├─ [2] IMAGE_VIEWING  (30 s, auto-advance)
              ├─ [3] WRITING        (4 min, auto-advance)
              ├─ [4] REVIEW
              └─ [5] SUBMITTED ──► PPDTSubmissionResultScreen
                                        └─► OLQ results (async via WorkManager)
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
| `PPDTImageViewingPhase` | `components/phases/PPDTImageViewingPhase.kt` | Coil `AsyncImage` + 30 s progress indicator |
| `PPDTWritingPhase` | `components/phases/PPDTWritingPhase.kt` | `OutlinedTextField` with char count (min 200, max 1000), keyboard padding |
| `PPDTReviewPhase` | `components/phases/PPDTReviewPhase.kt` | Read-only story preview, "Edit Story" returns to WRITING |

### Shared components

- `PPDTComponents.kt` — `PPDTTopBar` (phase name, timer, exit), `PPDTBottomBar` (Next/Review/Submit), `TimerChip` (MM:SS, red when < 30 s)
- `PPDTDialogs.kt` — `PPDTExitDialog`, `PPDTSubmitDialog`

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
    val resetsAt: String = ""
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

### Configuration-change recovery

`timerStartTime: Long` is stored in `UiState`. On ViewModel recreation, `restoreTimerIfNeeded()` computes elapsed time and resumes the timer with the remaining seconds — preventing both timer loss and timer restart on recomposition.

---

## 6. Image Loading Pipeline

### Source (Firestore)

```
test_content/ppdt/image_batches/{batchId}
  └── images: List<{
        id, imageUrl (Firebase Storage URL),
        imageDescription, context (for AI),
        viewingTimeSeconds (30), writingTimeMinutes (4),
        minCharacters (200), maxCharacters (1000),
        category, difficulty
      }>
```

Default batch: `batch_002_context_enhanced` (15+ images).

### Local cache (Room)

- **Entity:** `CachedPPDTImageEntity` → table `cached_ppdt_images`
- **DAO:** `PPDTImageCacheDao` — queries include least-used selection, batch fetch, usage tracking
- **Manager:** `core/data/.../repository/PPDTImageCacheManager.kt`
  - Target cache: 15 images; minimum: 5 (triggers `initialSync()`)
  - Selection: `getLeastUsedImages()` — rotates images so candidates don't repeat
  - Progressive: batches downloaded on-demand

### Display (UI)

```kotlin
// PPDTImageViewingPhase.kt
val imageRequest = remember(imageUrl) {
    ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
}
AsyncImage(model = imageRequest, contentScale = ContentScale.Fit)
```

`remember(imageUrl)` keeps the request stable across recompositions; Coil handles HTTP disk cache.

---

## 7. Subscription Check

Called before test content loads in `PPDTTestViewModel`:

```
SubscriptionManager.canTakeTest(TestType.PPDT, userId)
  → TestEligibility { canProceed, testsUsed, testsLimit, resetsAt }
```

- If limit reached: `isLimitReached = true` → `TestLimitReachedDialog` shown; test does not load.
- Usage persisted in Firestore: `users/{userId}/subscription/usage_{month}.ppdtTestsUsed` (incremented atomically on submission).
- Debug override: `BuildConfig.BYPASS_SUBSCRIPTION_LIMITS = true` returns 999 remaining.

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
| 3 | Fetch image context: `TestContentRepository.getPPDTQuestion(questionId)` (Room cache → Firestore fallback) |
| 4 | Build Gemini prompt: `PsychologyTestPrompts.generatePPDTAnalysisPrompt()` or `EnhancedPsychologyPrompts.buildPPDTPrompt()` |
| 5 | Call Gemini AI: `GeminiAIService.analyzePPDTResponse(prompt)` — model `gemini-2.5-flash`, 60 s timeout, max 3 retries with exponential backoff |
| 6 | Parse JSON response → 15 `OLQScore` objects |
| 7 | SSB validation: `ValidationIntegration.validateScores(olqScores, EntryType.NDA)` — checks Factor II critical rules |
| 8 | Build `OLQAnalysisResult`: overallScore (avg of 15), overallRating, top 3 strengths (lowest scores), bottom 3 weaknesses (highest scores) |
| 9 | Atomic Firestore batch write: `ppdt_results/{submissionId}` (full result) + `submissions/{submissionId}` (status → COMPLETED) |
| 10 | Invalidate dashboard cache: `GetOLQDashboardUseCase.invalidateCache(userId)` + push notification |

**Error path:** On failure after retries → `analysisStatus = FAILED`, failure notification sent, `Result.failure()` returned to WorkManager.

---

## 10. Gemini AI Prompt Design

**Prompt builders:**
- `core/data/.../ai/prompts/PsychologyTestPrompts.kt` → `generatePPDTAnalysisPrompt(submission, imageContext, candidateGender)`
- `core/data/.../ai/prompts/EnhancedPsychologyPrompts.kt` → `buildPPDTPrompt(...)` (uses `SSBPromptCore` for standardised SSB context)

**Scoring scale:** 1–10, **LOWER = BETTER** (SSB convention — opposite of typical grading)

| Rating | Score Range |
|--------|-------------|
| Exceptional | ≤ 3 |
| Good | ≤ 5 |
| Average | ≤ 7 |
| Needs Improvement | > 7 |

**Story signals that BOOST scores (lower numbers):**

| Signal | OLQs Boosted |
|--------|-------------|
| Proactive hero | INITIATIVE, COURAGE |
| Teamwork / helping others | COOPERATION, SOCIAL_ADJUSTMENT |
| Clear problem-solving | REASONING_ABILITY, EFFECTIVE_INTELLIGENCE |
| Past-Present-Future structure | ORGANIZING_ABILITY |
| Goal established in first 2–3 lines | SPEED_OF_DECISION |
| Optimistic outcome through effort (not luck) | DETERMINATION, LIVELINESS |

**Story signals that PENALISE (raise numbers):**

- Material rewards (money, prize, trophy) — caps maximum score
- Passive hero who is acted upon
- Unclear narrative / no central goal

**Mandatory JSON output from Gemini:**

```json
{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": { "score": 5, "confidence": 85, "reasoning": "..." },
    "REASONING_ABILITY": { "score": 6, "confidence": 80, "reasoning": "..." }
    // ... all 15 OLQs required
  }
}
```

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
  ├── version: String
  └── images: List<{
        id, imageUrl, imageDescription, context,
        viewingTimeSeconds, writingTimeMinutes,
        minCharacters, maxCharacters,
        category, difficulty
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
2. When `analysisStatus == COMPLETED` detected → calls `SubmissionRepository.getPPDTResult(submissionId)` (reads `ppdt_results` collection)
3. Builds `SSBRecommendationUIModel` from OLQ scores
4. Exposes `PPDTSubmissionResultUiState` (implements `UnifiedResultUiState` — shared interface across all test result ViewModels)

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

---

## 14. Domain Models Quick Reference

| Class | File | Purpose |
|-------|------|---------|
| `PPDTQuestion` | `core/domain/.../model/PPDTTest.kt` | Image URL, context for AI, viewing/writing time config |
| `PPDTSubmission` | same | Full submission including story, timings, `analysisStatus`, `olqResult` |
| `PPDTPhase` | same | Enum: INSTRUCTIONS → IMAGE_VIEWING → WRITING → REVIEW → SUBMITTED |
| `PPDTTestSession` | same | Active session tracking (sessionId, phaseId, startTimes) |
| `PPDTTestConfig` | same | Defaults for timing & character limits |
| `PPDTDetailedScores` | same | Instructor grading breakdown (perception, imagination, narration, characterDepiction, positivity) |
| `PPDTInstructorReview` | same | Manual instructor review (instructorId, finalScore 0–100, agreedWithAI) |
| `OLQAnalysisResult` | `core/domain/.../model/scoring/UnifiedOLQResult.kt` | Unified AI result (15 OLQ scores, overallScore, rating, strengths, weaknesses) |
| `OLQ` | `core/domain/.../model/interview/OLQ.kt` | Enum of all 15 qualities with Factor grouping & critical-flag metadata |
| `AnalysisStatus` | `UnifiedOLQResult.kt` | PENDING_ANALYSIS, ANALYZING, COMPLETED, FAILED |

---

## 15. Test Coverage

| Test File | Module | What It Covers |
|-----------|--------|---------------|
| `PPDTTestViewModelTest.kt` | `app` | Question loading, phase transitions, story writing, submission |
| `PPDTImageCacheManagerTest.kt` | `core:data` | Cache sync, eviction, status tracking |
| `PPDTImageCacheDaoTest.kt` | `core:data` | Room DAO queries |
| `PPDTPromptContextValidationTest.kt` | `core:data` | AI prompt generation validation |
| `GetOLQDashboardUseCaseTest.kt` | `core:domain` | Dashboard aggregation logic, timeout/cache behaviour |

Run all PPDT-relevant tests:

```bash
./gradlew :app:testDebugUnitTest --tests "*PPDT*"
./gradlew :core:data:testDebugUnitTest --tests "*PPDT*"
./gradlew :core:domain:testDebugUnitTest --tests "*PPDT*"
```

---

## 16. Content Ingestion Scripts

Images are uploaded to Firebase Storage and metadata written to Firestore via Node.js scripts in `scripts/`:

| Script | Purpose |
|--------|---------|
| `upload_ppdt_images.js` | Full ingestion with validation |
| `upload_ppdt_images_simple.js` | Simple upload (no validation) |
| `update_ppdt_urls_smart.js` | Update Storage URLs in Firestore |
| `update_ppdt_image_urls_fixed.js` | URL correction |
| `make_ppdt_images_public.js` | Public access config |
| `add_remaining_ppdt_images.js` | Batch remainder handling |

**Ingestion principle (from `CLAUDE.md`):** LLM is NOT used for `imageUrl`, `imageDescription`, or `context` fields — these are set deterministically. LLM is only used for enrichment tags (difficulty, category).

---

## 17. Known Issues & Improvement Areas

_Update this section as bugs are found and improvements are made._

| # | Area | Issue / Improvement | Status |
|---|------|---------------------|--------|
| 1 | Gender routing | `getPPDTQuestion(genderTag)` filter is a **no-op** until Phase 6 adds `genderTag` field to `CachedPPDTImageEntity` + Room migration. Routing infrastructure is in place (Phase 3); activation is automatic once Phase 5 image upload + Phase 6 migration ship. | Deferred to Phase 6 |
| 2 | `PPDTTestViewModel` | File is 620 lines (300-line limit). Requires split into `PPDTTestViewModel.kt` + `PPDTSubmitViewModel.kt` or similar. Pre-existing tech debt; not introduced by Phase 3. | Deferred |

---

## 18. Data Flow Diagram (End-to-End)

```
[StudentHomeScreen]
     │ tap PPDT
     ▼
[PPDTTestScreen] ──loadTest("ppdt_standard")──► [TestContentRepository]
                                                      │
                                              Room cache hit?
                                              ├─ YES: CachedPPDTImageEntity
                                              └─ NO : Firestore test_content/ppdt/image_batches
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
     │                             ├── Fetch submission
     │                             ├── Fetch image context (Room/Firestore)
     │                             ├── Build prompt (PsychologyTestPrompts)
     │                             ├── Gemini gemini-2.5-flash (60 s, 3 retries)
     │                             ├── Validate 15 OLQ scores (SSB rules)
     │                             ├── Firestore batch write:
     │                             │     ppdt_results/{id}   ← full OLQAnalysisResult
     │                             │     submissions/{id}    ← status COMPLETED
     │                             └── Invalidate dashboard cache + push notification
     │
     ▼
[PPDTSubmissionResultScreen]
     │ observePPDTSubmission() real-time listener
     ├─ analysisStatus == PENDING/ANALYZING: show loading
     └─ analysisStatus == COMPLETED:
           getPPDTResult(submissionId) ──► ppdt_results/{id}
           Display OLQ scores, rating, strengths, weaknesses

[StudentHomeScreen → OLQDashboardCard]
     │ GetOLQDashboardUseCase (5-min cache, 6-s per-type timeout)
     └── Phase1Results.ppdtOLQResult ──► unified OLQ dashboard
```

---

## 19. Current Gaps (Confirmed by Code Analysis)

These are verified gaps in the current implementation, not assumptions:

| # | Gap | Where | Impact |
|---|-----|--------|--------|
| 1 | `PPDTQuestion.context` is empty string in production | `batch_001` has no context; `batch_002` referenced in code but doesn't exist in Firestore | Gemini gets zero picture-specific guidance — all 50 pictures scored identically |
| 2 | Generic prompt — no per-picture rubric | `PsychologyTestPrompts.generatePPDTAnalysisPrompt()` | Gemini can't penalize wrong scene interpretation |
| 3 | `candidateGender` hardcoded to `"male"` | `PPDTAnalysisWorker` | Every candidate, regardless of gender, gets male-protagonist guidance |
| 4 | Gemini temperature not set (default ~0.7–1.0) | `GeminiAIService` | Score drift: same story submitted twice gets different scores |
| 5 | `batch_001` uses placeholder images | `scripts/ppdt_batch_001.json` | No real pictures in production — `via.placeholder.com` URLs |
| 6 | `minCharacters` inconsistency | Domain model says 50, UI enforces 200 | Inconsistent contract; neither is well-justified |
| 7 | `OLQScore.reasoning` never shown in UI | `PPDTSubmissionResultScreen` | Candidates see a score number but not why — feedback loop broken |
| 8 | No repeat-picture deduplication | `PPDTImageCacheManager.getLeastUsedImages()` rotates by count only | Same picture can repeat for frequent users; no anti-gaming logic |

---

## 20. Target Architecture — Multimodal Evaluation

**Decision:** Gemini receives the actual image bytes + the story (multimodal), not just a text description.  
**Rationale:** Gemini 2.5 Flash supports vision. Direct image input is more accurate than relying on a human-written text description and eliminates the context-authoring bottleneck for verifying scene accuracy.

### New evaluation flow (post-improvement)

```
PPDTAnalysisWorker
  │
  ├─ Fetch submission (story + questionId)
  ├─ Fetch PPDTQuestion (imageUrl + structured PPDTImageContext)
  ├─ Download image bytes from Firebase Storage          ← NEW
  │
  └─ GeminiAIService.analyzePPDTMultimodal(
         imageBytes: ByteArray,                          ← NEW (actual picture)
         story: String,
         imageContext: PPDTImageContext,                 ← NOW structured (see Section 21)
         candidateGender: String                         ← NOW from UserProfile
     )
```

### Key API changes required

| File | Current | Target |
|------|---------|--------|
| `GeminiAIService` | `analyzePPDTResponse(prompt: String)` | `analyzePPDTMultimodal(imageBytes, story, context, gender)` |
| `PPDTAnalysisWorker` | Fetches text context only | Also downloads image bytes from Firebase Storage |
| `GeminiAIService` | `temperature` not set (default) | `temperature = 0` for determinism |
| `PPDTAnalysisWorker` | `candidateGender = "male"` hardcoded | Read from `UserProfileRepository.getUserProfile(userId).gender` |

---

## 21. New Per-Picture Context Schema — PPDTImageContext

Replace `PPDTQuestion.context: String` with a structured domain class. The text `context` string is too loose — Gemini has no idea which elements are core vs. optional, which themes are penalized, or which OLQs a picture targets.

### Kotlin domain class (new, in `core/domain/model/PPDTTest.kt`)

```kotlin
data class PPDTImageContext(
    val sceneDescription: String,               // One-line human-readable scene summary
    val coreElements: List<String>,             // Must be acknowledged → EFFECTIVE_INTELLIGENCE penalty if missed
    val ambiguousElements: List<String>,        // Creative interpretation OK (hazy-tolerance)
    val expectedThemes: List<String>,           // Acceptable story directions → positive scoring
    val penalizedThemes: List<String>,          // Wrong/irrelevant story directions → heavy penalty
    val primaryOLQs: List<String>,              // OLQ names this picture is designed to test
    val deviationTolerance: DeviationTolerance, // How strictly scene accuracy is enforced
    val exemplarGoodHints: List<String>,        // Few-shot positive examples for Gemini calibration
    val exemplarBadHints: List<String>          // Few-shot negative examples for Gemini calibration
)

enum class DeviationTolerance { LOW, MEDIUM, HIGH }
```

### Firestore document shape (inside `test_content/ppdt/image_batches/{batchId}.images[]`)

```json
{
  "id": "ppdt_img_rescue_001",
  "imageUrl": "gs://ssbmax.appspot.com/ppdt/images/rescue_001.jpg",
  "imageDescription": "Hazy sketch: struggling figure in water, group on bank",
  "imageContext": {
    "sceneDescription": "A person struggling in a water body; a group of bystanders watching from the bank",
    "coreElements": ["water body", "struggling/drowning figure", "group of onlookers"],
    "ambiguousElements": ["river vs. lake vs. sea", "number of bystanders", "time of day"],
    "expectedThemes": ["rescue attempt", "leader takes charge", "community rallies to help", "overcoming fear"],
    "penalizedThemes": ["story unrelated to water or rescue", "supernatural/magical rescue", "bystanders passively congratulated for watching"],
    "primaryOLQs": ["COURAGE", "INITIATIVE", "SOCIAL_ADJUSTMENT", "SPEED_OF_DECISION"],
    "deviationTolerance": "MEDIUM",
    "exemplarGoodHints": ["Hero immediately assesses the situation and jumps in, directing others to get rope"],
    "exemplarBadHints": ["Story about a drama being filmed near a lake with no connection to the drowning figure"]
  },
  "category": "rescue",
  "difficulty": "medium",
  "viewingTimeSeconds": 30,
  "writingTimeMinutes": 4,
  "minCharacters": 200,
  "maxCharacters": 1000
}
```

### Backward compatibility note
The Room entity `CachedPPDTImageEntity.context: String` must be migrated to `imageContext: String` (JSON-serialized `PPDTImageContext`) with a Room database migration. Old records default to an empty `PPDTImageContext`.

---

## 22. Multimodal Gemini Prompt Design

The prompt sends both the image and a per-picture scoring rubric. Gemini directly verifies scene accuracy from the image, and the rubric tells it what to penalize.

```
[IMAGE ATTACHED — actual picture bytes]

You are an SSB PPDT examiner. A candidate viewed this picture for 30 seconds (it was shown hazy and low-resolution) and then wrote a story. Evaluate the story.

=== PICTURE BRIEFING ===
Scene: {imageContext.sceneDescription}

Core elements (candidate MUST acknowledge or loses EFFECTIVE_INTELLIGENCE marks):
{imageContext.coreElements → bulleted list}

Ambiguous elements (hazy picture — creative interpretation is acceptable):
{imageContext.ambiguousElements → bulleted list}

Expected story themes (score positively):
{imageContext.expectedThemes → bulleted list}

Penalized story themes (heavy penalty — story is off-target):
{imageContext.penalizedThemes → bulleted list}

Primary OLQs this picture tests: {imageContext.primaryOLQs}
Deviation tolerance: {imageContext.deviationTolerance}

Calibration examples:
  GOOD story: {imageContext.exemplarGoodHints[0]}
  BAD story: {imageContext.exemplarBadHints[0]}

=== CANDIDATE STORY ===
{story}
(Length: {charactersCount} chars | Writing time: {writingTimeTakenMinutes} min)

=== CANDIDATE PROFILE ===
Gender: {candidateGender}
[Note: candidate should ideally write a protagonist matching their gender]

=== SSB SCORING SCALE (LOWER = BETTER) ===
5 = Very Good | 6 = Good | 7 = Average | 8 = Poor | 9 = Fail/Garbage
Use ONLY scores 5–9. Never use 1–4 or 10.

=== MANDATORY SCORING RULES ===
1. SCENE ACCURACY (check against the image you can see):
   - Core elements missing from story → EFFECTIVE_INTELLIGENCE score ≥ 8
   - Completely wrong scene interpretation → EFFECTIVE_INTELLIGENCE = 9, penalize Factor III OLQs
   - Ambiguous elements mis-stated → no penalty (hazy tolerance)

2. STORY QUALITY (standard SSB):
   - Proactive hero, early goal → boost INITIATIVE, SPEED_OF_DECISION
   - Teamwork, helping others → boost COOPERATION, SOCIAL_ADJUSTMENT
   - Material reward (money/prize) → cap primary OLQ at 7
   - Passive hero → penalize INITIATIVE, SELF_CONFIDENCE
   - Past-Present-Future structure → boost ORGANIZING_ABILITY

3. DETERMINISM: Score identically if given the same story. Do not vary.

4. GARBAGE DETECTION: Gibberish/random text → score 9 for all OLQs.

=== OUTPUT FORMAT (JSON only, no markdown, start with {) ===
{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 6, "confidence": 85, "reasoning": "Candidate correctly identified the rescue scenario and showed practical judgment"},
    ...all 15 OLQs...
  }
}
```

---

## 23. OLQ Coverage Matrix — 50-Picture Bank Design

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

## 24. Picture Creation Pipeline

### Design principles
- **B&W photograph style** — photo-realistic images of real human figures in real settings (NOT pencil sketches). Think staged black-and-white photographs.
- **PPDT vs TAT difference:** PPDT pictures are made hazy/blurry intentionally; TAT pictures are relatively clear. Same base image type, different post-processing.
- **Low resolution + grainy** — Gaussian noise + slight blur applied over the base image. Not HD; deliberately hard to see clearly at a glance.
- **Human figures present** — 1 to 6 people in realistic settings (office, outdoor, group meeting, etc.); no abstract or single-object images
- **No text or labels** in the image
- **Ambiguous but recognizable** — candidate can identify the core scene and characters but fine details are debatable (which is the point)

### Image style reference
The SSB PPDT picture style (from actual SSB centres):
- Black and white base image (monochrome)
- Photo-realistic human figures — real people, real environments
- Grainy texture overlaid (film grain / noise)
- Slightly reduced contrast — mid-tones flattened
- PPDT: additional haze/blur layer (Gaussian blur ~2–4px radius) making it harder to discern fine details
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
- [ ] Matches the intended OLQ category from the matrix (Section 23)
- [ ] Aspect ratio consistent across the batch (~4:3)

### Context generation script (one-time offline, per picture)
**File:** `scripts/ppdt-picture-pipeline/generate_context.py`

For each image:
1. Send image to Gemini Vision with structured extraction prompt:
   ```
   "Analyze this SSB PPDT-style picture. Extract in JSON:
   sceneDescription, coreElements[], ambiguousElements[], expectedThemes[],
   penalizedThemes[], primaryOLQs[] (from: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY,
   ORGANIZING_ABILITY, POWER_OF_EXPRESSION, SOCIAL_ADJUSTMENT, COOPERATION,
   SENSE_OF_RESPONSIBILITY, INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION,
   INFLUENCE_GROUP, LIVELINESS, DETERMINATION, COURAGE, STAMINA),
   deviationTolerance (LOW|MEDIUM|HIGH)"
   ```
2. Human reviews JSON output in HTML preview (image left, extracted JSON right)
3. Edit/approve
4. Upload to Firestore `test_content/ppdt/image_batches/{batchId}`

### Deployment gate (same pattern as OIR ingestion)
- HTML preview with image + generated context side-by-side
- Gate blocks Firestore write until approved
- Batch operations (Firestore max 500 docs per write)
- Checkpoint-based resumable uploads (idempotent by `id`)

---

## 25. Pre-Test Profile Gate (Gender Check)

### Problem
`PPDTAnalysisWorker` hardcodes `candidateGender = "male"`. Gender is stored in the app's Settings → Profile screen but is never read before the test.

### Design

`PPDTTestViewModel.loadTest()` must run two checks in sequence before loading a picture:

```
loadTest(testId)
  ├─ [1] checkSubscriptionEligibility()     ← already exists
  ├─ [2] checkProfileCompleteness()         ← NEW
  │        └─ UserProfileRepository.getUserProfile(userId)
  │             ├─ gender is set → proceed
  │             └─ gender is null/empty → emit ProfileIncompleteEvent, stop loading
  └─ [3] loadPPDTQuestion(testId)           ← only if 1 & 2 pass
```

### UiState additions

```kotlin
data class PPDTTestUiState(
    // ... existing fields ...
    val isProfileIncomplete: Boolean = false,
    val profileRequiredField: String? = null   // "gender" — tells UI which field is missing
)
```

### UI behavior

When `isProfileIncomplete == true`:
- Show dialog: "Complete your profile to take PPDT. Your gender helps AI provide accurate assessment. Go to Settings → Profile."
- Two buttons: "Go to Settings" (navigates to profile screen) and "Cancel" (goes back)
- Test does NOT load until profile is complete

### Worker fix (when implementing)

In `PPDTAnalysisWorker`, replace:
```kotlin
val candidateGender = "male"  // TODO: get from user profile
```
with:
```kotlin
val candidateGender = userProfileRepository.getUserProfile(submission.userId)
    .getOrNull()?.gender ?: "unknown"
```
