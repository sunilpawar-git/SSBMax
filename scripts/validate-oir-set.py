#!/usr/bin/env python3
"""
validate-oir-set.py — Pre-upload safety checks for an OIR set.

Catches every class of bug encountered during Set 1 deployment before the
data reaches Firestore. Run this BEFORE every upload.

Usage:
  python3 scripts/validate-oir-set.py --set 2
  python3 scripts/validate-oir-set.py --set 2 --fix   # auto-fix where possible

Exit codes:
  0 — All checks passed
  1 — One or more checks failed (details printed above the summary)
"""

import argparse
import json
import re
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).parent
CONFIGS_DIR = SCRIPTS_DIR / "oir-set-configs"

# ── Colours ───────────────────────────────────────────────────────────────────

OK    = "✅"
WARN  = "⚠️ "
FAIL  = "❌"
INFO  = "ℹ️ "

errors   = []
warnings = []

def ok(msg):    print(f"  {OK}  {msg}")
def warn(msg):  warnings.append(msg); print(f"  {WARN} {msg}")
def fail(msg):  errors.append(msg);   print(f"  {FAIL} {msg}")
def info(msg):  print(f"  {INFO}  {msg}")
def section(title): print(f"\n{'─'*55}\n{title}\n{'─'*55}")


# ── Checks ────────────────────────────────────────────────────────────────────

def check_config(set_num: int) -> dict | None:
    section("1. Config file")
    p = CONFIGS_DIR / f"set_{set_num:03d}.json"
    if not p.exists():
        fail(f"Config not found: {p}")
        fail("Create it: cp scripts/oir-set-configs/set_001.json "
             f"scripts/oir-set-configs/set_{set_num:03d}.json")
        return None

    with open(p) as f:
        cfg = json.load(f)

    ok(f"Config found: {p.name}")

    required = ["setNumber", "pdfPageStart", "pdfPageEnd", "imageQuestions", "totalQuestions"]
    for key in required:
        if key not in cfg:
            fail(f"Missing field in config: '{key}'")
        else:
            ok(f"  {key}: {cfg[key]}")

    if cfg.get("setNumber") != set_num:
        fail(f"Config setNumber ({cfg.get('setNumber')}) != requested --set ({set_num})")

    return cfg


def check_url_map(set_num: int, cfg: dict) -> dict | None:
    section("2. Image URL map")
    p = SCRIPTS_DIR / f"image-url-map-set-{set_num:03d}.json"
    image_questions = cfg.get("imageQuestions", [])

    if not image_questions:
        ok("No imageQuestions in config — URL map not required.")
        return {}

    if not p.exists():
        fail(f"URL map not found: {p.name}")
        fail("Run: python3 scripts/process-oir-set.py "
             f"--set {set_num} --pdf <path> --phase images")
        return None

    with open(p) as f:
        url_map = json.load(f)

    ok(f"URL map found: {p.name} ({len(url_map)} entries)")

    # Check every imageQuestion has an entry
    set_pad2 = f"{set_num:02d}"
    missing = []
    for qnum in image_questions:
        expected_id = f"oir_s{set_pad2}_q{qnum:02d}"
        if expected_id not in url_map:
            missing.append(expected_id)

    if missing:
        fail(f"URL map missing entries for: {missing}")
        fail("Re-run the images phase to regenerate the URL map.")
    else:
        ok(f"All {len(image_questions)} imageQuestion IDs present in URL map")

    # Check URLs belong to this set (not a previous set's leftover).
    # Handles both plain slashes and URL-encoded %2F (Firebase Storage URLs use %2F).
    wrong_set = []
    for q_id, url in url_map.items():
        # WebP: "set_NN/"  or  SVG: "set_NNN/" (possibly URL-encoded as "set_NNN%2F")
        matches_set = any(needle in url for needle in [
            f"set_{set_pad2}/",
            f"set_{set_pad2}%2F",
            f"set_{set_num:03d}/",
            f"set_{set_num:03d}%2F",
        ])
        if not matches_set:
            wrong_set.append((q_id, url))

    if wrong_set:
        fail(f"URL map has {len(wrong_set)} URL(s) that don't match Set {set_num}:")
        for q_id, url in wrong_set[:5]:
            fail(f"  {q_id}: {url[:80]}…")
        fail("This was the Set 1 bug: old URL map had the wrong set's URLs.")
        fail("Fix: regenerate URL map with 'images' phase or manually update the file.")
    else:
        ok("All URLs in URL map reference the correct set storage path")

    # Warn about extra entries (questions in map but not in imageQuestions)
    extra = [k for k in url_map if k.split("_q")[1].lstrip("0") not in
             [str(q) for q in image_questions]]
    if extra:
        # Re-check using zero-padded comparison
        image_ids = {f"oir_s{set_pad2}_q{q:02d}" for q in image_questions}
        extra = [k for k in url_map if k not in image_ids]
        if extra:
            warn(f"URL map has extra entries not in imageQuestions: {extra}")
            warn("These will be silently ignored. Update imageQuestions if these "
                 "questions DO have images.")

    return url_map


