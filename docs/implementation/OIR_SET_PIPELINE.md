# OIR Set Pipeline — Agent Instructions

How to process OIR practice sets 2–20 and publish them to the app.
Follow these steps exactly, one set at a time.

---

## Source PDF

```
/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf
```

Use this path for all `--pdf` arguments. Do not ask the user for the PDF path.

---

## Prerequisites (one-time setup)

- `pymupdf` and `pillow` installed: `pip install pymupdf pillow`
- Firebase service account at `.firebase/service-account.json` or `serviceAccountKey.json`
- `firebase-admin` npm package installed in project root: `npm install`
- Set 1 already processed and in Firestore (baseline)

---

## Scripts reference

| Script | Purpose |
|---|---|
| `scripts/process-oir-set.py` | Master pipeline — orchestrates all three phases |
| `scripts/auto-crop-oir-images.py` | Phase 1a: crops question figure WebPs from PDF using text layer |
| `scripts/upload-oir-set-images.js` | Phase 1b: uploads WebPs to Firebase Storage, writes URL map JSON |
| `scripts/upload-oir-sets.js` | Phase 3: merges image URLs and uploads question JSON to Firestore |
| `scripts/oir-set-configs/set_0NN.json` | Per-set config (page range + image question list) |

### What each script does in detail

**`auto-crop-oir-images.py`**
- Renders each PDF page at 2× zoom (ZOOM = 2.0) via pymupdf
- Detects question boundaries from the PDF text layer by finding standalone `N.` markers at left margin (x0 < 80pt)
- Crops each image question's figure between its question marker and the next question's marker
- **Branding removal**: strips `PAGE_HEADER_PX = 40px` (top) and `PAGE_FOOTER_PX = 52px` (bottom) from every page and from any stitch seam — this removes "OIR TEST - PRACTICE QUESTIONS…" header and "N | P a g e  shop.ssbcrack.com" footer
- For questions that span two pages: stitches bottom of page A (minus footer) + top of page B (minus header) — branding never appears in the middle of an image
- **Output format: WebP only** (`WEBP_QUALITY = 85`) — saved as `q01.webp`, `q03.webp`, etc. in `scripts/oir_images/set_NN/`

**`upload-oir-set-images.js`**
- Reads all `q*.webp` files from `scripts/oir_images/set_NN/`
- Uploads to Firebase Storage path `oir_question_images/set_NN/qXX.webp` with `contentType: 'image/webp'`
- Writes `scripts/image-url-map-set-00N.json` with `{ "oir_sNN_qNN": "<download_url>" }` entries
- **Only processes `.webp` files** — any non-WebP file is ignored

**`upload-oir-sets.js`**
- Reads `scripts/oir-set-00N.json` (reviewed question JSON)
- Merges image URLs from `scripts/image-url-map-set-00N.json` into `questionImageUrl` fields
- Writes to Firestore: `test_content/oir/test_sets/set_00N`
- Verifies the write by reading back the document
- Supports `--dry-run` flag to preview without writing to Firestore

---

## Config files

Config files live in `scripts/oir-set-configs/`. Each is a small JSON with four fields:

```json
{
  "setNumber": N,
  "pdfPageStart": <first page of this set in the PDF, 1-based>,
  "pdfPageEnd":   <last page of this set in the PDF, 1-based>,
  "imageQuestions": [list of question numbers that have figures],
  "totalQuestions": 50
}
```

### Sets completed

| Set | Config | PDF pages | Image questions | Status |
|---|---|---|---|---|
| 1 | `set_001.json` | 7–20 | 1,3,4,5,6,7,8,12,14,17,21,24,25,34,35,36,38,39,43,44,47,48 | ✅ Live in Firestore |
| 2 | `set_002.json` | 23–36 | 1,3,4,5,6,7,8,12,14,17,20,21,23,24,25,34,35,36,38,39,43,44,47,48 | 🔄 JSON reviewed, pending upload |
| 3–20 | `set_003.json` … | TBD | TBD | Create before running pipeline |

