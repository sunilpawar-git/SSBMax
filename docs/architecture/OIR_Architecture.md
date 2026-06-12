# OIR Architecture

Covers both the **content pipeline** (PDF → Firestore) and the **serving layer** (Room cache → user test).

---

## Content Pipeline (PDF → Firestore)

### Design Principle
Keep the LLM out of the correctness path. `questionText`, `correctAnswerId`, and `explanation` are extracted deterministically from the PDF text layer. LLM is only used for non-critical enrichment (type, difficulty tags).

### Scripts
| Script | Role |
|---|---|
| `scripts/oir-extraction/oir_extract_v2.py` | Extract the 20 practice-set batches (001–020) from the SSBCrack PDF |
| `scripts/oir-extraction/oir_extract_part3.py` | Extract the 8 topic-family batches (021–028) from `OIR PART 3`; reuses v2 helpers (`composite_figure`, `page_images`, `OPT_IDS`) |
| `scripts/oir-extraction/upload-oir-batch.js` | Ingestion **gate** + upload images → Storage, questions → Firestore |
| `scripts/set-oir-meta-config.js` | Publish `test_content/oir/meta/config` `{contentVersion, batchCount}` — the client reconciliation trigger |

The upload script's `validateBatch()` is a **fail-closed ingestion gate**: it mirrors the error-level rules of the domain `OIRQuestionValidator` (non-empty options, `correctAnswerId` present unless a figure question, answer ∈ option ids, non-blank text) and rejects the write if any question violates them. The Kotlin validator stays the SSOT for the *rules*; the gate enforces them at write time.

### Extraction Pipeline (oir_extract_v2.py)
1. **Page range mapping** — `SET_TABLE` hardcodes `(q_start, a_start, a_end)` for all 20 sets (no guessing)
2. **Stage 1 — Questions** — text layer, sequential marker validation (`^(\d{1,2})[.)]`) prevents false positives
3. **Stage 2 — Answers + Explanations** — text layer, `^(\d{1,2})\.[ \t]*Answer:` regex; inter-entry text = real human explanation
4. **Stage 3 — Figure crops** — `doc.extract_image(xref)` returns clean raster (watermark is text-layer only); images composited onto white canvas at 3× zoom
5. **Stage 4 — Answer mapping** — deterministic: `"(2)"` → `opt_b`, `"5"` → `opt_e`, Yes/No matched to options, multi-answers embedded in explanation
6. **Stage 5 — Type heuristic** — keyword + figure-presence classification; no AI
7. **Stage 6 — Output** — `out/batch_pdf_{NNN}.json` + `out/batch_pdf_{NNN}_preview.html` (human-review gate) + `out/images/set{N}_q{MM}.png`

`questionImageUrl` is written as `gs://BUCKET/...` placeholder, rewritten to public HTTPS at upload time.

### Upload (upload-oir-batch.js)
```
node upload-oir-batch.js batch_pdf_001            # live upload
node upload-oir-batch.js batch_pdf_001 --dry-run  # offline validation only
node upload-oir-batch.js batch_pdf_001 --verify   # HEAD-check all questionImageUrls in Firestore
node upload-oir-batch.js batch_pdf_001 --repair   # re-upload missing images + patch gs:// placeholders
```
1. Validates all local image files exist before touching Firebase
2. Uploads PNGs to `gs://ssbmax-49e68.firebasestorage.app/oir/pdf_questions/` (public)
3. Rewrites `questionImageUrl` placeholders to `https://storage.googleapis.com/...`
4. Writes Firestore doc at `test_content/oir/batches/{batchId}`

**Recommended post-upload step:** always run `--verify` after a live upload to confirm every `questionImageUrl` resolves with HTTP 200. If any are broken, `--repair` re-uploads only the missing Storage objects and patches the Firestore doc — no full re-upload needed.

### Upload Status
✅ **All 28 batches extracted, uploaded, and meta doc PUBLISHED (June 7, 2026)**
- Batches 001–020: 1,000 questions (SSBCrack original sets) + Batches 021–028: 255 questions (Part-3 topic families)
- Total: ~1,255 questions live in Firestore
- Images: All live at `gs://ssbmax.../oir/pdf_questions/` (public HTTPS)
- Meta doc: `test_content/oir/meta/config` = `{ contentVersion: 2, batchCount: 28 }` (published)

