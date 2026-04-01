# OIR Set Pipeline — Agent Instructions

How to process OIR practice sets 2–20 and publish them to the app.
Follow these steps exactly, one set at a time.

---

## Prerequisites (one-time setup)

- `pymupdf` and `pillow` installed: `pip install pymupdf pillow`
- Firebase service account at `.firebase/service-account.json` or `serviceAccountKey.json`
- `firebase-admin` npm package installed in project root
- Set 1 already processed and in Firestore (baseline)

---

## Scripts quick reference

| Script | Purpose |
|---|---|
| `scripts/process-oir-set.py` | Master pipeline — runs all three phases |
| `scripts/auto-crop-oir-images.py` | Phase 1a: crops question figure WebPs from PDF |
| `scripts/upload-oir-set-images.js` | Phase 1b: uploads WebPs to Firebase Storage |
| `scripts/upload-oir-sets.js` | Phase 3: uploads question JSON to Firestore |
| `scripts/oir-set-configs/set_0NN.json` | Per-set config (page range + image question list) |

---

## Step-by-step for each new set

### Step 0 — Create the config file

```bash
cp scripts/oir-set-configs/set_001.json scripts/oir-set-configs/set_00N.json
```

Open `scripts/oir-set-configs/set_00N.json` and update these four fields:

```json
{
  "setNumber": N,
  "pdfPageStart": <first page of this set in the PDF, 1-based>,
  "pdfPageEnd":   <last page of this set in the PDF, 1-based>,
  "imageQuestions": [list of question numbers that have figures],
  "totalQuestions": 50
}
```

**How to find the page range:** Open the PDF, navigate to the page that starts "1." for this set and note its 1-based page number (`pdfPageStart`). The last page before the next set begins is `pdfPageEnd`.

**How to find image questions:** Scan the question pages. Any question whose answer requires looking at a diagram, figure, cube, arrow series, or shape pattern is an image question.

---

### Step 1 — Crop and upload images

```bash
python3 scripts/process-oir-set.py --set N --pdf /path/to/oir.pdf --phase images
```

This:
1. Renders each PDF page at 2× zoom
2. Auto-detects question boundaries from the PDF text layer
3. Crops each image question figure and strips SSBCrack header/footer branding
4. Saves `q01.webp`, `q03.webp`, etc. to `scripts/oir_images/set_0N/`
5. Uploads all WebPs to Firebase Storage (`oir_question_images/set_0N/`)
6. Writes `scripts/image-url-map-set-00N.json` with the download URLs

**Spot-check:** Open 3–4 WebP files in `scripts/oir_images/set_0N/` and verify:
- The figure is cleanly cropped (question text above, nothing below from the next question)
- No "N | P a g e  shop.ssbcrack.com" or "OIR TEST - PRACTICE QUESTIONS…" branding

If a crop is wrong, adjust `MARGIN_TOP` / `MARGIN_BOTTOM` constants in
`scripts/auto-crop-oir-images.py` and re-run this phase.

---

### Step 2 — Extract questions and generate draft JSON

```bash
python3 scripts/process-oir-set.py --set N --pdf /path/to/oir.pdf --phase questions
```

This:
1. Reads the PDF text layer to extract raw text per question
2. Detects labelled options `(a)/(b)/(c)/(d)` and True/Probable/False/Absurd patterns
3. Finds the "ANSWERS AND EXPLANATIONS" section and maps correct answers
4. Writes a draft `scripts/oir-set-00N.json`

The draft JSON has two special fields at the top level:
- `"_draft": true` — reminder flag that review is pending
- `"_reviewNeeded": [list of question numbers]` — questions that need manual attention

---

### Step 3 — Review the draft JSON

Open `scripts/oir-set-00N.json` and work through the `_reviewNeeded` list.

For **each question** in `_reviewNeeded`, check:

1. **`questionText`** — Is the extracted text accurate and complete?
   - Verbal questions: confirm the full sentence was captured
   - Image questions: the text is usually minimal ("See figure above.") — that is expected

