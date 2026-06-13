#!/usr/bin/env python3
"""
Step 2 — Generate PPDTImageContext for each extracted image via Gemini Vision.

Input:   output/images/ppdt_image_NNN.jpg  (64 files from step1)
         gender_map.json
Output:  output/ppdt_context_draft.json    (64-entry list, one per image)
         output/preview.html               (visual review page — MUST APPROVE before step3)

The script calls Gemini 1.5 Flash once per image to produce structured PPDTImageContext JSON.
Progress is saved after each image so the script is safe to interrupt and re-run.

Usage:
  python step2_generate_context.py                      # all 64 images
  python step2_generate_context.py --ids 1 2 3 4        # specific image IDs only
  python step2_generate_context.py --resume             # skip IDs already in draft JSON
  python step2_generate_context.py --preview-only       # regenerate preview.html from existing draft

Prerequisites:
  export GEMINI_API_KEY=your_key
  pip install -r requirements.txt
"""

import argparse
import json
import sys
import time
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
OUTPUT_DIR = SCRIPT_DIR / "output"
IMAGES_DIR = OUTPUT_DIR / "images"
DRAFT_JSON = OUTPUT_DIR / "ppdt_context_draft.json"
FAILED_JSON = OUTPUT_DIR / "failed.json"
PREVIEW_HTML = OUTPUT_DIR / "preview.html"
GENDER_MAP_JSON = SCRIPT_DIR / "gender_map.json"

VALID_OLQ_NAMES = {
    "EFFECTIVE_INTELLIGENCE", "REASONING_ABILITY", "ORGANIZING_ABILITY",
    "POWER_OF_EXPRESSION", "SOCIAL_ADJUSTMENT", "COOPERATION",
    "SENSE_OF_RESPONSIBILITY", "INITIATIVE", "SELF_CONFIDENCE",
    "SPEED_OF_DECISION", "INFLUENCE_GROUP", "LIVELINESS",
    "DETERMINATION", "COURAGE", "STAMINA",
}

REQUIRED_KEYS = {
    "sceneDescription", "coreElements", "ambiguousElements",
    "expectedThemes", "penalizedThemes", "primaryOLQs",
    "deviationTolerance", "exemplarGoodHints", "exemplarBadHints",
}
VALID_DEVIATION_TOLERANCES = {"LOW", "MEDIUM", "HIGH"}
ALL_IMAGE_IDS = list(range(1, 21)) + list(range(25, 69))  # gap at 21-24 intentional

EXPANSION_PROMPT = """You are an SSB PPDT expert psychologist evaluating a picture for officer selection tests.

Analyze the attached image carefully. This image will be shown to candidates for 30 seconds (slightly hazy/low-contrast) and they must write a 4-minute story about it.

Return STRICT JSON ONLY — no markdown fences, no explanations outside the JSON:
{
  "sceneDescription": "One concise objective sentence describing the scene",
  "coreElements": ["3–5 clearly visible elements even in hazy rendering"],
  "ambiguousElements": ["2–3 elements open to interpretation due to haziness"],
  "expectedThemes": ["3–4 story directions that score well in SSB context"],
  "penalizedThemes": ["2–3 story directions that miss picture intent or score poorly"],
  "primaryOLQs": ["2–4 OLQ names most testable by this picture"],
  "deviationTolerance": "LOW or MEDIUM or HIGH",
  "exemplarGoodHints": ["1–2 specific story elements that score well"],
  "exemplarBadHints": ["1–2 specific story elements that score poorly"]
}

Valid OLQ names (use EXACTLY as shown):
EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY,
POWER_OF_EXPRESSION, SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY,
INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS,
DETERMINATION, COURAGE, STAMINA

Deviation tolerance guide:
  LOW    = scene is very specific; story must closely follow the image
  MEDIUM = scene allows moderate creative interpretation
  HIGH   = ambiguous scene; wide range of valid story directions
"""


def load_gender_map() -> dict[str, str]:
    with open(GENDER_MAP_JSON) as f:
        raw = json.load(f)
    return {k: v for k, v in raw.items() if not k.startswith("_")}

