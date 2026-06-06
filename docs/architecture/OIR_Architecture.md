# OIR Architecture

Covers both the **content pipeline** (PDF → Firestore) and the **serving layer** (Room cache → user test).

---

## Content Pipeline (PDF → Firestore)

### Design Principle
Keep the LLM out of the correctness path. `questionText`, `correctAnswerId`, and `explanation` are extracted deterministically from the PDF text layer. LLM is only used for non-critical enrichment (type, difficulty tags).

### Scripts
| Script | Role |
|---|---|
| `scripts/oir-extraction/oir_extract_v2.py` | Extract questions, answers, figures from PDF |
| `scripts/oir-extraction/upload-oir-batch.js` | Upload images → Storage, questions → Firestore |

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
node upload-oir-batch.js batch_pdf_001            # live
node upload-oir-batch.js batch_pdf_001 --dry-run  # offline validation only
```
1. Validates all local image files exist before touching Firebase
2. Uploads PNGs to `gs://ssbmax-49e68.firebasestorage.app/oir/pdf_questions/` (public)
3. Rewrites `questionImageUrl` placeholders to `https://storage.googleapis.com/...`
4. Writes Firestore doc at `test_content/oir/batches/{batchId}`

### Upload Status
All 20 sets extracted and uploaded (June 2026). Outputs live at `scripts/oir-extraction/out/`.

---

## Serving Layer (App Runtime)

### Storage Layout & Organization

**Firestore** (20 separate documents):
```
test_content/oir/batches/
├─ batch_pdf_001 (50 Qs, source = Set 1)
├─ batch_pdf_002 (50 Qs, source = Set 2)
├─ ...
└─ batch_pdf_020 (50 Qs, source = Set 20)
```
Each Firestore doc has `{"batchId", "version", "totalQuestions", "questions": [...]}`

**Firebase Storage:** `oir/pdf_questions/set{N}_q{MM}.png` (public HTTPS URLs)

**Room (local cache):** All 1000 questions **flattened into ONE table** (`cached_oir_questions`):
```sql
cached_oir_questions (1000 rows)
├─ oir_pdf_s01_q0001 | VERBAL_REASONING      | batchId='batch_pdf_001' | lastUsed=null
├─ oir_pdf_s01_q0002 | NON_VERBAL_REASONING  | batchId='batch_pdf_001' | lastUsed=null
├─ ...
├─ oir_pdf_s02_q0001 | VERBAL_REASONING      | batchId='batch_pdf_002' | lastUsed=2 days ago
├─ ...
└─ oir_pdf_s20_q0050 | SPATIAL_REASONING     | batchId='batch_pdf_020' | lastUsed=null
```
**Key:** The `batchId` column preserves which batch each question came from (for auditing/analytics), but **batches are NOT isolated** — all 1000 are pooled together.

### Two-Phase Cache on First Launch
| Phase | Batches | Behaviour |
|---|---|---|
| Phase 1 (blocking) | 1–4 | Downloaded before first test can start (~200 questions) |
| Phase 2 (background) | 5–20 | Downloaded while user takes the first test |
| Subsequent launches | — | Skipped entirely if Room has ≥ 900 questions (zero Firestore reads) |

### How a Test Is Assembled (Pool Model, Not Batch-Sequential)

Each OIR test is a **freshly sampled 50-question set** drawn from the **1000-question flattened Room pool** — **NOT** in batch order, and **NOT** from a single batch. `OIRQuestionSelector.selectQuestions(50)` works like this:

1. Query Room for unused questions of each type (ignoring `batchId`):
   ```sql
   SELECT * FROM cached_oir_questions 
   WHERE type = 'VERBAL_REASONING' 
   AND (lastUsed IS NULL OR lastUsed < 7_days_ago) 
   ORDER BY RANDOM() 
   LIMIT 20
   ```
2. Enforce type distribution:

| Type | Count | Ratio |
|---|---|---|
| Verbal Reasoning | 20 | 40% |
| Non-Verbal Reasoning | 20 | 40% |
| Numerical Ability | 7 | 15% |
| Spatial Reasoning | 3 | 5% |

3. Repeat for each type (Non-Verbal, Numerical, Spatial)
4. Questions are sampled **from all 1000 questions**, **from all 20 batches**, **randomly** — the `batchId` field is **ignored** during selection
5. Final 50 are shuffled before presentation

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
- `recordTestUsage()` increments atomically via Firestore transaction after successful submission
- `markQuestionsUsed()` updates Room for the 7-day reuse window

### Submit Flow
```
canTakeTest()            → Firestore usage check (monthly gate)
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
| `core/data/.../OIRQuestionCacheManager.kt` | Sync / download / cache lifecycle |
| `core/data/.../OIRQuestionSelector.kt` | Type-distribution selection + 7-day reuse logic |
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

