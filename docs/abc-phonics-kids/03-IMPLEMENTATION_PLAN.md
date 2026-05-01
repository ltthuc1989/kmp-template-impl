# ABC Phonics Kids — Implementation Plan

**Version**: 1.1
**Status**: Mốc 1 = listener-only Level 1 (no voice scoring). Voice integration moved to v1.x.
**Audience**: Tech Lead + Devs.
**Prerequisite**: [01-PRD.md](01-PRD.md), [02-TECH_SPEC.md](02-TECH_SPEC.md), [.claude/skills/grabee/](../../.claude/skills/grabee/).
**Last revised**: 2026-05-01 (scope reduction: voice pipeline → v1.x roadmap; W4 reallocated to asset/polish).

---

## 2 Mốc Realistic

| Mốc | Phạm vi | Thời gian | Output |
|---|---|---|---|
| **Mốc 1** | Code-Complete v0.1 — 1 level (9 units) end-to-end **listener-only** (tap-based gameplay, no voice scoring), 1 platform store-submitted | **8 tuần** (W0-W8) | TestFlight / Internal Track APK |
| **Mốc 2** | Production v1.0 — 5 levels (45 units), cả 2 platforms | thêm **8-12 tuần** (tổng 16-20 tuần) | Public release |
| **v1.x** | Voice scoring re-introduction (Step 4 + 5 Gemini multimodal STT) | TBD post-Mốc 1 | Re-spike + ship |

**Lý do 2 mốc**: 8-tuần MVP với 5 levels + Firebase + voice + COPPA là fantasy cho solo/pair. **Mốc 1 đã giảm scope thêm**: bỏ voice scoring khỏi MVP để giảm risk + tăng buffer cho asset/polish. 8 tuần dành cho 1 level listener-only end-to-end + cross-device sync + parent dashboard. Voice scoring quay lại ở v1.x với re-spike + Plan B.

---

## Mốc 1: Code-Complete v0.1 (8 tuần)

### W0 — Spike & Setup (1 tuần)

**Mục tiêu**: validate kiến trúc + asset pipeline end-to-end trên 1 unit thật trước khi commit toàn bộ Level 1.

| Task | Cách làm |
|---|---|
| **Asset pipeline spike** trên Level 1 Unit 1 | TTS gen 4 word audio + 4 sentence audio (apple/bear/cat/dog). Midjourney gen 4 vocab images. Drop vào `core/resource/.../files/` đúng asset path. Verify Step 1-8 (listener-only) hoạt động end-to-end. _Voice scoring spike → defer v1.x._ |
| Setup Firebase project | Console → tạo "abc-phonics-kids" → enable Firestore + Storage. Apply security rules từ [02-TECH_SPEC.md §5](02-TECH_SPEC.md). |
| Download config files | `google-services.json` → `composeApp/`. `GoogleService-Info.plist` → `composeApp/iosMain/.../`. |
| **Configure AdMob/AppLovin kids-safe** | KHÔNG remove. Tạo `composeApp/.../ads/AdsInitializer.kt` với TFCD + TFUA + non-personalized + ageRestricted theo [02-TECH_SPEC.md §12](02-TECH_SPEC.md). Verify config qua Charles Proxy. |
| ~~Add Gemini API key~~ | _v1.x_ — không cần Mốc 1. |
| Verify base build | `./gradlew :composeApp:assembleDebug` pass. Verify KHÔNG có RECORD_AUDIO permission trong APK manifest. |

**Acceptance**: Asset pipeline e2e pass trên Unit 1; build pass; no RECORD_AUDIO trong manifest; AdMob/AppLovin config verified.

---

### W1 — Foundation (1 tuần)

| Task | Cách làm | Reference |
|---|---|---|
| Audit `feature/learningpath/` chi tiết | Đọc [LearningPathViewModel.kt](../../feature/learningpath/src/commonMain/kotlin/me/ltthuc/kmp/feature/learningpath/LearningPathViewModel.kt) + Screen + Navigation. Audit hiện tại: 8 step screens đã build listener-only (Identify + Blending tap-based, no mic). Quyết định: extend wire-up data layer + polish. | — |
| Room schema setup | Tạo `core/datasource/.../db/PhonicsDatabase.kt` + 5 entities + DAOs theo [02-TECH_SPEC.md §4](02-TECH_SPEC.md). _PronunciationAttemptEntity → v1.x._ | `/grabee Add Room database PhonicsDatabase với 5 entities (Level, Unit, Word, UserProgress, Profile) trong core:datasource. KSP đã wired sẵn cho 4 targets.` |
| Repositories skeleton | Tạo interface + impl: `ContentRepository`, `ProfileRepository`, `ProgressRepository`, `BackupRepository` trong `core:repository`. _VoiceRepository → v1.x._ | — |
| Firestore client wrap | `core/datasource/.../firestore/FirestoreClient.kt` — Ktor wrap REST API hoặc Firebase Android/iOS SDK qua expect/actual. | — |
| Bash | `./gradlew :core:datasource:assembleDebug` pass. `./gradlew detekt --auto-correct --continue`. | — |