def check_set_json(set_num: int, cfg: dict, url_map: dict) -> dict | None:
    section("3. Set JSON")
    p = SCRIPTS_DIR / f"oir-set-{set_num:03d}.json"

    if not p.exists():
        fail(f"Set JSON not found: {p.name}")
        fail("Run: python3 scripts/process-oir-set.py "
             f"--set {set_num} --pdf <path> --phase questions")
        return None

    with open(p) as f:
        data = json.load(f)

    ok(f"Set JSON found: {p.name}")

    # Draft flag
    if data.get("_draft"):
        pending = data.get("_reviewNeeded", [])
        fail(f"_draft flag is set — {len(pending)} question(s) flagged for review: "
             f"{pending[:10]}{'…' if len(pending) > 10 else ''}")
        fail("Complete the review, then remove the '_draft' and '_reviewNeeded' keys.")
    else:
        ok("No _draft flag")

    # Question count
    total = cfg.get("totalQuestions", 50)
    q_count = len(data.get("questions", []))
    if q_count != total:
        fail(f"Question count mismatch: JSON has {q_count}, config expects {total}")
    else:
        ok(f"Question count: {q_count}")

    # TODO explanations
    todos = [q["questionNumber"] for q in data.get("questions", [])
             if "TODO" in q.get("explanation", "")]
    if todos:
        fail(f"{len(todos)} question(s) still have TODO explanations: {todos[:15]}"
             f"{'…' if len(todos) > 15 else ''}")
    else:
        ok("All explanations filled in (no TODOs)")

    # correctAnswerId exists in options
    bad_answers = []
    for q in data.get("questions", []):
        option_ids = {o["id"] for o in q.get("options", [])}
        correct = q.get("correctAnswerId", "")
        if correct not in option_ids:
            bad_answers.append((q["questionNumber"], correct, sorted(option_ids)))

    if bad_answers:
        fail(f"{len(bad_answers)} question(s) have correctAnswerId not in their options:")
        for qnum, cid, opts in bad_answers[:10]:
            fail(f"  Q{qnum}: correctAnswerId='{cid}' but options are {opts}")
    else:
        ok("All correctAnswerIds are valid option IDs")

    # Question IDs follow expected pattern
    set_pad2 = f"{set_num:02d}"
    bad_ids = []
    for q in data.get("questions", []):
        expected_id = f"oir_s{set_pad2}_q{q['questionNumber']:02d}"
        if q.get("id") != expected_id:
            bad_ids.append((q.get("id"), expected_id))
    if bad_ids:
        fail(f"{len(bad_ids)} question(s) have unexpected IDs:")
        for actual, expected in bad_ids[:5]:
            fail(f"  Found '{actual}', expected '{expected}'")
    else:
        ok(f"All question IDs follow oir_s{set_pad2}_qNN pattern")

    return data


def check_image_questions(set_num: int, cfg: dict, data: dict, url_map: dict):
    section("4. Image question coverage")
    if not data or not url_map:
        info("Skipping (JSON or URL map not available)")
        return

    image_questions = set(cfg.get("imageQuestions", []))
    set_pad2 = f"{set_num:02d}"

    # Questions that should have images but will get null (not in url_map)
    missing_images = []
    for q in data.get("questions", []):
        qnum = q["questionNumber"]
        if qnum in image_questions:
            q_id = f"oir_s{set_pad2}_q{qnum:02d}"
            if q_id not in url_map:
                missing_images.append(qnum)

    if missing_images:
        fail(f"Q{missing_images} are in imageQuestions but have no URL in the map "
             "— they will render without a figure in the app")
    else:
        count = len(image_questions)
        ok(f"All {count} image questions have URLs ready for upload")