def load_draft() -> list[dict]:
    if DRAFT_JSON.exists():
        with open(DRAFT_JSON) as f:
            return json.load(f)
    return []


def save_draft(entries: list[dict]) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with open(DRAFT_JSON, "w") as f:
        json.dump(entries, f, indent=2)


def validate_entry(entry: dict) -> list[str]:
    """Return list of validation errors, empty if valid."""
    errors: list[str] = []
    ctx = entry.get("imageContext", {})

    missing_keys = REQUIRED_KEYS - set(ctx.keys())
    if missing_keys:
        errors.append(f"Missing keys: {missing_keys}")

    invalid_olqs = [o for o in ctx.get("primaryOLQs", []) if o not in VALID_OLQ_NAMES]
    if invalid_olqs:
        errors.append(f"Invalid OLQ names: {invalid_olqs}")

    if ctx.get("deviationTolerance") not in VALID_DEVIATION_TOLERANCES:
        errors.append(f"Invalid deviationTolerance: {ctx.get('deviationTolerance')!r}")

    if not ctx.get("coreElements"):
        errors.append("coreElements is empty")

    return errors


def call_gemini(image_path: Path, api_key: str, retries: int = 3) -> dict | None:
    """Call Gemini Vision and return parsed PPDTImageContext dict, or None on failure."""
    import google.generativeai as genai

    genai.configure(api_key=api_key)
    model = genai.GenerativeModel("gemini-1.5-flash")

    image_data = image_path.read_bytes()
    image_part = {"mime_type": "image/jpeg", "data": image_data}

    for attempt in range(1, retries + 1):
        try:
            response = model.generate_content(
                [image_part, EXPANSION_PROMPT],
                generation_config={"temperature": 0.2, "response_mime_type": "application/json"},
            )
            text = response.text.strip()
            # Strip markdown fences if model ignores the instruction
            if text.startswith("```"):
                text = text.split("```")[1]
                if text.startswith("json"):
                    text = text[4:]
            return json.loads(text)
        except json.JSONDecodeError as e:
            print(f"    [attempt {attempt}] JSON parse error: {e}", file=sys.stderr)
        except Exception as e:
            print(f"    [attempt {attempt}] Gemini error: {e}", file=sys.stderr)

        if attempt < retries:
            wait = 2 ** attempt
            print(f"    Retrying in {wait}s…")
            time.sleep(wait)

    return None


def process_images(ids: list[int], api_key: str, existing: dict[int, dict]) -> tuple[list[dict], list[dict]]:
    """Returns (all_entries, failed_entries)."""
    gender_map = load_gender_map()
    entries = list(existing.values())
    failed: list[dict] = []
    for image_id in ids:
        image_path = IMAGES_DIR / f"ppdt_image_{image_id:03d}.jpg"
        if not image_path.exists():
            print(f"[SKIP] Image not found: {image_path}")
            continue
        gender_tag = gender_map.get(str(image_id), "MIXED")
        print(f"[{image_id:03d}] {image_path.name}  genderTag={gender_tag}")
        ctx = call_gemini(image_path, api_key)
        if ctx is None:
            print(f"  → FAILED after all retries", file=sys.stderr)
            failed.append({"id": f"ppdt_image_{image_id:03d}", "imageNumber": image_id})
            continue
        entry = {"id": f"ppdt_image_{image_id:03d}", "imageNumber": image_id,
                 "genderTag": gender_tag, "imageContext": ctx}
        errors = validate_entry(entry)
        if errors:
            print(f"  → Validation warnings: {errors}", file=sys.stderr)
            entry["_validationErrors"] = errors
        entries.append(entry)
        entries.sort(key=lambda e: e["imageNumber"])
        save_draft(entries)
        print(f"  → OK  (draft saved, {len(entries)} total)")
        time.sleep(1)  # Rate limit: 1 req/sec
    return entries, failed


