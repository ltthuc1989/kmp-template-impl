# Play Store Listing — Default English (en-US)

⚠️ **Mốc 4 draft — adds Level 4 and Level 5.** Diff against [live.md](live.md) before
pasting into Play Console. Refresh that snapshot first:
`python3 marketing/store-listing/fetch-live.py en-US`.

**Live is the Mốc 3 copy (L1–L3)** — `live.md` fetched 2026-10-04, store "Updated on"
Sep 24 2026; its long description matches the Mốc 3 draft word for word. The short
description went live without `, no ads`, so this draft keeps that. What changes now: the
`LEVEL 4` and `LEVEL 5` blocks, a `5 levels` bullet, and `24 stories` → `40 stories` in the
short description, the opening line and `WHAT KIDS LEARN`. Everything else is untouched.

L4 and L5 blocks follow the same rule as L2/L3 below: every sound sits next to a word that
contains it, every word is verified present in that level of `curriculum.json`.

**Mốc 4 round 2 (owner, 2026-10-04): L4 and L5 say what the child reads, not the rule.**
The first L4/L5 draft explained the spelling rules (`fr in frog`, `sh in fish`,
`c in cat and city`, word endings); the owner found that hard to follow. L4/L5 now list
example words only, plus the faded silent-letter line. **L1–L3 and every other section are
unchanged** — same text as live.

**The minimal-diff policy is retired.** Mốc 2 kept live wording wherever possible. That
policy preserved a real defect: the copy names sounds instead of pointing at them, so a
parent who does not already read English aloud cannot understand it. Fixing that is not a
line edit, so this is a full rewrite. Every claim is still verified against the code — see
"Verified features".

## The rule this rewrite follows

**You cannot write a sound. You can point at a word that contains it.**

Anchoring each sound to a keyword (`a as in cat`) is how every phonics program teaches, and
it is also the only way a store listing can convey a sound to a reader who cannot yet
pronounce the examples. Three consequences:

1. **No sound is ever named without a word next to it.** Not "short a" — "the a in cat".
2. **No demonstration that requires hearing.** `cap → cape` is invisible to a reader who
   cannot pronounce either word. Cut entirely; the words kids can read do the selling.
3. **Every bullet stands alone.** People skim store listings. A line that only makes sense
   if you remember an earlier line is a broken line.

Also cut: `phoneme-grapheme correspondences`, `decoding`, `synthetic phonics`, `word
families`, `split digraph`. All correct, all teacher-training vocabulary. Note especially
that a layperson reads **synthetic** as "artificial" — the exact opposite of what it means
(synthesising sounds into a word). It was actively working against us.

## App title (16 chars) — UNCHANGED

```
ABC Phonics Kids
```

## Short description (59 chars) — CHANGED (24 → 40 stories)

```
Learn to read English sound by sound — ages 3-8, 40 stories
```

`Phonics` is dropped here because the app title already carries it and Play indexes title +
short description together. `sound by sound` is what the app actually does, in words a
parent uses.

## Long description

```
Phonics Kids — kids 3-8 learn to read English, one sound at a time. No ads, ever. 5 levels, 488+ words, 40 stories, 6 mini-games.

Reading English starts with sounds, not spellings. Phonics Kids teaches your child the sound each letter makes, then how to push those sounds together into a word — the way schools in the UK and US teach reading. The goal is a child who can work out a word nobody has read to them first. Built for preschool, kindergarten and first-grade readers, at home or learning English as a second language.

🌟 WHAT KIDS LEARN
✓ 5 levels — from A to Z all the way to words like television
✓ 488+ words, every one read aloud
✓ 40 stories — each word lights up as the narrator says it

📖 LEVEL 1: THE ALPHABET
✓ All 26 letters, A to Z
✓ Not just the ABC song — kids learn the sound each letter makes
✓ Kids trace each letter and the app checks every stroke
✓ 8 stories built from the letters just learned

📖 LEVEL 2: SHORT VOWELS
✓ The a in cat, the e in bed, the i in big, the o in hot, the u in cup
✓ Kids say the sounds and push them together out loud: c - a - t, cat
✓ Change one letter, read the whole group: cat, hat, bat, mat
✓ Kids trace the whole word — "cat", not just "c"
✓ 8 new stories, every word already learned

📖 LEVEL 3: LONG VOWELS
✓ Kids read longer words: cake, home, happy, blue, moon
✓ One sound, several spellings — rain or day, kids read both right
✓ 96 new words across 24 lessons
✓ 8 new stories — kids read them on their own

📖 LEVEL 4: BLENDS & DIGRAPHS
✓ Kids read trickier words: frog, snake, fish, three, splash
✓ 96 new words across 24 lessons
✓ 8 new stories

📖 LEVEL 5: LETTER COMBINATIONS
✓ Kids read long words: banana, umbrella, television
✓ Words with letters you don't say, like knife and write — the app fades those letters to help
✓ 96 new words across 24 lessons
✓ 8 new stories

🎮 6 MINI-GAMES per unit
Bubble Pop • Memory Match • Fill Letter • Pick Word • Spell Letters • Drag Words

📚 HOW IT TEACHES
One sound at a time, in the order schools use. Kids learn the sound each letter makes, then push those sounds together into a word. Nothing is memorised by its shape — every word is worked out. It is the method behind the UK National Curriculum and US Common Core.

👨‍👩‍👧 KID-SAFE
✓ No ads — no banners, no video ads, ever
✓ No sign-up, no email, no account
✓ No personal data collected (COPPA)
✓ Works offline after a one-time download
```

