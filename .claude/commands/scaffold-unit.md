---
description: Scaffold 1 phonics unit (8 step screens + Room seed) trong feature:learningpath
argument-hint: <level_id> <unit_number> [optional: attach UI mockup]
---

Bạn được gọi để scaffold 1 unit cho ABC Phonics Kids.

**Args**:
- `level_id`: $1 (vd `level-1`, `level-2`, ..., `level-5`)
- `unit_number`: $2 (1-9)
- Optional: nếu user attach UI mockup PNG/JPG → load [ui-from-screenshot.md](../skills/grabee/references/ui-from-screenshot.md) reference.

---

## Workflow

### Bước 1: Đọc content map
Read [docs/abc-phonics-kids/01-PRD.md](../../docs/abc-phonics-kids/01-PRD.md) section "Content Map" → tìm row cho `${level_id}` `unit ${unit_number}`. Lấy:
- Letters / phonics symbols (vd "Aa Bb Cc Dd")
- Sample words list
- Phonics Friends characters (nếu Level 1)
- Story title (nếu unit có story)

### Bước 2: Generate Room seed JSON
File: `core/resource/src/commonMain/composeResources/files/seed/${level_id}.json`

Append entry vào array `units` (hoặc tạo file nếu chưa có):
```json
{
  "id": "${level_id}-unit-${unit_number}",
  "levelId": "${level_id}",
  "number": ${unit_number},
  "title": "<từ content map>",
  "lettersJson": "[\"a\", \"b\", ...]",
  "phonicsJson": "[\"/æ/\", \"/b/\", ...]",
  "storyTitle": "<nếu có>",
  "storyAudioAsset": "audio/story/<filename>.mp3",
  "orderIndex": ${unit_number},
  "estimatedMinutes": 15,
  "words": [
    {
      "id": "apple",
      "word": "apple",
      "ipa": "/ˈæpəl/",
      "sentence": "I eat an apple.",
      "imageAsset": "vocab/apple.webp",
      "audioWordAsset": "audio/word/apple.mp3",
      "audioSentenceAsset": "audio/sentence/apple.mp3",
      "difficulty": "easy"
    },
    ...
  ]
}
```

Asset paths convention: xem [phonics-domain.md](../skills/grabee/references/phonics-domain.md) section "Asset Path Convention".

### Bước 3: Scaffold 7 step screens
Trong `feature/learningpath/src/commonMain/kotlin/me/ltthuc/kmp/feature/learningpath/step/`:

7 step types (mỗi cái có ViewModel + Screen + Composable). Index 4 (Blending) đã retire — screen bị xoá, index để trống, KHÔNG scaffold lại:
1. `step01_soundintro/SoundIntroScreen.kt` + ViewModel — animation chữ + audio
2. `step02_chant/ChantScreen.kt` + VM — rhythmic music + lyrics
3. `step03_vocabulary/VocabularyScreen.kt` + VM — word cards
4. `step04_identify/IdentifyScreen.kt` + VM — voice scoring với mic (load [voice-recognition.md](../skills/grabee/references/voice-recognition.md))
5. `step06_matching/MatchingScreen.kt` + VM — visual-audio match
6. `step07_tracing/TracingScreen.kt` + VM — letter tracing canvas
7. `step08_story/StoryScreen.kt` + VM — phonics reader

Pattern: tất cả **Pattern B** (`ScreenState<UiState>` + `actionState` cho voice/tap actions). Reference: [PaywallViewModel.kt](../../feature/billing/src/commonMain/kotlin/me/ltthuc/kmp/feature/billing/PaywallViewModel.kt).

**Nếu user attach UI mockup**:
- Load [ui-from-screenshot.md](../skills/grabee/references/ui-from-screenshot.md) reference.
- Đọc image qua Read tool trước khi viết code.
- Match layout/color/typography theo screenshot.
- Touch ≥ 64dp (kids).

### Bước 4: Wire navigation
File: `feature/learningpath/.../LearningPathNavigation.kt`

Thêm/verify entry:
```kotlin
entry<Destination.Learning.Step> { destination ->
    when (destination.stepIndex) {
        0 -> SoundIntroScreen(levelId = destination.levelId, unitId = destination.unitId, ...)
        1 -> ChantScreen(...)
        2 -> VocabularyScreen(...)
        3 -> IdentifyScreen(...)
        5 -> MatchingScreen(...)
        6 -> TracingScreen(...)
        7 -> StoryScreen(...)
        else -> error("Unknown stepIndex: ${destination.stepIndex}")
    }
}
```

### Bước 5: COPPA check
Load [coppa.md](../skills/grabee/references/coppa.md). Verify:
- Step 4 (Identify) voice không ghi file (memory-only).
- Không thu PII trong analytics events.

### Bước 6: 11-step Grabee checklist
Tham chiếu [Grabee SKILL.md](../skills/grabee/SKILL.md) section "Adding a New Feature — Checklist". Verify:
- [ ] Build.gradle.kts plugin combo đúng (đã có trong `feature:learningpath`).
- [ ] Destination registered.
- [ ] DI module updated nếu thêm ViewModel mới.
- [ ] Strings cho UI added vào `core:resource/.../values/strings.xml` + `values-ja/`.
- [ ] Composable accept `modifier: Modifier = Modifier`.

### Bước 7: Quality gate (BẮT BUỘC trước khi return)
```bash
./gradlew :feature:learningpath:compileDebugKotlinAndroid
./gradlew detekt --auto-correct --continue
```

Nếu fail → fix lỗi rồi return. KHÔNG return nếu build broken.

### Bước 8: Return summary
Báo cáo:
- File path đã tạo (list).
- Build status (pass/fail).
- COPPA check kết quả.
- Asset placeholder nào cần (nếu chưa có audio/image, ghi rõ "TODO: ElevenLabs gen audio for [word]" + "TODO: Midjourney gen image for [word]").

---

## References

- [docs/abc-phonics-kids/01-PRD.md](../../docs/abc-phonics-kids/01-PRD.md) — content map
- [docs/abc-phonics-kids/02-TECH_SPEC.md](../../docs/abc-phonics-kids/02-TECH_SPEC.md) — Room schema, Destination tree
- [.claude/skills/grabee/SKILL.md](../skills/grabee/SKILL.md) — patterns, 11-step checklist
- [.claude/skills/grabee/references/phonics-domain.md](../skills/grabee/references/phonics-domain.md) — domain vocab, entity skeletons
- [.claude/skills/grabee/references/coppa.md](../skills/grabee/references/coppa.md) — child data rules
- [.claude/skills/grabee/references/voice-recognition.md](../skills/grabee/references/voice-recognition.md) — mic + STT pipeline
- [.claude/skills/grabee/references/ui-from-screenshot.md](../skills/grabee/references/ui-from-screenshot.md) — khi attach mockup

---

## Example Invocations

```
/scaffold-unit level-1 unit-1
```

```
/scaffold-unit level-2 unit-3
[user attach mockup of vocabulary screen as PNG]
```