> **Known legacy duds (deferred):** 140 questions in batches 001–020 are genuine free-response
> fill-in-the-blank items (no options in the source). A free-response question type that rescues
> them is planned for a later sprint (`~/.claude/plans/staged-splashing-sifakis.md`). Until then
> they remain in Firestore but are **skipped at selection-time** (see Serving Layer) so they never
> reach an assembled test.

---

## Serving Layer (App Runtime)

### Storage Layout & Organization

**Firestore** (28 batch documents + 1 meta doc):
```
test_content/oir/batches/
├─ batch_pdf_001..020 (50 Qs each, source = SSBCrack Sets 1–20)
└─ batch_pdf_021..028 (255 Qs total, source = OIR PART 3 topic families)
test_content/oir/meta/config = { contentVersion: Int, batchCount: Int }
```
Each batch doc has `{"batchId", "version", "totalQuestions", "questions": [...]}`. The **meta doc** is
the content-version SSOT that drives client reconciliation (see below).

**Firebase Storage:** `oir/pdf_questions/set{N}_q{MM}.png` (public HTTPS URLs)

**Room (local cache):** All questions (~1255 across 28 batches) **flattened into ONE table** (`cached_oir_questions`):
```sql
cached_oir_questions (~1255 rows)
├─ oir_pdf_s01_q0001 | VERBAL_REASONING      | batchId='batch_pdf_001' | lastUsed=null
├─ oir_pdf_s01_q0002 | NON_VERBAL_REASONING  | batchId='batch_pdf_001' | lastUsed=null
├─ ...
├─ oir_pdf_s02_q0001 | VERBAL_REASONING      | batchId='batch_pdf_002' | lastUsed=2 days ago
├─ ...
└─ oir_pdf_s20_q0050 | SPATIAL_REASONING     | batchId='batch_pdf_020' | lastUsed=null
```
**Key:** The `batchId` column preserves which batch each question came from (for auditing/analytics), but **batches are NOT isolated** — all questions are pooled together.

### Content-Version Reconciliation (Firestore is SSOT, Room mirrors)
`OIRQuestionCacheManager.initialSync()` reads `test_content/oir/meta/config` and compares
`remote.contentVersion` to the locally-stored value in the single-row `oir_sync_metadata` table
(DB v20, `MIGRATION_19_20`):

| Condition | Action |
|---|---|
| `remote.contentVersion != local` (incl. fresh install where local = null) | **Reconcile** — clear all questions + re-download `batch_pdf_001..batchCount`, then persist the new version |
| version matches but some of `1..batchCount` missing | Top-up just the missing batches (idempotent) |
| version matches and all present | Skip (zero Firestore reads) |

So existing installs **self-heal** to new content on the next launch — bump `contentVersion` (and
`batchCount` when batches are added) via `set-oir-meta-config.js`; **no Kotlin change** is needed for
a future content drop. If the meta doc is unreadable, the manager falls back to a legacy 20-batch sync.

> ✅ **Release gate status (as of June 7, 2026):**
> - Firestore side: **META DOC PUBLISHED** — `test_content/oir/meta/config` = `{ contentVersion: 2, batchCount: 28 }`
> - DB side: **v20 migration ready** — `MIGRATION_19_20` creates `oir_sync_metadata` table; registered in DataModule
> - App side: **Reconciliation code ready** — `OIRQuestionCacheManager` reads meta doc; Phase 1/2 caching active
> - **Pending client release:** Merge PR #18 → ship v20 app build. Old builds fall back to legacy 20-batch sync (contentVersion reader absent). Once v20 ships, all users converge on 28-batch pool with ~1,255 questions.

### Two-Phase Latency Model
| Phase | Batches | Behaviour |
|---|---|---|
| Phase 1 (blocking) | 1–4 | Downloaded before first test can start |
| Phase 2 (background) | 5..`batchCount` | Downloaded while user takes the first test |

### How a Test Is Assembled (Pool Model, Not Batch-Sequential)

