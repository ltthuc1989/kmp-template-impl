# ABC Phonics Kids — Technical Specification

**Version**: 1.0
**Audience**: Tech Lead + Senior Devs.
**Prerequisite**: đã đọc [01-PRD.md](01-PRD.md) + [.claude/skills/grabee/SKILL.md](../../.claude/skills/grabee/SKILL.md).

Doc này map từng requirement của PRD vào module thực + file path cụ thể trong template `kmp-template-impl`.

---

## 1. Stack & Conventions (đã có sẵn — KHÔNG đổi)

| Layer | Library | Version |
|---|---|---|
| UI | Compose Multiplatform | 1.10.2 |
| Material | Material3 Expressive | 1.10.0-alpha05 |
| Theme | Material3 Expressive + kolor | 4.0.5 |
| Navigation | Navigation3 (`androidx.navigation3`) | 1.0.0-alpha06 |
| DI | Koin | 4.1.1 |
| Database | Room KMP | 2.7.1 |
| Networking | Ktor | 3.3.3 |
| Preferences | DataStore KMP | 1.2.0 |
| Billing | RevenueCat KMP | 2.8.0 |
| Logging | Napier | 2.7.1 |
| Config | BuildKonfig | 0.17.1 |

**Patterns** (ép buộc):
- MVVM, no domain/use-case layer.
- ScreenState<T> + AsyncLoadContents cho async screens.
- Pattern A (StateFlow pass-through) hoặc Pattern B (ScreenState + actionState).
- Navigation3 — `Destination` sealed interface trong `core:ui` + `EntryProviderScope` extension.
- `feature:*` không depend `feature:*`.
- Room KSP chỉ ở `core:datasource` (4 KMP targets).
- `suspendRunCatching` cho error handling.
- Napier cho logging (không bao giờ println).
- UiState `@Stable` + `ImmutableList`.

Chi tiết patterns: [.claude/skills/grabee/SKILL.md](../../.claude/skills/grabee/SKILL.md).

---

## 2. Module Diff (đã có vs cần thêm)

### Đã có (giữ nguyên hoặc extend)
| Module | Trạng thái | Ghi chú |
|---|---|---|
| `core:common` | Giữ | suspendRunCatching, Koin, Napier |
| `core:model` | Extend | Thêm phonics models (Level, Unit, Step, Word, etc. — xem mục 4) |
| `core:resource` | Extend | Thêm strings phonics (EN + JA), seed JSON cho L1, audio/image asset paths |
| `core:datasource` | **Extend lớn** | Thêm Room schema phonics + AudioRecorder + GeminiSpeechDataSource + SyncCodeDataSource + Firestore client |
| `core:repository` | **Extend lớn** | Thêm ContentRepository, ProfileRepository, ProgressRepository, BackupRepository, VoiceRepository |
| `core:billing` | Giữ | RevenueCat KMP đã wired |
| `core:ui` | Extend | Thêm `Destination.*` cho Onboarding/Profile/Learning/ParentDashboard |
| `feature:home` | **Extend** | Thêm avatar góc trên-phải tap → navigate Profile, hiển thị 5 levels carousel |
| `feature:learningpath` | **Extend lớn** (audit: skeleton placeholder) | Build full 8-step flow + level/unit selection. Hiện chỉ có sample UI hardcoded. |
| `feature:setting` | Giữ | Đã đủ cho settings cơ bản |
| `feature:billing` | Giữ | PaywallViewModel đã chuẩn Pattern B |

### Tạo mới
| Module | Mục đích |
|---|---|
| `feature:onboarding` | 1 screen với HorizontalPager 3 trang intro features. DataStore flag `hasSeenOnboarding`. |
| `feature:profile` | 1 screen + 4 dialog (set nickname, pick avatar, show syncCode, enter syncCode). Anonymous-first. |
| `feature:parent` | Parent dashboard ẩn sau math gate ("What is 5+7?"). Hiển thị progress analytics. |

