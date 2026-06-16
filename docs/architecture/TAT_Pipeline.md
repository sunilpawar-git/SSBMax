# TAT Pipeline Architecture

**Last updated:** June 2026 (baseline audit complete; upgrade roadmap locked)
**Status:** Living document — update when fixing bugs, improving flow, or adding features. Tracks current state AND target architecture.

Thematic Apperception Test (TAT) is a Phase 1 psychology test in SSBMax. The candidate is shown 11 picture cards (one at a time) and a blank card (card 12), each for 30 seconds, then writes a story for each within 4 minutes. All 12 stories are evaluated together by Gemini AI against 15 Officer-Like Qualities (OLQs), and the result feeds the unified OLQ dashboard.

**TAT vs PPDT:**

| Aspect | PPDT | TAT |
|--------|------|-----|
| Images | 1 image per test | 11 picture cards + 1 blank card |
| Story | 1 story | 12 stories |
| Image clarity | Deliberately hazy/blurred | Clear B&W photo-realistic |
| Analysis | Single multimodal Gemini call | 12 per-story assessments → 1 holistic synthesis (target) |
| Assessment depth | Per-image rubric (PPDTImageContext) | Per-image rubric (TATImageContext) + cross-story synthesis |
| Blank card | Not applicable | Card 12 — candidate projects from imagination |

---

## 1. User Journey Overview

```
StudentHomeScreen
  └─► TopicScreen (Phase 1 → Tests tab)
        └─► TATTestScreen
              ├─ [0] INSTRUCTIONS
              ├─ [1..12] For each card:
              │    ├─ IMAGE_VIEWING  (30 s, auto-advance)
              │    ├─ WRITING        (4 min, auto-advance)
              │    └─ REVIEW_CURRENT (user confirms → next card)
              └─ SUBMITTED ──► TATSubmissionResultScreen
                                    └─► OLQ results + per-OLQ reasoning (async via WorkManager)
```

Card 12 is a blank card — no image shown, candidate writes a story from imagination.

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

### Phase composables

| Composable | File | Responsibility |
|------------|------|----------------|
| `TATInstructionsPhase` | `components/phases/TATInstructionsPhase.kt` | Instructions card, "Start Test" button |
| `TATImageViewingPhase` | `components/phases/TATImageViewingPhase.kt` | Image display + 30 s viewing timer |
| `TATWritingPhase` | `components/phases/TATWritingPhase.kt` | `OutlinedTextField` + char count + 4 min timer |
| `TATReviewPhase` | `components/phases/TATReviewPhase.kt` | Read-only story preview, "Edit" / "Confirm" buttons |

### Shared components

- `TATComponents.kt` — `TATTopBar`, `TATBottomBar`, `TimerChip`
- `TATDialogs.kt` — `TATExitDialog`, `TATSubmitDialog`

### Known issue: file over limit

`TATTestViewModel.kt` is **677 lines** (limit 300). `TATTestUiState` is defined at the bottom of the same file — it should be its own file `TATTestUiState.kt`.

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
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: TATSubmission? = null,   // ⚠️ architectural smell — see §10
    val error: String? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L
)
```

**Computed properties:**
- `currentQuestion: TATQuestion?` — `questions.getOrNull(currentQuestionIndex)`
- `completedStories: Int` — `responses.size`
- `progress: Float` — `completedStories / questions.size`
- `canMoveToNextQuestion: Boolean` — story meets minCharacters AND phase is WRITING or REVIEW_CURRENT
- `canMoveToPreviousQuestion: Boolean` — `currentQuestionIndex > 0 && phase == WRITING` (⚠️ Bug B7 — must be removed)
- `canSubmitTest: Boolean` — `completedStories >= questions.size`

**Phase transition (repeated 12 times, once per card):**

```
[Per card N]
IMAGE_VIEWING (30 s, auto-advance)
  ──[timer = 0, auto]──►
WRITING (240 s, auto-advance)
  ──[story ≥ minChars, user taps Next]──►
REVIEW_CURRENT
  ──[editCurrentStory()]──► WRITING (restart timer ← Bug B1 risk)
  ──[confirmCurrentStory()]──►
    if N < 12 → IMAGE_VIEWING for card N+1
    if N = 12 → SUBMITTED ──► TATSubmissionResultScreen
