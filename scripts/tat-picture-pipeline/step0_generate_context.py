#!/usr/bin/env python3
"""
Step 0 — Read all 167 TAT PNGs via Gemini multimodal, extract scene descriptions,
          generate TATImageContext per image, assign card positions, write HTML preview.

Input:   SOURCE_DIR (167 PNGs at /Users/sunil/Downloads/TAT Work/Cropped_images/)
Output:  output/tat_context_draft.json   (167 entries — review and rename to finalize)
         output/preview.html             (HTML gate — review BEFORE running step1_upload.py)

Usage:
  python step0_generate_context.py                      # all 167 images
  python step0_generate_context.py --ids 1 2 3          # specific scene numbers
  python step0_generate_context.py --resume             # skip already-processed images
  python step0_generate_context.py --preview-only       # rebuild preview.html from draft

After review:
  cp output/tat_context_draft.json tat_image_contexts.json
  python step1_upload.py

API key read from GEMINI_API_KEY env var or local.properties.
"""

import argparse
import json
import re
import sys
import time
from collections import defaultdict
from pathlib import Path

SOURCE_DIR = Path("/Users/sunil/Downloads/TAT Work/Cropped_images")
SCRIPT_DIR = Path(__file__).parent
OUTPUT_DIR = SCRIPT_DIR / "output"
DRAFT_JSON = OUTPUT_DIR / "tat_context_draft.json"
FAILED_JSON = OUTPUT_DIR / "failed.json"
PREVIEW_HTML = OUTPUT_DIR / "preview.html"

VALID_OLQ_NAMES = {
    "EFFECTIVE_INTELLIGENCE", "REASONING_ABILITY", "ORGANIZING_ABILITY",
    "POWER_OF_EXPRESSION", "SOCIAL_ADJUSTMENT", "COOPERATION",
    "SENSE_OF_RESPONSIBILITY", "INITIATIVE", "SELF_CONFIDENCE",
    "SPEED_OF_DECISION", "INFLUENCE_GROUP", "LIVELINESS",
    "DETERMINATION", "COURAGE", "STAMINA",
}

REQUIRED_CONTEXT_KEYS = {
    "sceneDescription", "coreElements", "ambiguousElements",
    "expectedThemes", "penalizedThemes", "primaryOLQs",
    "deviationTolerance", "exemplarGoodHints", "exemplarBadHints",
}

VALID_DEVIATION_TOLERANCES = {"LOW", "MEDIUM", "HIGH"}

# OLQ → preferred card positions (1–11), in priority order
OLQ_POSITION_MAP: dict[str, list[int]] = {
    "INITIATIVE":              [1, 11],
    "SELF_CONFIDENCE":         [11, 1],
    "COOPERATION":             [2, 6],
    "SOCIAL_ADJUSTMENT":       [2, 8],
    "DETERMINATION":           [3, 10],
    "STAMINA":                 [3],
    "SPEED_OF_DECISION":       [4],
    "COURAGE":                 [4, 10],
    "ORGANIZING_ABILITY":      [5],
    "EFFECTIVE_INTELLIGENCE":  [5, 9],
    "SENSE_OF_RESPONSIBILITY": [6],
    "INFLUENCE_GROUP":         [7],
    "POWER_OF_EXPRESSION":     [7],
    "LIVELINESS":              [8],
    "REASONING_ABILITY":       [9],
}

POSITION_CATEGORIES = {
    1:  "Individual Adversity / Initiative",
    2:  "Social / Group Cooperation",
    3:  "Persistence / Stamina",
    4:  "Crisis / Quick Decision",
    5:  "Planning / Organizing",
    6:  "Duty / Responsibility",
    7:  "Leadership / Influence",
    8:  "Energy / Social Engagement",
    9:  "Problem Solving / Analysis",
    10: "Courage / Determination",
    11: "Self-Reliance / Confidence",
}