### Module graph cập nhật

```
composeApp
  ├── feature:onboarding   (NEW)
  ├── feature:home         (extend)
  ├── feature:profile      (NEW)
  ├── feature:learningpath (extend lớn)
  ├── feature:parent       (NEW)
  ├── feature:setting      (giữ)
  └── feature:billing      (giữ)
        ↓
  core:ui          ← +Destination cho 4 screen mới
  core:repository  ← +ContentRepo, +ProfileRepo, +ProgressRepo, +BackupRepo, +VoiceRepo
  core:datasource  ← +Room phonics schema, +AudioRecorder (expect/actual), +Gemini STT, +SyncCode/Firestore
  core:billing     ← (không đổi)
  core:model       ← +Level, +Unit, +Step, +Word, +Profile, +Progress models
  core:common      ← (không đổi)
  core:resource    ← +strings phonics EN/JA, +seed JSON L1
```

**Rule cứng**: `feature:*` không depend `feature:*`. Nếu cần share UI component → đẩy vào `core:ui`.

### Start destination logic
`composeApp/.../AppNavHost.kt` đọc `appSettingRepository.hasSeenOnboarding`:
- `false` → init backstack = `Destination.Onboarding`.
- `true` → init backstack = `Destination.Home`.

---

## 3. Destination Tree

File: [core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/screen/Destination.kt](../../core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/screen/Destination.kt)

```kotlin
@Immutable
@Serializable
sealed interface Destination : NavKey {

    @Serializable
    data object Onboarding : Destination
    // 1 screen với HorizontalPager 3 trang. Tap "Get Started" → save flag → navigate Home.

    @Serializable
    data object Home : Destination
    // Start destination khi hasSeenOnboarding=true. 5 levels carousel + avatar góc trên-phải.

    @Serializable
    data object Profile : Destination
    // 1 screen với Avatar + Nickname hiển thị + 4 button mở dialog/bottom sheet inline.

    @Serializable
    sealed interface Learning : Destination {
        @Serializable data object LevelSelection : Learning
        @Serializable data class UnitSelection(val levelId: String) : Learning
        @Serializable data class Step(val levelId: String, val unitId: String, val stepIndex: Int) : Learning
        @Serializable data class UnitComplete(val levelId: String, val unitId: String, val stars: Int) : Learning
    }

    @Serializable
    data object ParentDashboard : Destination
    // Ẩn sau math gate. Truy cập từ Settings → "Parent Dashboard".

    @Serializable
    data class Paywall(val source: String) : Destination   // ĐÃ CÓ — không đổi

    @Serializable
    sealed interface Setting : Destination {               // ĐÃ CÓ — extend
        @Serializable data object Root : Setting
        @Serializable data object License : Setting
    }
}
```

**ProfileScreen action dialogs** (KHÔNG là Destination, chỉ Compose state trong screen):
- `SetNicknameDialog` — `AlertDialog` với `OutlinedTextField`.
- `PickAvatarBottomSheet` — `ModalBottomSheet` với `LazyVerticalGrid` 12-16 avatars.
- `ShowSyncCodeDialog` — `AlertDialog` hiển thị 6-digit + nút Copy.
- `EnterSyncCodeDialog` — `AlertDialog` với 6-digit input + nút Restore.

**Polymorphic serializer**: nhớ register tất cả Destination mới trong `Destination.config` block.

---

## 4. Room Schema

File: `core/datasource/src/commonMain/kotlin/me/ltthuc/kmp/core/datasource/db/`

### Entities

