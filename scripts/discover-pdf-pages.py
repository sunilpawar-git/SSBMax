#!/usr/bin/env python3
"""
discover-pdf-pages.py — Find the PDF page range for every OIR practice set.

Scans the SSBCrack OIR PDF and outputs a table of:
  Set N → pdfPageStart, pdfPageEnd

Then generates stub config files for any sets that don't have one yet.

Usage:
  python3 scripts/discover-pdf-pages.py \\
    --pdf '/Users/sunil/Desktop/OIR Tests/20-OIR-Test-Practice-Sets-SSBCrack (2).pdf'

  # Also write config stubs for missing sets:
  python3 scripts/discover-pdf-pages.py --pdf '...' --write-configs

Dependencies:
  pip install pymupdf
"""

import argparse
import json
import re
import sys
from pathlib import Path

try:
    import fitz
except ImportError:
    print("❌ pymupdf not installed. Run: pip install pymupdf")
    sys.exit(1)

SCRIPTS_DIR = Path(__file__).parent
CONFIGS_DIR = SCRIPTS_DIR / "oir-set-configs"

# Patterns that appear as the title line at the start of each set in the PDF.
# The SSBCrack PDF uses variations of these headings.
SET_HEADING_PATTERNS = [
    # "OIR PRACTICE SET - 1", "OIR PRACTICE SET-1", "SET - 1", "SET-1"
    re.compile(r'\bOIR\s+(?:PRACTICE\s+)?SET\s*[-–—]\s*(\d{1,2})\b', re.IGNORECASE),
    re.compile(r'\bSET\s*[-–—]\s*(\d{1,2})\b', re.IGNORECASE),
    # "OIR SET 1", "PRACTICE SET 1"
    re.compile(r'\b(?:OIR\s+)?(?:PRACTICE\s+)?SET\s+(\d{1,2})\b', re.IGNORECASE),
    # Fallback: "1." at position 1 on a section-title-looking line
    # (handled by question-1 detection below)
]

# Typical imageQuestions lists observed in the SSBCrack OIR PDF.
# Sets tend to repeat the same image question positions.
# This is a reasonable default — always verify after running the questions phase.
DEFAULT_IMAGE_QUESTIONS = [1, 3, 4, 5, 6, 7, 8, 12, 14, 17, 21, 24, 25,
                           34, 35, 36, 38, 39, 43, 44, 47, 48]


def find_set_start_pages(pdf_doc: fitz.Document) -> dict[int, int]:
    """
    Scan every page and return {set_number: 1-based page number} for each set found.
    """
    found: dict[int, int] = {}

    for page_idx in range(pdf_doc.page_count):
        text = pdf_doc[page_idx].get_text("text")

        for pattern in SET_HEADING_PATTERNS:
            for m in pattern.finditer(text):
                snum = int(m.group(1))
                if 1 <= snum <= 50 and snum not in found:
                    found[snum] = page_idx + 1  # convert to 1-based
                    break

    return found


def find_first_question_pages(pdf_doc: fitz.Document) -> dict[int, int]:
    """
    Fallback: find the page where "1." first appears at the left margin
    (x0 < 80pt) — this marks the start of questions for each set.

    Returns {page_1based: True} — used to disambiguate set boundaries when
    the heading pattern doesn't match.
    """
    question_one_pages = {}
    for page_idx in range(pdf_doc.page_count):
        page = pdf_doc[page_idx]
        for word in page.get_text("words"):
            x0, text = word[0], word[4].strip()
            if text == "1." and x0 < 80:
                question_one_pages[page_idx + 1] = True
                break
    return question_one_pages


def build_page_ranges(start_pages: dict[int, int],
                      total_pages: int) -> list[tuple[int, int, int]]:
    """
    Given {set_number: start_page}, compute (set_number, start, end) for each set.
    end = next set's start - 1 (or total_pages for the last set).
    """
    sorted_sets = sorted(start_pages.items(), key=lambda x: x[1])
    ranges = []

    for i, (snum, start) in enumerate(sorted_sets):
        if i + 1 < len(sorted_sets):
            end = sorted_sets[i + 1][1] - 1
        else:
            end = total_pages
        ranges.append((snum, start, end))

    return ranges


