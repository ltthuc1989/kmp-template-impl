# Raw app captures — Level 2

Unframed device captures. Same style as the 7 screenshots currently live on Play Store:
full screen including status bar and gesture bar, no Figma frame and no headline overlay.

| File | Screen | Level / Unit / Step |
|---|---|---|
| `l2-u2-step0-blending.png` | Vowel blend — `a + g = ag`, `b + ag = bag` | L2 / Unit 2 (`ad ag ap at`) / step 0 |
| `l2-u2-step6-wordtracing.png` | Whole-word tracing — word `dad`, letter 2 of 3 active | L2 / Unit 2 / step 6 |
| `l2-unit-selection.png` | Unit selection — all 8 Level 2 units with `short a/e/i/o/u` badges | L2 / unit list |

**Use the `-shaped.png` variants, not these.** The live set is clipped to the display
shape — rounded corners and a transparent camera cutout — and a square-cornered screenshot
sitting next to them in the listing is obvious immediately. Both CLI capture paths were
checked and neither applies it:

| Capture path | Output |
|---|---|
| `adb exec-out screencap` | 1080×2424, opaque, square corners |
| emulator console `screenrecord screenshot` | 1080×2424, opaque, square corners |
| Android Studio → Running Devices → Screenshot | clipped to display shape — this is what the live set used |

So either capture through Android Studio, or capture via adb and run:

```bash
python3 ../apply-display-shape.py raw/*.png
```

That applies `display-shape-mask.png`, the alpha channel lifted out of a live store
screenshot. Output alpha is an exact match with the live set — verified pixel for pixel,
not approximated.

All files are 1080×2424, matching the live set.

## Capture conditions

| | |
|---|---|
| Device | Pixel 9a emulator (`sdk_gphone16k_arm64`, Android 17) |
| Resolution | 1080×2424 — the panel's **physical** size |
| Density | 420 — the panel's **physical** density |
| Build | `com.beely.phonicskids.debug` |
| Captured | 2026-08-17 (unit list: 2026-08-18) |

**The emulator ships with overrides that break this.** Before capturing, clear them or the
output is 1080×2400 at density 450 — 24px short and ~7% larger UI, so it will not match the
live set:

```bash
adb shell wm size reset && adb shell wm density reset
```

## Reproducing

1. Reset size and density as above, launch the app.
2. Home → **Short Vowels** → **Unit 2 (`ad ag ap at`)** → **Lesson 1: ad ag**.
   Unit 2 is inside the free zone (`FREE_UNITS_PER_LEVEL = 2`), so no purchase is needed,
   but Unit 1 must be completed first for the sequential gate to open.
3. Step 0 opens directly. The blend animation cycles through the lesson's four words and
   auto-advances, so the frame showing **both** progress bars filled is transient — tap a
   word dot to restart the cycle and burst-capture rather than trying to time one shot.
4. Tap the last segment of the top progress bar to jump to step 6.
5. Trace the first letter so the word row shows mixed state (one letter done, one active,
   one pending) — that is what makes it read as *word* tracing rather than letter tracing.
6. Run `apply-display-shape.py` on the results unless they came out of Android Studio.

## Why these screens

The first two are the only things Level 2 does that Level 1 cannot, so they are the screens
that justify Level 2 existing:

- **Blending** — the two-stage build (`a + g = ag`, then `b + ag = bag`) is the moment a kid
  stops recognising word shapes and starts decoding.
- **Whole-word tracing** — Level 1 traces one letter; Level 2 traces the whole word, and the
  per-letter progress row is what shows it.

The third is scope rather than mechanic — the unit list is the one frame that shows how much
Level 2 contains, with the short-vowel grouping legible at a glance.

## Not store-ready yet

These are raw captures for reference and for dropping into a frame later. Against the
current live set, two issues carry over:

- Roughly the bottom third is empty in the two step captures, so at Play Store list-view
  scale (~50%) little is legible. The unit list is the exception — it fills the frame.
  [asset-pipeline.md](../../asset-pipeline.md) specifies a Figma frame with a headline over
  each capture; that step has never been run for any screenshot.
- The status bar clock and system icons are visible, same as the live set.

Aspect ratio is 1:2.24, which exceeds the 2:1 shape Play prefers. The live set proves upload
is accepted, but confirm in Play Console before relying on it for a featuring slot.