```kotlin
@Entity(tableName = "level")
data class LevelEntity(
    @PrimaryKey val id: String,         // "level-1" .. "level-5"
    val number: Int,                    // 1..5
    val title: String,                  // "The Alphabet"
    val targetAge: String,              // "3-5 years"
    val color: String,                  // hex token name "primary" / "secondary"
    val totalUnits: Int,                // 9
    val isPremium: Boolean,             // false cho L1, true cho L2-5
    val orderIndex: Int,
)

@Entity(
    tableName = "unit",
    foreignKeys = [ForeignKey(LevelEntity::class, ["id"], ["levelId"], onDelete = CASCADE)],
    indices = [Index("levelId")],
)
data class UnitEntity(
    @PrimaryKey val id: String,         // "level-1-unit-1"
    val levelId: String,                // "level-1"
    val number: Int,                    // 1..9
    val title: String,                  // "Aa Bb Cc Dd"
    val lettersJson: String,            // JSON ["a", "b", "c", "d"]
    val phonicsJson: String,            // JSON ["/æ/", "/b/", "/k/", "/d/"]
    val storyTitle: String?,
    val storyAudioAsset: String?,
    val orderIndex: Int,
    val estimatedMinutes: Int,
)

@Entity(
    tableName = "word",
    foreignKeys = [ForeignKey(UnitEntity::class, ["id"], ["unitId"], onDelete = CASCADE)],
    indices = [Index("unitId")],
)
data class WordEntity(
    @PrimaryKey val id: String,         // "apple"
    val unitId: String,                 // "level-1-unit-1"
    val word: String,                   // "apple"
    val ipa: String,                    // "/ˈæpəl/"
    val sentence: String,               // "I eat an apple."
    val imageAsset: String,             // "vocab/apple.webp"
    val audioWordAsset: String,         // "audio/word/apple.mp3"
    val audioSentenceAsset: String,     // "audio/sentence/apple.mp3"
    val difficulty: String,             // "easy" / "medium" / "hard"
)

@Entity(
    tableName = "user_progress",
    indices = [Index("levelId"), Index("unitId"), Index(value = ["levelId", "unitId", "stepIndex"], unique = true)],
)
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val levelId: String,
    val unitId: String,
    val stepIndex: Int,                 // 0..7
    val status: String,                 // "not_started" / "in_progress" / "completed"
    val starsEarned: Int,               // 0..3
    val attempts: Int,
    val averageScore: Float,            // 0..100
    val completedAt: Long?,             // epoch millis
    val syncedToFirestore: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "pronunciation_attempt")
data class PronunciationAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val score: Int,                     // 0..100
    val feedback: String,               // kid-friendly text từ Gemini
    val attemptedAt: Long,
    // KHÔNG lưu audio buffer — COPPA-safe
)
// Auto-purge attempts > 7 ngày qua background task.

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,        // singleton row
    val nickname: String?,              // null nếu chưa set
    val avatarId: String,               // "avatar_default" hoặc "avatar_panda" etc.
    val syncCode: String?,              // null nếu chưa generate
    val updatedAt: Long,
)
```

### DAOs (mẫu)

```kotlin
@Dao
interface LevelDao {
    @Query("SELECT * FROM level ORDER BY orderIndex")
    fun observeAll(): Flow<List<LevelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(levels: List<LevelEntity>)
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE levelId = :levelId")
    fun observeByLevel(levelId: String): Flow<List<UserProgressEntity>>

    @Upsert
    suspend fun upsert(progress: UserProgressEntity)

    @Query("SELECT * FROM user_progress WHERE syncedToFirestore = 0")
    suspend fun pendingSync(): List<UserProgressEntity>
}
```

### Database

```kotlin
@Database(
    entities = [
        LevelEntity::class, UnitEntity::class, WordEntity::class,
        UserProgressEntity::class, PronunciationAttemptEntity::class,
        ProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PhonicsDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun unitDao(): UnitDao
    abstract fun wordDao(): WordDao
    abstract fun progressDao(): UserProgressDao
    abstract fun attemptDao(): PronunciationAttemptDao
    abstract fun profileDao(): ProfileDao
}
```

### KSP setup
**ĐÃ CÓ** trong `core/datasource/build.gradle.kts`:

```kotlin
dependencies {
    listOf("kspAndroid", "kspIosX64", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.androidx.room.compiler)
    }
}
```
Chỉ thêm entities/DAOs ở trên, không đổi build config.

### Seed strategy
- L1 bundled: file `core/resource/.../files/seed/level-1.json` chứa toàn bộ entities. Insert lần đầu mở app qua `RoomDatabase.Callback.onCreate()`.
- L2-L5: download on-demand từ Firebase Storage sau khi user mua premium → parse JSON → insert.

---

## 5. Firebase Scope (template chưa có — cần thêm)

### KHÔNG dùng
- ❌ **Firebase Auth** — anonymous-first model, KHÔNG cần auth.

### Dùng
- ✅ **Firestore** (cross-device backup):
  ```
  /users/{syncCode}    document
    - progress: { "level-1-unit-1": { stars: 3, completedAt: ... }, ... }
    - nickname: "Tom" (optional)
    - avatar: "avatar_panda"
    - updatedAt: 1234567890
  ```
  Document key = syncCode (6-digit). Không có sub-collection.

- ✅ **Cloud Storage** (read-only assets cho premium levels):
  ```
  /content/level-2/audio/word/{wordId}.mp3
  /content/level-2/audio/sentence/{wordId}.mp3
  /content/level-2/vocab/{wordId}.webp
  /content/level-2/seed.json
  ```
  Public read, write chỉ qua Admin SDK (CI/CD upload).

### Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{syncCode} {
      // Bất kỳ ai biết syncCode đều có thể read/write
      // (security through obscurity — acceptable cho non-PII progress data)
      // Whitelist field — chặn ai đó cố push PII vô tình
      allow read: if true;
      allow write: if request.resource.data.keys().hasOnly(
        ['progress', 'nickname', 'avatar', 'updatedAt']
      );
    }
  }
}
```

```javascript
// Cloud Storage rules
service firebase.storage {
  match /b/{bucket}/o {
    match /content/{path=**} {
      allow read: if true;
      allow write: if false;  // chỉ Admin SDK
    }
  }
}
```

### SyncCode generation
- Format: 6-digit numeric (`100000`-`999999`).
- 1M combinations → low collision với < 100K users.
- Server check: trước khi commit, query `/users/{candidate}`. Nếu exists, retry với code khác.
- v1.1 nâng lên 8-digit hoặc 4-word format ("apple-cat-sun-moon") cho kid-friendly.

### Cloud Functions
- **Defer v1.1**. MVP làm client-side aggregation. Lý do: Firestore free tier đủ cho < 50K users; Cloud Functions tăng độ phức tạp + cost không cần thiết.

### Setup steps (W0)
1. Tạo Firebase project (đặt name "abc-phonics-kids" trên console).
2. Add Android app: package `me.ltthuc.kmp`.
3. Add iOS app: bundle `me.ltthuc.kmp`.
4. Download `google-services.json` → `composeApp/`.
5. Download `GoogleService-Info.plist` → `composeApp/iosMain/.../`.
6. Enable Firestore (Native mode) + Cloud Storage.
7. Apply security rules trên.
8. Verify: `./gradlew :composeApp:assembleDebug` pass + test write document.

---

## 6. Voice Pipeline (COPPA-safe)

### Architecture

```
[User taps mic]
        │
        ▼
AudioRecorder.start()        ← expect class trong core:datasource/commonMain
        │                       actual: Android MediaRecorder, iOS AVAudioRecorder
        ▼
[Memory buffer fill]         ← KHÔNG ghi file, chỉ ByteArray trong RAM
        │
        ▼
[User taps stop]
        │
        ▼
encodeAac16kHz(buffer): ByteArray
        │
        ▼
GeminiSpeechDataSource.score(audio, targetWord, ipa)   ← Ktor POST
        │
        ▼