```

**`TATPhase` enum:** `INSTRUCTIONS`, `IMAGE_VIEWING`, `WRITING`, `REVIEW_CURRENT`, `SUBMITTED`

> **Target rename:** `REVIEW_CURRENT` → `REVIEW` (Phase 3)

---

## 5. Timer Logic

### Current implementation (dual-Job, has race condition)

| Phase | Duration | Trigger |
|-------|----------|---------|
| IMAGE_VIEWING | 30 s | `startTest()` / next card |
| WRITING | 240 s | IMAGE_VIEWING timer expiry |

Two separate Job fields: `viewingTimerJob: Job?` and `writingTimerJob: Job?`. Both use **delta-based** calculation (`endTime - System.currentTimeMillis()`, update every 200 ms).

**Bug B1 — Timer race condition:**

```
startViewingTimer()
  └─► viewingTimerJob = launch {
        ...timer loop...
        // expires:
        _uiState.update { it.copy(phase = WRITING, isTimerActive = false) }  // ← sets false
        startWritingTimer()  ← immediately sets isTimerActive = true
      } finally {
        _uiState.update { it.copy(isTimerActive = false) }  // ← races with writing timer!
      }
```

The `finally` block of the viewing timer coroutine fires **after** `startWritingTimer()` sets `isTimerActive = true`, flipping it back to `false`. Writing timer bar appears frozen.

**Config-change recovery:** `timerStartTime: Long` stored in UiState; `restoreTimerIfNeeded()` restarts the appropriate timer on ViewModel recreation. Partially effective but the dual-Job pattern makes it fragile.

### Target implementation (generation-token pattern — same as PPDT)

```kotlin
private var timerGeneration = 0L  // ViewModel-private, not in UiState

private fun startTimer(seconds: Int) {
    val myGeneration = ++timerGeneration
    _uiState.update { it.copy(timerStartTime = myGeneration, isTimerActive = true, ...) }
    timerJob = viewModelScope.launch {
        try { /* timer loop */ }
        finally {
            _uiState.update { current ->
                if (current.timerStartTime == myGeneration)
                    current.copy(isTimerActive = false)
                else current   // ← safe: newer timer already started
            }
        }
    }
}
```

Single `timerJob: Job?` replaces dual Jobs. Generation token guards against stale `finally` blocks.

---

## 6. loadTest() Flow

**Current (all inlined in ViewModel, 140 lines):**

```
loadTest(testId)
  Step 1: observeCurrentUser().first() — 3 s timeout
  Step 2: subscriptionManager.canTakeTest(TestType.TAT, userId)
           → LimitReached → show limit dialog, stop
           → NetworkError → show error, stop
           → Eligible → continue
  Step 3: testSessionRepository.createTestSession(userId, testId, TestType.TAT)
  Step 4: testContentRepository.getTATQuestions(testId)
           → currently calls TATImageCacheManager (see §7)
  → _uiState.update { phase = INSTRUCTIONS, questions = questions }
```

**Bugs / gaps:**
- No gender resolution → no gender-based image routing (compare: PPDT resolves `genderTag` before fetching images)
- No `createDraftSubmission()` → submission is created only at `submitTest()`, not at test start
- `testContentRepository.getTATQuestions()` uses only `batch_001` (see §7)
- No profile gate dialog (PPDT has `PPDTProfileRequiredDialog`)

**Target (Phase 2):**

```
loadTest(testId)
  → Auth check + subscription check (LoadTATTestUseCase)
  → Resolve genderTag from UserProfile (same pattern as PPDTTestViewModel)
      → no gender → isProfileIncomplete = true → show TATProfileRequiredDialog → stop
      → gender set → MALE/FEMALE/MIXED pool routing
  → TATSubmissionRepository.createDraftSubmission() → IN_PROGRESS submission
  → TATImageCacheManager.getImagesForTest(genderTag) → 12 questions (11 cards + blank)
```

---

## 7. Image Loading Pipeline

### Current state (broken)

**Firestore source:**
```
test_content/tat/image_batches/batch_001
  ├── totalImages: Int
  └── images: List<{id, imageUrl, ...}>
```

Only `batch_001` is ever downloaded. `TATImageCacheManager.initialSync()` calls `downloadBatch("batch_001")` hardcoded.

**Room cache (current):**
- Entity: `CachedTATImageEntity` → table `cached_tat_images`
- Fields: `id`, `imageUrl`, `batchId`, `usageCount`, `cachedAt`
- Missing: `cardPosition`, `imageContextJson`, `genderTag` (all needed for target)
- `TARGET_CACHE_SIZE = 12`, `MIN_CACHE_SIZE = 4`

**No staleness gate:** `initialSync()` only checks `currentCount >= TARGET_CACHE_SIZE`. Does not compare Firestore `version` field. No `lastStalenessCheckAt`.

**No gender routing:** `getImagesForTest()` calls `getLeastUsedImages(count)` — no gender filter.

**No `cardPosition` field:** Images have no concept of which position in the 12-card sequence they belong to. All 12 images are returned in database order.

### Target state (Phase 0 content + Phase 2 code)

**Image pool:**
- 132 images = 11 card positions × 12 batch variants
- 1 blank card (position 12) — added programmatically, no Firestore image

**Firestore schema (target):**
```
test_content/tat/image_batches/{batchId}    (batch_001 through batch_012)
  ├── totalImages: 11
  ├── version: "1.0.0"
  └── images: List<{
        id: String,                // "tat_b01_c01"
        imageUrl: String,          // Firebase Storage URL
        cardPosition: Int,         // 1–11 (fixed test sequence position)
        imageContext: {            // TATImageContext — same 9 fields as PPDTImageContext
          sceneDescription, coreElements[], ambiguousElements[],
          expectedThemes[], penalizedThemes[], primaryOLQs[],
          deviationTolerance, exemplarGoodHints[], exemplarBadHints[]
        },
        genderTag: "MALE" | "FEMALE" | "MIXED",
        category: String,
        difficulty: String,
        viewingTimeSeconds: 30,
        writingTimeMinutes: 4,
        minCharacters: 150,
        maxCharacters: 1500
      }>