---

## Step-by-step for each new set

### Step 0 — Create the config file

```bash
cp scripts/oir-set-configs/set_001.json scripts/oir-set-configs/set_00N.json
```

Open `scripts/oir-set-configs/set_00N.json` and update:
- `setNumber` → N
- `pdfPageStart` → first page of set N in the PDF (1-based, open the PDF and find the page starting at "1.")
- `pdfPageEnd` → last page before the next set begins
- `imageQuestions` → list of question numbers whose answer requires looking at a diagram, figure, cube, arrow series, or shape pattern

---

### Step 1 — Crop and upload images

```bash
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf' \
  --phase images
```

This runs `auto-crop-oir-images.py` then `upload-oir-set-images.js`.

**Branding is automatically stripped** — `PAGE_HEADER_PX = 40` and `PAGE_FOOTER_PX = 52` are applied to every crop and every stitch seam. If any "shop.ssbcrack.com" text or page numbers still appear in an output WebP, increase `PAGE_FOOTER_PX` in `auto-crop-oir-images.py` until clean.

**Spot-check before continuing:** Open 3–4 WebPs in `scripts/oir_images/set_0N/` and confirm:
- Figure is cleanly cropped — question text above, nothing from the next question below
- No "N | P a g e  shop.ssbcrack.com" footer
- No "OIR TEST - PRACTICE QUESTIONS…" header
- All files are `.webp` (never `.png` or `.jpg`)

If a crop boundary is wrong:
- `MARGIN_TOP = 8` — pixels kept above the question number; increase to capture more above the figure
- `MARGIN_BOTTOM = 18` — pixels stripped before next question's text; increase if next question's label bleeds in
- Re-run `--phase images` after adjusting

If a question marker is not found (`⚠️ Markers NOT found for questions: [N]`):
- **Fixed in Set 2**: the script now uses word-level extraction (`get_text("words")`) so both standalone `N.` blocks and inline `N. question text...` blocks are detected. No action needed for this in future sets.
- If markers still fail: some sets may use `N)` instead of `N.` → update regex in `find_question_positions()` in `auto-crop-oir-images.py`

---

### Step 2 — Extract questions and generate draft JSON

```bash
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf' \
  --phase questions
```

Outputs `scripts/oir-set-00N.json` with `"_draft": true` and `"_reviewNeeded": [...]`.

---

### Step 3 — Review the draft JSON

Open `scripts/oir-set-00N.json` and work through every question in `_reviewNeeded`.

For each question check:

1. **`questionText`** — Is the extracted text accurate and complete?
   - Verbal questions: confirm the full sentence was captured
   - Image questions: text is usually minimal ("See figure above.") — expected

2. **`correctAnswerId`** — Does it match the answer key in the PDF?
   - Answer IDs: `opt_a` / `opt_b` / `opt_c` / `opt_d` (verbal) or `opt_1` … `opt_5` (spatial)

3. **`options`** — Are the option labels correct?
   - Verbal: True / Probable / False / Absurd (or the extracted labels)
   - Spatial: 1 / 2 / 3 / 4 / 5 (or fewer if the figure has fewer options)

4. **`type`** — Set the correct question type:
   - `VERBAL_REASONING` — statement + True/Probable/False/Absurd
   - `SPATIAL_REASONING` — figure, cube, shape classification, alike/not alike
   - `SERIES_COMPLETION` — find the next item in a series
   - `ANALOGY` — A is to B as C is to ?
   - `ODD_ONE_OUT` — which one does not belong
   - `CLASSIFICATION` — Group A / Group B

**Special question types found in Set 2+ (require manual option construction):**

