# ABC Phonics Kids — Product Requirements Document (PRD)

**Version**: 1.0
**Status**: Approved for Mốc 1 (Code-Complete v0.1).
**Owner**: Product (TBD) + Tech Lead (TBD).

---

## 1. Risks & Constraints (đọc trước mọi thứ)

| # | Risk / Constraint | Mitigation |
|---|---|---|
| 1 | **Trademark "Phonics"** — không có chủ sở hữu (generic), nhưng "Oxford Phonics", "Hooked on Phonics", "Jolly Phonics" là trademark đã đăng ký. App phải là "ABC Phonics Kids" — KHÔNG dùng "Oxford" trong store listing, icon, screenshot. | Legal review trước store submission. Privacy policy + terms phải dùng tên này. |
| 2 | **COPPA mandatory** — luật liên bang Mỹ, mọi app target trẻ < 13 phải tuân. Phạt $50K/child/violation (TikTok $5.7M, Epic $275M). Apple/Google Kids Category từ chối app không compliant. | **Anonymous-first model**: không user account, không thu PII. Voice memory-only. Privacy policy đầy đủ. Chi tiết [02-TECH_SPEC.md](02-TECH_SPEC.md) + [skill ref](../../.claude/skills/grabee/references/coppa.md). |
| 3 | **Voice recognition trên giọng trẻ kém** — Google STT legacy adult-tuned, confidence < 0.5 với kids 3-5. Ảnh hưởng core gameplay. | **W0 spike**: test Gemini 2.5 Flash multimodal trên 3-5 sample giọng trẻ thật trước khi commit kiến trúc. Fallback Whisper on-device. Plan B: SoapBox Labs ($500/mo). |
| 4 | **Asset production** — 670+ word audio + 1340 sentence audio + 670 illustrations + 15 stories + 26 character designs. Studio voiceover trẻ em: $5-13K. | **AI tools**: ElevenLabs ($10) + Midjourney/Imagen 3 ($30) + Claude/Gemini cho IPA/JA translation (free). Total < $100. Tradeoff: 1-2 tuần curate + QA. |
| 5 | **Kids Category compliance** — Apple "Made for Kids" + Google "Designated for Families" cho phép ads NHƯNG yêu cầu compliant config (no behavioral targeting, no IDFA/GAID, age-appropriate). AdMob + AppLovin đều có kids-compliant mode (TFCD + TFUA + non-personalized). | **GIỮ AdMob + AppLovin** với cấu hình kids-safe: `tag_for_child_directed_treatment=TRUE`, `tag_for_under_age_of_consent=TRUE`, non-personalized only, không hiển thị giữa learning step. Premium subscription tắt ads. Chi tiết [02-TECH_SPEC.md §12](02-TECH_SPEC.md). |
| 6 | **8-tuần MVP fantasy** — docs gốc giả định 8 tuần cho 5 levels + 45 units + Firebase + voice + COPPA. Solo/pair dev không khả thi. | **2 mốc realistic**: Mốc 1 (8 tuần — 1 level v0.1), Mốc 2 (16-20 tuần tổng — 5 levels production). |

---

## 2. Vision & Goals

### Vision
Giúp trẻ 3-8 tuổi học phát âm tiếng Anh qua phonics, với feedback voice tức thì, không cần giáo viên kèm.

### Mission
- Methodology proven (5-level phonics curriculum giống Oxford Phonics World, nhưng tự sản xuất content).
- Engagement gamified (stars, level unlock, character mascots).
- Parent-friendly (no PII, transparent privacy, affordable subscription).
- Cross-platform (iOS + Android cùng codebase qua Compose Multiplatform).

### Success Metrics (12 tháng sau launch)
- DAU/MAU ≥ 0.4 (sticky learning).
- Free → Paid conversion 6-10%.
- Day-30 retention 25%+.
- Pronunciation score average 80%+ (improve 15%+ từ unit đầu đến cuối).
- App store rating ≥ 4.5 trên cả 2 platforms.

---

## 3. Target Users

### Primary: Trẻ 3-8 tuổi
- **Ages 3-5**: Level 1 (alphabet) — tap, listen, repeat. Touch ≥ 64dp, font lớn, animation rõ.
- **Ages 4-6**: + Level 2 (short vowels) — CVC blending.
- **Ages 5-7**: + Level 3 (long vowels) — magic E.
- **Ages 6-8**: + Level 4 (blends/digraphs) + Level 5 (advanced patterns).
- Đặc điểm: attention span 5-15 phút/session, cần feedback tức thì, khen nhiều > chê.