def write_config(snum: int, start: int, end: int, existing_configs: set):
    """Write a config stub for a set that doesn't have one yet."""
    config_path = CONFIGS_DIR / f"set_{snum:03d}.json"

    if config_path.exists():
        return False  # already exists

    cfg = {
        "setNumber": snum,
        "pdfPageStart": start,
        "pdfPageEnd": end,
        "imageQuestions": DEFAULT_IMAGE_QUESTIONS,
        "totalQuestions": 50,
        "_note": "imageQuestions is a default estimate — verify after running --phase questions"
    }

    CONFIGS_DIR.mkdir(parents=True, exist_ok=True)
    with open(config_path, "w") as f:
        json.dump(cfg, f, indent=2)

    return True


def main():
    parser = argparse.ArgumentParser(
        description="Find PDF page ranges for all OIR practice sets",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Print page range table:
  python3 scripts/discover-pdf-pages.py --pdf '/path/to/oir.pdf'

  # Also write config stubs for sets that don't have a config yet:
  python3 scripts/discover-pdf-pages.py --pdf '/path/to/oir.pdf' --write-configs
        """
    )
    parser.add_argument("--pdf", required=True, metavar="PATH",
                        help="Path to the OIR PDF file")
    parser.add_argument("--write-configs", action="store_true",
                        help="Write oir-set-configs/set_NNN.json stubs for new sets")
    args = parser.parse_args()

    pdf_path = Path(args.pdf)
    if not pdf_path.exists():
        print(f"❌ PDF not found: {pdf_path}")
        sys.exit(1)

    print(f"\n📄 Scanning PDF: {pdf_path.name}")
    pdf_doc = fitz.open(str(pdf_path))
    total_pages = pdf_doc.page_count
    print(f"   Total pages: {total_pages}")
    print()

    print("🔍 Detecting set boundaries…")
    start_pages = find_set_start_pages(pdf_doc)

    # Merge with question-1 detection if heading patterns missed some sets
    q1_pages = find_first_question_pages(pdf_doc)
    heading_pages = set(start_pages.values())

    # Any "1." start page that isn't already claimed by a heading match might
    # indicate a set not caught by the regex — warn but don't auto-assign
    unmatched_q1 = [p for p in q1_pages if p not in heading_pages]
    if unmatched_q1:
        print(f"   ⚠️  Found Q1 markers at pages {unmatched_q1} not matched by "
              "heading patterns. These may be additional sets — check manually.")

    pdf_doc.close()

    if not start_pages:
        print("❌ No set headings detected in the PDF.")
        print("   The regex patterns may not match this PDF's headings.")
        print("   Check a few pages manually and update SET_HEADING_PATTERNS in this script.")
        sys.exit(1)

    ranges = build_page_ranges(start_pages, total_pages)

    # Print table
    print()
    print(f"{'Set':>4}  {'pdfPageStart':>13}  {'pdfPageEnd':>10}  {'Pages':>6}  Config")
    print(f"{'─'*4}  {'─'*13}  {'─'*10}  {'─'*6}  {'─'*20}")

    existing_configs = {p.name for p in CONFIGS_DIR.glob("set_*.json")} if CONFIGS_DIR.exists() else set()

    written = 0
    for snum, start, end in ranges:
        config_name = f"set_{snum:03d}.json"
        has_config = config_name in existing_configs
        config_status = "✅ exists" if has_config else "⚠️  missing"
        print(f"{snum:>4}  {start:>13}  {end:>10}  {end-start+1:>6}  {config_status}")

        if args.write_configs and not has_config:
            if write_config(snum, start, end, existing_configs):
                written += 1

    print()

    if args.write_configs:
        if written:
            print(f"✅ Wrote {written} new config stub(s) to scripts/oir-set-configs/")
            print("   Review imageQuestions in each stub before running the pipeline.")
        else:
            print("ℹ️  All sets already have config files — nothing written.")
    else:
        missing_configs = [(snum, start, end) for snum, start, end in ranges
                           if f"set_{snum:03d}.json" not in existing_configs]
        if missing_configs:
            print(f"ℹ️  {len(missing_configs)} set(s) need config files.")
            print("   Re-run with --write-configs to create stubs automatically.")

    # Print ready-to-paste JSON snippet for easy copy
    print()
    print("📋 Page ranges for manual config creation:")
    print()
    for snum, start, end in ranges:
        print(f'  Set {snum:2d}: "pdfPageStart": {start:3d}, "pdfPageEnd": {end:3d}')
    print()


if __name__ == "__main__":
    main()
