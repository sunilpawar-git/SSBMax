# OIR Sets 2–50 Execution Guide

Complete reference for deploying OIR practice sets to production. Every section
is written from hard lessons learned during Set 1 deployment. Follow the
pre-flight checklist before every upload to avoid the bugs we already hit.

---

## Current Status

| Set | Config | PDF Pages | Images | JSON | Firestore |
|-----|--------|-----------|--------|------|-----------|
| 1   | `set_001.json` | 7–20  | SVG (22 files) at `oir/images/set_001/` | Reviewed ✅ | Live ✅ |
| 2   | `set_002.json` | 23–36 | WebP (24 files) uploaded ✅ | Draft — 30 Q need review ⚠️ | Not yet |
| 3–20 | create from template | TBD — run `discover-pdf-pages.py` | Pending | Pending | Pending |

> **Set 1 is architecturally different from all others.** It uses hand-crafted SVG
> files (not PDF-cropped WebPs) because the original PDF crops had visible
> watermarks. Sets 2–50 use the automated WebP pipeline.

---

## Architecture: What Each Piece Does

```
PDF
 │
 ▼
auto-crop-oir-images.py ──── renders pages at 2× zoom, detects question
                              boundaries from PDF text layer, strips branding,
                              saves as q01.webp…qNN.webp
                              Output: scripts/oir_images/set_NN/
 │
 ▼
upload-oir-set-images.js ─── uploads WebP files to Firebase Storage at
                              oir_question_images/set_NN/qNN.webp
                              generates token-based download URLs
                              Output: scripts/image-url-map-set-NNN.json
 │
 ▼
[MANUAL REVIEW] ─────────── edit scripts/oir-set-NNN.json
                              fill in question text, correct answers,
                              explanations; remove _draft flag
 │
 ▼
validate-oir-set.py ─────── pre-upload safety check (catches every bug
                              encountered in Set 1 before it hits Firestore)
 │
 ▼
upload-oir-sets.js ──────── reads oir-set-NNN.json, REPLACES questionImageUrl
                              with URLs from image-url-map-set-NNN.json,
                              writes to Firestore test_content/oir/test_sets/set_NNN
```

### CRITICAL: The URL Map Override

`upload-oir-sets.js` line 72:
```js
const imageUrl = imageUrlMap ? (imageUrlMap[q.id] || null) : q.questionImageUrl || null;
```

**When `image-url-map-set-NNN.json` exists, it ALWAYS wins.** Any `questionImageUrl`
you manually put in the JSON file is silently replaced. This caused the Set 1 bug
where Firestore ended up with old watermarked WebP URLs after we had carefully set
SVG URLs in the JSON.

Rules:
- Never manually edit `questionImageUrl` in the JSON — edit the URL map instead.
- Before uploading, verify the URL map has the right URLs (see pre-flight checklist).
- If the URL map is wrong, fix it and re-upload to Firestore. Room cache will serve
  stale data until the device clears cache.

---

## Lessons From Set 1 (Bugs and Fixes)

### Bug 1 — Old URL map overwrote new image URLs

**What happened:** We updated the SVG `questionImageUrl` fields in `oir-set-001.json`.
But `image-url-map-set-001.json` still had old WebP URLs. The upload script used
the URL map, discarding our SVG URLs. Firestore got old watermarked WebP URLs.
App showed watermarked images despite SVGs being in Storage.

**Fix:** Replaced `image-url-map-set-001.json` with SVG URLs, re-uploaded to Firestore.

**Prevention:** `validate-oir-set.py` now checks that URL map entries point to the
current set's storage path and match the `imageQuestions` list in the config.

---

### Bug 2 — SVG files had embedded hint/answer text

**What happened:** 18 of 22 SVGs had `<text>` elements with captions like
`"Both cubes are the same — pentagon and heart are on opposite faces"` or
`"Answer: 3"`. These appeared in the image area of the app, giving away the answer
before the user attempted the question.

**Fix:** Python one-liner stripped all caption `<text>` elements from SVGs.

**Prevention:** This only affects hand-crafted SVGs (Set 1). WebP images from the
auto-crop pipeline cannot contain text overlays unless the PDF itself had them.
If you ever create SVGs for a set: **never embed explanatory text, answer hints,
or question text inside the SVG file itself.** All of that lives in the JSON.