GEMINI_PROMPT = """You are an SSB TAT (Thematic Apperception Test) expert psychologist for Indian military officer selection.

Analyze the attached TAT image carefully. This image will be shown to candidates for 30 seconds — they then write a 4-minute story (beginning, middle, end) about it.

IMPORTANT: The bottom of the image contains a printed text description of the scene. Extract this text EXACTLY as it appears (character for character, including punctuation). If no text is visible at the bottom, write "NOT FOUND".

Return STRICT JSON ONLY — no markdown fences, no explanations outside the JSON:
{
  "extractedDescription": "Exact text printed at the bottom of the image, or NOT FOUND",
  "sceneDescription": "One concise objective sentence describing the visual scene",
  "coreElements": ["3–5 clearly visible elements every candidate will notice"],
  "ambiguousElements": ["2–3 elements open to interpretation"],
  "expectedThemes": ["3–4 story directions that score well in SSB context"],
  "penalizedThemes": ["2–3 story directions that score poorly or indicate poor OLQ"],
  "primaryOLQs": ["2–4 OLQ names most testable by this image"],
  "deviationTolerance": "LOW or MEDIUM or HIGH",
  "exemplarGoodHints": ["1–2 specific story elements that would impress an SSB evaluator"],
  "exemplarBadHints": ["1–2 specific story elements that would concern an SSB evaluator"],
  "suggestedPosition": 1
}

Valid OLQ names (use EXACTLY as shown, case-sensitive):
EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION,
SOCIAL_ADJUSTMENT, COOPERATION, SENSE_OF_RESPONSIBILITY, INITIATIVE, SELF_CONFIDENCE,
SPEED_OF_DECISION, INFLUENCE_GROUP, LIVELINESS, DETERMINATION, COURAGE, STAMINA

Deviation tolerance guide:
  LOW    = scene is very specific; candidate stories must closely follow the image
  MEDIUM = scene allows moderate creative interpretation
  HIGH   = ambiguous scene; wide range of valid story directions

For suggestedPosition (integer 1–11), choose based on the most prominent OLQ:
  1  = Initiative / Self-Confidence (solo challenge, individual start)
  2  = Cooperation / Social Adjustment (group/social scenario, peers interacting)
  3  = Determination / Stamina (persistence under sustained pressure)
  4  = Speed of Decision / Courage (emergency, urgency, time-critical)
  5  = Organizing Ability / Effective Intelligence (planning, coordination, leadership)
  6  = Sense of Responsibility / Cooperation (duty, community, service)
  7  = Influence Group / Power of Expression (communication, persuasion, leading)
  8  = Liveliness / Social Adjustment (energy, positive engagement, dynamic)
  9  = Reasoning Ability / Effective Intelligence (problem solving, analysis)
  10 = Determination / Courage (high-risk adversity, standing firm)
  11 = Self-Confidence / Initiative (individual agency, solo action under scrutiny)
"""


def parse_filename(filename: str) -> tuple[int, str, str] | None:
    """Parse TAT filename into (scene_number, gender_tag, image_id) or None."""
    m = re.match(r'^(\d+)(Men|Mixed|Women)\.png$', filename)
    if not m:
        return None
    n = int(m.group(1))
    suffix = m.group(2)
    if suffix == "Men":
        return n, "MALE", f"tat_{n:03d}_male"
    if suffix == "Women":
        return n, "FEMALE", f"tat_{n:03d}_female"
    return n, "MIXED", f"tat_{n:03d}_mixed"


def all_image_files() -> list[tuple[int, str, str, Path]]:
    """Return all TAT images sorted by (scene_number, gender_tag)."""
    results = []
    for p in SOURCE_DIR.iterdir():
        parsed = parse_filename(p.name)
        if parsed:
            scene_num, gender_tag, image_id = parsed
            results.append((scene_num, gender_tag, image_id, p))
    results.sort(key=lambda x: (x[0], x[1]))
    return results


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
    """Return list of validation errors (empty = valid)."""
    errors: list[str] = []
    ctx = entry.get("imageContext", {})

    missing = REQUIRED_CONTEXT_KEYS - set(ctx.keys())
    if missing:
        errors.append(f"Missing keys: {sorted(missing)}")

    invalid_olqs = [o for o in ctx.get("primaryOLQs", []) if o not in VALID_OLQ_NAMES]
    if invalid_olqs:
        errors.append(f"Invalid OLQs: {invalid_olqs}")

    if ctx.get("deviationTolerance") not in VALID_DEVIATION_TOLERANCES:
        errors.append(f"Invalid deviationTolerance: {ctx.get('deviationTolerance')!r}")

    if not ctx.get("coreElements"):
        errors.append("coreElements is empty")

    if not ctx.get("primaryOLQs"):
        errors.append("primaryOLQs is empty")

    pos = entry.get("cardPosition", 0)
    if not (1 <= pos <= 11):
        errors.append(f"cardPosition {pos} out of range 1–11")

    return errors


def call_gemini(image_path: Path, api_key: str, retries: int = 3) -> dict | None:
    """Call Gemini Vision and return parsed raw context dict, or None on failure."""
    from google import genai
    from google.genai import types

    client = genai.Client(api_key=api_key)
    image_data = image_path.read_bytes()

    for attempt in range(1, retries + 1):
        try:
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=[
                    types.Part.from_bytes(data=image_data, mime_type="image/png"),
                    GEMINI_PROMPT,
                ],
                config=types.GenerateContentConfig(
                    temperature=0.2,
                    response_mime_type="application/json",
                ),
            )
            text = response.text.strip()
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


