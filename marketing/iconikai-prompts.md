# IconikAI Prompts — Paste-Ready

Tool: https://www.iconikai.com/ (free start, $5-20 packs)
Goal: Generate 5-10 app icon variants for **ABC Phonics Kids** to A/B pick best.

---

## Step 1 — Setup on IconikAI

1. Sign up (Google login OK)
2. New Icon → "Generate with AI"
3. Paste **App name**: `ABC Phonics Kids`
4. Paste **App description** (short, 1 sentence):
   ```
   Phonics English learning app for kids ages 3-8 with 26 friendly mascots.
   ```
5. Generation count: try 4-6 per prompt to get variety

---

## Primary Prompts (try in order)

### Prompt 1 — Bee mascot + Letter A (PRIMARY)

```
A cheerful cartoon bee mascot character holding up a large yellow letter 'A'.
Bee has friendly smile, big round eyes, simple black-and-yellow stripes, small white wings.
Soft rounded squircle background in bright sunny yellow (#FFD93D).
Sky blue (#4D96FF) accent on the letter A with subtle gradient.
Flat design with very subtle shadow. No text. Centered composition with 10% safe-zone padding.
Friendly, modern, kid-safe style similar to Duolingo and Khan Academy Kids.
1024x1024.
```

### Prompt 2 — Open book + ABC letters (BACKUP)

```
Open book icon with colorful 3D letters A, B, C floating above its pages.
Letter A red, B green, C blue — all bright primary colors with friendly rounded edges.
Book has warm cream pages and brown spine, slightly tilted forward.
Soft yellow (#FFD93D) gradient background, rounded squircle shape.
Cute cartoon style, no realistic detail, flat with subtle shadow.
No text. Kid-friendly. Style reference: Lingokids, Reading Eggs.
1024x1024.
```

### Prompt 3 — Bee + Honeycomb Letters (CREATIVE)

```
Smiling bee mascot in center, surrounded by 4-6 small honeycomb hexagon cells
each containing a different cartoon letter (A, B, C, D).
Yellow honeycomb cells with thin gold outlines.
Background gradient from warm yellow (#FFD93D) at top to soft cream (#FFF9E6) at bottom.
Rounded squircle frame. No text. Flat design with subtle drop shadow.
Cheerful, playful, educational. Centered safe-zone for OS round mask.
1024x1024.
```

### Prompt 4 — Phonics mouth speech bubble (METHOD-FOCUSED)

```
Friendly cartoon mouth shape (no full face) speaking the letter 'A' inside a speech bubble.
Speech bubble is sky blue (#4D96FF) with white letter A bold inside.
Below: small phonetic sound wave decoration in mint green (#6BCB77).
Background: bright yellow (#FFD93D) rounded squircle.
Modern flat design, kid-safe, simple shapes, no text other than the letter A.
Style: educational, similar to Duolingo's owl simplification approach.
1024x1024.
```

### Prompt 5 — 3 Mascot Trio (BRAND ENSEMBLE)

```
Three small cartoon animal mascots peeking up from the bottom of the icon:
yellow bee on the left, brown bear in the center, white cat on the right.
All smiling, simple flat cartoon design, big round eyes.
Above them a single large playful letter 'A' in mint green (#6BCB77).
Bright yellow (#FFD93D) rounded squircle background with subtle radial gradient.
No text. Centered composition. Friendly, modern, like Khan Academy Kids or Lingokids.
1024x1024.
```

### Prompt 6 — Single Big Letter A + Bee buddy

```
Bold, friendly capital letter 'A' in the center taking 60% of the icon.
Letter A is rounded, bright sky blue (#4D96FF) with subtle gradient highlight.
A tiny yellow bee mascot perches on the top-left of the A, smiling.
Soft yellow (#FFD93D) gradient background, rounded squircle shape.
Flat design with very subtle shadow under the A.
No other text. Kid-safe, modern, like ABC Mouse or Hooked on Phonics modernized.
1024x1024.
```

### Prompt 7 — Star + Letters circle (REWARD VIBE)

```
Three colorful letters 'A B C' arranged in a small arc at the top half.
A bright gold star with cute eyes and smile in the center bottom.
Letters are: A red, B mint green, C sky blue — rounded chunky 3D-flat style.
Background: bright yellow (#FFD93D) rounded squircle with subtle sun-ray pattern.
Flat design, no text, kid-friendly, celebratory.
1024x1024.
```

