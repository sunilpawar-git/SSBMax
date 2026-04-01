#!/usr/bin/env python3
"""
Extract OIR Set 1 question images from SSBCrack PDF.

Pages 7-20 of the PDF (0-indexed: 6-19) contain Set 1.
Images are exported as PNG files to scripts/oir_images/set_01/.

Install dependency: pip install pymupdf
Usage: python scripts/extract-oir-images.py [path/to/pdf]
"""

import sys
import os

try:
    import fitz  # pymupdf
except ImportError:
    print("❌ pymupdf not installed. Run: pip install pymupdf")
    sys.exit(1)

# --- Configuration ---
PDF_PATH = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(__file__), "oir-set-pdf.pdf"
)
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "oir_images", "set_01")
# Pages 7-20 (1-indexed) → 0-indexed: 6-19
PAGE_START = 6
PAGE_END = 19  # inclusive
# Resolution multiplier (2.0 → 144 dpi, good quality without huge files)
ZOOM = 2.0


def main():
    if not os.path.exists(PDF_PATH):
        print(f"❌ PDF not found: {PDF_PATH}")
        print("   Usage: python scripts/extract-oir-images.py /path/to/oir.pdf")
        sys.exit(1)

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print(f"📄 Opening PDF: {PDF_PATH}")
    doc = fitz.open(PDF_PATH)
    total_pages = doc.page_count
    print(f"   Total pages: {total_pages}")

    end = min(PAGE_END, total_pages - 1)
    mat = fitz.Matrix(ZOOM, ZOOM)

    exported = 0
    for page_idx in range(PAGE_START, end + 1):
        page = doc[page_idx]
        pix = page.get_pixmap(matrix=mat, alpha=False)
        page_num = page_idx + 1  # 1-indexed for filename
        out_path = os.path.join(OUTPUT_DIR, f"page_{page_num:02d}.png")
        pix.save(out_path)
        size_kb = os.path.getsize(out_path) // 1024
        print(f"   ✅ Page {page_num} → {out_path} ({size_kb} KB)")
        exported += 1

    doc.close()
    print(f"\n🎉 Exported {exported} pages to: {OUTPUT_DIR}")
    print()
    print("📝 Next steps:")
    print("   1. Open each page_NN.png and identify which question figures it contains")
    print("   2. Crop individual question figures and save as q01.png, q03.png, etc.")
    print("      (Image-based questions in Set 1: Q1, Q3-Q8, Q12, Q14, Q17, Q21,")
    print("       Q24-Q25, Q34-Q36, Q38-Q39, Q43-Q44, Q47-Q48)")
    print("   3. Run: node scripts/upload-oir-set-images.js")


if __name__ == "__main__":
    main()