Gemini 2.5 Flash Multimodal API
  Body: {
    "contents": [{
      "parts": [
        { "inline_data": { "mime_type": "audio/aac", "data": <base64> } },
        { "text": "Score this child's pronunciation of '<word>' (IPA: /<ipa>/).
                   Return JSON: { score: 0-100, accuracy: low|medium|high,
                   feedback_short: <kid-friendly string max 50 chars> }" }
      ]
    }],
    "generationConfig": { "responseMimeType": "application/json" }
  }
        │
        ▼
{ score, accuracy, feedback_short }
        │
        ▼
Update ScreenState.Idle(uiState.copy(score = ..., feedback = ...))
        │
        ▼
audioBuffer = null   ← discard ngay (COPPA)
```

### COPPA guarantees
1. Voice **không bao giờ rời memory** — không ghi file local, không upload Cloud Storage.
2. Gemini call là **transient** — Google không train từ user data nếu API key cấu hình standard tier (verify trong dashboard).
3. **Không lưu audio attempt** — chỉ lưu score + feedback trong `PronunciationAttemptEntity`.
4. `PronunciationAttemptEntity` auto-purge sau 7 ngày qua background coroutine.

### Files to create
- [core/datasource/.../audio/AudioRecorder.kt](../../core/datasource/) — `expect class`.
- [core/datasource/.../audio/AudioRecorder.android.kt](../../core/datasource/) — `actual` Android (MediaRecorder).
- [core/datasource/.../audio/AudioRecorder.ios.kt](../../core/datasource/) — `actual` iOS (AVAudioRecorder).
- [core/datasource/.../speech/GeminiSpeechDataSource.kt](../../core/datasource/) — Ktor client.
- `core:repository:VoiceRepository` — wrap recorder + STT + scoring.

API key: BuildKonfig field `GEMINI_API_KEY`, đọc từ `local.properties` `GEMINI_API_KEY=...` hoặc env `GEMINI_API_KEY`.

Chi tiết template code: [skill ref voice-recognition.md](../../.claude/skills/grabee/references/voice-recognition.md).

### Error handling
| State | UX |
|---|---|
| Mic permission denied | Show dialog "Cần microphone để chấm điểm. Mở Settings → Privacy → Microphone." |
| Recording timeout (> 10s) | Auto-stop + score luôn |
| Network error → Gemini API | "Không có mạng. Thử lại?" + retry button |
| API timeout (> 8s) | "Hơi chậm. Thử lại?" + retry |
| Low confidence score < 30 | Friendly "Try again!" thay vì "Wrong" |
| Gemini response parse fail | Fallback score = 0, log Napier.e, retry 1 lần |

---

## 7. Pattern A vs Pattern B per Screen

| Screen | Pattern | Lý do |
|---|---|---|
| `OnboardingScreen` | A | Chỉ HorizontalPager + 1 action "Get Started" |
| `HomeScreen` (LevelSelection) | B | Load levels từ Room (Flow), thêm avatar tap action |
| `ProfileScreen` | A | StateFlow từ ProfileRepository pass-through, dialogs là local state |
| `LevelDetailScreen` (UnitSelection) | B | Load units + progress, action: tap unit → navigate |
| `LearningStepScreen` (8 step) | B | Load step content, action: voice record + score (action state) |
| `UnitCompleteScreen` | A | Hiển thị stars + tap "Next" |
| `ParentDashboardScreen` | B | Load aggregated analytics từ Room |
| `PaywallScreen` | B (đã có) | Reference: [PaywallViewModel.kt](../../feature/billing/src/commonMain/kotlin/me/ltthuc/kmp/feature/billing/PaywallViewModel.kt) |
| `SettingScreen` | A (đã có) | Reference: [SettingViewModel.kt](../../feature/setting/src/commonMain/kotlin/me/ltthuc/kmp/feature/setting/SettingViewModel.kt) |

---

## 8. DI Module Mapping

Mỗi module mới có file `di/<Name>Module.kt`:

```kotlin
// feature/onboarding/.../di/OnboardingModule.kt
val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}