Each OIR test is a **freshly sampled 50-question set** drawn from the **flattened Room pool (~1255 questions)** — **NOT** in batch order, and **NOT** from a single batch. `OIRQuestionSelector.selectQuestions(50)` works like this:

1. Query Room for unused questions of each type (ignoring `batchId`):
   ```sql
   SELECT * FROM cached_oir_questions 
   WHERE type = 'VERBAL_REASONING' 
   AND (lastUsed IS NULL OR lastUsed < 7_days_ago) 
   ORDER BY RANDOM() 
   LIMIT 20
   ```
   A composite index on `(type, lastUsed)` (DB v18) lets SQLite satisfy both predicates in a single index scan before applying `ORDER BY RANDOM()`. NULLs sort first in the ASC index, so `lastUsed IS NULL` is covered by the same range scan as `lastUsed < threshold`.
2. Enforce type distribution — defined **once** in `OIRQuestionDistribution` (domain SSOT), read by the selector. The bank has zero spatial questions, so the live split targets the three populated types via a largest-remainder `counts(total)`:

| Type | Count (of 50) | Ratio |
|---|---|---|
| Verbal Reasoning | 20 | 40% |
| Non-Verbal Reasoning | 20 | 40% |
| Numerical Ability | 10 | 20% |

   (`SPATIAL_REASONING` remains a valid enum value but is no longer a distribution target.)
3. **Selection-time validation:** `fetchByType` over-fetches (×3) and drops anything the domain `OIRQuestionValidator` rejects, so legacy duds (e.g. the 140 optionless fill-in-blank questions) are skipped here and never reach the test — keeping the assembled set at full count without deleting the data. Smart redistribution tops up across the three live types if one is short.
4. Questions are sampled **from the whole pool**, **from all batches**, **randomly** — `batchId` is **ignored** during selection.
5. The runtime `OIRQuestionValidator.validateAndFilter` in `OIRTestViewModel` remains as a **defensive assertion** — post-reconcile it should never fire; if it does, it signals an upstream breach and is logged loudly.
6. Final set is shuffled before presentation.

**Validity is defined once (the domain `OIRQuestionValidator`) and enforced at three layers:** the ingestion gate (write-time, new content), the selector (selection-time, skips legacy duds), and the ViewModel (runtime assertion).

**Example:** A user's first test might contain Q1 from batch_001 + Q87 from batch_002 + Q150 from batch_003, etc. — a random mix.

**7-day reuse window:** Questions unused in the last 7 days are preferred; already-used questions are only drawn when unused supply runs short. After test submission, `markQuestionsUsed()` updates `lastUsed` in Room.

### Why This Architecture?

| Design Decision | Reason |
|---|---|
| Firestore: 20 batch documents | Tracks download source, version history, audit trail; supports selective re-download |
| Room: 1 flattened table | Enables efficient type-based random sampling via SQL `ORDER BY RANDOM()` + `WHERE type = ?` |
| Selection: ignore `batchId` | Users get **diverse question coverage** across all 1000, not exhausting one batch before moving to the next |

### Subscription Limits (per calendar month)
| Tier | OIR tests/month |
|---|---|
| FREE | 1 |
| PRO | 5 |
| PREMIUM | Unlimited |

- `canTakeTest(TestType.OIR, userId)` checked before loading — reads Firestore server-side to prevent cache-clearing bypass
  - Returns `TestEligibility.Eligible` → proceed
  - Returns `TestEligibility.LimitReached` → show upgrade prompt (also used as fail-closed fallback for unknown errors)
  - Returns `TestEligibility.NetworkError` → show retryable error; security invariant preserved (only `IOException` / `FirebaseNetworkException` / Firestore `UNAVAILABLE` reach this path)
- `recordTestUsage()` increments atomically via Firestore transaction after successful submission
- `markQuestionsUsed()` updates Room for the 7-day reuse window

### Submit Flow
```
canTakeTest()            → Firestore usage check (monthly gate)
  ├─ NetworkError        → retryable error shown (UI stays on home screen)
  ├─ LimitReached        → upgrade prompt shown (fail-closed for unknown errors too)
  └─ Eligible            → continue ↓

getTestQuestions(50)     → Room query with type distribution + 7-day preference
[user takes test]
SubmitOIRTestUseCase:
  1. scoreCalculator.calculate(session)
  2. usageRecorder.recordTestUsage(OIR, userId)   ← Firestore atomic increment
  3. dashboardUseCase.invalidateCache(userId)
  4. submissionRepository.submitOIR(submission)   → submissionId
  5. testSessionRepository.endTestSession(sessionId)
markQuestionsUsed()      → Room (7-day suppression)
```