```

**Room schema (target additions to `CachedTATImageEntity`):**
- `cardPosition: Int` — 1-11; used for position-based selection query
- `imageContextJson: String = "{}"` — serialised `TATImageContext`
- `genderTag: String = "MIXED"` — same as `CachedPPDTImageEntity`
- `@Index` on: `cardPosition`, `genderTag`, `usageCount`, `batchId`

**New DAO method:**
```sql
SELECT * FROM cached_tat_images
WHERE cardPosition = :cardPosition
AND (genderTag = :genderTag OR genderTag = 'MIXED')
ORDER BY usageCount ASC, RANDOM()
LIMIT 1
```

**`TATImageCacheManager.getImagesForTest(genderTag)` (target):**
```
for cardPosition in 1..11:
    getLeastUsedImageByPosition(cardPosition, genderTag)
append blank card at position 12 (from TATTestConfig, no Firestore query)
markImagesAsUsed(selectedIds)
return 12 TATQuestions in fixed sequence
```

**Staleness gate (target — same as PPDTImageCacheManager):**
- `TATBatchMetadataEntity` gets new field `lastStalenessCheckAt: Long = 0L`
- `isCacheStale(batchId)` → reads last check time; if < 24 h, skips Firestore version call
- On Firestore read: bumps `lastStalenessCheckAt`, compares `version` field
- Cache invalidated by bumping `version` in Firestore (done by upload script)

**Gender-based image routing (target — same as PPDT):**
```
loadTest()
  └─ UserProfileRepository.getUserProfile(userId)
       ├─ profile == null (server confirms no profile) → isProfileIncomplete=true, stop
       ├─ gender == MALE   → GenderTag.MALE   → MALE+MIXED pool
       ├─ gender == FEMALE → GenderTag.FEMALE → FEMALE+MIXED pool
       └─ gender == OTHER / network error → null → full pool (no filter)
```

**Display (UI):**
```kotlin
// TATImageViewingPhase.kt (current)
AsyncImage(model = imageUrl, contentScale = ContentScale.Crop)
```

TAT images are clear B&W (no blur). Same `AspectRatio + ContentScale.Crop` layout as PPDT.

**Blank card (position 12) — target UI:**
```kotlin
if (isBlankCard) {
    // Hide image, show prompt
    Text(stringResource(R.string.tat_blank_card_prompt))
    // "Imagine any situation and write a story about it"
} else {
    AsyncImage(model = imageUrl, ...)
}
```

---

## 8. Subscription Check

Called in `loadTest()` via `SubscriptionManager.canTakeTest(TestType.TAT, userId)`:

```
TestEligibility.Eligible     → continue
TestEligibility.LimitReached → isLimitReached = true → show TestLimitReachedDialog
TestEligibility.NetworkError → show error state
```

**SSOT for limits:** `core/data/.../repository/SubscriptionManager.kt`

---

## 9. Per-Card Story Cycle

Repeated 12 times (once per card, including blank card):

```
[Card N starts]
IMAGE_VIEWING phase
  ├─ Image displayed (or blank card prompt for card 12)
  ├─ 30 s countdown timer
  └─ Timer expires → auto-advance to WRITING

WRITING phase
  ├─ OutlinedTextField (currentStory)
  ├─ Char count display (min 150, max 1500)
  ├─ 4 min countdown timer
  └─ Timer expires OR user taps "Review" → REVIEW_CURRENT
     ⚠️ Bug B6: timer expiry moves to REVIEW_CURRENT but currentStory NOT yet
                saved to responses[]. Story lost if user exits from review.

