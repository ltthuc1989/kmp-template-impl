# Play Store Listing — Default English (en-US)

⚠️ **Mốc 2 draft.** Diff against [live.md](live.md) before pasting into Play Console.
Refresh that snapshot first: `python3 marketing/store-listing/fetch-live.py en-US`.

**Near-minimal-diff policy.** No line is rewritten for its own sake. The changes are two
parallel level blocks, the counts that shipping Level 2 makes wrong, and one false offline
claim. `WHAT KIDS LEARN` narrows to cross-level totals so the two level blocks can be
parallel — a `LEVEL 2` heading with no `LEVEL 1` above it reads as if Level 1 were missing.
Its bullets move into `LEVEL 1`, wording intact.

## App title (16 chars) — UNCHANGED

```
ABC Phonics Kids
```

## Short description (69 chars) — CHANGED

```
Phonics English for kids 3-8 — A-Z, short vowels, 16 stories, 6 games
```

## Long description — ADD one block, fix story count, qualify the offline line

```
Phonics Kids — the ad-free way for kids 3-8 to learn to read English with phonics. 488+ words, 16 stories, 6 mini-games.

Phonics Kids helps kids ages 3-8 learn to read English through the proven phonics method. Master the alphabet A-Z, blend sounds into real words, and read 16 narrated stories — all reinforced by 6 engaging mini-games after every story.

🌟 WHAT KIDS LEARN
✓ 488+ vocabulary words with audio
✓ 16 stories — words light up as the narrator reads (karaoke style)

📖 LEVEL 1: THE ALPHABET
✓ All 26 letters A-Z with native pronunciation
✓ Letter sounds, not just letter names — where real decoding starts
✓ Smart letter tracing — app scores stroke accuracy
✓ 8 stories built from the letters kids just learned

📖 LEVEL 2: SHORT VOWELS
✓ Short a, e, i, o, u — the sounds that unlock real reading
✓ Sound blending: c-a-t → cat, out loud, step by step
✓ 25 word families (-am, -an, -at, -ig, -op, -ug and more)
✓ Whole-word tracing — kids write "cat", not just "c"
✓ 8 new stories using only words kids can already read

🎮 6 MINI-GAMES per unit
Bubble Pop • Memory Match • Fill Letter • Pick Word • Spell Letters • Drag Words

📚 PHONICS METHOD
Built on synthetic phonics — kids learn phoneme-grapheme correspondences, blending sounds into words, and decoding English. The method used by UK National Curriculum and US Common Core schools.

👨‍👩‍👧 KID-SAFE
✓ 100% COPPA-compliant — no personal data collected
✓ Ad-free — no banner, no rewarded ads, ever
✓ No sign-up, no email, no account needed
✓ Works offline after a one-time download
```

## Exactly what changed vs live

**Added — a Level 1 heading over the bullets that were already there, so the new Level 2
block has a sibling instead of appearing out of nowhere. Only the second bullet is new
copy; the rest is live wording moved down one section:**

```
📖 LEVEL 1: THE ALPHABET
✓ All 26 letters A-Z with native pronunciation
✓ Letter sounds, not just letter names — where real decoding starts
✓ Smart letter tracing — app scores stroke accuracy
✓ 8 stories built from the letters kids just learned
```

**Added — the block that carries the only genuinely new thing Level 2 gives a buyer:**

```
📖 LEVEL 2: SHORT VOWELS
✓ Short a, e, i, o, u — the sounds that unlock real reading
✓ Sound blending: c-a-t → cat, out loud, step by step
✓ 25 word families (-am, -an, -at, -ig, -op, -ug and more)
✓ Whole-word tracing — kids write "cat", not just "c"
✓ 8 new stories using only words kids can already read
```

Five lines, ranked by what actually sells: blending is the moment a kid stops recognising
shapes and starts reading, so it leads. Word families and whole-word tracing are the two
mechanics no competitor screenshot shows. No 🆕 badge — a store description is permanent
copy, not a changelog, and it ages into a lie the moment Level 3 ships. Announcing the
release is what the Play Console "What's new" field is for.

**Changed — the story count, plus two wording fixes:**