---

### Bug 3 — App cache served stale data after Firestore update

**What happened:** After re-uploading Set 1 to Firestore with correct SVG URLs,
the app continued showing old WebP images. Room DB had cached the old Firestore
data and was not fetching fresh data.

**Fix:** `adb shell pm clear com.ssbmax` to wipe Room DB cache.
App then re-fetched from Firestore and showed the correct SVGs.

**Prevention:** Always clear app cache (or reinstall) after any Firestore re-upload.
Add this to your post-upload verification step.

---

### Bug 4 — Firebase Storage 403 for new storage path

**What happened:** SVGs uploaded to `oir/images/set_001/` returned HTTP 403.
The `storage.rules` file only had rules for `oir_question_images/**` (WebP path)
and other known paths. The new `oir/` path was hitting the default deny-all rule.

**Fix:** Added `match /oir/{allPaths=**} { allow read; }` to `storage.rules`
and ran `firebase deploy --only storage`.

**Prevention:** For Sets 2–50, images are at `oir_question_images/set_NN/` using
token-based download URLs, which bypass Storage rules entirely. No storage.rules
change is needed. This bug only applies if you ever use a new storage path.

---

### Bug 6 — SVG fill-pattern misinterpretation (Set 1 Q8)

**What happened:** Q8 is a star series where the black half rotates 90° clockwise each
step (three given figures, five answer options). The SVG described Star 1 as
"FULLY BLACK" when it was actually a two-tone half-black star. This broke the entire
visual concept — the app rendered a decreasing-fill series instead of a rotating one.

Root cause: the SVGs were hand-crafted from verbal descriptions of the PDF, not traced
pixel-by-pixel. Fill-pattern questions (rotating fills, color inversions) are
especially easy to mis-describe without careful visual reference.

**Fix:** Rewrote Q8 using SVG `clipPath` with diagonal polygon clips through the star
centre (60, 55). Four re-usable clip paths handle all 4×90° rotations:

```svg
<defs>
  <clipPath id="clipUL"><polygon points="-50,-50 200,-50 60,55 -50,200"/></clipPath>
  <clipPath id="clipLL"><polygon points="-50,-50 0,0 120,110 200,200 -50,200"/></clipPath>
  <clipPath id="clipLR"><polygon points="200,-50 120,0 0,110 -50,200 200,200"/></clipPath>
  <clipPath id="clipUR"><polygon points="-50,-50 200,-50 200,200 120,110 0,0"/></clipPath>
</defs>
```

These clips are defined once and work inside any `<g transform="translate(dx,dy)">` because
`clipPathUnits="userSpaceOnUse"` interprets coordinates in the current (translated) space.

**Prevention:** This only affects Set 1 hand-crafted SVGs. Sets 2–50 use WebP auto-crops
— no SVG fill patterns to mis-describe. For any future hand-crafted SVG:
- Never describe a patterned fill from memory. Open the PDF side-by-side.
- Two-tone / half-fill figures: use `clipPath` polygons through the shape's centre.
- After creating an SVG, open it in a browser and compare it to the PDF before uploading.

**Set 1 SVG audit status** (checked against PDF):

| SVG | Fill risk | Status |
|-----|-----------|--------|
| Q01 cube_classification | Low (outline cubes) | Not yet verified |
| Q03 cube_alike | Low (outline cubes) | Not yet verified |
| Q04 cube_alike | Low (outline cubes) | Not yet verified |
| Q05 arrow_series | Low (arrows) | Not yet verified |
| Q06 class_ab_nested | Low (outline shapes) | Not yet verified |
| Q07 arrow_cross_series | Low (arrows + dot) | Not yet verified |
| **Q08 star_series** | **High (rotating fill)** | **✅ Fixed** |
| Q12 figure_analogy_diamond | Low (outline shapes) | Not yet verified |
| Q14 cross_letter_series | Low (text symbols) | Not yet verified |
| Q17 double_line_series | Medium (line rotation) | Not yet verified |
| Q21 hexagon_series | Medium (shape positions) | Not yet verified |
| Q24 pentagon_star_series | Low (count shapes) | Not yet verified |
| Q25 class_ab_combined | Low (outline shapes) | Not yet verified |
| Q34 class_ab_diff_nested | Low (outline shapes) | Not yet verified |
| Q35 class_ab_symmetry | Low (outline shapes) | Not yet verified |
| Q36 arrow_analogy | Medium (arrow + inner shape) | Not yet verified |
| Q38 class_ab_joined_lines | Low (outline shapes) | Not yet verified |
| Q39 class_ab_opposite_edges | Low (outline shapes) | Not yet verified |
| Q43 figure_analogy_color_reverse | Medium (fill inversion) | Not yet verified |
| Q44 figure_analogy_circles | Low (count shapes) | Not yet verified |
| Q47 class_ab_letters | Low (text symbols) | Not yet verified |
| Q48 class_ab_edge_count | Low (count edges) | Not yet verified |