def check_svg_files(set_num: int, cfg: dict):
    """
    Set 1 only: verify hand-crafted SVGs in scripts/oir-images/set_001/.

    Checks:
    1. All imageQuestions have a corresponding SVG file.
    2. No SVG contains embedded caption/answer text (Bug 2 + Bug 6 prevention).
       Captions are <text> elements with >15 chars of natural language content.
    """
    if set_num != 1:
        return  # SVG check only applies to Set 1; Sets 2+ use WebP auto-crops

    section("5. Set 1 SVG file check")

    svg_dir = SCRIPTS_DIR / "oir-images" / "set_001"
    if not svg_dir.exists():
        warn(f"SVG directory not found: {svg_dir}")
        warn("Expected hand-crafted SVGs for Set 1 image questions.")
        return

    image_questions = cfg.get("imageQuestions", [])
    set_pad2 = f"{set_num:02d}"
    missing_svgs = []
    svgs_with_captions = []

    for qnum in image_questions:
        # SVG filenames follow pattern: oir_s01_qNN_*.svg
        pattern = f"oir_s{set_pad2}_q{qnum:02d}_*.svg"
        matches = list(svg_dir.glob(pattern))

        if not matches:
            missing_svgs.append(qnum)
            continue

        # Check each matched SVG for embedded caption text
        for svg_path in matches:
            content = svg_path.read_text()
            # Find all <text ...>CONTENT</text> elements
            text_contents = re.findall(r'<text[^>]*>([^<]+)</text>', content)
            captions = [t.strip() for t in text_contents
                        if len(t.strip()) > 15 and not t.strip().isdigit()
                        and not re.match(r'^[1-5A-E]$', t.strip())]
            if captions:
                svgs_with_captions.append((qnum, svg_path.name, captions[:2]))

    if missing_svgs:
        fail(f"Missing SVG files for Q{missing_svgs}")
        fail("Expected files matching: oir_s01_qNN_*.svg in scripts/oir-images/set_001/")
    else:
        ok(f"All {len(image_questions)} image questions have SVG files")

    if svgs_with_captions:
        fail(f"{len(svgs_with_captions)} SVG(s) contain embedded caption/answer text:")
        for qnum, fname, captions in svgs_with_captions:
            fail(f"  Q{qnum} ({fname}): {captions}")
        fail("This spoils the answer before the user attempts the question (Bug 2).")
        fail("Remove <text> elements with explanatory content from the SVG files.")
    else:
        ok("No embedded captions found in SVG files")

    # Remind about medium-risk SVGs that need manual PDF comparison
    medium_risk = [q for q in [17, 21, 36, 43] if q in image_questions]
    if medium_risk:
        warn(f"Q{medium_risk} are medium-risk SVGs (fill patterns / complex shapes).")
        warn("Verify these in the app against the PDF before considering Set 1 complete.")


def check_verbal_answers(set_num: int, data: dict):
    section("5. Verbal answer spot-check")
    if not data:
        info("Skipping (JSON not available)")
        return

    # Known Set 1 PDF errors — warn if we see the same wrong answer IDs
    known_errors = {
        1: {26: ("opt_d", "opt_a", "PDF says Newspaper but People is correct"),
            27: ("opt_d", "opt_b", "PDF says Umbrella (opt_d) but Utmost (opt_b) is correct")},
    }

    set_errors = known_errors.get(set_num, {})
    for q in data.get("questions", []):
        qnum = q["questionNumber"]
        if qnum in set_errors:
            wrong_id, correct_id, note = set_errors[qnum]
            if q.get("correctAnswerId") == wrong_id:
                fail(f"Q{qnum}: correctAnswerId='{wrong_id}' matches known PDF error. "
                     f"Should be '{correct_id}'. Note: {note}")
            else:
                ok(f"Q{qnum}: answer looks correct ('{q.get('correctAnswerId')}')")

    if not set_errors:
        ok(f"No known PDF answer errors for Set {set_num} (verify new sets manually)")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Pre-upload validation for an OIR set",
        epilog="Run this before every 'python3 scripts/process-oir-set.py --phase upload'"
    )
    parser.add_argument("--set", required=True, type=int, metavar="N",
                        help="Set number to validate (1–50)")
    args = parser.parse_args()

    set_num = args.set
    print(f"\n{'='*55}")
    print(f"  OIR Set {set_num} — Pre-upload Validation")
    print(f"{'='*55}")

    cfg      = check_config(set_num)
    url_map  = check_url_map(set_num, cfg) if cfg else None
    data     = check_set_json(set_num, cfg, url_map) if cfg else None

    if cfg and data and url_map is not None:
        check_image_questions(set_num, cfg, data, url_map)

    if cfg and data:
        check_verbal_answers(set_num, data)

    # Summary
    print(f"\n{'='*55}")
    if not errors and not warnings:
        print(f"  {OK}  ALL CHECKS PASSED — safe to upload Set {set_num}")
        print(f"\n  Next:")
        print(f"    node scripts/upload-oir-sets.js --set {set_num} --dry-run")
        print(f"    python3 scripts/process-oir-set.py --set {set_num} --phase upload")
        print(f"{'='*55}\n")
        sys.exit(0)
    else:
        if errors:
            print(f"  {FAIL}  {len(errors)} ERROR(S) — fix before uploading:")
            for e in errors:
                print(f"       • {e}")
        if warnings:
            print(f"  {WARN}  {len(warnings)} WARNING(S):")
            for w in warnings:
                print(f"       • {w}")
        print(f"{'='*55}\n")
        sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
