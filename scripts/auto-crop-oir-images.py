#!/usr/bin/env python3
"""
auto-crop-oir-images.py

Automatically crops OIR question figure images from a PDF using the
embedded text layer to find exact question boundaries — no manual pixel
coordinates needed.

Usage:
  python3 scripts/auto-crop-oir-images.py --pdf /path/to/oir.pdf --set 2

Config file:  scripts/oir-set-configs/set_002.json
Output dir:   scripts/oir_images/set_02/   (q*.webp)

Config JSON format:
{
  "setNumber": 2,
  "pdfPageStart": 21,          # 1-based page number in the PDF
  "pdfPageEnd": 34,
  "imageQuestions": [1, 3, 5, ...],  # question numbers that have figures
  "totalQuestions": 50
}

Dependencies:
  pip install pymupdf pillow
"""

import argparse
import json
import re
import sys
from pathlib import Path

import fitz  # pymupdf
from PIL import Image

ZOOM = 2.0  # render scale: PDF points × 2 → pixels in exported image
MARGIN_TOP = 8    # pixels to keep above the question number text
MARGIN_BOTTOM = 18  # pixels to strip from the bottom (before next question's text)
WEBP_QUALITY = 85

# Each SSBCrack OIR page has a repeating branded header and footer.
# Strip these when stitching across pages or when a crop starts at page top.
PAGE_HEADER_PX = 40  # "OIR TEST - PRACTICE QUESTIONS WITH ANSWER & EXPLANATIONS"
PAGE_FOOTER_PX = 52  # "N | P a g e   shop.ssbcrack.com"

SCRIPTS_DIR = Path(__file__).parent
CONFIGS_DIR = SCRIPTS_DIR / "oir-set-configs"
IMAGES_ROOT = SCRIPTS_DIR / "oir_images"


# ── helpers ──────────────────────────────────────────────────────────────────

def render_page(pdf_doc: fitz.Document, page_index: int) -> Image.Image:
    """Render a PDF page at ZOOM scale and return as a PIL Image."""
    page = pdf_doc[page_index]
    mat = fitz.Matrix(ZOOM, ZOOM)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    # Convert fitz pixmap → PIL Image without writing a temp file
    return Image.frombytes("RGB", [pix.width, pix.height], pix.samples)


def find_question_positions(pdf_doc: fitz.Document, page_index: int) -> dict[int, float]:
    """
    Return {question_number: y_pixel_position} for all question-number
    markers found on the given page.

    Question number markers are standalone text blocks matching '\\d{1,2}.'
    at the left margin of the page (x0 < 80 PDF points).
    """
    page = pdf_doc[page_index]
    positions: dict[int, float] = {}

    # "blocks" returns: (x0, y0, x1, y1, text, block_no, block_type)
    for block in page.get_text("blocks"):
        x0, y0, x1, y1, text = block[0], block[1], block[2], block[3], block[4]
        text_stripped = text.strip()

        # Match standalone "N." or "NN." where N is 1–50, at the left margin
        if re.match(r'^\d{1,2}\.$', text_stripped) and x0 < 80:
            qnum = int(text_stripped.rstrip('.'))
            if 1 <= qnum <= 50:
                # Convert PDF-space Y (points) to pixel Y at the render zoom
                y_px = y0 * ZOOM
                # Keep only the first occurrence per question number on this page
                if qnum not in positions:
                    positions[qnum] = y_px

    return positions


def crop_pil(img: Image.Image, top: int, bottom: int) -> Image.Image:
    w, h = img.size
    t = min(max(top, 0), h)
    b = min(max(bottom, 0), h)
    return img.crop((0, t, w, b))


def stitch(img_a: Image.Image, img_b: Image.Image) -> Image.Image:
    w = max(img_a.width, img_b.width)
    combined = Image.new("RGB", (w, img_a.height + img_b.height), "white")
    combined.paste(img_a, (0, 0))
    combined.paste(img_b, (0, img_a.height))
    return combined


def save_webp(img: Image.Image, out_path: Path):
    img.save(out_path, format='WEBP', quality=WEBP_QUALITY, method=6)
    kb = out_path.stat().st_size // 1024
    print(f"   ✅ {out_path.name}  ({kb} KB)")


# ── main logic ────────────────────────────────────────────────────────────────

def build_global_position_map(
    pdf_doc: fitz.Document,
    page_start: int,  # 0-based
    page_end: int     # 0-based inclusive
) -> dict[int, tuple[int, float]]:
    """
    Returns {question_number: (page_index_0based, y_pixel)} for all
    question markers found across the set's pages.
    """
    global_map: dict[int, tuple[int, float]] = {}
    for page_idx in range(page_start, page_end + 1):
        positions = find_question_positions(pdf_doc, page_idx)
        for qnum, y_px in positions.items():
            if qnum not in global_map:  # first occurrence wins
                global_map[qnum] = (page_idx, y_px)
    return global_map


