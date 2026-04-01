#!/usr/bin/env python3
"""
process-oir-set.py — Master pipeline for OIR Sets 2–20

Automates the full workflow for each OIR practice set in three phases:

  images    — Auto-crop question figures from PDF + upload to Firebase Storage
  questions — Extract question text from PDF + generate draft oir-set-NNN.json
  upload    — Upload the (reviewed) draft JSON to Firestore

Usage:
  # All three phases in sequence:
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf

  # Individual phases:
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase images
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase questions
  python3 scripts/process-oir-set.py --set 2 --phase upload

Prerequisites:
  pip install pymupdf pillow
  node_modules with firebase-admin installed (npm install in project root)

Config file (required before running):
  scripts/oir-set-configs/set_00N.json
  Copy from set_001.json and update setNumber / pdfPageStart / pdfPageEnd / imageQuestions.

Typical workflow for a new set:
  1. cp scripts/oir-set-configs/set_001.json scripts/oir-set-configs/set_002.json
     — edit: setNumber, pdfPageStart, pdfPageEnd, imageQuestions

  2. python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase images
     — crops + uploads all question figure WebP files

  3. python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase questions
     — extracts text, generates scripts/oir-set-002.json draft

  4. Review scripts/oir-set-002.json:
     — verify question text, correctAnswerId, options for each question
     — fill in TODO explanations
     — remove _draft and _reviewNeeded fields when satisfied

  5. python3 scripts/process-oir-set.py --set 2 --phase upload
     — uploads to Firestore
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

try:
    import fitz  # pymupdf
except ImportError:
    print("❌ pymupdf not installed. Run: pip install pymupdf")
    sys.exit(1)

SCRIPTS_DIR = Path(__file__).parent
CONFIGS_DIR = SCRIPTS_DIR / "oir-set-configs"
PROJECT_ROOT = SCRIPTS_DIR.parent


# ── Config ────────────────────────────────────────────────────────────────────

def load_config(set_num: int) -> dict:
    config_path = CONFIGS_DIR / f"set_{set_num:03d}.json"
    if not config_path.exists():
        print(f"❌ Config not found: {config_path}")
        print(f"   Create it by copying scripts/oir-set-configs/set_001.json and editing:")
        print(f"     setNumber, pdfPageStart, pdfPageEnd, imageQuestions")
        sys.exit(1)
    with open(config_path) as f:
        return json.load(f)


# ── Phase 1: Images ───────────────────────────────────────────────────────────

def run_images_phase(set_num: int, pdf_path: Path, cfg: dict):
    print("=" * 60)
    print(f"📸  PHASE 1/3 — IMAGES  (Set {set_num})")
    print("=" * 60)

    image_questions = cfg.get("imageQuestions", [])
    print(f"   {len(image_questions)} image questions to crop: {image_questions}")
    print()

    # Step 1: Auto-crop using PDF text layer
    print("▶  Step 1/2: Auto-cropping images via PDF text layer…")
    result = subprocess.run(
        [
            sys.executable,
            str(SCRIPTS_DIR / "auto-crop-oir-images.py"),
            "--pdf", str(pdf_path),
            "--set", str(set_num),
        ],
        cwd=str(PROJECT_ROOT),
    )
    if result.returncode != 0:
        print("\n❌ Auto-crop failed. Fix errors above before uploading.")
        sys.exit(1)

    # Step 2: Upload WebP files to Firebase Storage
    print("\n▶  Step 2/2: Uploading WebP images to Firebase Storage…")
    result = subprocess.run(
        [
            "node",
            str(SCRIPTS_DIR / "upload-oir-set-images.js"),
            "--set", str(set_num),
        ],
        cwd=str(PROJECT_ROOT),
    )
    if result.returncode != 0:
        print("\n❌ Image upload failed.")
        sys.exit(1)

    print(f"\n✅  Images phase complete — {len(image_questions)} images uploaded.")


# ── Phase 2: Questions ────────────────────────────────────────────────────────

def _all_text_blocks(pdf_doc: fitz.Document, page_start: int, page_end: int):
    """
    Return a flat list of (page_idx, y0, x0, text) for all text blocks
    in pages page_start..page_end (0-based, inclusive), sorted by page then Y.
    """
    blocks = []
    for page_idx in range(page_start, page_end + 1):
        page = pdf_doc[page_idx]
        for b in page.get_text("blocks"):
            x0, y0, x1, y1, text, _bno, btype = b
            if btype == 0 and text.strip():  # text block only
                blocks.append((page_idx, y0, x0, text.strip()))
    blocks.sort(key=lambda b: (b[0], b[1]))
    return blocks


def _find_question_starts(blocks: list, total_questions: int) -> dict:
    """
    Return {qnum: block_index} for all question-number markers found in blocks.
    """
    starts = {}
    for i, (_, _, x0, text) in enumerate(blocks):
        if re.match(r'^\d{1,2}\.$', text) and x0 < 80:
            qnum = int(text.rstrip('.'))
            if 1 <= qnum <= total_questions and qnum not in starts:
                starts[qnum] = i
    return starts


def extract_question_texts(pdf_doc: fitz.Document, page_start: int, page_end: int,
                            total_questions: int) -> dict:
    """
    Extract raw text for each question number.
    Returns {qnum: raw_text_string}.
    """
    blocks = _all_text_blocks(pdf_doc, page_start, page_end)
    starts = _find_question_starts(blocks, total_questions)

    sorted_qnums = sorted(starts.keys())
    question_texts = {}

    for idx, qnum in enumerate(sorted_qnums):
        start_i = starts[qnum]
        end_i = starts[sorted_qnums[idx + 1]] if idx + 1 < len(sorted_qnums) else len(blocks)

        # Collect all text after the "N." marker up to the next question
        parts = [text for (_, _, _, text) in blocks[start_i + 1:end_i] if text]
        question_texts[qnum] = "\n".join(parts)

    return question_texts


def extract_answers(pdf_doc: fitz.Document, page_start: int, page_end: int) -> dict:
    """
    Search for an ANSWERS or EXPLANATIONS section in the set's page range
    (and up to 5 pages beyond) and return {qnum: answer_char}.

    Handles common SSBCrack formats:
      "1.(c)"   "1. (c)"   "1 - c"   "1.c"   "1 c"
    """
    answers: dict[int, str] = {}
    search_end = min(page_end + 5, pdf_doc.page_count - 1)
    in_answers = False

    for page_idx in range(page_start, search_end + 1):
        page = pdf_doc[page_idx]
        text = page.get_text("text")

        if not in_answers:
            if re.search(r'\b(ANSWERS|EXPLANATION)', text, re.IGNORECASE):
                in_answers = True
            else:
                continue

        # Match patterns like: 1.(c)  1. c  1-c  1 c  1.c
        for m in re.finditer(
            r'\b(\d{1,2})\s*[.\-]\s*\(?([a-eA-E1-5])\)?',
            text
        ):
            qnum = int(m.group(1))
            ans = m.group(2).lower()
            if 1 <= qnum <= 50 and qnum not in answers:
                answers[qnum] = ans

    return answers


def _ans_to_option_id(ans: str, is_numeric_options: bool = False) -> str:
    """Convert an answer character/digit to an option ID."""
    if ans.isdigit():
        return f"opt_{ans}" if is_numeric_options else f"opt_{chr(ord('a') + int(ans) - 1)}"
    return f"opt_{ans.lower()}"


def _parse_options(text: str):
    """
    Try to detect labelled options in question text.
    Returns (clean_question_text, options_list) or (text, None) if not found.
    """
    # Pattern: (a) ... (b) ... or (A) ... (B) ...
    if re.search(r'\([a-eA-E]\)', text):
        # Find where options start
        first = re.search(r'\n?\s*\([aA]\)', text)
        q_text = text[: first.start()].strip() if first else text
        opts_text = text[first.start():] if first else text

        options = []
        letter_map = {c: f"opt_{c}" for c in "abcde"}
        for m in re.finditer(r'\(([a-eA-E])\)\s*(.+?)(?=\s*\([a-eA-E]\)|$)',
                              opts_text, re.DOTALL):
            letter = m.group(1).lower()
            label = m.group(2).strip().replace("\n", " ")
            options.append({"id": letter_map.get(letter, f"opt_{letter}"), "text": label})

        if options:
            return q_text, options

    # Pattern: True / Probable / False / Absurd  (OIR verbal reasoning)
    if re.search(r'\bProbable\b', text, re.IGNORECASE):
        clean = re.sub(r'\b(True|Probable|False|Absurd)\b', '', text,
                       flags=re.IGNORECASE).strip()
        return clean, [
            {"id": "opt_a", "text": "True"},
            {"id": "opt_b", "text": "Probable"},
            {"id": "opt_c", "text": "False"},
            {"id": "opt_d", "text": "Absurd"},
        ]

    return text, None


def run_questions_phase(set_num: int, pdf_path: Path, cfg: dict):
    print("=" * 60)
    print(f"📝  PHASE 2/3 — QUESTIONS  (Set {set_num})")
    print("=" * 60)

    image_questions = set(cfg["imageQuestions"])
    total_questions = cfg["totalQuestions"]
    page_start = cfg["pdfPageStart"] - 1   # 0-based
    page_end = cfg["pdfPageEnd"] - 1       # 0-based

    pdf_doc = fitz.open(str(pdf_path))

    print("\n🔍  Extracting question text…")
    question_texts = extract_question_texts(pdf_doc, page_start, page_end, total_questions)
    print(f"   Text found for {len(question_texts)}/{total_questions} questions")

    print("🔍  Searching for answers section…")
    answers = extract_answers(pdf_doc, page_start, page_end)
    print(f"   {len(answers)} answers extracted" if answers
          else "   ⚠️  Answers section not found — correctAnswerId will need manual review")

    pdf_doc.close()

    questions = []
    review_needed = []
    auto_filled = 0

    for qnum in range(1, total_questions + 1):
        q_id = f"oir_s{set_num:02d}_q{qnum:02d}"
        is_image = qnum in image_questions
        raw = question_texts.get(qnum, "")
        ans = answers.get(qnum, "")

        if is_image:
            # Spatial / figure question — text may be minimal or absent
            q_text = raw.strip() or "See figure above."
            # Spatial options are numbered 1–5 by default; user adjusts if needed
            options = [{"id": f"opt_{n}", "text": str(n)} for n in range(1, 6)]
            q_type = "SPATIAL_REASONING"
            # Answer digit → opt_N; letter → opt_N via position
            correct_id = _ans_to_option_id(ans, is_numeric_options=True) if ans else "opt_1"
            review_needed.append(qnum)

        else:
            # Verbal / text question
            q_text, options = _parse_options(raw)
            q_text = q_text.strip() or f"TODO: Enter question {qnum} text"

            if options is None:
                # No option markers detected — use True/Probable/False/Absurd default
                options = [
                    {"id": "opt_a", "text": "True"},
                    {"id": "opt_b", "text": "Probable"},
                    {"id": "opt_c", "text": "False"},
                    {"id": "opt_d", "text": "Absurd"},
                ]
                review_needed.append(qnum)

            q_type = "VERBAL_REASONING"
            correct_id = _ans_to_option_id(ans) if ans else "opt_a"
            if not ans:
                review_needed.append(qnum)
            else:
                auto_filled += 1

        questions.append({
            "id": q_id,
            "questionNumber": qnum,
            "type": q_type,
            "questionText": q_text,
            "options": options,
            "correctAnswerId": correct_id,
            "explanation": f"TODO: Add explanation for Q{qnum}",
            "difficulty": "MEDIUM",
            "tags": [],
            "questionImageUrl": None,
        })

    # Remove duplicates from review_needed
    review_needed = sorted(set(review_needed))

    draft = {
        "setNumber": set_num,
        "title": f"OIR Practice Set {set_num}",
        "version": "1.0",
        "totalQuestions": total_questions,
        "_draft": True,
        "_reviewNeeded": review_needed,
        "questions": questions,
    }

    out_path = SCRIPTS_DIR / f"oir-set-{set_num:03d}.json"
    out_path.write_text(json.dumps(draft, indent=2, ensure_ascii=False))

    preview = review_needed[:15]
    ellipsis = "…" if len(review_needed) > 15 else ""
    print(f"\n✅  Draft written → {out_path.name}")
    print(f"   {total_questions} questions  |  {auto_filled} answers auto-filled")
    print(f"   ⚠️  {len(review_needed)} question(s) need review: {preview}{ellipsis}")
    print()
    print("📋  Review checklist before uploading:")
    print("   1. Verify extracted question text (especially verbal questions)")
    print("   2. Check correctAnswerId for every question")
    print("   3. For image questions: confirm options count (1–5 or fewer)")
    print("   4. Update question types where needed:")
    print("      VERBAL_REASONING / SPATIAL_REASONING / SERIES_COMPLETION")
    print("      ANALOGY / ODD_ONE_OUT / CLASSIFICATION")
    print("   5. Replace all 'TODO: Add explanation…' entries")
    print("   6. Remove _draft and _reviewNeeded keys when satisfied")
    print()
    print(f"   Then: python3 scripts/process-oir-set.py --set {set_num} --phase upload")


# ── Phase 3: Upload ───────────────────────────────────────────────────────────

def run_upload_phase(set_num: int):
    print("=" * 60)
    print(f"☁️   PHASE 3/3 — UPLOAD  (Set {set_num})")
    print("=" * 60)

    set_json = SCRIPTS_DIR / f"oir-set-{set_num:03d}.json"
    if not set_json.exists():
        print(f"\n❌  Set JSON not found: {set_json}")
        print(f"   Run --phase questions first.")
        sys.exit(1)

    with open(set_json) as f:
        data = json.load(f)

    if data.get("_draft"):
        pending = data.get("_reviewNeeded", [])
        print(f"\n⚠️  Draft flag is set — {len(pending)} question(s) may still need review.")
        print("   Uploading anyway (you can re-upload after editing).")

    print()
    result = subprocess.run(
        ["node", str(SCRIPTS_DIR / "upload-oir-sets.js"), "--set", str(set_num)],
        cwd=str(PROJECT_ROOT),
    )
    if result.returncode != 0:
        print("\n❌  Upload failed.")
        sys.exit(1)

    print(f"\n✅  Upload phase complete — Set {set_num} is now in Firestore.")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Master pipeline for OIR set processing (Sets 2–20)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
examples:
  # Full pipeline (all three phases):
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf

  # Images only (crop + upload to Storage):
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase images

  # Extract questions only (generates draft JSON for review):
  python3 scripts/process-oir-set.py --set 2 --pdf /path/to/oir.pdf --phase questions

  # Upload only (after reviewing draft JSON):
  python3 scripts/process-oir-set.py --set 2 --phase upload
        """,
    )
    parser.add_argument("--set", required=True, type=int, metavar="N",
                        help="Set number (2–20)")
    parser.add_argument("--pdf", metavar="PATH",
                        help="Path to OIR PDF (required for images and questions phases)")
    parser.add_argument("--phase", choices=["images", "questions", "upload"],
                        help="Run only this phase (default: all three in sequence)")
    args = parser.parse_args()

    set_num = args.set
    cfg = load_config(set_num)

    phases = [args.phase] if args.phase else ["images", "questions", "upload"]

    # Validate PDF for phases that need it
    pdf_path = None
    needs_pdf = {"images", "questions"} & set(phases)
    if needs_pdf:
        if not args.pdf:
            print(f"❌  --pdf is required for phase(s): {', '.join(sorted(needs_pdf))}")
            sys.exit(1)
        pdf_path = Path(args.pdf)
        if not pdf_path.exists():
            print(f"❌  PDF not found: {pdf_path}")
            sys.exit(1)

    print(f"\n🚀  OIR Set {set_num} Pipeline — phases: {' → '.join(phases)}")
    print()

    for phase in phases:
        if phase == "images":
            run_images_phase(set_num, pdf_path, cfg)
        elif phase == "questions":
            run_questions_phase(set_num, pdf_path, cfg)
        elif phase == "upload":
            run_upload_phase(set_num)
        print()

    print("🎉  Pipeline complete for Set", set_num)


if __name__ == "__main__":
    main()