2. **`correctAnswerId`** — Does it match the answer key in the PDF?
   - Answer IDs: `opt_a` / `opt_b` / `opt_c` / `opt_d` (verbal) or `opt_1` … `opt_5` (spatial)

3. **`options`** — Are the option labels correct?
   - Verbal: True / Probable / False / Absurd (or extracted labels)
   - Spatial: 1 / 2 / 3 / 4 / 5 (or fewer if the figure only has fewer options)

4. **`type`** — Set the correct question type:
   - `VERBAL_REASONING` — statement + True/Probable/False/Absurd
   - `SPATIAL_REASONING` — figure, cube, shape classification, alike/not alike
   - `SERIES_COMPLETION` — find the next item in a series
   - `ANALOGY` — A is to B as C is to ?
   - `ODD_ONE_OUT` — which one does not belong
   - `CLASSIFICATION` — Group A / Group B

5. **`explanation`** — Replace `"TODO: Add explanation for QN"` with a concise explanation
   of why the correct answer is correct (1–3 sentences).

When the review is complete:
- Remove the `"_draft": true` line
- Remove the `"_reviewNeeded": [...]` line
- Save the file

---

### Step 4 — Upload to Firestore

```bash
python3 scripts/process-oir-set.py --set N --phase upload
```

This calls `node scripts/upload-oir-sets.js --set N`, which:
1. Loads `scripts/oir-set-00N.json`
2. Merges image URLs from `scripts/image-url-map-set-00N.json`
3. Writes the document to `test_content/oir/test_sets/set_00N` in Firestore
4. Verifies the write by reading back the document

---

### Step 5 — Verify in the app

1. Force-close the app and reopen (or clear app data) to trigger a fresh Firestore sync
2. Navigate to OIR → Practice Sets
3. Set N should appear in the list
4. Start a test on Set N and confirm:
   - All 50 questions load
   - Image questions display figures
   - Correct answer feedback works
   - Progress is saved on exit and resume

---

## Troubleshooting

### Image question missing / wrong crop

The auto-crop uses PDF text-layer markers (`N.` at left margin x0 < 80pt). If a marker is
missed, the script will warn: `⚠️ Markers NOT found for questions: [N]`.

Fixes:
- Open the PDF and confirm the question number format. Some sets may use `N)` instead of `N.`
  → update the regex in `find_question_positions()` in `auto-crop-oir-images.py`
- If the marker is found but the crop boundary is wrong, adjust `MARGIN_TOP` (default 8px)
  and `MARGIN_BOTTOM` (default 18px) in `auto-crop-oir-images.py`

### Answers not extracted

The extractor searches for `\bANSWERS\b` heading. If the PDF spells it differently
(e.g. "Answer Key", "SOLUTION") you will see: `⚠️ Answers section not found`.

Fix: in `extract_answers()` in `process-oir-set.py`, add the variant to the regex:
```python
if re.search(r'\b(ANSWERS|EXPLANATION|ANSWER KEY|SOLUTION)', text, re.IGNORECASE):
```

### Upload fails with PERMISSION_DENIED

The Firestore security rules may not allow writes from the service account, or the
service account file is missing. Check:
```
.firebase/service-account.json   ← preferred
serviceAccountKey.json           ← fallback
```

### App shows old data after upload

Room DB caches Firestore data. Clear the app's data storage on the device, or
increment the Room DB version to trigger a migration.

---

## Config files created so far

| Set | Config | Status |
|---|---|---|
| 1 | `scripts/oir-set-configs/set_001.json` | ✅ Done — Set 1 live in Firestore |
| 2–20 | `scripts/oir-set-configs/set_002.json` … | Create before running pipeline |

---

## Full pipeline command (all phases at once)

```bash
python3 scripts/process-oir-set.py --set N --pdf /path/to/oir.pdf
```

Use individual `--phase` flags when you want to pause between phases for review.
