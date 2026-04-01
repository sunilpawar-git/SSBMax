#!/usr/bin/env python3
"""
Crop individual question figures from the extracted Set 1 page PNGs.

Pages are 1190 x 1684px (A4 at 144 dpi / 2× zoom).
For questions whose figures span two consecutive pages, sections are stitched.

Output: scripts/oir_images/set_01/q*.webp
Usage:  python scripts/crop-oir-set1-images.py
"""

from pathlib import Path
from PIL import Image

PAGES_DIR = Path(__file__).parent / "oir_images" / "set_01"
OUT_DIR = PAGES_DIR  # write q*.webp alongside the page_*.png files

# Each page has a repeating header ("OIR TEST - PRACTICE QUESTIONS…") and
# footer ("N | P a g e   shop.ssbcrack.com").  Strip these when stitching
# across pages or when a question crop starts/ends at a page boundary.
HEADER_PX = 40   # pixels to skip at the top of a page (header line)
FOOTER_PX = 52   # pixels to strip from the bottom of a page (footer line)


def page(n: int) -> Image.Image:
    return Image.open(PAGES_DIR / f"page_{n:02d}.png")


def crop(img: Image.Image, top: int, bottom: int) -> Image.Image:
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


def save(img: Image.Image, q: int):
    out = OUT_DIR / f"q{q:02d}.webp"
    img.save(out, format='WEBP', quality=85, method=6)
    kb = out.stat().st_size // 1024
    print(f"   ✅ q{q:02d}.webp  ({kb} KB)")


# ── page references (loaded lazily via dict to avoid loading all at once) ──
_cache: dict[int, Image.Image] = {}


def p(n: int) -> Image.Image:
    if n not in _cache:
        _cache[n] = page(n)
    return _cache[n]


print("✂️  Cropping OIR Set 1 question images…")
print()

# ── Q01: Group A/B cube sets + answer cubes 1,2,3  (page 9, top, skip title) ──
save(crop(p(9), 170, 620), 1)

# ── Q03: Two pentagon/heart shapes – "are they the same?"  (page 9) ──
save(crop(p(9), 845, 1055), 3)

# ── Q04: Two dice/cube shapes  (page 9) ──
save(crop(p(9), 1055, 1265), 4)

# ── Q05: Arrow series + answer options  (bottom of page 9 + top of page 10) ──
save(stitch(crop(p(9), 1265, p(9).height - FOOTER_PX), crop(p(10), HEADER_PX, 545)), 5)

# ── Q06: Class A / Class B shapes + options 1–4  (page 10) ──
save(crop(p(10), 550, 1140), 6)

# ── Q07: Dot-arrow series + options 1–5  (bottom of page 10 + tiny top of page 11) ──
save(stitch(crop(p(10), 1145, p(10).height - FOOTER_PX), crop(p(11), HEADER_PX, 85)), 7)

# ── Q08: Star series + options 1–5  (page 11) ──
save(crop(p(11), 85, 665), 8)

# ── Q12: Diamond analogy A→B, C→?  + options  (bottom of page 11 + top of page 12) ──
save(stitch(crop(p(11), 870, p(11).height - FOOTER_PX), crop(p(12), HEADER_PX, 625)), 12)

# ── Q14: Cross / branch-shape series + options 1–5  (page 12) ──
save(crop(p(12), 635, 1255), 14)

# ── Q17: Parallelogram series + options  (bottom of page 12 + top of page 13) ──
save(stitch(crop(p(12), 1270, p(12).height - FOOTER_PX), crop(p(13), HEADER_PX, 235)), 17)

# ── Q21: Hexagon series + options 1–5  (page 13) ──
save(crop(p(13), 245, 905), 21)

# ── Q24: Star/pentagon series + options  (bottom of page 13 + top of page 14) ──
save(stitch(crop(p(13), 915, p(13).height - FOOTER_PX), crop(p(14), HEADER_PX, 550)), 24)

# ── Q25: Class A / B shapes + options 1–4  (page 14, lower half) ──
save(crop(p(14), 555, 1160), 25)

# ── Q34: Class A / B shapes + options 1–4  (page 15, skipping Q32/Q33 text) ──
save(crop(p(15), 215, 645), 34)

# ── Q35: Class A / B shapes + options 1–4  (page 15, middle) ──
save(crop(p(15), 650, 1255), 35)

# ── Q36: Arrow analogy A→B, C→? + options  (bottom of page 15 + top of page 16) ──
save(stitch(crop(p(15), 1265, p(15).height - FOOTER_PX), crop(p(16), HEADER_PX, 485)), 36)

# ── Q38: Class A / B + options 1–4  (page 16, middle, skipping Q37 text) ──
save(crop(p(16), 630, 1075), 38)

# ── Q39: Class A / B + options 1–4  (page 16, lower, stop before Q40 text) ──
save(crop(p(16), 1080, 1580), 39)

# ── Q43: Multi-row star analogy A→B + C→ options  (page 17, skip header) ──
save(crop(p(17), HEADER_PX, 905), 43)

# ── Q44: Shape analogy + options  (page 17, middle) ──
save(crop(p(17), 910, 1260), 44)

# ── Q47: KAN/MVL letter classification + options  (bottom of page 17 + top of page 18) ──
save(stitch(crop(p(17), 1355, p(17).height - FOOTER_PX), crop(p(18), HEADER_PX, 230)), 47)

# ── Q48: Class A / B shapes + options  (page 18) ──
save(crop(p(18), 240, 885), 48)

print()
print(f"🎉  Done — 22 WebP images saved to: {OUT_DIR}")
print("     Next: node scripts/upload-oir-set-images.js")