---

### W2 — Onboarding + Profile (1 tuần)

| Task | Prompt | Acceptance |
|---|---|---|
| `feature:onboarding` | `/grabee Add feature:onboarding với 1 screen, HorizontalPager 3 trang intro features ("5 Levels", "Tap-based Phonics Gameplay", "Offline Mode"), Pattern A. Destination.Onboarding. Lưu hasSeenOnboarding=true vào DataStore qua AppSettingRepository khi tap "Get Started". Complete all 11 steps.` | Mở app lần đầu → onboarding. Tap "Get Started" → vào Home. Mở lần 2 → vào thẳng Home. |
| `feature:profile` | `/grabee Add feature:profile với 1 screen, Pattern B. Destination.Profile. Hiển thị avatar + nickname hiện tại + 4 button (Set Nickname, Pick Avatar, Show My SyncCode, Restore From SyncCode). Mỗi action mở dialog/bottom sheet inline (không tạo Destination riêng). Complete all 11 steps.` | 4 dialog hoạt động. Nickname/Avatar lưu Room. |
| `feature:home` extend | Thêm avatar IconButton góc trên-phải. Tap → `LocalNavBackStack.current.add(Destination.Profile)`. | Avatar tap → Profile screen. |
| Start destination logic | `composeApp/.../AppNavHost.kt` đọc `appSettingRepository.hasSeenOnboarding` (Flow). Init backstack tương ứng. | Manual smoke test. |

---

### W3 — Learning Flow Skeleton + Content L1 (1 tuần)

| Task | Cách làm |
|---|---|
| Extend `Destination.Learning` | Thêm sealed interface với 4 sub: `LevelSelection`, `UnitSelection(levelId)`, `Step(levelId, unitId, stepIndex)`, `UnitComplete(...)` theo [02-TECH_SPEC.md §3](02-TECH_SPEC.md). |
| LevelSelection screen | Extend `feature:learningpath`. Pattern B. Load `LevelEntity` từ Room qua `ContentRepository`. Hiển thị 5 levels carousel với progress %. |
| UnitSelection screen | Pattern B. Load 9 `UnitEntity` cho levelId. Hiển thị 3×3 grid với star count + lock state. |
| Step screens wire-up | 8 step screens đã build sẵn listener-only (xem `feature/learningpath/.../step/`). Wire data từ Room qua ViewModels. KHÔNG tạo `RecordButton` (defer v1.x). |
| Seed L1 content | Generate `core/resource/.../files/seed/level-1.json` với 9 units, ~100 words. Insert qua `RoomDatabase.Callback.onCreate()`. **Asset assets từ AI tools (W4 parallel với asset production)**. |
| 3 units L1 hoàn chỉnh | Units 1, 2, 3 với asset thật để test end-to-end flow. Units 4-9 placeholder data, polish ở W5. |

**Reference**: command `/scaffold-unit level-1 unit-1` (xem [.claude/commands/scaffold-unit.md](../../.claude/commands/scaffold-unit.md)).

---

### W4 — Asset Production + Tap UX Polish (1 tuần) — buffer reallocated

> Trước đây tuần này dành cho voice pipeline; sau khi defer voice → v1.x, dồn buffer cho asset production + tap UX hardening (engagement metric chính của listener-only release).

