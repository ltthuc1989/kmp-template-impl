# COPPA Compliance — ABC Phonics Kids

Reference cho `/grabee` khi sinh code liên quan child data, audio, analytics, hoặc Firebase.

**Khi nào load**: user prompt nhắc đến audio/voice/recording/microphone/profile/nickname/syncCode/Firestore/analytics/parental.

---

## Vì sao quan trọng

COPPA (Children's Online Privacy Protection Act) — luật liên bang Mỹ, áp dụng app target trẻ < 13.
- Phạt: tối đa **$50,120/vi phạm/đứa trẻ**.
- Án thật: TikTok $5.7M, Epic Games $275M, YouTube $170M.
- Apple/Google Kids Category **từ chối** app không compliant.
- Tương đương: GDPR-K (EU), Age-Appropriate Design Code (UK), DPDP Act (India).

App này theo **Anonymous-first model**: không account, không thu PII → tuân COPPA bằng cách **không thu data cần consent**.

---

## Quy tắc cứng (KHÔNG VI PHẠM)

### 1. KHÔNG thu PII (Personally Identifiable Information)
- ❌ Email, real name, phone, address, birthday.
- ❌ Photo của trẻ, tên trường học, location precise.
- ✅ Optional nickname (do trẻ tự chọn, có thể là "Tom" hoặc "🐼"). Đây KHÔNG phải PII vì không link với danh tính thật.
- ✅ Avatar chọn từ preset (không upload ảnh thật).

### 2. Voice là MEMORY-ONLY
- ❌ Ghi voice ra file (kể cả tmp).
- ❌ Upload voice lên Cloud Storage / S3 / Firebase Storage.
- ❌ Lưu raw audio bytes trong Room hoặc DataStore.
- ✅ Capture vào `ByteArray` in-memory.
- ✅ Send transient đến Gemini API (Google không train từ standard tier — verify dashboard).
- ✅ Discard buffer ngay sau khi nhận response.
- ✅ Lưu chỉ score + feedback text trong `PronunciationAttemptEntity`.
- ✅ Auto-purge `PronunciationAttemptEntity` sau 7 ngày qua background coroutine.

### 3. KHÔNG analytics user-ID
- ❌ Firebase Analytics với userId, không tracking individual sessions.
- ❌ Mixpanel, Amplitude, hoặc bất kỳ tool nào persist user identity.
- ✅ Aggregate-only metrics (vd: total opens, không per-user).
- ✅ Crashlytics OK vì không user-linked (default config).

### 4. Ads SDK — kids-safe config bắt buộc
**KHÔNG remove ads** — kids apps được phép ads với compliant config.

Bắt buộc khi gen code đụng ads:
- ✅ AdMob: `setTagForChildDirectedTreatment(TRUE)` + `setTagForUnderAgeOfConsent(TRUE)` + `setMaxAdContentRating(G)` trong `RequestConfiguration`.
- ✅ AdMob: non-personalized only — `Bundle().putString("npa", "1")` trong `AdRequest.addNetworkExtrasBundle`.
- ✅ AppLovin: `AppLovinPrivacySettings.setIsAgeRestrictedUser(true, ctx)` + `setHasUserConsent(false, ctx)` + `setDoNotSell(true, ctx)`.
- ✅ Premium user → return early, không show ads. Check `isPremium` flag trước mọi ad render.
- ✅ Ads chỉ hiển thị home screen + UnitComplete screen — KHÔNG giữa learning step (1-8).
- ✅ Parental gate trước khi tap ad navigate ra (Apple Kids Category yêu cầu).
- ❌ KHÔNG dùng Unity Ads, Facebook Audience Network — không trong "Families Self-Certified" list.
- ❌ KHÔNG add `NSUserTrackingUsageDescription` (skip ATT — không cần IDFA cho non-personalized).

Code template: xem [docs/abc-phonics-kids/02-TECH_SPEC.md §12](../../../../docs/abc-phonics-kids/02-TECH_SPEC.md).

Khi user prompt "remove ads" → confirm: muốn remove hoàn toàn (free tier hết monetization phụ) hay chỉ tắt cho premium? Đa phần intent là cái sau.

### 5. Firestore document whitelist
Khi write `users/{syncCode}` document, chỉ cho phép field:
- `progress` (Map<String, ProgressData>)
- `nickname` (String?, optional)
- `avatar` (String, preset id)
- `updatedAt` (Long)

Security rule enforce:
```javascript
allow write: if request.resource.data.keys().hasOnly(
  ['progress', 'nickname', 'avatar', 'updatedAt']
);
```
Code Kotlin cũng phải validate trước khi gọi Firestore — defense in depth.

### 6. Parental gate trước action nguy hiểm
- Settings → external link (privacy policy, terms) → **math gate trước** ("What is 5+7?").
- Parent dashboard → math gate trước.
- Subscription purchase → standard app store auth (đã có).
- Restore SyncCode → math gate trước (chống trẻ vô tình đè data).

---

## Code Patterns

### AudioRecorder (memory-only)

```kotlin
// commonMain
expect class AudioRecorder {
    fun start()
    suspend fun stop(): ByteArray   // returns AAC-encoded buffer
    fun release()
}

// Sử dụng
class VoiceRepository(private val recorder: AudioRecorder, private val stt: GeminiSpeechDataSource) {
    suspend fun recordAndScore(targetWord: String, ipa: String): Score {
        recorder.start()
        delay(MAX_RECORDING_MS)        // hoặc đợi user tap stop
        val buffer = recorder.stop()
        try {
            return stt.score(buffer, targetWord, ipa)
        } finally {
            // Buffer là local val → eligible for GC ngay khi function exits.
            // Không lưu bất kỳ đâu.
        }
    }
}
```

### Profile (no PII)

```kotlin
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val nickname: String?,         // optional, kid-chosen
    val avatarId: String,          // preset id, vd "avatar_panda"
    val syncCode: String?,         // optional, lazy generated
    val updatedAt: Long,
)
// KHÔNG có: email, realName, birthday, phone, photoUrl
```

### Analytics (aggregate only)

```kotlin
// ✅ OK
analytics.logEvent("level_complete", mapOf("level_number" to "1"))
analytics.logEvent("voice_score", mapOf("score_bucket" to "high"))

// ❌ BAD
analytics.logEvent("level_complete", mapOf(
    "user_id" to userId,           // user-link
    "nickname" to nickname,        // PII-ish
    "device_id" to deviceId,       // tracking
))
```

### Parental Gate

```kotlin
@Composable
fun ParentalGateDialog(onPass: () -> Unit, onDismiss: () -> Unit) {
    val a = remember { Random.nextInt(2, 9) }
    val b = remember { Random.nextInt(2, 9) }
    val correct = a + b
    var input by remember { mutableStateOf("") }
    AlertDialog(
        title = { Text(stringResource(Res.string.parental_gate_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.parental_gate_question, a, b))
                OutlinedTextField(value = input, onValueChange = { input = it.filter(Char::isDigit) })
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.toIntOrNull() == correct,
                onClick = onPass,
            ) { Text(stringResource(Res.string.action_continue)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
        onDismissRequest = onDismiss,
    )
}
```

---

## String Keys (cần thêm vào core:resource)

EN (`values/strings.xml`):
```xml
<string name="parental_gate_title">For parents only</string>
<string name="parental_gate_question">What is %1$d + %2$d?</string>
<string name="profile_nickname_hint">Pick a fun nickname (optional)</string>
<string name="profile_synccode_explain">Save this code to keep your progress on a new device.</string>
<string name="voice_permission_denied">Need microphone to score your voice. Open Settings → Privacy → Microphone.</string>
<string name="privacy_policy_short">We collect: progress, optional nickname, device language. No email, no PII, no voice stored.</string>
```

JA (`values-ja/strings.xml`): translate tương ứng.

---

## File Mapping (khi tạo feature mới)

| Feature | File quan trọng | COPPA check |
|---|---|---|
| `feature:profile` | `ProfileScreen.kt` | Không thu email/birthday. Avatar từ preset. |
| `core:datasource/audio` | `AudioRecorder.kt` (expect/actual) | Memory-only, không file output. |
| `core:datasource/speech` | `GeminiSpeechDataSource.kt` | Discard audio buffer sau call. Verify Gemini standard tier không train. |
| `core:datasource/firestore` | `FirestoreClient.kt` | Whitelist field trước write. |
| `core:datasource/analytics` | `AnalyticsTracker.kt` (nếu có) | Aggregate only, no userId. |
| `feature:parent` | `ParentDashboardScreen.kt` | Math gate trước khi vào. |
| `feature:setting` | external link | Math gate trước open browser. |

---

## Pre-commit Checklist

Trước khi `git commit` code đụng audio/profile/firestore/analytics:

- [ ] Voice không ghi file (grep `MediaRecorder.setOutputFile`, `AVAudioRecorder.url` đụng đâu phải tmp + delete ngay).
- [ ] ProfileEntity không có field PII (email, realName, birthday, phone, photoUrl).
- [ ] Firestore write có whitelist field (Kotlin side + security rules).
- [ ] Analytics event không chứa userId, nickname, deviceId.
- [ ] Math gate trước parent dashboard + external link + restore syncCode.
- [ ] String keys cho consent/privacy/parental có trong cả EN + JA.
- [ ] AdMob `RequestConfiguration` có TFCD + TFUA + MAX_AD_CONTENT_RATING_G (grep `setTagForChildDirectedTreatment`).
- [ ] AppLovin `setIsAgeRestrictedUser(true, ...)` (grep).
- [ ] Mọi ad render check `isPremium` flag trước.
- [ ] Ads không xuất hiện trong file `step*Screen.kt` (chỉ home + UnitCompleteScreen).

---

## Khi user prompt yêu cầu vi phạm

Nếu user nói "lưu voice ra file để debug" hoặc "thêm Firebase Auth email login":

1. **STOP, không sinh code ngay**.
2. Trả lời: nêu rule + lý do COPPA.
3. Đề xuất alternative: vd "lưu hash của voice (không phải audio) cho debug", hoặc "anonymous Firestore qua syncCode thay Auth".
4. Đợi user confirm trước khi tiếp tục.