Verify the "Medium" risk SVGs (Q17, Q21, Q36, Q43) in the app with the PDF open.
If any look wrong, rebuild using the clipPath technique shown above.

---

### Bug 5 — PDF answer key errors (Set 1 Q26 and Q27)

The SSBCrack PDF answer key has at least two confirmed errors in Set 1:

| Q | PDF says | Correct answer | Reason |
|---|----------|----------------|--------|
| 26 | 4 / Newspaper | People (opt_a) | Alphabetically P > N; "People" is the correct continuation |
| 27 | 4 / Umbrella | Utmost (opt_b) | PDF option 4 IS Umbrella; Utmost is option 2 and the correct answer |

**Always verify verbal answers against the question text, not just the PDF key.**
The pipeline auto-extracts PDF answers, but verbal reasoning questions should be
spot-checked for logic.

---

## Standard Workflow — One Set at a Time

### Step 0 — Find PDF page range (first time per set)

```bash
python3 scripts/discover-pdf-pages.py \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf'
```

Outputs a table like:
```
Set  1:  pages  7–22   (detected from "OIR PRACTICE SET - 1")
Set  2:  pages 23–38
...
```

Use this to fill in `pdfPageStart` / `pdfPageEnd` for each config file.

---

### Step 1 — Create config file

```bash
cp scripts/oir-set-configs/set_001.json scripts/oir-set-configs/set_00N.json
```

Edit `set_00N.json`:
```json
{
  "setNumber": N,
  "pdfPageStart": <from discover-pdf-pages output>,
  "pdfPageEnd":   <from discover-pdf-pages output>,
  "imageQuestions": [1, 3, 4, 5, 6, 7, 8, 12, 14, 17, 21, 24, 25, 34, 35, 36, 38, 39, 43, 44, 47, 48],
  "totalQuestions": 50
}
```

For `imageQuestions`: start with Set 1's list as a baseline. Adjust after running
the questions phase — any question that has no extractable text is likely image-based.

---

### Step 2 — Crop and upload images

```bash
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf' \
  --phase images
```

**Spot-check (mandatory before continuing):**
Open 4–5 files in `scripts/oir_images/set_NN/` and verify:
- [ ] Figure is clearly cropped — no question text from neighboring questions
- [ ] No "N | P a g e  shop.ssbcrack.com" footer
- [ ] No "OIR TEST - PRACTICE QUESTIONS…" header
- [ ] Multi-page questions stitched cleanly at the seam
- [ ] All files are `.webp` (never `.png`)

If branding still shows, increase `PAGE_FOOTER_PX` or `PAGE_HEADER_PX` in
`auto-crop-oir-images.py` and re-run.

---

### Step 3 — Extract questions

```bash
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf' \
  --phase questions
```

Outputs `scripts/oir-set-00N.json` with `"_draft": true`.

---

### Step 4 — Review the draft JSON

Open `scripts/oir-set-00N.json` and go through every question in `_reviewNeeded`.

**For each question, verify:**

1. **`questionText`** — Is it complete and accurate?
   - Verbal questions: confirm the full sentence, no truncation at page boundary
   - Image questions: should be "See figure above." or a short instruction

2. **`correctAnswerId`** — Cross-check against the PDF answer key AND the question logic
   - Known PDF errors: see Bug 5 above
   - Verbal reasoning: verify the logic yourself, don't just trust the key