| Task | Cách làm |
|---|---|
| TTS audio batch — Level 1 Unit 1-9 | Gen 100+ word audio + 100+ sentence audio qua TTS pipeline (xem memory `project_gemini-tts-config.md`). QA review batch 50 — re-gen file thiếu prosody. |
| Vocab images batch | Midjourney `--sref` lock style, gen ~100 vocab images cho L1. Batch QA 50 → re-prompt nếu lệch style. |
| Story illustrations L1 | 3 story sets × ~10 panels = 30 illustrations. Lock character designs. |
| Step 4 (Identify) tap UX hardening | Edge cases: same-letter familiar slot logic, decoy diversity, hint timing. Smoke test 9 units. |
| Step 5 (Blending) tap UX hardening | Letter sweep speed scaling (`INITIAL_LETTER_MS - roundIndex * SPEED_STEP_MS`) — verify đúng range cho từ 3-7 chữ. |
| Step 7 (Tracing) canvas polish | Touch target ≥ 64dp, line-on-template feedback, success animation. |
| Audio asset wiring | Replace stub `LISTEN_DURATION_MS` delay bằng real audio playback duration trong Identify/Blending/Vocabulary/Sound Intro/Chant/Story screens. |

**Acceptance**: 9 units có asset thật (audio + image); 8 step screens chạy end-to-end với asset thật trên Unit 1; no mock delay still in place.

---

### W5 — Progress Sync + Parent Dashboard + Backup (1 tuần)

| Task | Cách làm |
|---|---|
| `ProgressRepository` | Insert `UserProgressEntity` mỗi lần hoàn thành step. Background coroutine sync `pendingSync()` → Firestore qua syncCode. |
| `BackupRepository` | `generateSyncCode()`: gọi Firestore generate unique 6-digit. `pushProgress()`: upload toàn bộ progress. `restoreFromCode(code)`: download + merge vào Room. |
| ProfileScreen integration | "Show My SyncCode" → lazy generate + push. "Restore From SyncCode" → fetch + merge. |
| `feature:parent` | `/grabee Add feature:parent với ParentDashboardScreen, Pattern B. Destination.ParentDashboard. Load aggregated progress (per-level completion %, total time, weak units < 70% completion — KHÔNG có pronunciation score, defer v1.x). Math gate ("What is 5+7?") trước khi vào.` |
| Multi-device handoff test | Device 1: complete units → show syncCode. Device 2: enter code → progress xuất hiện. |

**Acceptance**: cross-device restore hoạt động. Parent dashboard hiển thị đúng analytics.

---

### W6 — Polish + Localization + Accessibility (1 tuần)

| Task | Cách làm |
|---|---|
| Animations + transitions | 300ms smooth transitions giữa screens. Star reveal animation. Button tap haptic. |
| JA localization | Translate tất cả UI strings sang `core/resource/.../values-ja/strings.xml`. Vocabulary giữ EN. |
| Accessibility audit | Touch targets ≥ 64dp (kids motor). Font ≥ 20sp body, ≥ 28sp heading. Color contrast WCAG AAA. Screen reader labels. |
| Performance | Profile cold start. Optimize Room queries (add index nếu query > 100ms). Image loading optimize Coil3. |
| Empty + error states | Mỗi ScreenState.Error có message + retry. Empty levels (chưa có data) handled. |

---

### W7 — Internal Beta + Display Rebrand (1 tuần)

| Task | Cách làm |
|---|---|
| Recruit 3-5 trẻ thật + parents | Friends/family. Cài app qua TestFlight/Internal Track. |
| Beta test scenarios | (a) Onboarding flow. (b) Hoàn thành Unit 1 listener-only (tap-based Identify + Blending). (c) Story step 8 narrated read-along. (d) Restore syncCode trên device khác. |
| Bug fix priority list | Crash > UX confusion > polish. |
| **Display name rebrand** | 3 dòng strings + iOS Info.plist theo [02-TECH_SPEC.md §11](02-TECH_SPEC.md). ~5 phút. |
| Privacy policy draft | Template từ TermsFeed/Iubenda. Customize: "no PII collected, no microphone access, syncCode optional". _Note: voice section sẽ thêm khi v1.x ship._ |

---

### W8 — Release Build + Store Submission (1 tuần)

| Task | Cách làm |
|---|---|
| Release build | `./gradlew :composeApp:assembleRelease` (Android) + Xcode Archive (iOS). Verify ProGuard/R8 không break Compose. |
| Verify ads kids-safe | Test với AdMob "Designed for Families" mode bật trong console. AppLovin "Coppa" mode trong MAX dashboard. Verify TFCD + TFUA flags trong network request (Charles Proxy). Verify `npa=1` (non-personalized). |
| Store metadata draft | App name "ABC Phonics Kids", description, screenshots, age rating "4+", category "Education > Kids". |
| Privacy Nutrition Label (iOS) | Data collected: optional nickname, optional Firestore sync (not linked to user). Voice: not collected (no microphone access in Mốc 1). |
| Google Play Data Safety form | Same disclosures. |
| **Apple "Made for Kids" / Google "Designated for Families" enrollment** | Apple: bật trong App Information. Google: enroll Designated for Families program. |
| Submit | Internal Track / TestFlight first. External review sau 1 tuần internal stable. |