// feature/profile/.../di/ProfileModule.kt
val profileModule = module {
    viewModelOf(::ProfileViewModel)
}

// feature/parent/.../di/ParentModule.kt
val parentModule = module {
    viewModelOf(::ParentDashboardViewModel)
}

// core/repository/.../di/RepositoryModule.kt — extend
val repositoryModule = module {
    // existing: BillingRepository, AppSettingRepository
    singleOf(::ContentRepositoryImpl) bind ContentRepository::class
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
    singleOf(::ProgressRepositoryImpl) bind ProgressRepository::class
    singleOf(::BackupRepositoryImpl) bind BackupRepository::class
    singleOf(::VoiceRepositoryImpl) bind VoiceRepository::class
}

// core/datasource/.../di/DataSourceModule.kt — extend
val dataSourceModule = module {
    // existing: Room db, Ktor client, DataStore
    singleOf(::AudioRecorder)
    singleOf(::GeminiSpeechDataSource)
    singleOf(::SyncCodeDataSource)
    singleOf(::FirestoreClient)
}
```

Đăng ký trong `composeApp/.../di/Koin.kt`:

```kotlin
fun KoinApplication.applyModules() {
    modules(appModule, commonModule, billingModule, dataSourceModule, repositoryModule)
    modules(homeModule, settingModule, billingFeatureModule, learningPathModule)
    modules(onboardingModule, profileModule, parentModule)   // ← thêm
}
```

---

## 9. Performance Baselines

| Metric | Target | Tool |
|---|---|---|
| Cold start | < 2s | Android Studio Profiler / Xcode Instruments |
| Frame rate | 60fps | Layout Inspector |
| Room query | < 100ms p95 | DAO benchmark test |
| Gemini STT roundtrip | < 3s p95 | Network log |
| App size (Android) | < 60MB | `./gradlew :composeApp:assembleRelease` |
| Memory (steady state) | < 200MB | Profiler |

Reference devices: Pixel 6 (Android 13), iPhone 12 (iOS 17).

---

## 10. Acceptance Criteria (mỗi REQ)

| Requirement | Acceptance |
|---|---|
| First launch onboarding | Mở app lần đầu → 3-page carousel. Tap "Get Started" → DataStore `hasSeenOnboarding=true`. Mở app lần 2 → vào thẳng Home. Verify: `./gradlew test` test ViewModel + manual smoke. |
| Anonymous Home | Mở app (lần 2+) → Home hiển thị 5 levels + avatar default. Không có login screen. |
| Profile opt-in | Tap avatar → ProfileScreen. 4 button hoạt động. SyncCode lazy generate khi tap "Show My SyncCode" lần đầu. |
| Cross-device restore | Device 2: nhập syncCode → fetch Firestore → progress xuất hiện. Test thủ công với 2 emulator. |
| 8-step flow | Tap Level 1 → Unit 1 → Step 1. Hoàn thành 8 steps → UnitComplete với stars. |
| Voice scoring | Tap mic → record → stop → score xuất hiện trong < 3s. Audio không lưu file (verify qua filesystem inspector). |
| Premium gating | Free user tap Level 2 → Paywall mở. Mua → Level 2-5 unlock. Test với RevenueCat sandbox. |
| Localization | Đổi device language sang JA → UI dịch. Vocabulary giữ EN. |
| Ads kids-compliant | AdMob TFCD + TFUA + non-personalized verify qua Charles Proxy network log. AppLovin `setIsAgeRestrictedUser=true`. Premium user không thấy ads. Ads KHÔNG hiển thị giữa learning step (chỉ home + UnitComplete). |

---

## 11. Display-name Rebrand Checklist (W7)

**Code namespace**: `me.ltthuc.kmp` (đã rename từ template gốc — applicationId cũng đã đổi sang `me.ltthuc.kmp`).

| File | Đổi |
|---|---|
| `core/resource/src/commonMain/composeResources/values/strings.xml` | `<string name="app_name">ABC Phonics Kids</string>` |
| `core/resource/src/commonMain/composeResources/values-ja/strings.xml` | `<string name="app_name">ABCフォニックス キッズ</string>` |
| `composeApp/src/androidMain/AndroidManifest.xml` | Verify `android:label="@string/app_name"` |
| `composeApp/.../Info.plist` (iOS) | `CFBundleDisplayName` = "ABC Phonics Kids" |
| `composeApp/build.gradle.kts` | **GIỮ** `applicationId = "me.ltthuc.kmp"` |
| Privacy policy + store metadata | Viết với "ABC Phonics Kids" |

Tradeoff: dev internal vẫn thấy "Grabee" trong code/Gradle output. Bình thường — pattern legal name vs trade name.

---

## 12. Ads Compliance Configuration (W0 — Kids Category)

**KHÔNG remove ads**. Apple "Made for Kids" + Google "Designated for Families" cho phép ads với compliant config. AdMob (Designed for Families program) + AppLovin (Families Self-Certified) đều OK.

### Bắt buộc config

#### AdMob (Android + iOS)
Tại app init, set request configuration TRƯỚC mọi ad request:

```kotlin
// composeApp/.../ads/AdsInitializer.kt
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

