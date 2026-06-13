#!/usr/bin/env python3
"""
Step 1 — Extract individual images from 2×2 grid cards.

Input:  16 PNG cards at SOURCE_DIR (each 2816×1536 px, four 1408×768 quadrants)
Output: 64 JPEG files in output/images/ppdt_image_NNN.jpg

Caption bar height varies per image (16-24%); it is detected automatically per quadrant
by scanning from the bottom up in 20-row windows until photo content is reached.
Cards 89k6vu and k1dy2i are duplicates of 2cn4kv and are skipped.

Usage:
  python step1_extract_cards.py                    # process all 16 cards
  python step1_extract_cards.py --cards aoko0a     # process one card (partial name match)
  python step1_extract_cards.py --dry-run          # validate paths without writing files
  python step1_extract_cards.py --self-test        # process first card only, verify 4 outputs
"""

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image

SOURCE_DIR = Path("/Users/sunil/Downloads/PPDT Images by Gemini")
OUTPUT_DIR = Path(__file__).parent / "output" / "images"

JPEG_QUALITY = 85

# Caption bar detection constants.
# Caption bar is mostly bright (white background with dark text). Photo content is noisy
# and averages ~100-130. When a 20-row window from the bottom averages < 140, we are
# in photo territory — the caption bar ends there.
_DETECT_WINDOW = 20
_DETECT_THRESHOLD = 140
_MIN_CONTENT_RATIO = 0.50   # Fail-safe: keep at least 50% of quadrant height as photo

# Mapping from PNG filename to the 4 image numbers it contains (TL, TR, BL, BR order).
# 89k6vu and k1dy2i are confirmed duplicates of 2cn4kv — omitted from this map.
CARD_IMAGE_MAP: dict[str, list[int]] = {
    "Gemini_Generated_Image_aoko0aoko0aoko0a.png": [1,  2,  3,  4],
    "Gemini_Generated_Image_pwa1g4pwa1g4pwa1.png": [5,  6,  7,  8],
    "Gemini_Generated_Image_8n08kv8n08kv8n08.png": [9,  10, 11, 12],
    "Gemini_Generated_Image_2bfz492bfz492bfz.png": [13, 14, 15, 16],
    "Gemini_Generated_Image_2cn4kv2cn4kv2cn4.png": [17, 18, 19, 20],
    "Gemini_Generated_Image_cjewt5cjewt5cjew.png": [25, 26, 27, 28],
    "Gemini_Generated_Image_7io1tb7io1tb7io1.png": [29, 30, 31, 32],
    "Gemini_Generated_Image_l9ltw6l9ltw6l9lt.png": [33, 34, 35, 36],
    "Gemini_Generated_Image_nrjvqbnrjvqbnrjv.png": [37, 38, 39, 40],
    "Gemini_Generated_Image_7lwmac7lwmac7lwm.png": [41, 42, 43, 44],
    "Gemini_Generated_Image_uef8pruef8pruef8.png": [45, 46, 47, 48],
    "Gemini_Generated_Image_jmua0ljmua0ljmua.png": [49, 50, 51, 52],
    "Gemini_Generated_Image_ua188iua188iua18.png": [53, 54, 55, 56],
    "Gemini_Generated_Image_b0yk2cb0yk2cb0yk.png": [57, 58, 59, 60],
    "Gemini_Generated_Image_hhq39hhhq39hhhq3.png": [61, 62, 63, 64],
    "Gemini_Generated_Image_4utddw4utddw4utd.png": [65, 66, 67, 68],
}

_QUADRANT_LABELS = ["TL", "TR", "BL", "BR"]


