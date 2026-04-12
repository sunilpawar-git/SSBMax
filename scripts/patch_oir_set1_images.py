#!/usr/bin/env python3
"""
Patch oir-set-001.json: fill questionImageUrl for all visual questions
and add imageAlt text. Run once; idempotent.
"""
import json, pathlib, sys

BASE = "oir/images/set_001"

# question-number → (svg filename, imageAlt description)
IMAGE_MAP = {
    1:  ("oir_s01_q01_cube_classification.svg",
         "Group A cubes (consonants: c/t/q, q/d/r, b/s/c) and Group B cubes (vowels: e/a/o, u/o/a, i/e/u). Options: 1(d/a/z) 2(e/q/o) 3(c/m/y)"),
    3:  ("oir_s01_q03_cube_alike.svg",
         "Two isometric cubes: Cube 1 has cross (+) and heart symbols; Cube 2 has star and pentagon symbols."),
    4:  ("oir_s01_q04_cube_alike.svg",
         "Two isometric cubes: Cube 1 has a sad-face and dot; Cube 2 has a circle and bracket symbol."),
    5:  ("oir_s01_q05_arrow_series.svg",
         "Arrow series showing upward and downward arrows. Each step the first downward arrow converts to upward. Options 1-5, answer is all-up option 5."),
    6:  ("oir_s01_q06_class_ab_nested.svg",
         "Class A: two similar closed figures nested (concentric squares, circles, triangles). Class B: open/single shapes. Options 1-4; answers 2 and 3."),
    7:  ("oir_s01_q07_arrow_cross_series.svg",
         "Cross-shaped arrow figures with a central dot. Long arm rotates 90° clockwise each step; short arm constant. Options 1-5; answer 3."),
    8:  ("oir_s01_q08_star_series.svg",
         "5-pointed stars with rotating black/white fill pattern. Analogy: each pair is color-inverse. Options 1-5; answer 3."),
    12: ("oir_s01_q12_figure_analogy_diamond.svg",
         "A:B::C:? Diamond outline → all opposite edges connected. C is hexagon. Answer option 2."),
    14: ("oir_s01_q14_cross_letter_series.svg",
         "Cross shapes with letter boxes at each arm. Letters shift back by 2, then 3, then 4 positions each step. Options 1-5; answer 2 (O/A/R/X)."),
    17: ("oir_s01_q17_double_line_series.svg",
         "Double parallel lines rotating 45° clockwise each step. Options 1-5; answer 4."),
    21: ("oir_s01_q21_hexagon_series.svg",
         "Hexagon with a diamond outside and circle inside, both rotating. Diamond anticlockwise, circle clockwise. Options 1-5; answer 5."),
    24: ("oir_s01_q24_pentagon_star_series.svg",
         "Series of pentagons and 5-pointed stars in increasing groups. Next shape is a 5-pointed star. Options 1-5; answer 3."),
    25: ("oir_s01_q25_class_ab_combined.svg",
         "Class A: two similar shapes combined/overlapping. Class B: single plain shapes. Options 1-4; answers 1 and 2."),
    34: ("oir_s01_q34_class_ab_diff_nested.svg",
         "Class A: two DIFFERENT closed figures, one inside the other. Options 1-4; answers 1 and 4."),
    35: ("oir_s01_q35_class_ab_symmetry.svg",
         "Class A: shapes with vertical line of symmetry. Class B: asymmetric shapes. Options 1-4; answers 1 and 3."),
    36: ("oir_s01_q36_arrow_analogy.svg",
         "A:B::C:? Arrow rotated 180°, inner shape rotated 90° clockwise. C is left arrow. Answer option 5 (right arrow + rotated inner shape)."),
    38: ("oir_s01_q38_class_ab_joined_lines.svg",
         "Class A: two lines that are joined (crossing or meeting). Class B: separate parallel lines. Options 1-4; answers 2 and 4."),
    39: ("oir_s01_q39_class_ab_opposite_edges.svg",
         "Class A: lines inside shape connecting one edge to its opposite edge. Options 1-4; answers 3 and 4."),
    43: ("oir_s01_q43_figure_analogy_color_reverse.svg",
         "A:B::C:? Color state reversed; lines outside shape move inside. C is black triangle. Answer option 2."),
    44: ("oir_s01_q44_figure_analogy_circles.svg",
         "A:B::C:? Number of points = number of circles added. 4-pt star→4 circles; 5-pt star→5 circles. Answer option 3."),
    47: ("oir_s01_q47_class_ab_letters.svg",
         "Class A: K, A, N (3 strokes). Class B: M, V, L. Options I, Z, E, T. Answers 1(I) and 2(Z)."),
    48: ("oir_s01_q48_class_ab_edge_count.svg",
         "Class A: two shapes where one has more edges than the other. Options 1-4; answers 3 and 4."),
}

root   = pathlib.Path(__file__).parent
infile = root / "oir-set-001.json"

with open(infile) as f:
    data = json.load(f)

patched = 0
for q in data["questions"]:
    qn = q["questionNumber"]
    if qn in IMAGE_MAP:
        fname, alt = IMAGE_MAP[qn]
        new_url = f"{BASE}/{fname}"
        if q.get("questionImageUrl") != new_url:
            q["questionImageUrl"] = new_url
            q["imageAlt"] = alt
            patched += 1
    else:
        # Ensure non-visual questions are explicitly null
        if "imageAlt" in q:
            del q["imageAlt"]

with open(infile, "w") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"Patched {patched} questions → {infile}")
print("All 50 questions written. Visual questions now have questionImageUrl set.")