**Mốc 1 Deliverable**: 1 platform internal beta with 1 level (9 units, real assets, **listener-only** tap-based gameplay, cross-device sync, parent dashboard). _Voice scoring là v1.x roadmap._

---

## Mốc 2: Production v1.0 (8-12 tuần thêm)

### Asset Production Track (W4-W12, parallel với code)
1 person curate full-time:
- W4-W6: ElevenLabs gen 670 word audio + 670 sentence audio. QA review batch 50.
- W4-W6: Midjourney gen 670 vocab images + 26 character designs. Lock `--sref` trước khi gen vocab.
- W7-W9: Gen 15 story illustration sets (10 panels each).
- W10-W12: Final QA pass + integrate vào seed JSON L2-L5.

**Asset budget**: < $100 (ElevenLabs $10 + Midjourney $30 + curation labor).

### Code Track (W9-W18)
| Tuần | Task |
|---|---|
| W9-W12 | **Multi-agent scaffolding 36 units còn lại**. Xem section "Multi-Agent" dưới. |
| W13-W14 | Premium gating UI. Firestore sync ở scale. Offline mode L2-5 (download on-demand). |
| W15-W16 | External beta 50-100 users. Iterate tap UX + engagement based on feedback. _STT threshold tuning quay lại v1.x._ |
| W17-W18 | COPPA legal review (lawyer $500-2K). Privacy policy finalize. Both-platform store submission. |

---

## Multi-Agent Strategy (Code Scaffolding)

**Hiểu lầm cần sửa từ docs gốc**: Claude Code dùng **Agent tool** với `subagent_type` (general-purpose / Explore / Plan), KHÔNG có agent file `.claude/agents/*.md` chạy độc lập.

### Khi nào dùng
- ✅ Scaffold 36 units L2-L5 (W9-W12) — repetitive code, asset paths đã sẵn.
- ✅ Generate 36 seed JSON files từ content map.
- ❌ KHÔNG dùng cho: kiến trúc decision, voice pipeline integration, COPPA review.

### Workflow

**Bước 1: Main thread tạo template chuẩn**
- Hoàn thiện Unit 1 Level 2 (level-2-unit-1) với asset thật.
- Verify build pass + smoke test.
- Đây là "reference unit" cho agents copy.

**Bước 2: Delegate parallel**
- Max **3-5 agents song song** (tránh rate limit + memory pressure).
- Mỗi agent xử lý 1 level (8 units còn lại trong level đó), sequential bên trong.

Prompt template cho 1 agent:
```
Bạn được giao scaffold 8 units (unit-2 đến unit-9) cho Level 2 trong feature:learningpath
của project ABC Phonics Kids.

**Reference template**: feature/learningpath/.../step/level-2-unit-1/ (đã hoàn thiện).
Copy pattern y hệt cho 8 units còn lại, thay đổi:
  - Word lists từ docs/abc-phonics-kids/01-PRD.md (Content Map Level 2 Unit 2-9).
  - Asset paths: vocab/<word>.webp, audio/word/<word>.mp3, audio/sentence/<word>.mp3.

Mỗi unit cần:
  1. Seed entry trong core/resource/.../files/seed/level-2.json (append mode).
  2. UnitEntity + WordEntities trong Room migration (nếu cần).
  3. Wire navigation Destination.Learning.UnitSelection(levelId="level-2") sẽ list ra.

Quality gate: chạy ./gradlew :feature:learningpath:compileDebugKotlinAndroid trước khi return.
Nếu fail, fix lỗi rồi return.

Reference docs:
  - .claude/skills/grabee/references/phonics-domain.md
  - .claude/skills/grabee/SKILL.md (11-step checklist)
  - docs/abc-phonics-kids/02-TECH_SPEC.md §3 (Destination), §4 (Room)

Báo cáo: list file đã tạo + verify build pass.
```

**Bước 3: Main thread integrate**
- Review từng agent output.
- Verify navigation wire-up đúng.
- Run full integration test.

**Limit**: 3-5 agents. Không launch 36 agents cùng lúc — context overhead + cost > benefit.

**Important**: multi-agent giải quyết **code scaffolding**, KHÔNG giải quyết **asset production** — đó là AI image/voice tools track riêng (ElevenLabs, Midjourney) chạy parallel.