def crop_question(
    pdf_doc: fitz.Document,
    q: int,
    global_map: dict[int, tuple[int, float]],
    rendered_pages: dict[int, Image.Image],
    set_page_end: int  # 0-based last page of the set
) -> Image.Image:
    """
    Crop the figure image for question q using auto-detected boundaries.
    Handles both single-page and two-page (stitched) questions.
    """
    if q not in global_map:
        raise ValueError(f"Question {q} not found in PDF text layer")

    q_page, q_y = global_map[q]
    # Clamp top so we never include the branded page header
    top_px = max(int(q_y) - MARGIN_TOP, PAGE_HEADER_PX)

    # Find the next question's position to determine the bottom boundary
    next_q = q + 1
    while next_q <= 50 and next_q not in global_map:
        next_q += 1

    img_q = rendered_pages[q_page]

    if next_q > 50 or next_q not in global_map:
        # Last image question on the last page — crop to near bottom of page
        bottom_px = img_q.height - MARGIN_BOTTOM
        return crop_pil(img_q, top_px, bottom_px)

    next_page, next_y = global_map[next_q]
    bottom_px = max(0, int(next_y) - MARGIN_BOTTOM)

    if next_page == q_page:
        # Same page: simple crop
        return crop_pil(img_q, top_px, bottom_px)
    else:
        # Spans two pages: stitch bottom of current page + top of next page.
        # Strip the page footer from piece A and the page header from piece B
        # so the SSBCrack branding doesn't appear in the middle of the image.
        piece_a = crop_pil(img_q, top_px, img_q.height - PAGE_FOOTER_PX)
        img_next = rendered_pages[next_page]
        piece_b = crop_pil(img_next, PAGE_HEADER_PX, bottom_px)
        return stitch(piece_a, piece_b)


def main():
    parser = argparse.ArgumentParser(description="Auto-crop OIR question images from PDF")
    parser.add_argument("--pdf", required=True, help="Path to the OIR PDF file")
    parser.add_argument("--set", required=True, type=int, help="Set number (1–20)")
    args = parser.parse_args()

    pdf_path = Path(args.pdf)
    if not pdf_path.exists():
        print(f"❌  PDF not found: {pdf_path}")
        sys.exit(1)

    set_num = args.set
    config_path = CONFIGS_DIR / f"set_{set_num:03d}.json"
    if not config_path.exists():
        print(f"❌  Config not found: {config_path}")
        print(f"   Create it from the template in scripts/oir-set-configs/set_001.json")
        sys.exit(1)

    with open(config_path) as f:
        cfg = json.load(f)

    image_questions: list[int] = cfg["imageQuestions"]
    page_start_1based: int = cfg["pdfPageStart"]
    page_end_1based: int = cfg["pdfPageEnd"]

    # Convert to 0-based page indices
    page_start = page_start_1based - 1
    page_end = page_end_1based - 1

    out_dir = IMAGES_ROOT / f"set_{set_num:02d}"
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"✂️   Auto-cropping OIR Set {set_num} question images…")
    print(f"     PDF pages {page_start_1based}–{page_end_1based}  |  {len(image_questions)} image questions")
    print()

    pdf_doc = fitz.open(str(pdf_path))

    # Render all pages upfront (cache them)
    print("📄  Rendering pages…")
    rendered_pages: dict[int, Image.Image] = {}
    for page_idx in range(page_start, page_end + 1):
        rendered_pages[page_idx] = render_page(pdf_doc, page_idx)
        print(f"     page {page_idx + 1}  {rendered_pages[page_idx].size[0]}×{rendered_pages[page_idx].size[1]}px")
    print()

    # Build the question-position map from the text layer
    print("🔍  Detecting question boundaries from text layer…")
    global_map = build_global_position_map(pdf_doc, page_start, page_end)
    detected = sorted(global_map.keys())
    print(f"     Detected question markers: {detected}")
    print()

    # Warn if any image question's marker was not found
    missing = [q for q in image_questions if q not in global_map]
    if missing:
        print(f"⚠️   Markers NOT found for questions: {missing}")
        print("     These questions will be skipped.")
        print("     Check the PDF text layer or adjust the config.")
        print()

    # Crop and save each image question
    errors = 0
    for q in image_questions:
        if q not in global_map:
            print(f"   ⚠️  q{q:02d}  skipped (marker not found)")
            errors += 1
            continue
        try:
            img = crop_question(pdf_doc, q, global_map, rendered_pages, page_end)
            save_webp(img, out_dir / f"q{q:02d}.webp")
        except Exception as e:
            print(f"   ❌  q{q:02d}  error: {e}")
            errors += 1

    pdf_doc.close()

    print()
    if errors == 0:
        print(f"🎉  Done — {len(image_questions)} WebP images saved to: {out_dir}")
    else:
        print(f"⚠️   Done with {errors} error(s) — check output above")
    print(f"     Next: node scripts/upload-oir-set-images.js")


if __name__ == "__main__":
    main()