### Secondary: Cha mẹ / giáo viên ESL
- Cần: progress visibility (parent dashboard), assurance privacy/safety (COPPA-compliant), affordable price.
- Hành vi: download app cho con, set up profile (optional), check progress weekly.

### User Personas (rút gọn)
- **Lucy (4 tuổi)**: dùng tablet hằng ngày 15 phút. Free tier Level 1.
- **Maria (parent, 32)**: download cho 2 con, upgrade premium sau 1 tuần thấy progress.
- **Sarah (giáo viên ESL, 28)**: dùng trên iPad lớp học, recommend cho parents.

---

## 4. Core Features

### 4.1 Onboarding (1-time intro carousel)
- 3 trang giới thiệu tính năng app (5 levels, voice recognition, offline mode).
- Hiển thị 1 lần đầu mở app, lưu DataStore flag `hasSeenOnboarding=true`. Lần sau bỏ qua thẳng Home.
- KHÔNG yêu cầu nickname/email/syncCode ở đây.

### 4.2 Anonymous-first Home + Profile (opt-in)
- Mở app (lần 2+) → vào thẳng Home (LevelSelection 5 levels).
- Avatar default ở góc trên-phải.
- Tap avatar → mở `ProfileScreen` với 4 action:
  - **Set Nickname**: text input, max 20 char, không validate (cho phép emoji, kid-friendly).
  - **Pick Avatar**: bottom sheet 12-16 avatar preset (animal/character).
  - **Show My SyncCode**: lazy generate 6-digit code lần đầu, lưu local + push Firestore. Hiển thị to + có nút "Copy".
  - **Restore From SyncCode**: text input 6-digit → fetch Firestore → merge vào Room local.

### 4.3 Learning Flow (5 levels × 9 units × 8 steps)
Mỗi unit là 1 sequence 8 step:
1. **Sound Intro** — animation chữ + audio sound (vd: "A says /æ/").
2. **Chant** — rhythmic repetition with music.
3. **Vocabulary** — 4+ words featuring sound (apple, ant, ax, alligator).
4. **Identify** — recognition game (chọn ảnh đúng với sound).
5. **Blending** — sound combination (c-a-t → cat).
6. **Matching** — visual-audio matching.
7. **Tracing** — letter formation (finger tracing on canvas).
8. **Story** — context reading (phonics reader).

Mỗi step kiếm được 0-3 stars dựa trên performance.
Mỗi unit cần ≥ 6 stars để mở unit kế tiếp (configurable).

### 4.4 Voice Recognition (COPPA-safe)
- Tap microphone → record (memory-only, AAC mono 16kHz).
- Stop → POST audio + target word vào Gemini 2.5 Flash multimodal.
- Receive: `{ score: 0-100, accuracy: low|medium|high, feedback_short: "Great try!" }`.
- Show feedback + stars + animation.
- **Discard audio buffer immediately** (no Cloud Storage, no local file).

### 4.5 Progress Tracking
- Local: Room DB (`UserProgressEntity`).
- Cloud sync (optional, qua syncCode): Firestore document, only `progress + nickname + avatar + updatedAt`.
- Parent Dashboard (ẩn sau math gate "What is 5+7?" để chống trẻ vô tình mở):
  - Per-level completion %.
  - Time spent per session.
  - Pronunciation score trends.
  - Weak areas (sounds < 70% average).

### 4.6 Subscription (Premium)
- **Free**: Level 1 complete (9 units, 100+ words, 3 stories).
- **Premium**: Levels 2-5 (36 units, 500+ words, 12 stories).
- Pricing:
  - Monthly: $9.99
  - Annual: $69.99 (~42% off)
  - Lifetime: $149.99 (one-time, v1.1+)
- 7-day free trial all features.
- Backed bởi RevenueCat KMP (đã wired ở `core:billing`).
- **KHÔNG ads** — RevenueCat là kênh duy nhất.

### 4.7 Localization
- EN + JA từ ngày đầu (template đã có JA strings).
- Vocabulary giữ EN (đó là điểm dạy).
- UI strings dịch đầy đủ.

### 4.8 Offline Mode
- Level 1: bundled trong app (~10MB).
- Levels 2-5: download on-demand sau khi mua premium, cache Room local.
- Voice recognition: cần network (Gemini API). Whisper on-device fallback v1.1.

---

## 5. Out of Scope (MVP)

- ❌ Multi-child profiles trên 1 device (parent có nhiều con).
- ❌ Social features (leaderboard với bạn, share progress).
- ❌ Cloud Functions cho server-side aggregation — client-side đủ cho MVP.
- ❌ Custom voice acoustic model (dùng Gemini default).
- ❌ Web version (chỉ Android + iOS).
- ❌ Tablet-specific UI (responsive trong kích cỡ phone là đủ MVP).
- ❌ Lifetime tier (chỉ Monthly + Annual cho v1.0).
- ❌ Whisper on-device (defer v1.1).