fun initializeAds(context: Context) {
    val configuration = RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)  // TFCD
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)          // TFUA
        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)                         // General audience
        .build()
    MobileAds.setRequestConfiguration(configuration)
    MobileAds.initialize(context)
}

// Khi load ad, force non-personalized
val extras = Bundle().apply { putString("npa", "1") }   // non-personalized ads
val adRequest = AdRequest.Builder()
    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
    .build()
```

#### AppLovin
```kotlin
// composeApp/.../ads/AdsInitializer.kt
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk

AppLovinPrivacySettings.setIsAgeRestrictedUser(true, context)        // < 16 = restricted
AppLovinPrivacySettings.setHasUserConsent(false, context)            // không tracking
AppLovinPrivacySettings.setDoNotSell(true, context)                  // CCPA compliance

AppLovinSdk.getInstance(context).mediationProvider = "max"
AppLovinSdk.getInstance(context).initializeSdk()
```

#### iOS additional (Info.plist)
- KHÔNG add `NSUserTrackingUsageDescription` (skip ATT prompt — không cần IDFA cho non-personalized).
- AdMob tự động dùng SKAdNetwork attribution thay IDFA.

### UX Rules (NEVER violate)

| Rule | Enforce |
|---|---|
| KHÔNG hiển thị ads giữa learning step (1-8) | UI logic gate |
| Banner ads chỉ trên home screen + level selection | Layout-level decision |
| Interstitial ads chỉ sau khi hoàn thành unit (UnitCompleteScreen) | ViewModel trigger |
| Rewarded video ads (optional) — "Watch ad to unlock 1 unit" | Optional v1.1 |
| Premium subscription **TẮT toàn bộ ads** | Check `isPremium` flag trước mọi ad show |
| Ad content rating ≤ G (general audience) | `MAX_AD_CONTENT_RATING_G` config |
| Parental gate trước khi tap ad | iOS Apple yêu cầu cho Kids Category |

### Code Pattern

```kotlin
@Composable
fun BannerAdWithGate(modifier: Modifier = Modifier) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    if (isPremium) return  // ← premium tắt ads

    var showParentGate by remember { mutableStateOf(false) }
    BannerAdView(
        modifier = modifier,
        onAdClick = { showParentGate = true },   // ← parental gate trước khi navigate ra
    )
    if (showParentGate) {
        ParentalGateDialog(
            onPass = { /* allow ad navigation */ },
            onDismiss = { showParentGate = false },
        )
    }
}
```

### Files to verify

| File | Check |
|---|---|
| [gradle/libs.versions.toml](../../gradle/libs.versions.toml) | Giữ `admob`, `applovin`, `applovin-quality-service` |
| [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts) | Giữ BuildKonfig `ADMOB_*`, `APPLOVIN_*` + AppLovin plugin |
| `composeApp/.../ads/AdsInitializer.kt` | **Tạo mới** — config TFCD + TFUA + non-personalized + ageRestricted |
| `local.properties` | Giữ `ADMOB_ANDROID_APP_ID`, `ADMOB_IOS_APP_ID`, `APPLOVIN_SDK_KEY` |

### Build verification

```bash
# Verify ads SDK present (expected, not removed)
./gradlew :composeApp:assembleDebug
unzip -l composeApp/build/outputs/apk/debug/composeApp-debug.apk | grep -E "(com/google/android/gms/ads|com/applovin)" | head -5
# Expected: nhiều class hiển thị (đó là ads SDK đã bundle đúng)
```

### Pre-submission checklist

- [ ] AdMob test với "Designed for Families" enabled trong AdMob console.
- [ ] AppLovin test với "Coppa" mode bật trong MAX dashboard.
- [ ] Verify TFCD + TFUA flags log trong network request (Charles Proxy).
- [ ] Verify non-personalized ads (`npa=1` trong request).
- [ ] Verify content rating G only (no MA/T/PG content).
- [ ] Test Premium user → ads completely hidden.
- [ ] Apple "Made for Kids" flag bật trong App Store Connect.
- [ ] Google "Designated for Families" enrolled.
- [ ] Privacy Nutrition Label ghi rõ: "Data not linked to user", "Used for advertising (non-personalized)".

### Trade-off (transparency)

- **Revenue thấp hơn adult ads**: $0.50-2 CPM (kids non-personalized) vs $5-20 CPM (adult personalized).
- **Cần config kỹ**: misconfig = COPPA violation = phạt $50K/child.
- **UX cost**: ads distraction, có thể giảm engagement.
- **Premium incentive**: ads chính là motivation để upgrade.

### Khi nào nên REMOVE ads (defer decision đến Mốc 2)
- Nếu user feedback ads gây distraction nặng.
- Nếu subscription conversion > 15% (đã đủ revenue).
- Nếu Apple Kids review trả về với issue ads-related (rare nhưng possible).

---

## 13. Critical Files Reference

### Existing (đọc để hiểu pattern)
- [.claude/skills/grabee/SKILL.md](../../.claude/skills/grabee/SKILL.md) — patterns
- [feature/billing/.../PaywallViewModel.kt](../../feature/billing/src/commonMain/kotlin/me/ltthuc/kmp/feature/billing/PaywallViewModel.kt) — Pattern B reference
- [feature/setting/.../SettingViewModel.kt](../../feature/setting/src/commonMain/kotlin/me/ltthuc/kmp/feature/setting/SettingViewModel.kt) — Pattern A reference
- [core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/screen/](../../core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/screen/) — ScreenState, Destination, AsyncLoadContents
- [core/billing/](../../core/billing/) — RevenueCat KMP wrapper

### To create
- `feature/onboarding/` — module mới
- `feature/profile/` — module mới
- `feature/parent/` — module mới
- `core/datasource/.../audio/AudioRecorder.kt` (+ android/ios actuals)
- `core/datasource/.../speech/GeminiSpeechDataSource.kt`
- `core/datasource/.../sync/SyncCodeDataSource.kt`
- `core/datasource/.../db/PhonicsDatabase.kt` + 6 entities + DAOs
- `core/repository/.../{Profile,Progress,Backup,Voice,Content}Repository.kt`
- `core/resource/.../files/seed/level-1.json`

---

**Next**: timeline tuần-by-tuần trong [03-IMPLEMENTATION_PLAN.md](03-IMPLEMENTATION_PLAN.md).