def assign_card_positions(entries: list[dict]) -> None:
    """
    Assign cardPosition (1–11) to all entries.
    All variants of the same scene number receive the same position.
    Greedily fills least-used positions first, guided by primaryOLQ preferences.
    """
    scene_map: dict[int, list[dict]] = defaultdict(list)
    for e in entries:
        scene_map[e["sceneNumber"]].append(e)

    # Track total image count (not scene count) per position
    position_image_counts: dict[int, int] = defaultdict(int)

    def candidate_positions(scene_entries: list[dict]) -> list[int]:
        suggested = scene_entries[0].get("suggestedPosition", 0)
        all_olqs: list[str] = []
        for e in scene_entries:
            all_olqs.extend(e.get("imageContext", {}).get("primaryOLQs", []))

        candidates: list[int] = []
        if 1 <= suggested <= 11:
            candidates.append(suggested)
        for olq in all_olqs:
            for pos in OLQ_POSITION_MAP.get(olq, []):
                if pos not in candidates:
                    candidates.append(pos)
        # Append remaining positions as fallback
        for pos in range(1, 12):
            if pos not in candidates:
                candidates.append(pos)
        return candidates

    for scene_num in sorted(scene_map.keys()):
        scene_entries = scene_map[scene_num]
        candidates = candidate_positions(scene_entries)
        # Pick candidate with fewest images assigned so far
        chosen = min(candidates, key=lambda p: position_image_counts[p])
        category = POSITION_CATEGORIES.get(chosen, "General")
        for e in scene_entries:
            e["cardPosition"] = chosen
            e["category"] = category
        position_image_counts[chosen] += len(scene_entries)

    # Distribution report
    print("\nCard position distribution:")
    print(f"  {'Pos':>3}  {'Images':>6}  Category")
    print(f"  {'-'*3}  {'-'*6}  {'-'*35}")
    for pos in range(1, 12):
        count = position_image_counts[pos]
        cat = POSITION_CATEGORIES.get(pos, "")
        flag = " ⚠ LOW" if count < 10 else ""
        print(f"  {pos:>3}  {count:>6}  {cat}{flag}")
    print(f"  {'TOT':>3}  {sum(position_image_counts.values()):>6}")