def _detect_content_height(quadrant_arr: np.ndarray) -> int:
    """Return the row index where photo content ends (caption bar starts).

    Scans from the bottom of the quadrant upward in sliding windows. When the
    window mean drops below _DETECT_THRESHOLD the window is in photo territory —
    the caption bar top is just below that transition.
    """
    qh = quadrant_arr.shape[0]
    min_content = int(qh * _MIN_CONTENT_RATIO)

    for r in range(qh - _DETECT_WINDOW, min_content, -1):
        window_mean = quadrant_arr[r : r + _DETECT_WINDOW].mean()
        if window_mean < _DETECT_THRESHOLD:
            return r + _DETECT_WINDOW  # caption starts at r+WINDOW

    # Fallback: strip 25% from bottom
    return int(qh * 0.75)


def extract_card(
    card_path: Path,
    image_ids: list[int],
    output_dir: Path,
    dry_run: bool = False,
) -> list[Path]:
    """Crop one card into 4 captionless JPEGs. Returns output paths."""
    img = Image.open(card_path).convert("RGB")
    arr = np.array(img)
    W, H = img.size
    qw, qh = W // 2, H // 2

    quadrant_defs = [
        ("TL", (0, 0, qh, qw)),
        ("TR", (0, qw, qh, W)),
        ("BL", (qh, 0, H, qw)),
        ("BR", (qh, qw, H, W)),
    ]

    saved: list[Path] = []
    for (label, (r0, c0, r1, c1)), image_id in zip(quadrant_defs, image_ids):
        q_arr = arr[r0:r1, c0:c1]
        content_h = _detect_content_height(q_arr)
        cropped = Image.fromarray(q_arr[:content_h])

        out_path = output_dir / f"ppdt_image_{image_id:03d}.jpg"
        if not dry_run:
            cropped.save(out_path, "JPEG", quality=JPEG_QUALITY, optimize=True)
        saved.append(out_path)
        caption_px = qh - content_h
        print(
            f"  {label} (id={image_id:03d}) → {out_path.name}  "
            f"[{qw}×{content_h}px, stripped {caption_px}px caption]"
            f"{'  [DRY RUN]' if dry_run else ''}"
        )

    return saved


def _filter_map(partial_names: list[str] | None) -> dict[str, list[int]]:
    if not partial_names:
        return CARD_IMAGE_MAP
    return {k: v for k, v in CARD_IMAGE_MAP.items() if any(p in k for p in partial_names)}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Extract PPDT images from 2×2 grid cards")
    parser.add_argument("--cards", nargs="*", metavar="NAME",
                        help="Partial card filenames to process (default: all 16)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Validate paths and card map without writing files")
    parser.add_argument("--self-test", action="store_true",
                        help="Process only the first card and verify 4 outputs exist")
    args = parser.parse_args(argv)

    if args.self_test:
        first_key = next(iter(CARD_IMAGE_MAP))
        target_map = {first_key: CARD_IMAGE_MAP[first_key]}
        print(f"[self-test] Processing first card only: {first_key}")
    else:
        target_map = _filter_map(args.cards)

    if not target_map:
        print("ERROR: No matching cards found.", file=sys.stderr)
        return 1

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    total = 0
    missing = 0

    for filename, image_ids in target_map.items():
        card_path = SOURCE_DIR / filename
        if not card_path.exists():
            print(f"[SKIP] Not found: {card_path}", file=sys.stderr)
            missing += 1
            continue
        print(f"\nCard: {filename}  →  images {image_ids}")
        extract_card(card_path, image_ids, OUTPUT_DIR, dry_run=args.dry_run)
        total += len(image_ids)

    print(f"\n{'[DRY RUN] ' if args.dry_run else ''}Extracted {total} images → {OUTPUT_DIR}")

    if missing:
        print(f"WARNING: {missing} card(s) not found in {SOURCE_DIR}", file=sys.stderr)

    if args.self_test and not args.dry_run:
        first_ids = next(iter(target_map.values()))
        for img_id in first_ids:
            p = OUTPUT_DIR / f"ppdt_image_{img_id:03d}.jpg"
            if not p.exists():
                print(f"[self-test FAIL] Missing expected output: {p}", file=sys.stderr)
                return 1
        print(f"[self-test PASS] {len(first_ids)} images written and verified.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
