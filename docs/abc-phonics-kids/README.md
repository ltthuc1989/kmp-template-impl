# ABC Phonics Kids — Documentation

App học phonics tiếng Anh cho trẻ 3-8 tuổi, build trên template `kmp-template-impl` (KMP + Compose Multiplatform).

**Status**: Planning → Mốc 1 (Code-Complete v0.1, 1 level end-to-end, ~8 tuần).
**App display name**: ABC Phonics Kids. **Code namespace giữ nguyên** `me.matsumo.grabee` (không rebrand sâu).

---

## Đọc theo thứ tự nào

| Vai trò | Đọc trước | Sau đó |
|---|---|---|
| **PM / Stakeholder** | [01-PRD.md](01-PRD.md) — risks, content, monetization | [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md) — timeline |
| **Tech Lead** | [02-TECH_SPEC.md](02-TECH_SPEC.md) — module mapping, Room schema, Destination tree | [.claude/skills/grabee/](../../.claude/skills/grabee/) skill references |
| **Dev mới onboard** | This README → [02-TECH_SPEC.md](02-TECH_SPEC.md) → [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md) | Gõ `/grabee <task>` |
| **Designer / Content** | [01-PRD.md](01-PRD.md) — section "Content Map" + "Asset Production" | — |

---

## Tài liệu nào trả lời câu hỏi nào

| Câu hỏi | File |
|---|---|
| App này làm gì, cho ai, kiếm tiền thế nào? | [01-PRD.md](01-PRD.md) |
| 5 levels có gì? Bao nhiêu units, từ vựng nào? | [01-PRD.md](01-PRD.md) — Content Map |
| Voice/image/audio asset lấy ở đâu? Tốn bao nhiêu? | [01-PRD.md](01-PRD.md) — Asset Production |
| Module nào extend, module nào tạo mới? | [02-TECH_SPEC.md](02-TECH_SPEC.md) — Module Diff |
| Room schema cụ thể? Destination tree? Firebase scope? | [02-TECH_SPEC.md](02-TECH_SPEC.md) |
| Voice pipeline COPPA-safe hoạt động sao? | [02-TECH_SPEC.md](02-TECH_SPEC.md) — Voice Pipeline + [skill ref](../../.claude/skills/grabee/references/voice-recognition.md) |
| Tuần nào làm gì? Mốc nào? Risk nào? | [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md) |
| Pattern A vs Pattern B ViewModel? Navigation3? | [.claude/skills/grabee/SKILL.md](../../.claude/skills/grabee/SKILL.md) |
| Khi sinh code đụng audio/data trẻ em, làm gì? | [.claude/skills/grabee/references/coppa.md](../../.claude/skills/grabee/references/coppa.md) |
| Đính kèm UI mockup, agent có hiểu không? | [.claude/skills/grabee/references/ui-from-screenshot.md](../../.claude/skills/grabee/references/ui-from-screenshot.md) — gõ `/grabee` + attach image |

---

## Quyết định page-1 (đã chốt — không tranh luận lại)

| # | Quyết định |
|---|---|
| 1 | Display name "ABC Phonics Kids" qua `app_name` string, **giữ namespace** `me.matsumo.grabee`. |
| 2 | Asset production qua **AI tools** (ElevenLabs + Midjourney + Claude/Gemini), tổng < $100. |
| 3 | STT: **Gemini 2.5 Flash multimodal** (audio + target word → score trong 1 call) + Whisper fallback. KHÔNG Google STT legacy. |
| 4 | COPPA: **Anonymous-first**, Profile opt-in (nickname + avatar + syncCode), KHÔNG Firebase Auth, KHÔNG VPC. |
| 5 | **GIỮ AdMob/AppLovin** với kids-safe config (TFCD + TFUA + non-personalized + ageRestricted). Premium tắt ads. Ads chỉ home + UnitComplete, không giữa learning step. |

Chi tiết & lý do trong [01-PRD.md](01-PRD.md) section "Risks & Constraints".

---

## Workflow dev hằng ngày

```bash
# 1. Add feature mới (vd profile)
/grabee Add feature:profile với 1 screen + 4 dialog (set nickname, pick avatar,
        show syncCode, enter syncCode), Pattern B, Destination.Profile

# 2. Scaffold 1 unit (level 1, unit 1)
/scaffold-unit level-1 unit-1
# (optional) Attach UI mockup PNG khi scaffold step screens

# 3. Build + lint
./gradlew :composeApp:assembleDebug
./gradlew detekt --auto-correct --continue

# 4. Test
./gradlew test
```

---

## Mốc thời gian

| Mốc | Phạm vi | Thời gian |
|---|---|---|
| **Mốc 1** | Code-Complete v0.1 — 1 level (9 units) end-to-end, store-submitted 1 platform | **8 tuần** (W0-W8) |
| **Mốc 2** | Production v1.0 — 5 levels (45 units), cả 2 platforms | thêm **8-12 tuần** (tổng 16-20 tuần) |

Chi tiết tuần-by-tuần trong [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md).

---

## Notes for future reader

- Tên "Grabee" trong code/logs/Gradle output là **template legacy** — store hiển thị "ABC Phonics Kids". Bình thường (legal name vs trade name).
- Skill `/grabee` cũng giữ tên — đây là knowledge base về stack, không phải brand.
- Nếu cần rebrand sâu sau này, xem [02-TECH_SPEC.md](02-TECH_SPEC.md) section "Display-name rebrand checklist".
