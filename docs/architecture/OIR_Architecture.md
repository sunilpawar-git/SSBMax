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

### Storage Layout
- **Firestore:** `test_content/oir/batches/batch_pdf_001` … `batch_pdf_020` (1000 questions total)
- **Firebase Storage:** `oir/pdf_questions/set{N}_q{MM}.png` (public HTTPS URLs)
- **Room (local cache):** managed by `OIRQuestionCacheManager` + `OIRQuestionSelector`

### Two-Phase Cache on First Launch
| Phase | Batches | Behaviour |
|---|---|---|
| Phase 1 (blocking) | 1–4 | Downloaded before first test can start (~200 questions) |
| Phase 2 (background) | 5–20 | Downloaded while user takes the first test |
| Subsequent launches | — | Skipped entirely if Room has ≥ 900 questions (zero Firestore reads) |

### How a Test Is Assembled
Each OIR test is a **freshly sampled 50-question set** drawn from the 1000-question Room pool — users are NOT given the 20 PDF sets in order. `OIRQuestionSelector.selectQuestions(50)` enforces:

| Type | Count | Ratio |
|---|---|---|
| Verbal Reasoning | 20 | 40% |
| Non-Verbal Reasoning | 20 | 40% |
| Numerical Ability | 7 | 15% |
| Spatial Reasoning | 3 | 5% |

- **7-day reuse window:** questions unused in the last 7 days are preferred; already-used questions are only drawn when unused supply runs short
- Final 50 are **shuffled** before presentation

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