### Prompt 8 — Headphones + ABC (AUDIO-FOCUSED)

```
Cute pair of kid headphones in pink (#FF8FA3) and white wrapping around three letters A B C.
Letters: A yellow, B green, C blue — rounded, cheerful.
A small music-note decoration floating beside the headphones.
Background: warm cream-to-yellow (#FFF9E6 → #FFD93D) gradient, rounded squircle.
Flat design, friendly cartoon, no text, modern educational style.
1024x1024.
```

---

## Negative Prompts (paste in "Avoid" / "Negative" field)

```
text, words, letters in title, realistic photo, scary, dark, gloomy,
adult content, complex composition, photorealistic, 3D rendering,
serious face, school chalkboard, classroom, generic stock icon,
crosshatch shading, gritty texture
```

---

## Style references (mention in description if IconikAI accepts)

> "Style similar to: Duolingo, Khan Academy Kids, Lingokids, Endless Alphabet.
> NOT like: Hooked on Phonics (too brand-locked), ABCmouse (too cluttered)."

---

## Brand constraints to enforce in every prompt

| Property | Value |
|---|---|
| Primary color | `#FFD93D` (yellow) — must be dominant 50%+ |
| Secondary | `#6BCB77` (mint green) or `#4D96FF` (sky blue) |
| Shape | Rounded squircle (Apple/Google guideline mask) |
| Text | NO text in icon (international + Apple Kids guideline) |
| Safe zone | 10% padding from edge (icon edges may get masked round) |
| Style | Flat or semi-flat, subtle shadow only |
| Mood | Cheerful, modern, kid-safe, educational |
| Mascot | Optional but consistent (bee if used) |

---

## Workflow after generation

**Step 1 — Generate** all 8 prompts (~30 min total, 4-6 outputs each = 32-48 candidates)

**Step 2 — Shortlist 5** based on:
- ✅ Reads clearly at 60×60 pixels (Android home screen size)
- ✅ No text visible
- ✅ Bright yellow dominant
- ✅ Friendly, not generic
- ✅ Distinguishable from competitor icons (Monkey Junior = white BG, Hooked on Phonics = red, ABCmouse = blue mouse)

**Step 3 — Test at sizes**:
Download 1024×1024 → manually scale to:
- 192 (Android xxxhdpi)
- 96 (Android xhdpi)
- 60 (Android home screen)
- 48 (Android list view)

Open all 4 sizes side-by-side. Pick icons readable at **48 pixel**.

**Step 4 — Polish in Figma** (top 2 picks):
- Adjust colors to exact brand hex
- Add subtle radial gradient if missing
- Verify centered composition
- Export final 1024×1024 PNG

**Step 5 — Generate Android adaptive** (Icon Kitchen):
- Upload polished PNG
- Separate foreground (bee + letter) from background (yellow)
- Download XML files → drop into `composeApp/src/androidMain/res/mipmap-anydpi-v26/`

**Step 6 — A/B test after launch**:
- Upload 2 finalists as variants in Play Console Store Listing Experiments
- Run 14 days minimum
- Pick variant with highest "store listing → install" conversion

---

## Quick paste-text for IconikAI form

If IconikAI has separate fields, here's the exact mapping:

**App name field**:
```
ABC Phonics Kids
```

**Category field** (if asked):
```
Education / Kids / Language Learning
```

**Description field**:
```
Phonics English learning app for kids ages 3-8 — alphabet, letter sounds, blending, reading games. 26 friendly mascots.
```

**Style preference**:
```
Friendly cartoon, flat design with subtle shadow, modern educational, like Duolingo + Khan Academy Kids
```

**Color palette field**:
```
Primary: yellow #FFD93D
Secondary: mint green #6BCB77
Accent: sky blue #4D96FF
```

**Mood**:
```
Cheerful, playful, safe, modern, educational
```

**Avoid**:
```
text, words, scary, dark, photorealistic, complex, school chalkboard
```

---

## Cost expectation

| Tier | What you get |
|---|---|
| **Free start** | 3-5 initial generations (often enough for 1-2 usable picks) |
| **$5 pack** | ~20 generations |
| **$20 pack** | ~80 generations + HD export |

Recommend: start free → if first 5 generations promising → upgrade to $5 pack for variety. **Don't exceed $20 — at that point Figma + Midjourney is cheaper**.