def build_preview(entries: list[dict]) -> None:
    """Generate HTML review gate from draft entries."""
    GENDER_COLORS = {"MALE": "#b3d9ff", "FEMALE": "#ffb3d9", "MIXED": "#d9ffb3"}

    # Compute distribution stats for the header table
    pos_stats: dict[int, dict] = {i: {"total": 0, "female_mixed": 0} for i in range(1, 12)}
    for e in entries:
        pos = e.get("cardPosition", 0)
        if 1 <= pos <= 11:
            pos_stats[pos]["total"] += 1
            if e.get("genderTag") in ("FEMALE", "MIXED"):
                pos_stats[pos]["female_mixed"] += 1

    parts = [
        "<!DOCTYPE html><html><head><meta charset='utf-8'>",
        "<title>TAT Context Preview</title>",
        "<style>",
        "body{font-family:sans-serif;max-width:1200px;margin:0 auto;padding:16px;color:#222}",
        "h1,h2{color:#333}",
        ".stats{background:#f7faff;border:1px solid #dce;border-radius:8px;padding:16px;margin-bottom:24px}",
        "table{border-collapse:collapse} th,td{border:1px solid #ccc;padding:4px 8px;text-align:center}",
        "th{background:#eee}",
        ".entry{border:1px solid #ddd;border-radius:8px;padding:16px;margin-bottom:16px}",
        ".grid{display:grid;grid-template-columns:260px 1fr;gap:16px}",
        ".entry img{width:100%;border-radius:4px;display:block}",
        ".badge{display:inline-block;padding:2px 8px;border-radius:4px;font-weight:bold;font-size:12px}",
        ".pos-badge{background:#333;color:#fff;font-size:12px;padding:2px 8px;border-radius:4px;margin-left:4px}",
        ".err{color:#c00;font-weight:bold;background:#fff0f0;padding:4px 8px;border-radius:4px;margin:4px 0;display:block}",
        ".field{margin:6px 0} .field b{color:#555}",
        ".olq-chip{display:inline-block;background:#e8f4fd;border:1px solid #bee3f8;padding:1px 6px;border-radius:3px;font-size:12px;margin:2px}",
        "details{margin:4px 0} summary{cursor:pointer;font-weight:bold;color:#2b6cb0;font-size:13px}",
        "ul{margin:4px 0;padding-left:18px} li{font-size:13px}",
        ".extracted{font-size:11px;color:#777;border-top:1px solid #eee;padding-top:4px;margin-top:4px}",
        "</style></head><body>",
    ]

    parts.append(f"<h1>TAT Context Preview — {len(entries)} images</h1>")
    parts.append('<div class="stats"><h2>Distribution Check</h2>')
    parts.append('<table><tr><th>Position</th><th>Category</th><th>Total Images</th>'
                 '<th>FEMALE+MIXED</th><th>Status</th></tr>')
    all_ok = True
    for pos in range(1, 12):
        t = pos_stats[pos]["total"]
        fm = pos_stats[pos]["female_mixed"]
        ok = t >= 10 and fm >= 3
        if not ok:
            all_ok = False
        status = "✅" if ok else "⚠️ CHECK"
        cat = POSITION_CATEGORIES.get(pos, "")
        parts.append(f"<tr><td>{pos}</td><td style='text-align:left'>{cat}</td>"
                     f"<td>{t}</td><td>{fm}</td><td>{status}</td></tr>")
    overall = "✅ All positions OK" if all_ok else "⚠️ Some positions need attention"
    parts.append(f'</table><p style="margin-top:8px"><b>{overall}</b></p></div>')
    parts.append("<p><b>Review ALL entries below. Fix _validationErrors, then:</b><br>"
                 "<code>cp output/tat_context_draft.json tat_image_contexts.json</code><br>"
                 "<code>python step1_upload.py --dry-run</code></p>")

    for e in sorted(entries, key=lambda x: (x.get("cardPosition", 99), x.get("sceneNumber", 0), x.get("genderTag", ""))):
        gt = e.get("genderTag", "MIXED")
        ctx = e.get("imageContext", {})
        errs = e.get("_validationErrors", [])
        pos = e.get("cardPosition", "?")
        extracted = e.get("extractedDescription", "")
        src_file = e.get("sourceFile", "")
        img_path = SOURCE_DIR / src_file
        img_src = f"file://{img_path}"
        color = GENDER_COLORS.get(gt, "#eee")

        err_html = "".join(f'<span class="err">⚠ {x}</span>' for x in errs)
        olq_html = "".join(f'<span class="olq-chip">{o}</span>' for o in ctx.get("primaryOLQs", []))

        def list_field(label: str, items: list) -> str:
            lis = "".join(f"<li>{v}</li>" for v in items)
            return f'<div class="field"><b>{label}:</b><ul>{lis}</ul></div>'

        def text_field(label: str, val: str) -> str:
            return f'<div class="field"><b>{label}:</b> {val}</div>'

        parts.append(f'<div class="entry">')
        parts.append(f'<div class="grid">')
        # Left column: image
        parts.append(f'<div>')
        parts.append(f'<img src="{img_src}" alt="{src_file}">')
        parts.append(f'<p><b>{e.get("id", "")}</b><br>'
                     f'<span class="badge" style="background:{color}">{gt}</span>'
                     f'<span class="pos-badge">Position {pos}</span></p>')
        if extracted and extracted != "NOT FOUND":
            parts.append(f'<p class="extracted">📋 {extracted}</p>')
        parts.append('</div>')
        # Right column: context
        parts.append('<div>')
        parts.append(err_html)
        parts.append(text_field("Scene", ctx.get("sceneDescription", "")))
        parts.append(f'<div class="field"><b>Primary OLQs:</b> {olq_html}</div>')
        parts.append(text_field("Deviation Tolerance", ctx.get("deviationTolerance", "")))
        parts.append(f'<details><summary>Core Elements</summary>'
                     f'{list_field("", ctx.get("coreElements", []))}</details>')
        parts.append(f'<details><summary>Ambiguous Elements</summary>'
                     f'{list_field("", ctx.get("ambiguousElements", []))}</details>')
        parts.append(f'<details><summary>Expected Themes</summary>'
                     f'{list_field("", ctx.get("expectedThemes", []))}</details>')
        parts.append(f'<details><summary>Penalized Themes</summary>'
                     f'{list_field("", ctx.get("penalizedThemes", []))}</details>')
        parts.append(f'<details><summary>Story Hints</summary>'
                     f'{list_field("Good", ctx.get("exemplarGoodHints", []))}'
                     f'{list_field("Bad", ctx.get("exemplarBadHints", []))}</details>')
        parts.append('</div>')
        parts.append('</div></div>')

    parts.append("</body></html>")
    PREVIEW_HTML.write_text("".join(parts), encoding="utf-8")
    print(f"\nPreview → {PREVIEW_HTML}")
    print("Open in browser. Review all 167 entries.")
    print("When satisfied:")
    print("  cp output/tat_context_draft.json tat_image_contexts.json")
    print("  python step1_upload.py --dry-run")


