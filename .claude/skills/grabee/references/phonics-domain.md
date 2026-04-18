# Phonics Domain — ABC Phonics Kids

Domain reference cho `/grabee` khi sinh code liên quan phonics learning flow.

**Khi nào load**: user prompt nhắc đến level/unit/word/step/phonics/learning/vocabulary.

---

## Domain Vocabulary

| Term | Định nghĩa | Code mapping |
|---|---|---|
| **Level** | 1 trong 5 group lớn theo độ khó (Alphabet, Short Vowels, Long Vowels, Blends, Advanced) | `LevelEntity`, `Destination.Learning.LevelSelection` |
| **Unit** | 9 unit/level. Mỗi unit dạy 1 cluster letter/sound/family | `UnitEntity`, `Destination.Learning.UnitSelection(levelId)` |
| **Step** | 8 steps/unit (sound-intro, chant, vocabulary, identify, blending, matching, tracing, story) | sub-screens trong `feature:learningpath`, `Destination.Learning.Step(levelId, unitId, stepIndex)` |
| **Word** | 1 từ vựng có image + audio + IPA + sentence | `WordEntity` |
| **Star** | 0-3 stars/step dựa trên performance (voice score, accuracy) | field `starsEarned` trong `UserProgressEntity` |
| **Phonics Friend** | Character mascot per letter (Angry Apple, Big Bear, ...) | reference image asset, không là entity riêng |
| **Blending** | Ghép sound thành word (c-a-t → cat) | step type `blending` |
| **Tracing** | Vẽ letter bằng ngón tay trên canvas | step type `tracing` |
| **SyncCode** | 6-digit code cho cross-device backup, KHÔNG phải account | `ProfileEntity.syncCode` |

---

## Room Entity Skeleton (chuẩn cho Pattern A/B)

Chi tiết schema xem [docs/abc-phonics-kids/02-TECH_SPEC.md §4](../../../../docs/abc-phonics-kids/02-TECH_SPEC.md). Quick reference:

```kotlin
@Entity LevelEntity(id: String, number: Int, title: String, totalUnits: Int, isPremium: Boolean, ...)
@Entity UnitEntity(id, levelId, number, title, lettersJson, phonicsJson, ...)
@Entity WordEntity(id, unitId, word, ipa, sentence, imageAsset, audioWordAsset, ...)
@Entity UserProgressEntity(id, levelId, unitId, stepIndex, status, starsEarned, ...)
@Entity PronunciationAttemptEntity(id, wordId, score, feedback, attemptedAt)  // auto-purge 7 ngày
@Entity ProfileEntity(id=0, nickname, avatarId, syncCode, updatedAt)         // singleton
```

**KSP rule**: chỉ trong `core:datasource/build.gradle.kts`. KHÔNG thêm KSP vào feature module.

---

## Content Loading Strategy

| Level | Strategy |
|---|---|
| **Level 1** (free) | **Bundled**. Seed JSON ở `core/resource/.../files/seed/level-1.json` (~2MB) + assets bundled. Insert lần đầu mở app qua `RoomDatabase.Callback.onCreate()`. |
| **Levels 2-5** (premium) | **On-demand download**. Sau khi user mua premium, download `seed/level-{n}.json` + assets từ Firebase Storage `/content/level-{n}/`. Cache Room local + asset directory. |

DAO pattern (Pattern A friendly):
```kotlin
@Dao interface LevelDao {
    @Query("SELECT * FROM level ORDER BY orderIndex")
    fun observeAll(): Flow<List<LevelEntity>>   // ← feed Pattern A trực tiếp
}
```

---

## Pattern A vs Pattern B Mapping per Screen

| Screen | Pattern | Lý do |
|---|---|---|
| `OnboardingScreen` | A | HorizontalPager + 1 action |
| `HomeScreen` (LevelSelection) | B | Async load levels từ Room + avatar action |
| `ProfileScreen` | A | StateFlow từ ProfileRepository pass-through, dialogs là Compose state |
| `UnitSelectionScreen` | B | Load 9 units + progress |
| `LearningStepScreen` (8 step types) | B | Load step content + voice action với feedback (`actionState`) |
| `UnitCompleteScreen` | A | Display stars + tap "Next" |
| `ParentDashboardScreen` | B | Load aggregated analytics từ Room |