| Pattern | Fix |
|---|---|
| "Find if the two cubes are alike" (Q3, Q4) | options: `[{id:"opt_1",text:"Yes"},{id:"opt_2",text:"No"}]`; correctAnswerId: `opt_1` or `opt_2` |
| "Write the last/second letter of the rearranged sentence/word" (Q9, Q10, Q41, Q42) | Add 5 letter options with one being the correct letter |
| "Which two sets of letters will come next" (Q13) | Add 4–5 complete letter-pair options |
| "Choose one word from each line" (Q18) | Restructure as 4–5 complete-sentence options |
| Arithmetic fill-in-the-blank "Subtract/Add" (Q20, Q23) | Solve from the figure image; add 4–5 digit options |
| "Which two numbers will come next" (Q45) | Add 4–5 number-pair options |

5. **`explanation`** — Replace every `"TODO: Add explanation for QN"` with a concise explanation of why the correct answer is correct (1–3 sentences).

When review is complete:
- Remove the `"_draft": true` line
- Remove the `"_reviewNeeded": [...]` line
- Save the file

**Do not proceed to Step 4 while `_draft: true` is present.**

---

### Step 4 — Dry-run upload (sanity check)

```bash
node scripts/upload-oir-sets.js --set N --dry-run
```

Prints the document that would be uploaded — question count, image count, type breakdown, sample IDs — without writing anything to Firestore. Confirm counts look right before the real upload.

---

### Step 5 — Upload to Firestore

```bash
python3 scripts/process-oir-set.py --set N --phase upload
```

This calls `node scripts/upload-oir-sets.js --set N`, which:
1. Loads `scripts/oir-set-00N.json`
2. Merges image URLs from `scripts/image-url-map-set-00N.json`
3. Writes the document to `test_content/oir/test_sets/set_00N` in Firestore
4. Verifies the write by reading back the document and printing the question count

---

### Step 6 — Verify in the app

1. Force-close the app and reopen (or clear app data) to trigger a fresh Firestore sync
2. Navigate to OIR → Practice Sets
3. Set N should appear in the list
4. Start a test on Set N and confirm:
   - All 50 questions load
   - Image questions display figures (no branding, no cropping artifacts)
   - Correct answer feedback works
   - Progress is saved on exit and resume

---

## Full pipeline (all phases at once)

```bash
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf'
```

**Note:** Running all phases at once skips the spot-check and review steps. Always use individual `--phase` flags so you can verify images and review the JSON before uploading.

---

## Troubleshooting

### Branding visible in cropped WebP

`auto-crop-oir-images.py` strips `PAGE_HEADER_PX = 40` (top) and `PAGE_FOOTER_PX = 52` (bottom). If branding still shows:
- Increase `PAGE_FOOTER_PX` (for footer/page number) or `PAGE_HEADER_PX` (for header) and re-run `--phase images`
- The two-page stitch path strips both constants at the seam — check that the stitch line itself is clean

### Image question missing / wrong crop

The auto-crop uses PDF text-layer markers (`N.` at left margin x0 < 80pt). If a marker is missed, the script warns: `⚠️ Markers NOT found for questions: [N]`.

Fixes:
- Some sets use `N)` instead of `N.` → update the regex in `find_question_positions()` in `auto-crop-oir-images.py`
- If the marker is found but the crop boundary is wrong, adjust `MARGIN_TOP` (default 8px) and `MARGIN_BOTTOM` (default 18px)

### Answers not extracted

The extractor searches for `\bANSWERS\b` or `\bEXPLANATION\b`. If the PDF spells it differently (e.g. "Answer Key", "SOLUTION") you will see: `⚠️ Answers section not found`.

Fix: in `extract_answers()` in `process-oir-set.py`, add the variant:
```python
if re.search(r'\b(ANSWERS|EXPLANATION|ANSWER KEY|SOLUTION)', text, re.IGNORECASE):
```

### Upload fails with PERMISSION_DENIED

Check that the service account file exists:
```
.firebase/service-account.json   ← preferred
serviceAccountKey.json           ← fallback
```

### App shows old data after upload

Room DB caches Firestore data. Clear the app's data storage on the device, or clear app data via Android settings.