def main(argv: list[str] | None = None) -> int:
    import os

    parser = argparse.ArgumentParser(description="Generate TATImageContext via Gemini Vision")
    parser.add_argument("--ids", nargs="*", type=int, metavar="N",
                        help="Specific SCENE numbers to process (1–65)")
    parser.add_argument("--resume", action="store_true",
                        help="Skip image IDs already in draft JSON")
    parser.add_argument("--preview-only", action="store_true",
                        help="Rebuild preview.html from existing draft; no Gemini calls")
    args = parser.parse_args(argv)

    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        props = SCRIPT_DIR.parent.parent / "local.properties"
        if props.exists():
            for line in props.read_text().splitlines():
                if line.startswith("GEMINI_API_KEY="):
                    api_key = line.split("=", 1)[1].strip()
                    break
    if not api_key and not args.preview_only:
        print("ERROR: GEMINI_API_KEY not set. Add to local.properties or export it.", file=sys.stderr)
        return 1

    existing = load_draft()
    existing_by_id: dict[str, dict] = {e["id"]: e for e in existing}

    if args.preview_only:
        if not existing:
            print("ERROR: No draft JSON found. Run without --preview-only first.", file=sys.stderr)
            return 1
        # Build preview from draft as-is; do NOT re-assign positions (user may have edited them)
        build_preview(existing)
        return 0

    if not SOURCE_DIR.exists():
        print(f"ERROR: Source directory not found: {SOURCE_DIR}", file=sys.stderr)
        return 1
    all_files = all_image_files()

    if args.ids:
        id_set = set(args.ids)
        all_files = [(sn, gt, iid, p) for sn, gt, iid, p in all_files if sn in id_set]

    if args.resume:
        all_files = [(sn, gt, iid, p) for sn, gt, iid, p in all_files if iid not in existing_by_id]
        print(f"Resuming: {len(all_files)} images remaining")

    if not all_files:
        print("Nothing to process.")
        all_entries = list(existing_by_id.values())
        assign_card_positions(all_entries)
        save_draft(all_entries)
        build_preview(all_entries)
        return 0

    print(f"Processing {len(all_files)} images via Gemini…")
    failed: list[dict] = []

    for scene_num, gender_tag, image_id, image_path in all_files:
        print(f"[{image_id}] {image_path.name}")
        raw = call_gemini(image_path, api_key)
        if raw is None:
            print(f"  → FAILED after all retries", file=sys.stderr)
            failed.append({"id": image_id, "sceneNumber": scene_num, "sourceFile": image_path.name})
            continue

        extracted_desc = raw.pop("extractedDescription", "NOT FOUND")
        suggested_pos = raw.pop("suggestedPosition", 0)
        if isinstance(suggested_pos, float):
            suggested_pos = int(suggested_pos)

        entry: dict = {
            "id": image_id,
            "sourceFile": image_path.name,
            "sceneNumber": scene_num,
            "genderTag": gender_tag,
            "extractedDescription": extracted_desc,
            "suggestedPosition": suggested_pos,
            "cardPosition": 0,   # assigned after all images processed
            "category": "",      # assigned with cardPosition
            "difficulty": "Medium",
            "imageContext": raw,
        }

        errors = validate_entry(entry)
        if errors:
            print(f"  → Validation warnings: {errors}", file=sys.stderr)
            entry["_validationErrors"] = errors

        existing_by_id[image_id] = entry
        save_draft(list(existing_by_id.values()))
        print(f"  → OK  ({len(existing_by_id)} total)")
        time.sleep(1)  # 1 req/sec rate limit

    if failed:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        with open(FAILED_JSON, "w") as f:
            json.dump(failed, f, indent=2)
        print(f"\n{len(failed)} image(s) failed → {FAILED_JSON}", file=sys.stderr)

    all_entries = list(existing_by_id.values())
    assign_card_positions(all_entries)
    # Re-validate after positions are assigned (clears stale cardPosition=0 errors)
    for entry in all_entries:
        errors = validate_entry(entry)
        if errors:
            entry["_validationErrors"] = errors
        elif "_validationErrors" in entry:
            del entry["_validationErrors"]
    save_draft(all_entries)
    build_preview(all_entries)

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