def build_preview(entries: list[dict]) -> None:
    """Generate preview.html from draft JSON using Jinja2."""
    try:
        from jinja2 import Environment, FileSystemLoader
        env = Environment(loader=FileSystemLoader(str(SCRIPT_DIR)))
        template = env.get_template("preview_template.html")
    except ImportError:
        print("WARNING: jinja2 not installed, generating basic HTML preview")
        _build_basic_preview(entries)
        return

    html = template.render(entries=entries, valid_olqs=VALID_OLQ_NAMES)
    PREVIEW_HTML.write_text(html, encoding="utf-8")
    print(f"\nPreview written → {PREVIEW_HTML}")
    print("OPEN IN BROWSER AND REVIEW ALL 64 ENTRIES BEFORE RUNNING step3_upload.py")


def _build_basic_preview(entries: list[dict]) -> None:
    """Minimal HTML fallback when jinja2 is not installed."""
    COLORS = {"FEMALE": "#ffb3d9", "MALE": "#b3d9ff", "MIXED": "#d9ffb3"}
    parts = [
        "<!DOCTYPE html><html><head><meta charset='utf-8'>"
        "<style>body{font-family:sans-serif;max-width:1100px;margin:0 auto;padding:16px}"
        ".e{display:flex;gap:16px;border-bottom:1px solid #ddd;padding:16px 0}"
        ".err{color:red;font-weight:bold}</style></head><body>",
        f"<h2>PPDT Preview — {len(entries)} images  "
        "(⚠ Install jinja2 for the full UI)</h2>",
        "<p><b>Review carefully. Fix _validationErrors before running step3.</b></p>",
    ]
    for e in entries:
        g = e.get("genderTag", "MIXED")
        ctx = e.get("imageContext", {})
        errs = e.get("_validationErrors", [])
        img = f"images/ppdt_image_{e['imageNumber']:03d}.jpg"
        color = COLORS.get(g, "#eee")
        badge = f"<b style='background:{color};padding:2px 6px'>{g}</b>"
        err_html = "".join(f"<p class=err>⚠ {x}</p>" for x in errs)
        fields = "".join(
            f"<p><b>{k}:</b> {', '.join(v) if isinstance(v, list) else v}</p>"
            for k, v in ctx.items()
        )
        parts.append(
            f"<div class=e><div><img src='{img}' width=260><p>{e['id']} {badge}</p></div>"
            f"<div>{err_html}{fields}</div></div>"
        )
    parts.append("</body></html>")
    PREVIEW_HTML.write_text("".join(parts), encoding="utf-8")
    print(f"\nPreview written → {PREVIEW_HTML}")
    print("OPEN IN BROWSER AND REVIEW ALL 64 ENTRIES BEFORE RUNNING step3_upload.py")


def main(argv: list[str] | None = None) -> int:
    import os

    parser = argparse.ArgumentParser(description="Generate PPDTImageContext via Gemini Vision")
    parser.add_argument("--ids", nargs="*", type=int, metavar="N",
                        help="Specific image IDs to process (default: all 64)")
    parser.add_argument("--resume", action="store_true",
                        help="Skip image IDs already present in draft JSON")
    parser.add_argument("--preview-only", action="store_true",
                        help="Regenerate preview.html from existing draft; skip Gemini calls")
    args = parser.parse_args(argv)

    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key and not args.preview_only:
        print("ERROR: GEMINI_API_KEY environment variable not set.", file=sys.stderr)
        return 1

    existing_entries = load_draft()
    existing_by_id: dict[int, dict] = {e["imageNumber"]: e for e in existing_entries}

    if args.preview_only:
        if not existing_entries:
            print("ERROR: No draft JSON found. Run without --preview-only first.", file=sys.stderr)
            return 1
        build_preview(existing_entries)
        return 0

    ids_to_process = args.ids if args.ids else ALL_IMAGE_IDS
    if args.resume:
        ids_to_process = [i for i in ids_to_process if i not in existing_by_id]
        print(f"Resuming: {len(ids_to_process)} images remaining")

    if not ids_to_process:
        print("Nothing to process.")
        build_preview(list(existing_by_id.values()))
        return 0

    all_entries, failed = process_images(ids_to_process, api_key, existing_by_id)

    if failed:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        with open(FAILED_JSON, "w") as f:
            json.dump(failed, f, indent=2)
        print(f"\n{len(failed)} image(s) failed — see {FAILED_JSON}", file=sys.stderr)

    build_preview(all_entries)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
