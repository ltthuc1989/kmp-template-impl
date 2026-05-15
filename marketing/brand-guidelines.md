# ABC Phonics Kids — Brand Guidelines

Single source of truth for design, copy, and marketing assets.

## Logo

- **Primary mascot**: Bee 🐝 (carrying letter A)
- **Wordmark**: "ABC Phonics Kids" — font: Quicksand Bold (free Google Font)
- **Tagline (EN default)**: "Phonics English for ESL kids"
- **Tagline (VN)**: "Học phonics tiếng Anh cho bé"

## Color Palette

| Token | Hex | Usage |
|---|---|---|
| Primary Yellow | `#FFD93D` | Alphabet/sun/bee, CTA buttons, app icon BG |
| Mint Green | `#6BCB77` | Positive feedback, correct answer, success state |
| Sky Blue | `#4D96FF` | Trust signals, parent dashboard, info badges |
| Warm White | `#FFF9E6` | Light background, screenshot canvas |
| White | `#FFFFFF` | Card background, modal |
| Text Dark | `#1A1A1A` | Body text, headings |
| Text Muted | `#6B7280` | Secondary text, captions |
| Error Red | `#FF6B6B` | Wrong answer, warning (sparing use — kids prefer encouragement) |

## Typography

- **Headings**: Quicksand Bold 700 (24-32pt store assets, 20-28sp in-app)
- **Body**: Quicksand Regular 400 (16-18pt store assets, 14-16sp in-app)
- **App UI**: SF Pro (iOS) / Roboto (Android) — system default for native feel
- **Kids reading text**: Comic Neue or Andika (handwriting-style for letter shape clarity)

## Tone of Voice

- **Friendly, encouraging, never "schooly"** — kids learn through play, not pressure
- **Use kid pronouns**: "your child", "bé", "anak", "criança", "niño", "お子様", "자녀"
- **Emoji OK** but not overuse (1-2 per paragraph; 3-4 in long description sections)
- **Avoid jargon**: explain "synthetic phonics" via comparison ("the science of reading"), not academic terms
- **Active voice**: "Your child learns letter sounds" not "Letter sounds are learned"
- **Specific numbers**: "670+ words", "26 mascots", "5 levels" — concrete > vague

## Mascot Cast — 26 Phonics Friends (Aa-Zz)

| Letter | Mascot | Color theme |
|---|---|---|
| Aa | Angry Apple | Red + green |
| Bb | Big Bear | Brown + blue |
| Cc | Cool Cat | Black + white + sunglasses |
| Dd | Dancing Dog | Yellow + green |
| Ee | Eager Egg | Pale yellow + brown |
| Ff | Funny Fish | Orange + blue |
| Gg | Giggling Gorilla | Brown + pink |
| Hh | Happy Horse | Brown + white |
| Ii | Inquisitive Insect | Green + yellow |
| Jj | Jumping Jet | Silver + blue |
| Kk | Kicking Kangaroo | Tan + brown |
| Ll | Loyal Lion | Gold + orange |
| Mm | Mighty Monkey | Brown + red |
| Nn | Nimble Nut | Brown + green |
| Oo | Octopus | Purple + pink |
| Pp | Peachy Peach | Pink + green |
| Qq | Queenly Queen | Purple + gold |
| Rr | Racing Rabbit | White + pink |
| Ss | Splashy Seal | Gray + blue |
| Tt | Tiny Turtle | Green + brown |
| Uu | Useful Umbrella | Red + white |
| Vv | Vroom Van | Blue + white |
| Ww | Wise Wolf | Gray + white |
| Xx | Xtra Fox | Orange + white |
| Yy | Yawning Yak | Brown + tan |
| Zz | Zigzag Zebra | Black + white |

## Asset Naming Convention

```
icon-1024.png                          // App Store master
icon-512.png                            // Play Store
icon-{size}.png                         // sizes: 192, 180, 167, 152, 144, 120, 96, 72, 48
screenshot-{locale}-{slot}.png          // e.g. screenshot-vi-1.png (slot 1-8)
feature-graphic-{locale}.png            // 1024×500 Play Store feature graphic
promo-video-{locale}.mp4                // 1080×1920 vertical
```

## Forbidden / Trademark Avoidance

❌ Never use in title, description, screenshots, or icon:
- "Oxford Phonics" / "Oxford"
- "Hooked on Phonics"
- "Jolly Phonics" / "Jolly"
- "Reading Eggs"
- "Monkey Junior" / "Monkey"
- "Pinkfong" / "Baby Shark"
- "ABCmouse" / "Age of Learning"
- "Khan Academy"
- "Duolingo" / "Duo"
- "ELSA Speak"
- "Lingokids"
- "LittleFox"

✅ Safe to mention as method reference (NOT brand):
- "synthetic phonics"
- "UK National Curriculum" (educational standard, public)
- "US Common Core" (educational standard, public)

## Imagery Rules (for AI gen + Figma)

- **Bright, saturated** colors (kids prefer high contrast)
- **Friendly cartoon style** — flat or semi-flat, no realistic 3D
- **Rounded shapes** (kids feel safer)
- **Single focal point** per image (kids can't process busy compositions)
- **Diverse representation**: avoid showing kids' faces (privacy + universal appeal); show hands, mascots, objects instead
- **No text in app icon** (Apple guideline + bad for international)
- **Mascots always facing forward** with eye contact (engagement)

## Accessibility Floor

- **WCAG AAA contrast** on text (7:1 ratio for body, 4.5:1 for large)
- **Touch targets ≥ 64dp** in app (kids motor skills — bigger than Apple HIG 44pt)
- **Body text ≥ 20sp** in-app (kids small fingers + still learning to read)
- **Heading text ≥ 28sp** in-app
- **High-contrast mode** support (testing tool: macOS Display preferences)