## Why each section changed

| Section | Before | Now | Reason |
|---|---|---|---|
| Opening | `the ad-free way for kids 3-8 to learn to read English with phonics` | `kids 3-8 learn to read English, one sound at a time` | Play truncates after ~80 chars; the first line must sell alone. `one sound at a time` says the method without naming it |
| Intro | `through the proven phonics method` + `blend sounds into real words` | `Reading English starts with sounds, not spellings` + `push those sounds together` | `proven` is unfalsifiable filler; `blend` is the technical term for pushing sounds together, so it is said the long way once instead |
| Intro close | *(none)* | `work out a word nobody has read to them first` | This is the plain-English definition of **decoding** — the actual outcome a parent is buying. Says it without the word |
| WHAT KIDS LEARN | `488+ vocabulary words with audio` | `488+ words, every one read aloud` | `vocabulary` and `audio` are both replaceable with words a parent already says |
| LEVEL 1 | `Letter sounds, not just letter names — where real decoding starts` | `Not just the ABC song — kids learn the sound each letter makes` | Sound-vs-name is the most important distinction in early phonics, but a parent does not need to hold it as a concept. The ABC song is the thing every parent already recognises as "knows the letters but cannot read yet" — it names the gap without teaching a distinction |
| LEVEL 2 | `Short a, e, i, o, u — the sounds that unlock real reading` | `The a in cat, the e in bed, the i in big, the o in hot, the u in cup` | `short a` is teacher shorthand and means nothing to a parent. Five keyword anchors, all five words verified present in L2 |
| LEVEL 2 | `25 word families (-am, -an, -at, -ig, -op, -ug and more)` | `Change one letter, read the whole group: cat, hat, bat, mat` | `word family` is jargon and the hyphenated rimes are noise. The rewrite *demonstrates* onset substitution instead of naming it |
| LEVEL 3 | `Magic e — cap into cape` + `Words stay whole — the app lights up the part being sounded out` | `Kids read longer words: cake, home, happy, blue, moon` + `One sound, several spellings — rain or day` | See "The rule this rewrite follows". The second old line described a UI mechanism from the code's point of view, not a benefit |
| PHONICS METHOD | `synthetic phonics`, `phoneme-grapheme correspondences`, `decoding` | plain restatement, standards claim kept | Retitled `HOW IT TEACHES`. `Nothing is memorised by its shape` is the sharpest line here: it is the direct contrast with whole-word apps, which is the competitive wedge |
| KID-SAFE | `100% COPPA-compliant` listed first | `No ads` first, COPPA last | Reordered by what a parent actually decides on. `rewarded ads` → `video ads` |

**`488+ words` stays** — kept at the owner's direction after the risk below was raised.

**Kept verbatim** — the `🎮` game list, and `Works offline after a one-time download`.

## Known risk, accepted by the owner

`488+ words` counts word **entries** across all five levels. Since Mốc 4 every level ships
(`LevelRepository.kt:39` → `LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3", "L4", "L5")`), so
nothing behind the number is "Coming Soon" any more. The only gap left is repeats: 488
entries are **426 unique words** (62 appear in more than one level).

Decision: keep `488+` (owner, Mốc 3). The risk is now much smaller than when it was
accepted (264/488 reachable then). If it ever needs to change, `420+ words` is the strictly
defensible number; edit the opening line and the `✓ 488+ words, every one read aloud`
bullet.

## Other known-inaccurate line kept

- Nothing in the copy discloses what the in-app purchase unlocks (2 free units per level,
  then a one-time per-level purchase). Users hit the paywall unannounced. Revisit if 1-star
  reviews cite the paywall.

## Target keywords

| Keyword | Where it lives now |
|---|---|
| `phonics kids` | Title (`ABC Phonics Kids`) |
| `learn to read English` | Short description + intro |
| `letter sounds` | LEVEL 1 bullet 2 |
| `short vowels` / `long vowels` | Level block headings |
| `blends and digraphs` | LEVEL 4 heading |
| `silent letters` | LEVEL 5 bullet 2 |
| `sound out words` / `blend sounds` | Intro + LEVEL 2 (`push those sounds together`) |
| `phonics for kids` | Body |
| `ESL kids reading` | Intro (`learning English as a second language`) |
| `preschool` / `kindergarten` | Intro |

