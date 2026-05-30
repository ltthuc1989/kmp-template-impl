# Feature Graphic Prompts — Paste-Ready

**Target**: Google Play Store Feature Graphic — 1024×500px (~2:1 ratio)
**Apple**: KHÔNG cần feature graphic — Apple dùng screenshots + App Preview video thay vì banner.

**Workflow**:
1. Generate raw 16:9 banner qua Gemini (NO text)
2. Crop top/bottom trong Figma → 1024×500
3. Overlay app icon + locale-specific tagline trong Figma (8 locales)
4. Export 9 PNGs → upload Play Console per locale

**Composition rule**: LEFT 40% chừa trống cho text overlay sau. RIGHT 60% là visual.

---

## Setup chung (paste mỗi prompt)

```
=== BASE CONSTRAINTS ===

Wide horizontal banner for Google Play Store feature graphic.
16:9 landscape composition (will be cropped to 1024×500 later).
IMPORTANT: leave the LEFT 40% of the banner relatively empty/simple — this space
will be overlaid with app name text and tagline later.
Put visual focus on the RIGHT 60% of the composition.

Bright sunny yellow (#FFD93D) gradient background.
Cheerful kid-safe cartoon style, flat design with soft shadows.
NO text, NO words, NO letters in title/heading style — only abstract decorative
letters (A, B, C) as visual elements.
Style similar to Duolingo and Khan Academy Kids banners.

=== AVOID ===
Avoid: any text/words/heading in the image, realistic photo, dark or scary mood,
complex cluttered composition, 3D realistic rendering, school chalkboard imagery,
anything that fills the LEFT side of the banner.
```

---

## 7 Concept Prompts

### Concept 1 — Mascot Lineup (RECOMMENDED first try)

```
[BASE]

Right side: a horizontal lineup of 5 cute cartoon animal mascots (yellow bee,
brown bear, white cat, brown dog, green frog), all smiling and looking at the
viewer. Above them, 3 large playful letters 'A B C' floating in different bright
colors (red, mint green, sky blue). Small sparkle decorations around the letters.

[AVOID]
```

### Concept 2 — Reading Scene

```
[BASE]

Right side: an open storybook with warm cream pages tilted slightly. 3-4 small
cartoon mascots (bee, bear, cat) gathered around the book, looking excited and
pointing at the pages. Floating letters A, B, C rising out of the book pages
like magic, in bright colors. Soft warm lighting suggesting reading time.

[AVOID]
```

### Concept 3 — Sky with Floating Letters

```
[BASE]

Right side: bright cartoon sky scene with fluffy white clouds. Colorful 3D-flat
letters A through G floating at different heights, each a different bright color.
A small yellow bee mascot flying happily between the letters. A subtle rainbow
arc in the background.

[AVOID]
```

### Concept 4 — Letter Adventure Path

```
[BASE]

Right side: a winding cartoon path with large colorful letter blocks (A, B, C,
D, E) acting as stepping stones. Two small mascots — a yellow bee and a smiling
cat — walking along the letter path. Green grass and tiny flowers around the
path. Warm friendly atmosphere.

[AVOID]
```

### Concept 5 — Festival Balloons

```
[BASE]

Right side: celebration scene with 3-4 mascots (bee, bear, cat, dog) holding
strings of letter-shaped balloons (A, B, C, D in different bright colors).
Confetti and sparkles around them. Joyful celebration mood. Mascots are dancing
or jumping happily.

[AVOID]
```

### Concept 6 — Letter Garden

```
[BASE]

Right side: a cartoon garden where the letters A, B, C, D grow out of the ground
like colorful plants. A friendly yellow bee mascot is tending to the letter-plants
with a tiny watering can. Small flowers and butterflies decorate the scene.
Soft pastel green grass at the bottom.

[AVOID]
```

### Concept 7 — Device Peeking

```
[BASE]

Right side: a cartoon smartphone tilted at 15-degree angle showing a blank yellow
screen. 3-4 cartoon mascots (bee, bear, cat) peeking out from behind and around
the phone, smiling. Letters A, B, C floating beside the phone with subtle motion
lines. Phone has rounded squircle screen, no text inside.

[AVOID]
```

---

## Workflow tận dụng Gemini Pro (gemini.google.com)

**Phase 1 — Script batch** (~12 min free tier API):
```bash
cd /Volumes/Entertainment/GeminiGenerator/opw_audio_project
source venv/bin/activate
python scripts/generate_feature_graphic.py
```
Output: 28 variants (7 concepts × 4) tại `output/feature_graphic/{timestamp}_batch/`

**Phase 2 — Refine top picks** trong [gemini.google.com](https://gemini.google.com/) Pro:
- Upload 1 variant ưng
- Conversational edit:
  - `"Move all mascots 20% to the right, keep left side empty"`
  - `"Make the yellow background brighter and more vibrant"`
  - `"Replace the frog with a smiling fox"`
  - `"Add 3 small floating stars in the upper-right corner"`
- Iterate đến khi ưng

**Phase 3 — Crop + overlay text** (Figma):
1. Import refined PNG vào Figma
2. Create frame 1024×500
3. Position image (crop top + bottom ~38px mỗi cạnh để fit)
4. Add overlay layer (LEFT 40%):
   - App icon 192×192
   - Tagline H1 per locale (vd VN: "Học phonics tiếng Anh cho bé")
   - Subhead optional
   - Badges: "FREE • COPPA-safe • Offline"
5. Use Figma Variables cho 8 locales → bulk export

**Phase 4 — Per-locale tagline** (đã có trong [brand-guidelines.md](brand-guidelines.md)):

| Locale | Tagline overlay |
|---|---|
| 🇻🇳 vi | "Học phonics tiếng Anh cho bé" |
| 🇮🇩 id | "Belajar phonics Inggris untuk anak" |
| 🇹🇭 th | "เรียน phonics ภาษาอังกฤษเด็ก" |
| 🇧🇷 pt-BR | "Phonics inglês para crianças" |
| 🇲🇽 es-MX | "Phonics inglés para niños" |
| 🇯🇵 ja | "子供向けフォニックス英語" |
| 🇰🇷 ko | "유아 영어 파닉스 학습" |
| 🇹🇷 tr | "Çocuklar için İngilizce phonics" |
| en-US | "Phonics English for ESL kids" |

---

## Where to edit prompts in script

File: [scripts/generate_feature_graphic.py](/Volumes/Entertainment/GeminiGenerator/opw_audio_project/scripts/generate_feature_graphic.py)

| Section | Lines | Edit để... |
|---|---|---|
| `BASE_CONSTRAINTS` | 38-54 | Đổi rule common (palette, style, left-zone reservation) |
| `CONCEPTS` list | 56-118 | Thêm/sửa/xóa concept prompts |
| `NEGATIVE_HINT` | 120-124 | Đổi avoid list |

---

## Cost & timing

| Tier | 28 variants (7 concepts × 4) | 4 variants (1 concept) |
|---|---|---|
| Free tier API | $0, ~12 min | $0, ~2 min |
| Paid tier API | $0.56 ($0.02/img Imagen Fast), ~1 min | $0.08, ~10s |
| Pro chat (manual) | Free with Pro $20/mo, ~30 min | Free, ~5 min |

**Recommend**: Script batch (free API tier) → Pro chat refine top picks.