### Key Files
| File | Responsibility |
|---|---|
| `core/data/.../OIRQuestionCacheManager.kt` | Sync lifecycle + content-version reconciliation (meta doc → clear/redownload/top-up) |
| `core/data/.../OIRQuestionSelector.kt` | Type-distribution selection + selection-time validity filter + 7-day reuse logic |
| `core/domain/.../model/OIRQuestionDistribution.kt` | Distribution SSOT (V40/NV40/N20, largest-remainder `counts()`) |
| `core/data/.../local/entity/OIRSyncMetadataEntity.kt` | Single-row local content-version mirror (DB v20) |
| `core/domain/.../validation/OIRQuestionValidator.kt` | Validity SSOT — enforced at ingestion (gate), selection (selector), and runtime (assertion) |
| `scripts/oir-extraction/upload-oir-batch.js` (`validateBatch`) | Write-time enforcement of the validator's rules |
| `core/data/.../SubscriptionManager.kt` | Monthly limits — single source of truth |
| `core/domain/.../usecase/oir/SubmitOIRTestUseCase.kt` | Orchestrates score → usage → submit → end session |

---

## Applying This Pattern to Future Tests (WAT, SRT, GPE)

1. **Extraction:** Use `oir_extract_v2.py` as reference — text layer for correctness, figure geometry for image assignment, HTML preview gate before upload
2. **Firestore path:** `test_content/{testType}/batches/{batchId}`
3. **Cache:** Follow two-phase pattern in a new `{TestType}QuestionCacheManager`
4. **Selector:** Implement type-distribution ratios appropriate for the new test
5. **Subscription limits:** Add `TestType.{NEW}` case to `SubscriptionManager.getTestLimitForTier()`

---

## Scaling & Expansion

### Can We Add More Questions?

**Yes.** The architecture is designed to scale from 1000 to 10,000+ questions with **zero logic changes**:

| Component | Scaling Capacity | Bottleneck |
|---|---|---|
| Firestore batches | Unlimited (40 batches = 2000 Qs, 100 batches = 5000 Qs, etc.) | Storage cost, not logic |
| Room local table | Android devices typically handle 10,000+ rows comfortably | Device storage, not SQL query speed |
| Selection logic | Works identically | Query still does `ORDER BY RANDOM()` — indexes make it fast |
| Two-phase cache | Adjust Phase 1 count if needed (currently 1–4 = 200 Qs) | Still blocking → background pattern |
| 7-day reuse window | Works better with more questions | More unused questions available |

### How to Add 1000+ More Questions

1. **Extract new batches** via `oir_extract_v2.py`
   - Add new sets to `SET_TABLE` in the script
   - Run extraction for new sets
   
2. **Upload new batches** via `upload-oir-batch.js`
   - Firestore docs automatically written at `test_content/oir/batches/batch_pdf_021`, `batch_pdf_022`, etc.
   - Existing batches (001–020) remain untouched

3. **App automatically downloads new batches**
   - `initialSync()` will detect new batches and phase 2 will download them
   - No code changes needed

4. **Selection logic unchanged**
   - Query still reads from flattened Room table
   - Type distribution still enforced
   - 7-day window still applied
   - Users automatically get access to all 1000+ questions

### Cost of Expansion

| Scenario | Firestore Reads | Room Storage | User Impact |
|---|---|---|---|
| 1000 Qs (20 batches) | ~20 on first launch | ~50 MB | All batches in ~1 min |
| 5000 Qs (100 batches) | ~100 on first launch | ~250 MB | Phase 1–4 blocks (few secs), then background |
| 10,000 Qs (200 batches) | ~200 on first launch | ~500 MB | Consider increasing Phase 1 batches |

**Optimization if scaling to 10k+:** Increase Phase 1 batches from 1–4 to 1–10 to reduce time-to-first-test.