The rewrite trades some exact-match keyword density for comprehension. Title still carries
`Phonics`; the level headings still carry `short vowels` and `long vowels`; and Play ranks
partly on conversion, which unreadable copy suppresses.

## Verified features (audit 2026-10-04)

| Claim in the copy | Source of truth |
|---|---|
| 5 levels | `curriculum.json` L1–L5; `LevelRepository.kt:39` launches L2–L5 (L1 always open) |
| 488+ words | `curriculum.json` — 488 word entries across all 5 levels (426 unique), all reachable |
| 40 stories | `stories/level_1.json` … `level_5.json`, 8 each, 32/32 scenes with `word_timings` in every level |
| words light up as narrated | `step/common/KaraokeText.kt` |
| television (5 levels bullet) | L5 U8 |
| 26 letters, A to Z (L1) | `curriculum.json` — L1 `"The Alphabet"`, 26 lessons |
| tracing + stroke check (L1) | `step/tracing/TracingScorer.kt` (75% threshold) |
| cat / bed / big / hot / cup (L2) | all five verified present in L2 `curriculum.json` |
| cat, hat, bat, mat (L2) | L2 `-at` family = `bat cat hat mat rat` |
| whole-word tracing (L2, L3) | `step/wordtracing/`, routed in `StepScreen.kt` for every level except L1 |
| cake, home, happy, blue, moon (L3) | all five verified present in L3 `curriculum.json` |
| rain, day — one sound, two spellings (L3) | L3 U4 `ai` (rain) and `ay` (day), both `soundSpelling: "aaay"` |
| frog / snake / stop (L4) | L4 U2 `fr`, U3 `sn`, U3 `st` |
| fish / lunch / three (L4) | L4 U4 `sh`, `ch`, U5 `th` |
| string, splash (L4) | L4 U7 `str`, `spl` |
| c in cat/city, g in goat/giraffe | cat, goat in L1; city, giraffe in L4 U8 (`C`, `G` lessons) |
| car, bird, nurse, house, boy, draw (L5) | L5 U1 `ar ir ur`, U2 `ou oy`, U3 `aw` |
| knife / write / lamb, shown faded (L5) | L5 U7 `KN-WR`, `MB-VE`; faded via `SILENT_ALPHA = 0.4f` in `step/vowelblend/ClusterBlendContent.kt` |
| tiger, banana, umbrella, television (L5) | L5 U5–U6, U8 |
| picture, station, beautiful (L5) | L5 U8 `TURE`, `TION`, `FUL` |
| 96 words, 24 lessons (L3, L4, L5) | `curriculum.json` — 8 units × 3 lessons each, 96 unique per level |
| 6 mini-games per unit | `game/GameRegistry.kt` → `DEFAULT_UNIT_GAMES` |
| L4, L5 content shipped | `content_manifest.json` packs L4U3–U8, L5U3–U8 on CDN; U1–U2 bundled in `files/audio/level_4|5/` |
| L4, L5 purchasable | `SubscriptionPlan.kt:27-28` → `phonics_level_4`, `phonics_level_5` |
| ad-free | no AdMob/AppLovin in `gradle/libs.versions.toml`; store shows no "Contains ads" badge |

**Reference data no longer claimed in the copy** (kept in case it returns): 25 word families
in L2; 18 long-vowel spellings in L3 (4 split digraphs + 14 vowel teams).

## Do NOT claim

- ❌ "100% free" / "no in-app purchases" — IAP is live and the store badge shows it.
- ❌ Unqualified "works offline" — audio must be downloaded per level first.
- ❌ "every word has a picture" — 82 of 96 L3 words are emoji-only; just 14 have WebP art.
- ❌ "every silent letter is faded" everywhere — fading is in the Level 5 sound-blending step only.
- ⚠️ "488+ words" is kept by owner decision; 426 unique.

## Action items

- [x] Ads removed from code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (verified live — no badge)
- [x] Pricing: Free with in-app purchases (verified live — badge shown)
- [x] Mốc 3 copy (L2 + L3 + rewrite) pasted — verified live 2026-10-04
- [ ] Paste this draft into Play Console → en-US (adds L4 + L5, 40 stories)
- [ ] Re-run `fetch-live.py` after publishing to re-baseline `live.md`
- [ ] Reshoot screenshots — add at least one Level 4/5 screen
- [ ] Check the feature graphic for a hard-coded "24 stories" / "8 stories"
- [ ] `phonics_level_4` and `phonics_level_5` live and priced in Play Console
