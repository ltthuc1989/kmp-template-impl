# Asset Pipeline — Tools + Workflow

End-to-end production guide for app icon, screenshots, feature graphic, promo video.
Budget: **~$40**, **~24 hrs work** (3-4 days part-time).

---

## 1. App Icon

### Tools

| Tool | URL | Pricing | Use |
|---|---|---|---|
| **IconikAI** ⭐ | https://www.iconikai.com/ | Free start, $5-20 packs | AI app-icon-specialized — store-ready safe zone + sizing |
| Appicons.ai | https://appicons.ai/ | Free start | Backup AI icon generator |
| Recraft.ai | https://www.recraft.ai/generate/icons | Free tier | Style transfer with reference image |
| Icon Kitchen | https://icon.kitchen/ | Free | Android adaptive icon export (foreground + background XML) |
| Figma | https://figma.com/ | Free | Polish step after AI gen |

### Workflow

**Step 1 — Generate variants** (IconikAI, ~30 min):

Prompt:
```
Cheerful app icon for kids phonics English learning app.
Bee mascot (yellow + black stripes) holding letter 'A' in front of mouth.
Bright yellow (#FFD93D) circular background with subtle radial gradient.
Sky blue (#4D96FF) accent on letter A.
Rounded square (Apple/Google guideline), flat design with subtle drop shadow.
No text. Friendly cartoon style. Kid-safe, no scary elements.
1024×1024 PNG, transparent corners.
```

Generate 8-10 variants. Pick 3 best.

**Step 2 — Refine in Figma** (~2 hrs):
- Adjust colors to brand palette
- Add subtle gradient if missing
- Verify safe zone (10% padding from edge — content not clipped on round/squircle masks)
- Export master 1024×1024 PNG

**Step 3 — Generate Android adaptive** (Icon Kitchen, ~30 min):
- Upload master 1024×1024
- Separate foreground (bee + A) from background (yellow circle)
- Download `ic_launcher.xml` + `ic_launcher_foreground.xml` + `ic_launcher_background.xml`
- Replace files in `composeApp/src/androidMain/res/mipmap-anydpi-v26/`

**Step 4 — Export iOS sizes** (Figma + plugin):
- Use Figma plugin "App Icon Generator" or manually export:
  - 1024 (App Store)
  - 180 (iPhone @3x)
  - 167 (iPad Pro @2x)
  - 152 (iPad @2x)
  - 120 (iPhone @2x)
- Drop into `composeApp/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`

**Step 5 — A/B test** (after launch, ≥ 14 days):
- Play Console → Store Listing Experiments — 3 icon variants
- Pick winner with higher conversion rate

---

## 2. Screenshots (8 locales × 8 screens = 64 outputs)

### Tools

| Tool | URL | Pricing | Use |
|---|---|---|---|
| **Figma master + Screenshots Pro** ⭐ | https://screenshots.pro/ | $19 one-time | Localization-at-scale, CSV import → batch 64 outputs |
| AppLaunchpad | https://theapplaunchpad.com/ | Free tier (watermark), $20-30/mo | 1000+ templates, multi-locale built-in |
| AppMockUp Studio | https://app-mockup.com/ | Free | Cleaner UI, free fully |
| AppScreenStudio | https://www.appscreenstudio.com/en | Free | "2-minute screenshots" |
| Figma alone | https://figma.com/ | Free | Full control, manual locale toggle (slow for 64) |

### Workflow

**Step 1 — Capture real app screens** (1 hr):
- Run app on Android emulator (Pixel 6 Pro, 1440×3120 → Play Store accepts ≥ 1284×2778)
- Capture 8 screens per locale (Hook, Method, Mascots, Game, Coverage, Stories, Parent, CTA)
- Save raw: `marketing/store-listing/{locale}/raw-{1-8}.png`

**Step 2 — Design Figma master** (4 hrs):

File structure:
```
Frame: 1290×2796 (iPhone 6.7" / Android equivalent)
├─ Background gradient layer (locked, #FFD93D → #FFF9E6)
├─ Decorative shapes (locked)
├─ Headline text (variable — "Bé đọc tiếng Anh sau 4 tuần" / "Read in 4 weeks" / etc.)
├─ Subhead text (variable)
├─ Device frame (Pixel 6 mock, 1080×2400 inset)
└─ App screen image (from raw-{slot}.png, 1080×2400)
```

Use **Figma Variables** for text layers:
- Create variable collection "Screenshot Copy"
- Modes: vi-VN, id-ID, th-TH, pt-BR, es-MX, ja-JP, ko-KR, tr-TR, en-US
- For each slot 1-8 × each locale = string value

**Step 3 — Bulk export** (option A: Figma manual, 3 hrs):
- Toggle mode → export all 8 slots → 8 minutes per locale × 9 locales = 72 min
- Naming: `screenshot-{locale}-{1-8}.png`

**Step 3 — Bulk export** (option B: Screenshots Pro $19, 30 min total):
- Export Figma master 8 slots → upload to Screenshots Pro
- Upload CSV with locale × slot text mapping (64 rows)
- Generate all 64 outputs → ZIP download

**Step 4 — Per-locale review** (1 hr):
- Verify Thai/JP/KR text overflow (Thai needs more vertical space)
- Check text contrast on busy backgrounds
- Native speaker review priority locales (VN, JP)