**Quy tắc chung**:
- Pattern A: chỉ pass-through StateFlow + dispatch write.
- Pattern B: có `_screenState: ScreenState<UiState>` + thường có `_actionState` cho user action với feedback (vd voice scoring success/error).

---

## Common ViewModel Skeletons

### Pattern B for LearningStepScreen (voice scoring example)

```kotlin
class StepIdentifyViewModel(
    private val contentRepo: ContentRepository,
    private val voiceRepo: VoiceRepository,
    private val progressRepo: ProgressRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<StepIdentifyUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<StepIdentifyUiState>> = _screenState.asStateFlow()

    private val _actionState = MutableStateFlow<RecordActionState>(RecordActionState.Idle)
    val actionState: StateFlow<RecordActionState> = _actionState.asStateFlow()

    fun fetch(levelId: String, unitId: String) {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()
            _screenState.value = suspendRunCatching {
                StepIdentifyUiState(
                    words = contentRepo.getWordsForUnit(unitId).toImmutableList(),
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_load_unit) },
            )
        }
    }

    fun recordAndScore(word: WordEntity) {
        viewModelScope.launch {
            _actionState.value = RecordActionState.Recording
            _actionState.value = suspendRunCatching {
                val score = voiceRepo.recordAndScore(word.word, word.ipa)
                progressRepo.upsertAttempt(word.id, score)
                RecordActionState.Success(score)
            }.getOrElse {
                Napier.e("Voice scoring failed", it)
                RecordActionState.Error(it.message ?: "Unknown")
            }
        }
    }
}

@Stable
data class StepIdentifyUiState(val words: ImmutableList<WordEntity>)

@Stable
sealed interface RecordActionState {
    data object Idle : RecordActionState
    data object Recording : RecordActionState
    data class Success(val score: Score) : RecordActionState
    data class Error(val message: String) : RecordActionState
}
```

---

## Asset Path Convention

```
core/resource/src/commonMain/composeResources/files/
  seed/
    level-1.json       (bundled)
  audio/
    word/
      apple.mp3        (bundled cho L1, downloaded cho L2-5)
      ant.mp3
    sentence/
      apple.mp3
    chant/
      level-1-unit-1.mp3
    story/
      rabbits-house.mp3
  vocab/
    apple.webp
    ant.webp
  characters/
    angry-apple.webp
  story/
    rabbits-house/
      panel-01.webp
      ...
```

**Convention**:
- File name lowercase, dash-separated.
- Audio MP3 mono 22kHz, ≤ 50KB.
- Image WebP 256×256 cho vocab, 512×512 cho characters, 1024×768 cho story panels.
- Reference từ Compose: `Res.drawable.vocab_apple` hoặc `Res.readBytes("files/audio/word/apple.mp3")`.

---

## COPPA Constraints (rất quan trọng)

Khi sinh code liên quan child data, audio, analytics → **PHẢI load [coppa.md](coppa.md) reference**.

Tóm tắt:
- KHÔNG ghi voice file (memory-only).
- KHÔNG thu PII (email, real name, birthday, location).
- KHÔNG analytics user-ID.
- KHÔNG third-party ad SDK.
- Auto-purge `PronunciationAttemptEntity` sau 7 ngày.

---

## Common Mistakes to Avoid

❌ Tạo Room schema trong `core:repository` hoặc `feature:*` (chỉ `core:datasource`).
❌ Hardcode level/unit content trong code (dùng seed JSON).
❌ Lưu voice recording ra file (memory-only, COPPA).
❌ Thêm `feature:learningpath` depend `feature:profile` (rule: feature không depend feature).
❌ Quên `@Stable` + `ImmutableList` cho UiState.
❌ Println cho debug (dùng Napier).
❌ Hardcode Gemini API key (BuildKonfig field).

✅ DAO trả `Flow<List<X>>` cho Pattern A pass-through.
✅ `suspendRunCatching` quanh mọi I/O.
✅ Asset reference qua `Res.drawable.*` / `Res.readBytes("files/...")`.