3. **`options`** — Are labels correct and complete?
   - Verbal (True/Probable/False/Absurd): auto-filled, but verify
   - Spatial/image: options are 1–5 by default; adjust if the figure has fewer

4. **`type`** — Use the correct type string:

   | Type | When to use |
   |------|-------------|
   | `VERBAL_REASONING` | Statement rated True / Probable / False / Absurd |
   | `SPATIAL_REASONING` | Cube alike/unlike, Class A/B, figure identification |
   | `SERIES_COMPLETION` | Find the next figure/number/letter in a series |
   | `ANALOGY` | A is to B as C is to ? |
   | `ODD_ONE_OUT` | Which one does not belong |
   | `CLASSIFICATION` | Group the figures into two classes |

5. **`explanation`** — Replace every `"TODO: Add explanation for QN"` with
   1–3 sentences explaining why the correct answer is correct.

**Special question patterns that need manual option construction:**

| Pattern | How to handle |
|---------|---------------|
| "Find if two cubes are alike" | options: `[{id:"opt_a",text:"Yes"},{id:"opt_b",text:"No"}]` |
| "Write the last letter of rearranged word" (Q9/10/41/42) | 5 letter options; one is the correct letter |
| "Which two letter-sets come next" (Q13) | 4–5 complete letter-pair options |
| "Choose one word from each line" (Q18) | 4–5 sentence options |
| Arithmetic fill-in-the-blank (Q20/23) | Solve from the figure; 4–5 digit options |
| "Which two numbers come next" (Q45) | 4–5 number-pair options |

**When review is complete:**
- Remove `"_draft": true`
- Remove `"_reviewNeeded": [...]`
- Save the file

Do NOT proceed to Step 5 while `_draft: true` is present.

---

### Step 5 — Pre-flight validation (mandatory)

```bash
python3 scripts/validate-oir-set.py --set N
```