**Step 5 — Upload to Play Console**:
- Per locale → Store presence → Main store listing → Phone screenshots
- Min 2, max 8 screenshots per locale

---

## 3. Feature Graphic (Play Store 1024×500)

### Tools

| Tool | Pricing | Use |
|---|---|---|
| **Figma** ⭐ | Free | 1 master + locale text variants |
| Canva | Free tier | Play Store template gallery |
| Mediamodifier | $9-29/mo | Paid template option |

### Workflow

**Step 1 — Design master in Figma** (2 hrs):

Layout:
```
┌─────────────────────────────────────────────────────────────┐
│  ┌──────────┐                                                │
│  │   ICON   │   {locale-specific tagline H1}                 │
│  │   192×   │   {locale-specific subhead}                    │
│  │   192    │                                                │
│  └──────────┘   FREE • COPPA-safe • Offline                  │
│                                                              │
│              [3 mascot lineup: Bee + Cat + Bear]            │
│              [floating letters: A B C]                       │
└─────────────────────────────────────────────────────────────┘
Background: gradient #FFD93D → #FFF9E6
```

Use Figma variables for tagline text per locale.

**Step 2 — Export per locale** (15 min):
- Toggle variable mode → export 9 PNGs
- Naming: `feature-graphic-{locale}.png`
- Upload to Play Console per locale

---

## 4. Promo Video (15-30s, optional — boosts conversion ~20%)

### Tools

| Tool | Pricing | Use |
|---|---|---|
| **CapCut** ⭐ | Free | Easy mobile/desktop video edit |
| DaVinci Resolve | Free | Pro-grade if learning curve OK |
| ElevenLabs voiceover | Free tier, $5/mo Starter | Per-locale voiceover |
| Pixabay Music | Free | Royalty-free BGM |
| Mixkit | Free | Royalty-free BGM |

### Script (30s, per locale)

```
0-3s   Hook: "Bé đọc tiếng Anh sau 4 tuần?"
       (close-up of kid hand tapping screen, app icon overlay)

3-12s  Demo cuts (2-3s each):
       - Sound Intro screen (letter A bouncing + sound wave)
       - Identify game (kid taps correct emoji, mascot celebrates)
       - Tracing canvas (finger draws letter)
       - Story screen (word highlights as audio plays)

12-22s Mascots reveal + level map:
       - "26 nhân vật" lineup pan
       - "5 cấp độ" map zoom

22-30s CTA:
       - "Miễn phí Cấp 1"
       - App icon + Play Store badge
       - End frame: "ABC Phonics Kids"
```

### Workflow

**Step 1 — Screen recording** (1 hr):
- Android Studio emulator → ADB screen record OR
- Real device → built-in screen recorder
- Capture 4 step types × 3-5 sec each at 60fps

**Step 2 — Edit in CapCut** (2 hrs):
- Import clips
- Add background music (Pixabay "Happy Kids" tracks)
- Add text overlays per script
- Add app icon transitions
- Export 1080×1920 H.264 MP4

**Step 3 — Voiceover per locale** (1 hr per locale):
- ElevenLabs: pick child-friendly voice
- Generate VO script (Vietnamese, Indonesian, etc.)
- Replace audio track in CapCut

**Step 4 — Upload** (15 min):
- Upload to YouTube as **unlisted**
- Copy URL → paste in Play Console → Store listing → Video URL

---

## 5. Bundle Cost + Timeline

| Asset | Tool | Cost | Time |
|---|---|---|---|
| App icon | IconikAI + Figma + Icon Kitchen | $0-20 | 4-6 hrs |
| Screenshots (9 locales × 8) | Figma + Screenshots Pro | $19 | 8-10 hrs |
| Feature graphic | Figma | $0 | 2-3 hrs |
| Promo video | CapCut + ElevenLabs (use existing $10 budget) | $0 | 4-6 hrs (single locale) + 1 hr/extra locale |
| **TOTAL** | — | **~$40** | **~24 hrs core + 8 hrs/extra locale video** |

## 6. Execution checklist

- [ ] Day 1: Generate app icon variants on IconikAI, pick 3 best
- [ ] Day 1: Polish top pick in Figma → export 1024 + Android adaptive XML
- [ ] Day 2: Capture 8 raw screens trên emulator
- [ ] Day 2: Build Figma screenshot master with locale variables
- [ ] Day 3: Pay $19 Screenshots Pro → batch export 64 screenshots
- [ ] Day 3: Build feature graphic master + export 9 locales
- [ ] Day 4: Record promo video (VN first), edit, voiceover, upload YouTube
- [ ] Day 4: Upload all assets to Play Console VN locale → submit Internal Test
- [ ] Day 5-7: Other locales follow

## 7. QA before submit

- [ ] App icon readable trên white, dark, OS launcher backgrounds
- [ ] Screenshots text không truncate trên 5.5" preview (smallest target)
- [ ] Screenshots text readable ở 50% zoom (Play Store list view)
- [ ] Feature graphic readable ở 50% size
- [ ] Promo video first 3s convey value MUTED (Play Store autoplays muted)
- [ ] Native speaker QA mỗi locale (Fiverr $5-15/locale if no community tester)
