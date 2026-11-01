"""Recreates app/src/main/assets/fonts/Rubik-Bold.ttf and Rubik-Medium.ttf.

The app bundles two static instances of the Rubik variable font (SIL Open Font License, no
Reserved Font Name), subset to basic Latin, Hebrew letters and niqqud to keep the APK small.

    pip install fonttools
    # Rubik[wght].ttf from https://github.com/google/fonts/tree/main/ofl/rubik
    python3 tools/make_fonts.py Rubik[wght].ttf app/src/main/assets/fonts
"""
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

UNICODES = (
    list(range(0x20, 0x7F))        # basic Latin
    + list(range(0xA0, 0x100))     # Latin-1 punctuation and symbols
    + list(range(0x0591, 0x05C8))  # Hebrew points (niqqud), maqaf
    + list(range(0x05D0, 0x05EB))  # Hebrew letters
    + list(range(0x05F0, 0x05F5))  # Yiddish ligatures, geresh, gershayim
    + list(range(0xFB1D, 0xFB50))  # Hebrew presentation forms
    + list(range(0x2010, 0x2028))  # dashes and quotes
    + list(range(0x2030, 0x203B))
    + [0x20AA, 0x2022, 0x200E, 0x200F]
)


def make(source, weight, out_path):
    font = instancer.instantiateVariableFont(TTFont(source), {"wght": weight}, updateFontNames=True)
    options = subset.Options()
    options.layout_features = ["*"]  # keep mark positioning for niqqud
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.notdef_outline = True
    options.drop_tables += ["STAT"]
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=UNICODES)
    subsetter.subset(font)
    font.save(out_path)


if __name__ == "__main__":
    source, out_dir = sys.argv[1], sys.argv[2]
    make(source, 700, out_dir + "/Rubik-Bold.ttf")
    make(source, 500, out_dir + "/Rubik-Medium.ttf")