---

## 6. Content Map (5 Levels × 9 Units = 45 Units)

### Level 1: The Alphabet (Ages 3-5, 9 units, 100+ words)
| Unit | Letters | Sample Words | Phonics Friends |
|---|---|---|---|
| 1 | Aa Bb Cc Dd | apple, bear, cat, dog | Angry Apple, Big Bear, Cool Cat, Dancing Dog |
| 2 | Ee Ff Gg Hh | egg, fish, gorilla, horse | Eager Egg, Funny Fish, Giggling Gorilla, Happy Horse |
| 3 | Ii Jj Kk Ll | insect, jet, kangaroo, lion | ... |
| 4 | Mm Nn Oo Pp | monkey, nut, octopus, peach | ... |
| 5 | Qq Rr Ss Tt | queen, rabbit, seal, turtle | ... |
| 6 | Uu Vv Ww Xx | umbrella, van, wolf, fox | ... |
| 7 | Yy Zz | yo-yo, zebra | Yawning Yak, Zigzag Zebra |
| 8 | Review 1 (A-M) | mixed | — |
| 9 | Review 2 (N-Z) | mixed | — |

Stories L1: "Rabbit's House", "The Picnic", "What I Want".

### Level 2: Short Vowels (Ages 4-6, 9 units, 150+ CVC words)
| Unit | Word Families | Sample Words |
|---|---|---|
| 1 | Short a (-am, -an, -ap, -at) | ham, can, cap, bat |
| 2 | Short a + more | cab, dad, bag |
| 3 | Short e (-ed, -en, -et, -eg) | bed, hen, jet, leg |
| 4 | Short i (-ib, -id, -ig, -ip, -it) | bib, kid, big, dip, bit |
| 5 | Short o (-ob, -og, -op, -ot, -ox) | cob, dog, hop, cot, box |
| 6 | Short u (-ub, -ug, -um, -un, -ut) | cub, bug, gum, bun, cut |
| 7 | Mixed a/e/i | — |
| 8 | Mixed o/u | — |
| 9 | Review all short vowels | — |

Stories L2: "At the Farm", "Fun in the Mud", "No Jam".

### Level 3: Long Vowels (Ages 5-7, 9 units, 120+ words)
| Unit | Pattern | Sample Words |
|---|---|---|
| 1 | Long a (ai, ay, a_e) | rain, day, cake |
| 2 | Long e (ee, ea, e_e) | tree, leaf, Pete |
| 3 | Long i (ie, igh, i_e, y) | pie, night, bike, fly |
| 4 | Long o (oa, ow, o_e) | boat, snow, home |
| 5 | Long u (ue, u_e) | blue, cube |
| 6-9 | Reviews | — |

Stories L3: "A Day with Mom", "At the Bay", "I am a Spy".

### Level 4: Blends & Digraphs (Ages 6-8, 9 units, 150+ words)
| Unit | Pattern | Sample Words |
|---|---|---|
| 1 | L-blends (bl, cl, fl, gl, pl, sl) | black, cloud, flag, glass, plant, slide |
| 2 | R-blends (br, cr, dr, fr, gr, pr, tr) | brown, crab, drink, frog, grass, print, tree |
| 3 | S-blends (sc, sk, sm, sn, sp, st, sw) | — |
| 4 | Final blends (ft, lt, mp, nd, nk, nt, pt, sk, st) | — |
| 5 | Digraphs ch, sh | chair, ship |
| 6 | Digraphs th, wh, ph | think, when, phone |
| 7-8 | Mixed | — |
| 9 | Review | — |

Stories L4: "A Nice Trip", "Fun Day at School", "On a Ship".

### Level 5: Advanced Patterns (Ages 7-9, 9 units, 150+ words)
| Unit | Pattern | Sample Words |
|---|---|---|
| 1 | R-controlled (ar, ir, ur, er, or) | star, bird, turtle, sister, fork |
| 2 | Diphthongs (ou, ow, oi, oy, oo) | mouse, brown, coin, boy, moon |
| 3 | Variant vowels (au, aw, all, wa, oar) | sauce, draw, ball, water, board |
| 4 | Air/ear sounds (are, air, ea, ear, eer) | care, hair, bread, bear, deer |
| 5 | Silent letters (kn, wr, mb, gh) | knee, write, lamb, ghost |
| 6-9 | Reviews + soft c/g | — |

Stories L5: "Dawn's Hiccups", "I Love the City", "The Painter Is in Town".