REVIEW_CURRENT phase
  ├─ Read-only story preview
  ├─ "Edit Story" → back to WRITING (timer restarts ← Bug B1 risk)
  └─ "Confirm" → confirmCurrentStory() → moveToNextQuestion()
                  ⚠️ Bug B7: moveToPreviousQuestion() also exists → allows
                              editing confirmed stories → breaks analysis model

[moveToNextQuestion()]
  ├─ Saves currentStory to responses[] (⚠️ only place it's saved — see Bug B6)
  ├─ if N < 12 → next card's IMAGE_VIEWING
  └─ if N = 12 → isTimerActive = false (test complete, allow submit)
```

**Target (Phase 2) — per-card confirm:**
```
confirmCurrentStory()
  → saveCurrentStoryToResponses()   ← always called first (fixes B6)
  → enqueue TATStoryAnalysisWorker(submissionId, storyIndex=N, imageId, storyText)
  → advance to next card (or mark complete)

No moveToPreviousQuestion() — removed entirely (fixes B7)
```

---

## 10. submitTest() Flow

**Current:**

```
submitTest()
  Step 1: observeCurrentUser().first() — 3 s timeout
  Step 2: userProfileRepository.getUserProfile() → resolve subscriptionType
  Step 3: Create TATSubmission object (local, in ViewModel)
          {userId, testId, stories=responses, analysisStatus=PENDING_ANALYSIS}
  Step 4: submitTATTest(submission, batchId=null)
          → writes to Firestore: submissions/{submissionId}
  Step 4a: enqueueTATAnalysisWorker(submissionId)
  Step 5: Record analytics (difficultyManager.recordPerformance)
  Step 6: subscriptionManager.recordTestUsage(TAT, userId, submissionId)
  Step 7: _uiState.update { isSubmitted=true, submission=submission, phase=SUBMITTED }
          ⚠️ Stores full TATSubmission in UiState "to bypass Firestore permission issues"
  Step 8: _navigationEvents.trySend(NavigateToResult)
```

**WorkManager enqueue:**
```kotlin
workManager.enqueueUniqueWork(
    "tat_analysis_$submissionId",
    ExistingWorkPolicy.KEEP,
    OneTimeWorkRequestBuilder<TATAnalysisWorker>()
        .setInputData(workDataOf(KEY_SUBMISSION_ID to submissionId))
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
)
```

**Target (Phase 2):**
```
submitTest()
  → TATSubmissionRepository.finalizeSubmission(submissionId, allStories)
      updates Firestore: {status=SUBMITTED_PENDING_REVIEW, analysisStatus=PENDING_SYNTHESIS}
  → enqueue TATSynthesisWorker(submissionId)
  (no local TATSubmission in UiState — result screen fetches from Firestore)
```

---

## 11. TATAnalysisWorker (Current — bulk text-only, has bugs)

**File:** `app/src/main/kotlin/com/ssbmax/workers/TATAnalysisWorker.kt` (285 lines)

**Flow:**
```
doWork()
  1. Get TATSubmission from Firestore by submissionId
  2. Verify status == PENDING_ANALYSIS (idempotency guard)
  3. Update status → ANALYZING
  4. PsychologyTestPrompts.generateTATAnalysisPrompt(submission)
     → feeds all 12 stories as text blob; no image bytes; no TATImageContext
  5. aiService.analyzeText(prompt) — text-only Gemini call
     → retry up to 3 attempts (own loop)
     → outer try/catch also uses runAttemptCount < MAX_AI_RETRIES → double retry (9 total)
  6. ValidationIntegration.validateScores(olqScores, EntryType.NDA)  ← Bug B4
  7. Build OLQAnalysisResult { overallScore, overallRating, strengths, weaknesses }
  8. submissionRepository.updateTATOLQResult(submissionId, olqResult)
  9. getOLQDashboard.invalidateCache(userId)
 10. notificationHelper.showTATResultsReadyNotification(submissionId)
```

**Confirmed bugs:**

| # | Bug | Location | Impact |
|---|-----|----------|--------|
| B2 | `overallScore <= 3.0f → "Exceptional"` — never true on 5-9 scale | ~line 115 | Every user gets "Needs Improvement" |
| B3 | `aiConfidence = 85` hardcoded | ~line 150 | Inaccurate confidence shown |
| B4 | `EntryType.NDA` hardcoded; no UserProfileRepository injection | ~line 107 | Wrong SSB validation for OTA/GRADUATE |

**Rating scale fix (B2):**
```kotlin
// Current prompt output: 5–9 scale (5=Exceptional, 9=Fail)
// Fix:
overallScore <= 5.5f -> "Exceptional"
overallScore <= 6.5f -> "Good"
overallScore <= 7.5f -> "Average"
else -> "Needs Improvement"
```

---

## 12. Target: Progressive Assessment Architecture (Phase 2)

Replaces the single bulk Gemini call with a two-tier system: **12 lightweight per-story workers + 1 deep holistic synthesis worker**.

### TATStoryAnalysisWorker (×12, per story)

```
TATStoryAnalysisWorker(submissionId, storyIndex=N, imageId, storyText)
  → read TATImageContext from Room (by imageId)
  → TATStoryAnalysisPrompts.buildPrompt(storyText, imageContext, candidateGender)
  → Gemini call → extracts TATStoryAssessment:
      olqIndicators: Map<OLQ, Float>   ← evidence weights 0.0–1.0 (NOT final scores)
      heroCharacteristics: List<String>
      dominantThemes: List<String>
      psychSignals: List<String>
      narrativeQuality: Float
  → upsert TATStoryAssessmentEntity into Room (id = "${submissionId}_${storyIndex}")
  → retry 3×; on all failures, store {failed=true}
  WorkManager unique name: "tat_story_${submissionId}_${storyIndex}", REPLACE policy
```

Enqueued **as each story is confirmed** (not at submitTest). All 12 may run concurrently.

### TATSynthesisWorker (×1, at submitTest)

```
TATSynthesisWorker(submissionId)
  → verify submission is PENDING_SYNTHESIS
  → fetch user profile → gender, entryType
  → poll Room for TATStoryAssessmentEntity every 5 s, up to 12 attempts (60 s total)
      break when completedCount ≥ 8 OR all 12 present
  → if completedCount < 8 → enqueue TATAnalysisWorker (fallback), return
  → TATSynthesisPrompts.buildPrompt(all assessments ordered by storyIndex, profile)
  → deep Gemini call → holistic 15 OLQ scores + per-OLQ reasoning + cross-story analysis
  → write OLQAnalysisResult → psych_results/{submissionId}
  → update submission → COMPLETED
  → invalidate OLQ dashboard cache → send notification
  Retry 3×; on all retries exhausted → enqueue TATAnalysisWorker (fallback)
```

### New Room entity: `TATStoryAssessmentEntity`

```kotlin
@Entity(tableName = "tat_story_assessments",
        indices = [Index("submissionId"), Index(value=["submissionId","storyIndex"], unique=true)])
data class TATStoryAssessmentEntity(
    @PrimaryKey val id: String,          // "${submissionId}_${storyIndex}"
    val submissionId: String,
    val storyIndex: Int,
    val imageId: String,
    val olqIndicatorsJson: String,       // Map<OLQ, Float> serialized
    val heroCharacteristicsJson: String,
    val narrativeQuality: Float,
    val psychSignalsJson: String,
    val dominantThemesJson: String,
    val analyzedAt: Long,
    val aiConfidence: Int,
    val failed: Boolean = false,
    val analysisStatus: String = "PENDING"
)
```

### New domain model: `TATStoryAssessment`

```kotlin
data class TATStoryAssessment(
    val submissionId: String,
    val storyIndex: Int,                      // 0-11
    val imageId: String,
    val olqIndicators: Map<OLQ, Float>,       // Evidence weights 0.0-1.0 (NOT final OLQ scores)
    val heroCharacteristics: List<String>,
    val narrativeQuality: Float,
    val psychSignals: List<String>,
    val dominantThemes: List<String>,
    val analyzedAt: Long,
    val aiConfidence: Int,
    val failed: Boolean = false
)
```

### New domain model: `TATImageContext`

Identical structure to `PPDTImageContext`:
```kotlin
data class TATImageContext(
    val sceneDescription: String = "",
    val coreElements: List<String> = emptyList(),
    val ambiguousElements: List<String> = emptyList(),
    val expectedThemes: List<String> = emptyList(),
    val penalizedThemes: List<String> = emptyList(),
    val primaryOLQs: List<String> = emptyList(),
    val deviationTolerance: DeviationTolerance = DeviationTolerance.MEDIUM,
    val exemplarGoodHints: List<String> = emptyList(),
    val exemplarBadHints: List<String> = emptyList()
)
```

---

## 13. AI Scoring Model

### OLQ Framework

15 Officer-Like Qualities assessed across 4 SSB Factors:

| Factor | OLQs |
|--------|------|
| Factor I — Effective Intelligence | Effective Intelligence, Reasoning Ability, Organizing Ability, Power of Expression |
| Factor II — Social Adaptability | Social Adaptability, Cooperation, Sense of Responsibility, Group Influence |
| Factor III — Dynamic Quality | Speed of Decision, Ability to Influence, Liveliness |
| Factor IV — Courage | Determination, Stamina, Self Confidence, Courage |

**Scoring scale: 5–9** (lower is better)
- 5 = Exceptional
- 6 = Very Good
- 7 = Average (SSB pass threshold)
- 8 = Below Average
- 9 = Fail

**Critical SSB Rule (Factor II):** If average Factor II score ≥ 8, candidate is auto-rejected regardless of other scores. This must be validated in `ValidationIntegration`.

**Rating bands (correct for 5-9 scale):**
```
overallScore <= 5.5 → "Exceptional"
overallScore <= 6.5 → "Good"
overallScore <= 7.5 → "Average"
else               → "Needs Improvement"
```

### Per-story assessment scope (TATStoryAnalysisWorker)

Extracts **evidence weights** (0.0–1.0), NOT final OLQ scores:
- `olqIndicators: Map<OLQ, Float>` — how much this story evidences each OLQ
- `heroCharacteristics: List<String>` — psychological traits of the story's protagonist
- `dominantThemes: List<String>` — recurring narrative motifs
- `psychSignals: List<String>` — psychological signals (proactive/reactive, optimistic/pessimistic)
- `narrativeQuality: Float` — narrative structure score 0.0–1.0

### Holistic synthesis scope (TATSynthesisWorker)

Reads all 12 `TATStoryAssessment` entries and computes:
- Final 15 OLQ scores (5–9 scale) with per-OLQ reasoning
- Cross-story OLQ pattern (consistent strength vs inconsistent vs deteriorating)
- Narrative evolution (story 1 → 12: does hero agency improve?)
- Dominant personality themes across all 12 stories
- Contradiction flags (e.g., high Determination in stories 1-4, absent in 9-12)

---

## 14. TATImageContext per Card Position

Real SSB TAT cards probe specific OLQs by design. Each card position has an intended OLQ focus that `TATImageContext.primaryOLQs` must reflect:

| Card Position | Intended OLQ Focus | Scene Category |
|---|---|---|
| 1 | Initiative, Self-Confidence | Individual adversity |
| 2 | Cooperation, Social Adjustment | Group/social scenario |
| 3 | Determination, Stamina | Persistence under pressure |
| 4 | Speed of Decision, Courage | Emergency / urgency |
| 5 | Organizing Ability, Effective Intelligence | Planning / leadership |
| 6 | Sense of Responsibility, Cooperation | Duty / community |
| 7 | Influence Group, Power of Expression | Communication / leadership |
| 8 | Liveliness, Social Adjustment | Energy / positive engagement |
| 9 | Reasoning Ability, Effective Intelligence | Problem solving |
| 10 | Determination, Courage | Adversity / risk |
| 11 | Self-Confidence, Initiative | Individual agency |
| 12 (blank) | EI, Initiative, Organizing Ability | Imagination — heavily weighted |

Each of the 12 batch variants for a given position uses a different image but must probe the same primary OLQs.

---

## 15. Result Screen

**`TATSubmissionResultScreen.kt`** / **`TATSubmissionResultViewModel.kt`**

Result is loaded by `submissionId` (passed as nav arg). Shows:
- Per-OLQ scores with reasoning
- Overall score + rating band
- Strengths (lowest 3 OLQ scores) + weaknesses (highest 3)
- Recommendations

**Current issue:** `submission: TATSubmission?` stored in `TATTestUiState` to "bypass Firestore permission issues". Result screen uses this local copy instead of fetching from Firestore. This is an architectural smell — result screen should fetch independently via ViewModel.

---

## 16. Firestore Schema

### Submission document

```
submissions/{submissionId}
  ├── userId: String
  ├── testType: "TAT"
  ├── testId: String
  ├── status: "IN_PROGRESS" | "SUBMITTED_PENDING_REVIEW" | "COMPLETED" | "FAILED"
  ├── analysisStatus: "NOT_STARTED" | "PENDING_ANALYSIS" | "ANALYZING" | "PENDING_SYNTHESIS" | "COMPLETED" | "FAILED"
  ├── stories: List<{questionId, story, charactersCount, viewingTimeTaken, writingTimeTaken, submittedAt}>
  ├── totalTimeTakenMinutes: Int
  ├── submittedAt: Timestamp
  └── olqResult: OLQAnalysisResult? (null until COMPLETED)
```

### Result document

```
psych_results/{submissionId}
  ├── submissionId: String
  ├── testType: "TAT"
  ├── olqScores: Map<OLQ, {score, confidence, reasoning}>
  ├── overallScore: Float
  ├── overallRating: String
  ├── strengths: List<String>
  ├── weaknesses: List<String>
  ├── recommendations: List<String>
  ├── analyzedAt: Timestamp
  └── aiConfidence: Int
```

---

## 17. Known Bugs (Baseline — pre-upgrade)

| # | Bug | File | Line | Impact | Fix Phase |
|---|-----|------|------|--------|-----------|
| B1 | Timer race: viewing `finally` sets `isTimerActive=false` after writing timer starts | `TATTestViewModel.kt` | ~517 | Writing timer bar appears frozen | Phase 1 |
| B2 | `overallRating` thresholds wrong for 5-9 scale (`<=3.0` never true) | `TATAnalysisWorker.kt` | ~115 | Every user gets "Needs Improvement" | Phase 1 |
| B3 | `aiConfidence = 85` hardcoded | `TATAnalysisWorker.kt` | ~150 | Inaccurate confidence shown | Phase 1 |
| B4 | `EntryType.NDA` hardcoded; user profile never consulted | `TATAnalysisWorker.kt` | ~107 | Wrong SSB validation for OTA/GRADUATE | Phase 1 |
| B5 | `minCharacters` has 3 inconsistent values: domain=50, config=50, analytics=150 | `TATTest.kt`, `TATTestConfig`, `submitTest()` | Multiple | Wrong UI char-count validation | Phase 1 |
| B6 | Writing timer expiry → `REVIEW_CURRENT` but `currentStory` not saved to `responses[]` | `TATTestViewModel.kt` | ~560 | Story lost if user exits from review | Phase 1 |
| B7 | `moveToPreviousQuestion()` allows editing confirmed stories | `TATTestViewModel.kt` | ~314 | Breaks per-story analysis model | Phase 1 |

---

## 18. Content Pipeline (Phase 0)

Produces the 132 `TATImageContext` records that Phase 2 depends on. This is a **content session**, not a code session, and must complete before Phase 2 goes to production.

**What you provide:**
- ~124 individual image files (PNG/JPG) — 11 card positions × ~11-12 batch variants
- 1 MD description file — one description entry per image

**What I produce:**
- Full 9-field `TATImageContext` per image (read image + description → generate context)
- `cardPosition` assignment, `genderTag` classification (MALE/FEMALE/MIXED), `category`, `difficulty`
- `preview.html` — HTML preview gate showing each image + full context (mandatory review before any upload)
- `gender_map.json` — ground-truth gender classification after your review
- `scripts/tat-picture-pipeline/step2_upload.py` — upload script (idempotent, --dry-run mode)

**Blank card (position 12):**
- No image file — handled programmatically
- `TATImageContext` still generated with `isBlankCard=true`, high `deviationTolerance`, `primaryOLQs = [EI, INITIATIVE, ORGANIZING_ABILITY]`

**Scripts location:** `scripts/tat-picture-pipeline/`

---

## 19. Upgrade Roadmap

| Phase | What | Depends on |
|-------|------|------------|
| **Phase 0** | Content pipeline — 132 TATImageContext records, upload to Firebase | Separate content session |
| **Phase 1** | Bug fixes B1-B7, extract LoadTATTestUseCase, move TATTestUiState to own file | Nothing (independent) |
| **Phase 2** | Progressive assessment architecture, image infrastructure, gender routing, blank card | Phase 0 content live |
| **Phase 3** | BaseTestViewModel (5 psych tests), WAT/SRT/SD bug fixes, multimodal per-story, shared OLQResultContent | Phase 2 complete |

### Phase 0 deliverables

| Deliverable | Path |
|-------------|------|
| Image contexts | `scripts/tat-picture-pipeline/tat_image_contexts.json` |
| Gender map | `scripts/tat-picture-pipeline/gender_map.json` |
| HTML preview | `scripts/tat-picture-pipeline/preview.html` |
| Upload script | `scripts/tat-picture-pipeline/step2_upload.py` |
| Firestore content live | `test_content/tat/image_batches/batch_001..batch_012` |

### Phase 1 — Quick wins (8 isolated fixes)

1. **1.1** Fix timer race — generation-token pattern (Bug B1)
2. **1.2** Fix rating thresholds for 5-9 scale (Bug B2)
3. **1.3** Fix `minCharacters = 150` everywhere (Bug B5)
4. **1.4** Inject `UserProfileRepository` into `TATAnalysisWorker`, resolve `entryType` (Bug B4)
5. **1.5** Use actual `aiConfidence` from Gemini response (Bug B3)
6. **1.6** Extract `LoadTATTestUseCase` + move `TATTestUiState` to own file
7. **1.7** Fix story loss — `saveCurrentStoryToResponses()` before timer-expiry phase transition (Bug B6)
8. **1.8** Remove `moveToPreviousQuestion()` entirely (Bug B7)

### Phase 2 — New files

| File | Type |
|------|------|
| `core/domain/.../model/TATImageContext.kt` | New domain model |
| `core/domain/.../model/TATStoryAssessment.kt` | New domain model |
| `core/data/.../local/entity/TATStoryAssessmentEntity.kt` | New Room entity |
| `core/data/.../local/dao/TATStoryAssessmentDao.kt` | New DAO |
| `app/.../workers/TATStoryAnalysisWorker.kt` | New WorkManager worker |
| `app/.../workers/TATSynthesisWorker.kt` | New WorkManager worker |
| `core/data/.../ai/prompts/TATStoryAnalysisPrompts.kt` | New AI prompts |
| `core/data/.../ai/prompts/TATSynthesisPrompts.kt` | New AI prompts |
| `app/.../ui/tests/tat/components/TATProfileRequiredDialog.kt` | New dialog |

### Phase 2 — Modified files

| File | Change |
|------|--------|
| `TATTestViewModel.kt` | Gender routing, profile gate, createDraft, per-story worker enqueue, finalizeSubmission |
| `TATSubmissionRepository.kt` | `createDraftSubmission()`, `finalizeSubmission()` |
| `TATImageCacheManager.kt` | 12-batch sync, staleness gate, position-based selection, blank card |
| `CachedTATImageEntity.kt` | `cardPosition`, `imageContextJson`, `genderTag`, @Index |
| `TATBatchMetadataEntity.kt` | `lastStalenessCheckAt` |
| `TATImageCacheDao.kt` | `getLeastUsedImageByPosition()` |
| `TATTest.kt` | `TATImageContext` + `genderTag` + `cardPosition` + `isBlankCard` on `TATQuestion`; new enums |
| `TATImageViewingPhase.kt` | Blank card UI branch |
| `TATInstructionsPhase.kt` | Blank card instruction bullet |
| Room migration file | New table + new columns |

---

## 20. Verification Plan

### After Phase 0
- Open app → start TAT test → verify real TAT images load from Firebase Storage in correct card-position order
- Verify male candidate sees MALE+MIXED pool; female sees FEMALE+MIXED pool
- Verify card 12 shows blank card UI (no image)

### After Phase 1
```bash
./gradlew :app:testDebugUnitTest --tests "*TAT*"
./gradlew :app:lintDebug
```
Manual checks:
- Rotate device during IMAGE_VIEWING → writing timer still starts correctly (B1)
- Submit test → result shows "Good" or "Average" for average stories (B2)
- Check `minCharacters` validation triggers at 150 chars (B5)
- Confirm story 3 → verify no "Previous" button (B7)
- Let writing timer expire → verify story is saved before REVIEW (B6)

### After Phase 2
```bash
./gradlew :core:data:testDebugUnitTest --tests "*Migration*"
./gradlew :app:testDebugUnitTest --tests "*TATStory*"
./gradlew :app:testDebugUnitTest --tests "*TATSynthesis*"
./gradlew check
```
Manual checks:
- Complete TAT → result screen shows cross-story theme analysis
- Kill app after story 6 → reopen → fresh test (orphan cleaned at next login)
- Confirm story 5 → no "Previous" button visible
- Card 12 → blank background + imagination prompt shown

### After Phase 3
```bash
./gradlew testDebugUnitTest
./gradlew check
```
Manual checks:
- TAT and PPDT result screens both use shared `OLQResultContent` composable
- WAT/SRT/SD results show correct rating bands (≤5.5 = Exceptional, etc.)

---

## 21. File Map (TAT pipeline — all relevant files)

```
app/src/main/kotlin/com/ssbmax/
├── ui/tests/tat/
│   ├── TATTestScreen.kt
│   ├── TATTestViewModel.kt           ← 677 lines (over 300 limit — Phase 1 extract)
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
    └── TATAnalysisWorker.kt          ← current analysis worker (kept as fallback)

core/domain/src/main/kotlin/com/ssbmax/core/domain/
├── model/
│   └── TATTest.kt                    ← TATQuestion, TATSubmission, TATPhase, TATTestConfig
├── repository/
│   └── SubmissionRepository.kt       ← getTATSubmission(), updateTATAnalysisStatus()
└── usecase/submission/
    └── SubmitTATTestUseCase.kt

core/data/src/main/kotlin/com/ssbmax/core/data/
├── repository/
│   └── TATImageCacheManager.kt       ← image cache (Phase 2: major rewrite)
├── local/
│   ├── entity/
│   │   └── CachedTATImageEntity.kt   ← Phase 2: add cardPosition, imageContextJson, genderTag
│   └── dao/
│       └── TATImageCacheDao.kt       ← Phase 2: add getLeastUsedImageByPosition()
└── ai/prompts/
    └── PsychologyTestPrompts.kt      ← generateTATAnalysisPrompt() lives here

docs/architecture/
└── TAT_Pipeline.md                   ← this file

scripts/tat-picture-pipeline/         ← Phase 0 (to be created)
├── tat_image_contexts.json
├── gender_map.json
├── preview.html
└── step2_upload.py
```
