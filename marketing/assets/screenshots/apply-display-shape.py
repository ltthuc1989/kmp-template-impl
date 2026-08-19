#!/usr/bin/env python3
"""Clip a raw screencap to the Pixel 9a display shape, matching the live store set.

Every screenshot currently on the Play Store has rounded corners and a punched-out camera
hole, both fully transparent — they were captured through Android Studio's Running Devices →
Screenshot, which clips to the display shape on the IDE side. Neither CLI capture path does
this: `adb exec-out screencap` and the emulator console's `screenrecord screenshot` both
return the raw framebuffer, opaque with square corners. Mixing the two styles in one listing
is obvious at a glance, so raw captures get clipped here first.

    python3 apply-display-shape.py raw/*.png              # writes <name>-shaped.png
    python3 apply-display-shape.py raw/foo.png -o out.png

Requires Pillow. If it is not importable, create a venv:
    python3 -m venv .venv && .venv/bin/pip install Pillow

## Where the mask comes from

`display-shape-mask.png` is the alpha channel lifted straight out of a live store
screenshot — not a reconstruction. All seven live screenshots were checked and carry a
byte-identical alpha channel, so one extraction covers the whole set and output matches the
store pixel for pixel.

Rebuilding it geometrically was tried first (rounded rect r=143, cutout circle d=78 centred
at 538.5,85.5, drawn at 4x and downsampled) and landed at 99.85% — the ~3.9k differing
pixels were all antialiasing along the arcs. Close, but there is no reason to approximate
something we can copy exactly. Those numbers are recorded here only as documentation of the
shape; the mask file is the source of truth.

If the device or capture resolution ever changes, re-extract rather than editing constants:

    python3 -c "from PIL import Image; \\
      Image.open('a-live-screenshot.png').convert('RGBA').split()[3] \\
      .save('display-shape-mask.png', optimize=True)"
"""

import argparse
import pathlib
import sys

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required — see the module docstring for a venv one-liner.")

MASK_PATH = pathlib.Path(__file__).parent / "display-shape-mask.png"


def load_mask() -> Image.Image:
    if not MASK_PATH.exists():
        sys.exit(f"missing {MASK_PATH.name} — see the docstring for how to re-extract it")
    return Image.open(MASK_PATH).convert("L")


def shape(path: pathlib.Path, mask: Image.Image, out: pathlib.Path | None) -> pathlib.Path:
    im = Image.open(path).convert("RGBA")
    if im.size != mask.size:
        raise SystemExit(
            f"{path.name}: expected {mask.size[0]}x{mask.size[1]} but got "
            f"{im.size[0]}x{im.size[1]}. The mask only applies to the physical-resolution "
            "capture — run `adb shell wm size reset && adb shell wm density reset` and "
            "recapture, or re-extract the mask for the new size."
        )
    im.putalpha(mask)
    out = out or path.with_name(f"{path.stem}-shaped.png")
    im.save(out)
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("images", nargs="+", type=pathlib.Path)
    ap.add_argument("-o", "--output", type=pathlib.Path,
                    help="output path (only valid with a single input)")
    args = ap.parse_args()

    if args.output and len(args.images) > 1:
        return ap.error("-o only works with a single input image")

    mask = load_mask()
    for path in args.images:
        if path.stem.endswith("-shaped"):
            print(f"  skip {path.name} (already shaped)")
            continue
        print(f"  {path.name} -> {shape(path, mask, args.output).name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