### Tổng kê
- **Levels**: 5
- **Units**: 45 (9 × 5)
- **Words**: 670+ vocabulary
- **Stories**: 15 (3/level)
- **Phonics Friends**: 26 characters (Level 1 chính, dùng lại ở Level 2-5)
- **Activities**: 360 game variations (45 units × 8 steps)

---

## 7. Asset Production (AI Tools)

### Audio (Voice)
- **Tool**: ElevenLabs.
- **Volume**: 670 word audio + 670 sentence audio + 15 story narrations = ~1,400 audio files.
- **Voice profile**: 1-2 child-friendly voices (vd: female teacher voice + child voice for stories). Voice cloning lock cho consistency.
- **Format**: MP3 mono 22kHz, ≤ 50KB/file.
- **Cost**: ~$10 (Starter tier 30K characters đủ).
- **QA**: human review batch 50 files, re-generate nếu thiếu emotion/prosody.

### Images (Vocab + Characters)
- **Tool**: Midjourney (chính, dùng `--sref` lock style) hoặc Imagen 3 (Google AI Studio).
- **Volume**: 670 vocab images + 26 character designs + 150 story illustration panels (~10 panels × 15 stories) = ~850 images.
- **Style**: friendly cartoon, bright colors, simple background. Lock 1 art reference ngày đầu.
- **Format**: PNG/WebP 256×256 cho vocab, 512×512 cho characters, 1024×768 cho story.
- **Cost**: ~$30 (Midjourney Standard $30/mo đủ 1 tháng).
- **QA**: human review batch 50 images, re-prompt nếu lệch style.

### Text (IPA, Translation)
- **Tool**: Claude / Gemini (free trong dev tier).
- **Volume**: 670 IPA transcriptions + 670 JA translations (UI strings, không vocab).
- **Cost**: $0 (within free tier).

### Total Budget
| Item | Cost |
|---|---|
| ElevenLabs audio | $10 |
| Midjourney images | $30 |
| Claude/Gemini IPA + JA | $0 |
| Curation labor (1 person × 1-2 weeks) | ~$2K-4K |
| **Total** | **~$2K-4K** vs $5K-13K studio |

---

## 8. Monetization

### Free Tier
- Level 1 complete (9 units, 100+ words, 3 stories).
- Voice recognition + scoring.
- Profile + cross-device sync (syncCode).
- Basic progress tracking.
- **Kids-safe ads**: AdMob + AppLovin với TFCD/TFUA config, non-personalized, age-appropriate. Hiển thị **giữa learning sessions** (sau khi hoàn thành unit) hoặc trên home screen — KHÔNG bao giờ giữa learning step.

### Premium ($9.99/mo, $69.99/yr)
- Levels 2-5 unlock (36 units, 500+ words, 12 stories).
- **No ads**.
- Parent dashboard với detailed analytics.
- Offline mode tất cả levels.
- Priority customer support.

### 7-day Free Trial
- Full premium access.
- Yêu cầu credit card (standard app store).
- Auto-convert sau 7 ngày, cancel anytime.

### Wire-up
- RevenueCat KMP (đã có ở `core:billing`).
- Reference ViewModel: [feature/billing/.../PaywallViewModel.kt](../../feature/billing/src/commonMain/kotlin/me/matsumo/grabee/feature/billing/PaywallViewModel.kt).
- Trigger paywall: tap level locked, sau hoàn thành Level 1, từ Settings.

### Revenue Projection
| Year | Downloads | Paid Users (8% conv) | ARPU | Revenue |
|---|---|---|---|---|
| 1 | 500K | 40K | $50 | $2M |
| 2 | 1.5M | 120K | $55 | $6.6M |

---

## 9. UX Principles

- **Touch targets ≥ 64dp** (kids motor skills, không 44pt như Apple HIG adult).
- **High contrast** WCAG AAA cho text.
- **Fonts ≥ 20sp** cho body, ≥ 28sp cho heading.
- **Icons + text** luôn đi cùng (kids chưa đọc thành thạo).
- **Animations 300ms** smooth transitions.
- **Visual + audio feedback** cho mọi tap.
- **No ads, no popup, no interruption** ngoài học.
- **Parent gate** (math problem) trước mọi link external/setting nguy hiểm.

---

## 10. Approval

| Role | Sign-off | Date |
|---|---|---|
| Product | _________ | _____ |
| Tech Lead | _________ | _____ |
| Legal (COPPA) | _________ | _____ |
| Design | _________ | _____ |

**Next**: tech specs trong [02-TECH_SPEC.md](02-TECH_SPEC.md), timeline trong [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md).
