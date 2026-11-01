"""Prints Android vector pathData for the launcher icon.

The icon shows the letter Alef taken from the bundled Rubik Bold font plus a
small star. Run with the project's font to regenerate the numbers used in
app/src/main/res/drawable/ic_launcher_*.xml:

    python3 tools/make_icon_paths.py app/src/main/assets/fonts/Rubik-Bold.ttf
"""
import math
import sys

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont


def glyph_path(font_path, char, box_center, box_height):
    font = TTFont(font_path)
    glyphs = font.getGlyphSet()
    name = font.getBestCmap()[ord(char)]
    bp = BoundsPen(glyphs)
    glyphs[name].draw(bp)
    xmin, ymin, xmax, ymax = bp.bounds
    s = box_height / (ymax - ymin)
    cx, cy = (xmin + xmax) / 2, (ymin + ymax) / 2
    # font units (y up) -> icon units (y down), centred on box_center
    t = (s, 0, 0, -s, box_center[0] - cx * s, box_center[1] + cy * s)
    pen = SVGPathPen(glyphs, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
    glyphs[name].draw(TransformPen(pen, t))
    return pen.getCommands()


def star_path(cx, cy, r_outer, r_inner, points=5):
    pts = []
    for i in range(points * 2):
        r = r_outer if i % 2 == 0 else r_inner
        a = -math.pi / 2 + i * math.pi / points
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return "M" + " L".join("%.2f,%.2f" % p for p in pts) + " Z"


if __name__ == "__main__":
    font = sys.argv[1]
    alef = "א"
    print("ALEF  ", glyph_path(font, alef, (56, 60), 38))  # colour icon
    print("MONO  ", glyph_path(font, alef, (54, 55), 42))  # themed (monochrome) icon
    print("STAR  ", star_path(36, 34, 6.5, 2.8))