| Where | Live | Draft | Why |
|---|---|---|---|
| Opening line | `8 stories` | `16 stories` | L2 added 8 stories |
| Intro paragraph | `read 8 narrated stories` | `read 16 narrated stories` | same |
| Intro paragraph | `sound out words` | `blend sounds into real words` | one phrase — makes the intro carry L2's keyword |
| WHAT KIDS LEARN | `8 stories — words light up…` | `16 stories — words light up…` | L2 added 8 stories |
| WHAT KIDS LEARN | 4 bullets | 2 bullets — totals only | the other two are Level 1 specifics; they move under the `LEVEL 1` heading unchanged |
| WHAT KIDS LEARN | `Alphabet A-Z with native pronunciation` | `All 26 letters A-Z with native pronunciation` | now under `LEVEL 1`; `26` is the concrete number and matches `curriculum.json` |
| KID-SAFE | `Works completely offline` | `Works offline after a one-time download` | audio streams from Firebase and must be downloaded per level (`feature/download`); `completely` fails on first launch, and the short qualifier keeps the bullet scannable |

**`488+ words` stays** — kept at the owner's direction after the risk below was raised.

**Kept verbatim** — every other line, including the whole `🎮`, `📚`, and `👨‍👩‍👧` sections and
the opening `"the ad-free way for kids 3-8"`. Level headings match the in-app titles
(`curriculum.json` → `"The Alphabet"`, `"Short Vowels"`), so the store and the app agree.

## Known risk, accepted by the owner

`488+ words` counts the vocabulary across all five levels, but only L1 and L2 ship —
`LevelRepository.kt:31` renders L3–L5 as Coming Soon and no audio or image assets exist for
them, so 312 of those words are unreachable. Actually playable: **176 unique words** (104 in
L1 + 94 in L2, 22 shared). The store now shows an "In-app purchases" badge, so an inflated
content count sits directly beside a paid product.

Decision: keep `488+`. Revisit if Play flags the listing or reviews cite missing content.
If it ever needs to change, `176 words` is the defensible number and the two lines to edit
are the opening line and the `✓ 488+ vocabulary words with audio` bullet.

## Other known-inaccurate line kept per minimal-diff policy

- Nothing in the copy discloses what the in-app purchase unlocks (2 free units per level,
  then a one-time per-level purchase). Users hit the paywall unannounced. Kept out per
  minimal-diff; revisit if 1-star reviews cite the paywall.

## Target keywords

| Keyword | Strategy |
|---|---|
| `phonics kids` | Title + description |
| `learn to read English` | Intro |
| `short vowels` | Short description + Level 2 block (new) |
| `word families` | Level 2 block (new) |
| `blending sounds` | Intro + Level 2 block + phonics method (new) |
| `ESL kids reading` | Intro |
| `phonics for kids` | Body |

## Verified features (audit 2026-08-17)

| Claim | Source of truth |
|---|---|
| 488+ words | `curriculum.json` — 488 word entries across **all 5 levels**; only 176 are reachable (see "Known risk") |
| 16 stories | `files/stories/level_1.json` (8) + `level_2.json` (8), both with audio + word timing |
| 25 word families (L2) | `curriculum.json` — 30 `displayLetter`s across L2 minus 5 bare vowels |
| 26 letters, 8 units (L1) | `curriculum.json` — L1 `"The Alphabet"`, 26 lessons, letters A–Z |
| 6 mini-games per unit | `game/GameRegistry.kt` → `DEFAULT_UNIT_GAMES` |
| Karaoke word-sync | `step/common/KaraokeText.kt` |
| Letter tracing + scoring | `step/tracing/TracingScorer.kt` (75% threshold) |
| Whole-word tracing (L2+) | `step/wordtracing/` |
| Sound blending (L2+) | `step/vowelblend/`, routed in `StepScreen.kt:59-61` |
| Ad-free | No AdMob/AppLovin in `gradle/libs.versions.toml`; store shows no "Contains ads" badge |
| L3–L5 not reachable | `LevelRepository.kt:31` → `LAUNCHED_PREMIUM_LEVELS = setOf("L2")`; no audio/image assets exist |

## Do NOT claim

- ❌ "5 levels" — only L1 and L2 are enterable.
- ❌ "100% free" / "no in-app purchases" — IAP is live and the store badge shows it.
- ❌ Unqualified "works offline" — audio must be downloaded per level first.
- ⚠️ "488+ words" is kept by owner decision, not because it is defensible — see "Known risk".

## Action items

- [x] Ads removed from code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (verified live — no badge)
- [x] Pricing: Free with in-app purchases (verified live — badge shown)
- [ ] Paste this draft into Play Console → en-US
- [ ] Re-run `fetch-live.py` after publishing to re-baseline `live.md`
- [ ] Reshoot screenshots — current set is Level 1 only, no blending or word-tracing frame
- [ ] Check the feature graphic for a hard-coded "488+ words" or "8 stories"