---

## Risk Register

| Risk | Probability | Impact | Mitigation | Owner |
|---|---|---|---|---|
| ~~Gemini STT scoring kém~~ | _v1.x_ | _v1.x_ | Re-spike khi v1.x. Plan B: SoapBox Labs ($500/mo) hoặc Whisper on-device. | _v1.x Tech Lead_ |
| Asset consistency (Midjourney style drift) | Medium | Medium | Lock `--sref` + character designs ngày đầu. Human review batch 50. | Designer/Curator |
| COPPA legal exposure | Low | Critical | Anonymous-first. **Mốc 1 không record voice → giảm đáng kể compliance surface.** Lawyer review trước launch ($500-2K). Privacy policy template từ trusted vendor. | Legal + Tech |
| Apple Kids review reject vì ads misconfig | Medium | High | W0 config AdMob TFCD/TFUA + AppLovin ageRestricted. Test với "Designed for Families" mode bật. Parental gate trước ad click. Verify network log không gửi IDFA. | Tech Lead |
| Solo dev burnout | High | High | 8w Mốc 1 realistic (đã giảm scope: bỏ voice → buffer cho asset/polish). 16-20w Mốc 2 chỉ khi asset production parallel + AI assist tốt. Buffer 20%. | Project Manager |
| Firebase quota | Low | Medium | Free tier đủ < 50K users. Alert at 80% quota. Optimize: cache aggressively, batch writes. | Tech Lead |
| ~~iOS audio recording permission flow buggy~~ | _v1.x_ | _v1.x_ | Khi v1.x, test sớm. iOS yêu cầu `NSMicrophoneUsageDescription` trong Info.plist. | _v1.x Tech Lead_ |
| SyncCode collision | Very Low | Low | 1M combos đủ < 100K users. Server check + retry. v1.1 nâng 8-digit. | Backend |
| AI-generated content reject (illustrator style violates Apple guidelines) | Low | Medium | Pre-submission review. Có style guide rõ ràng (no realistic faces, no scary content). | Designer |
| Tap UX không hấp dẫn bằng voice scoring | Medium | Medium | Engagement metric: target completion rate ≥ 85% Step 4/5 + retention day-7 ≥ 30%. Nếu fail → ưu tiên ship voice scoring v1.x sớm. | Tech Lead + Product |

---

## Dev Workflow (hằng ngày)

```bash
# Add feature mới
/grabee Add feature:profile với 1 screen + 4 dialog (set nickname, pick avatar,
        show syncCode, enter syncCode), Pattern B, Destination.Profile.
        Complete all 11 steps.

# Scaffold unit
/scaffold-unit level-1 unit-1
# Optional: attach UI mockup PNG

# Build + lint
./gradlew :composeApp:assembleDebug
./gradlew detekt --auto-correct --continue

# Test
./gradlew test
./gradlew :core:datasource:testDebugUnitTest

# Run on device
./gradlew :composeApp:installDebug

# Release build
./gradlew :composeApp:assembleRelease     # Android
# iOS: Xcode Archive
```

---

## Success Criteria (Mốc 1 done)

- [ ] App khởi động < 2s.
- [ ] Onboarding 3-page hoạt động + DataStore flag.
- [ ] Profile 4 action hoạt động (nickname, avatar, show/enter syncCode).
- [ ] Cross-device restore qua syncCode test pass với 2 emulator.
- [ ] Level 1 complete 9 units, 100+ words, asset thật.
- [ ] **NO RECORD_AUDIO permission trong APK + NO NSMicrophoneUsageDescription trong Info.plist** (verify Mốc 1 listener-only).
- [ ] 8 step screens (incl. tap-based Identify + Blending) chạy end-to-end với asset thật.
- [ ] Parent dashboard math gate + analytics.
- [ ] EN + JA localization.
- [ ] Touch targets ≥ 64dp, font ≥ 20sp, contrast WCAG AAA.
- [ ] AdMob + AppLovin config kids-safe (TFCD + TFUA + non-personalized + ageRestricted).
- [ ] Ads chỉ hiển thị home screen + UnitComplete, KHÔNG giữa learning step.
- [ ] Premium user verify ads completely hidden.
- [ ] Privacy policy publish + linked trong app.
- [ ] Internal beta 3-5 trẻ thật, không crash, UX hợp lý.
- [ ] 1 platform store-submitted (TestFlight/Internal Track).

---

**Next**: bắt đầu W0 spike. Khi xong, tick off và move W1.