This script checks:
- Config file exists
- URL map exists and covers every question in `imageQuestions`
- URL map entries belong to this set (not a previous set's URLs)
- Set JSON exists and `_draft` is removed
- No `TODO` explanations remain
- All `correctAnswerId` values are valid option IDs in the question
- Question count matches `totalQuestions`

**Only proceed to Step 6 if validation passes with zero errors.**

---

### Step 6 — Dry-run (sanity check)

```bash
node scripts/upload-oir-sets.js --set N --dry-run
```

Prints the document that would be uploaded: question count, image count, type
breakdown. Confirm these match expectations before the real upload.

---

### Step 7 — Upload to Firestore

```bash
python3 scripts/process-oir-set.py --set N --phase upload
```

The script:
1. Loads `scripts/oir-set-00N.json`
2. Replaces `questionImageUrl` with URLs from `image-url-map-set-00N.json`
3. Writes to Firestore at `test_content/oir/test_sets/set_00N`
4. Reads back and verifies question count

---

### Step 8 — Verify on device

```bash
adb shell pm clear com.ssbmax
adb shell am start -n com.ssbmax/.MainActivity
```

In the app, navigate to **OIR → Practice Sets → Set N** and confirm:
- [ ] Set appears in the list
- [ ] All 50 questions load
- [ ] Image questions show the figure (no watermarks, no cropping artifacts)
- [ ] **For each image question: open the PDF side-by-side and confirm the figure matches** — especially for series, analogy, and fill-pattern questions (Bug 6)
- [ ] Selecting the correct answer shows "Correct!" with the explanation
- [ ] Selecting a wrong answer shows the correct answer highlighted
- [ ] "Next" button advances to the next question
- [ ] Progress is saved when you exit mid-test and resume

> For Set 1 specifically, pay extra attention to Q17, Q21, Q36, Q43 — these are
> medium-risk SVGs that have not yet been verified against the PDF.

---

## Completing Set 2 (Already Partially Done)

Set 2 images are already uploaded to Firebase Storage (24 WebPs).
The URL map at `scripts/image-url-map-set-002.json` is correct.

**Only the JSON review remains:**

```
# Open and review:
scripts/oir-set-002.json   ← _draft: true, 30 questions need review

# After review, validate and upload:
python3 scripts/validate-oir-set.py --set 2
node scripts/upload-oir-sets.js --set 2 --dry-run
python3 scripts/process-oir-set.py --set 2 --phase upload
```

Do NOT re-run the images phase for Set 2 — images are already in Storage and the
URL map is correct. Re-running would overwrite the URL map with new token-based
URLs and waste Storage writes.

---

## Troubleshooting

### "Image shows old/wrong picture after upload"

Root cause: URL map had wrong URLs, or Room DB is serving cached Firestore data.

Fix:
1. Check `image-url-map-set-NNN.json` has the correct URLs
2. If URLs are wrong: fix the file, re-run `node scripts/upload-oir-sets.js --set N`
3. Always clear app data: `adb shell pm clear com.ssbmax`

### "Branding visible in cropped WebP"

Increase stripping constants in `auto-crop-oir-images.py`:
- Footer (page number + URL): `PAGE_FOOTER_PX = 52` → try 70
- Header: `PAGE_HEADER_PX = 40` → try 60
Re-run `--phase images` and spot-check again.

### "Markers NOT found for questions: [N]"

The PDF text layer doesn't have a clean `N.` marker at x0 < 80pt for question N.
Possible causes and fixes:

| Cause | Fix |
|-------|-----|
| Question uses `N)` instead of `N.` | Update regex in `find_question_positions()` |
| Question marker is indented past 80pt | Increase `x0 < 80` threshold |
| Image question with no text label | Add N to `imageQuestions`; crop won't work, mark for manual SVG |

### "Answer section not found"

The extractor looks for `ANSWERS AND EXPLANATIONS`, `ANSWER KEY`, or `SOLUTION`.
If the PDF uses a different heading, add it to the regex in `extract_answers()`:
```python
if re.search(r'\bANSWERS AND EXPLANATIONS\b|\bANSWER KEY\b|\bSOLUTION\b|\bYOUR HEADING\b', ...)
```

### "Upload fails with PERMISSION_DENIED"

The service account file is missing or path is wrong.
Expected locations (checked in order):
1. `.firebase/service-account.json`
2. `serviceAccountKey.json` (project root)

### "_draft flag is set — questions may still need review"

The upload script warns but proceeds. This means you have `"_draft": true` still
in the JSON. Fix: complete the review, remove the flag, re-run `validate-oir-set.py`.

### "App shows stale data / Set not appearing"

Room DB caches Firestore data for offline use. After any Firestore update:
```bash
adb shell pm clear com.ssbmax      # wipes Room DB + all app storage
```
Or in Android Settings → Apps → SSBMax → Clear Data.

---

## Image Storage Architecture

| Set | Storage path | URL format | Storage rule needed? |
|-----|--------------|------------|----------------------|
| 1   | `oir/images/set_001/*.svg` | Public URL (no token) | Yes — `match /oir/{allPaths=**}` added ✅ |
| 2–20 | `oir_question_images/set_NN/*.webp` | Token URL (`?token=xxx`) | No — token bypasses rules |

Token-based URLs for Sets 2+ do not expire unless manually revoked in Firebase Console.
They are safe to use in production.

---

## PDF Page Range Reference

Run once to fill in all configs:
```bash
python3 scripts/discover-pdf-pages.py \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf'
```

Known ranges confirmed from Set 1 and Set 2 configs:

| Set | pdfPageStart | pdfPageEnd |
|-----|-------------|------------|
| 1   | 7           | 22         |
| 2   | 23          | 36         |
| 3–20 | Run discover-pdf-pages.py | — |

---

## Quick Reference Commands

```bash
# Full pipeline for a new set (all three phases):
python3 scripts/process-oir-set.py --set N \
  --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf'

# Individual phases:
python3 scripts/process-oir-set.py --set N --pdf '...' --phase images
python3 scripts/process-oir-set.py --set N --pdf '...' --phase questions
python3 scripts/process-oir-set.py --set N --phase upload

# Validate before uploading:
python3 scripts/validate-oir-set.py --set N

# Dry-run upload:
node scripts/upload-oir-sets.js --set N --dry-run

# Force app refresh after upload:
adb shell pm clear com.ssbmax && adb shell am start -n com.ssbmax/.MainActivity

# Find PDF page ranges for all sets:
python3 scripts/discover-pdf-pages.py --pdf '...'
```
