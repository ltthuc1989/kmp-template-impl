# Voice Recognition Pipeline — ABC Phonics Kids

Reference cho `/grabee` khi sinh code voice capture, STT, hoặc pronunciation scoring.

**Khi nào load**: user prompt nhắc đến audio recording, microphone, voice scoring, pronunciation, STT, Gemini speech, MediaRecorder, AVAudioRecorder.

**Prerequisite**: đọc [coppa.md](coppa.md) trước (memory-only voice rule).

---

## Architecture Overview

```
[User taps mic in LearningStepScreen]
        │
        ▼
ViewModel.startRecording()
        │
        ▼
VoiceRepository.recordAndScore(targetWord, ipa)
        │
        ├── AudioRecorder.start()           ← expect/actual
        │       (memory buffer, AAC mono 16kHz)
        │
        ├── User taps stop OR timeout 10s
        │
        ├── buffer = AudioRecorder.stop(): ByteArray
        │
        ├── GeminiSpeechDataSource.score(buffer, targetWord, ipa)
        │       (Ktor POST → Gemini 2.5 Flash multimodal)
        │
        └── return Score(value, accuracy, feedback)
                │
                ▼
ViewModel updates _actionState = RecordActionState.Success(score)
        │
        ▼
Screen shows stars + feedback animation
        │
        ▼
ProgressRepository.upsertAttempt(wordId, score)
        │
        ▼
[buffer eligible for GC — không lưu file, không upload]
```

---

## Files to Create

| File | Purpose |
|---|---|
| `core/datasource/src/commonMain/kotlin/me/matsumo/grabee/core/datasource/audio/AudioRecorder.kt` | `expect class` |
| `core/datasource/src/androidMain/kotlin/me/matsumo/grabee/core/datasource/audio/AudioRecorder.android.kt` | Android `actual` (MediaRecorder) |
| `core/datasource/src/iosMain/kotlin/me/matsumo/grabee/core/datasource/audio/AudioRecorder.ios.kt` | iOS `actual` (AVAudioRecorder) |
| `core/datasource/src/commonMain/kotlin/me/matsumo/grabee/core/datasource/speech/GeminiSpeechDataSource.kt` | Ktor STT client |
| `core/datasource/src/commonMain/kotlin/me/matsumo/grabee/core/datasource/speech/Score.kt` | DTO |
| `core/repository/src/commonMain/kotlin/me/matsumo/grabee/core/repository/voice/VoiceRepository.kt` | Wrap recorder + STT |

---

## AudioRecorder — `expect class` (commonMain)

```kotlin
package me.matsumo.grabee.core.datasource.audio

class AudioRecorderException(message: String, cause: Throwable? = null) : Exception(message, cause)

expect class AudioRecorder() {
    /**
     * Start recording into in-memory buffer.
     * Throws AudioRecorderException nếu mic permission denied hoặc init fail.
     */
    fun start()

    /**
     * Stop recording and return AAC-encoded ByteArray.
     * Buffer chỉ tồn tại trong scope của caller — không lưu file.
     */
    suspend fun stop(): ByteArray

    /** Release native resources. */
    fun release()
}
```

---

## Android `actual` (MediaRecorder pattern)

```kotlin
package me.matsumo.grabee.core.datasource.audio

import android.media.MediaRecorder
import android.os.Build
import io.github.aakira.napier.Napier
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class AudioRecorder actual constructor() {
    private var recorder: MediaRecorder? = null
    private var tmpFile: File? = null

    actual fun start() {
        // MediaRecorder bắt buộc ghi file → ghi tmp file ngắn rồi xoá ngay sau khi đọc bytes.
        // Memory-only thuần cần AudioRecord (low-level) — phức tạp hơn, defer nếu cần.
        val tmp = File.createTempFile("rec", ".aac").apply { deleteOnExit() }
        tmpFile = tmp

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(getContext()) else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioChannels(1)
            setOutputFile(tmp.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                tmp.delete()
                throw AudioRecorderException("Mic init failed", e)
            }
        }
    }

    actual suspend fun stop(): ByteArray = withContext(Dispatchers.IO) {
        recorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Napier.w("Recorder stop failed", e)
            }
            release()
        }
        recorder = null

        val tmp = tmpFile ?: throw AudioRecorderException("No recording in progress")
        val bytes = tmp.readBytes()
        tmp.delete()                      // ← xoá file ngay (COPPA — memory-only intent)
        tmpFile = null
        bytes
    }

    actual fun release() {
        recorder?.release()
        recorder = null
        tmpFile?.delete()
        tmpFile = null
    }

    private fun getContext(): android.content.Context = TODO("inject via Koin in actual constructor")
}
```

**Note**: `MediaRecorder` bắt buộc setOutputFile. Workaround: ghi tmp file → đọc bytes → delete ngay. Nếu cần pure memory, phải dùng `AudioRecord` (low-level, encode AAC manual) — phức tạp, defer nếu UAT pass.

**Permission**:
- Android: `<uses-permission android:name="android.permission.RECORD_AUDIO" />` trong `AndroidManifest.xml`. Request runtime permission trước khi `start()`.

---

## iOS `actual` (AVAudioRecorder pattern)

```kotlin
package me.matsumo.grabee.core.datasource.audio

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class AudioRecorder actual constructor() {
    private var recorder: AVAudioRecorder? = null
    private var tmpUrl: NSURL? = null

    actual fun start() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, error = null)
        session.setActive(true, error = null)

        val tmpDir = NSTemporaryDirectory()
        val filename = "rec_${NSDate().timeIntervalSince1970.toLong()}.m4a"
        val url = NSURL.fileURLWithPath(tmpDir + filename)
        tmpUrl = url

        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to kAudioFormatMPEG4AAC,
            AVSampleRateKey to 16_000.0,
            AVNumberOfChannelsKey to 1,
            AVEncoderAudioQualityKey to AVAudioQualityMedium,
        )

        val rec = AVAudioRecorder(uRL = url, settings = settings, error = null)
        if (!rec.prepareToRecord() || !rec.record()) {
            throw AudioRecorderException("AVAudioRecorder failed to start")
        }
        recorder = rec
    }

    actual suspend fun stop(): ByteArray {
        recorder?.stop()
        recorder = null

        val url = tmpUrl ?: throw AudioRecorderException("No recording in progress")
        val data = NSData.dataWithContentsOfURL(url) ?: throw AudioRecorderException("Read tmp failed")
        // Convert NSData → ByteArray
        val bytes = ByteArray(data.length.toInt())
        data.getBytes(bytes.refTo(0), data.length)

        // Delete tmp file ngay
        NSFileManager.defaultManager.removeItemAtURL(url, error = null)
        tmpUrl = null
        return bytes
    }

    actual fun release() {
        recorder?.stop()
        recorder = null
        tmpUrl?.let { NSFileManager.defaultManager.removeItemAtURL(it, error = null) }
        tmpUrl = null
    }
}
```

**Permission**:
- iOS: `NSMicrophoneUsageDescription` trong `Info.plist`. AVAudioSession sẽ tự prompt.

---

## GeminiSpeechDataSource — Ktor client

```kotlin
package me.matsumo.grabee.core.datasource.speech

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import me.matsumo.grabee.BuildKonfig

class GeminiSpeechDataSource(
    private val httpClient: HttpClient,
    private val apiKey: String = BuildKonfig.GEMINI_API_KEY,
) {

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun score(audioAac: ByteArray, targetWord: String, ipa: String): Score {
        val base64Audio = Base64.encode(audioAac)
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "audio/aac", data = base64Audio)),
                        GeminiPart(text = buildPrompt(targetWord, ipa)),
                    ),
                ),
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2,
            ),
        )

        val response: GeminiResponse = httpClient.post(ENDPOINT) {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Empty Gemini response")

        return Json.decodeFromString<Score>(text)
    }

    private fun buildPrompt(word: String, ipa: String): String = """
        Score this child's pronunciation of the word "$word" (IPA: /$ipa/).
        Be encouraging — this is a kid aged 3-8 learning English.
        Return JSON only:
        {
          "score": 0-100,
          "accuracy": "low" | "medium" | "high",
          "feedback_short": "<kid-friendly text max 50 chars, positive tone>"
        }
    """.trimIndent()

    companion object {
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }
}

@Serializable
data class Score(
    val score: Int,                 // 0-100
    val accuracy: String,           // "low" / "medium" / "high"
    @kotlinx.serialization.SerialName("feedback_short")
    val feedbackShort: String,
)

// --- Gemini API DTOs (rút gọn) ---
@Serializable internal data class GeminiRequest(val contents: List<GeminiContent>, val generationConfig: GeminiGenerationConfig)
@Serializable internal data class GeminiContent(val parts: List<GeminiPart>)
@Serializable internal data class GeminiPart(val text: String? = null, val inlineData: GeminiInlineData? = null)
@Serializable internal data class GeminiInlineData(val mimeType: String, val data: String)
@Serializable internal data class GeminiGenerationConfig(val responseMimeType: String, val temperature: Double)
@Serializable internal data class GeminiResponse(val candidates: List<GeminiCandidate>)
@Serializable internal data class GeminiCandidate(val content: GeminiContent)
```

**BuildKonfig setup** (composeApp/build.gradle.kts):
```kotlin
buildKonfig {
    packageName = "me.matsumo.grabee"
    defaultConfigs {
        buildConfigField(STRING, "GEMINI_API_KEY", localProperties.getProperty("GEMINI_API_KEY") ?: "")
    }
}
```

---

## VoiceRepository

```kotlin
package me.matsumo.grabee.core.repository.voice

import io.github.aakira.napier.Napier
import me.matsumo.grabee.core.common.suspendRunCatching
import me.matsumo.grabee.core.datasource.audio.AudioRecorder
import me.matsumo.grabee.core.datasource.speech.GeminiSpeechDataSource
import me.matsumo.grabee.core.datasource.speech.Score

interface VoiceRepository {
    suspend fun recordAndScore(targetWord: String, ipa: String): Score
    fun cancelRecording()
}

class VoiceRepositoryImpl(
    private val recorder: AudioRecorder,
    private val stt: GeminiSpeechDataSource,
) : VoiceRepository {

    override suspend fun recordAndScore(targetWord: String, ipa: String): Score {
        recorder.start()
        // Caller controls when to stop via cancelRecording or natural stop.
        // For simplicity below: timeout-based stop.
        val buffer = recorder.stop()
        Napier.d("Recorded ${buffer.size} bytes")
        return stt.score(buffer, targetWord, ipa)
    }

    override fun cancelRecording() {
        recorder.release()
    }
}
```

(Production code: cần expose `start()` + `stop()` separately để UI control timing. Above là rút gọn.)

---

## Error States & UX

| State | Detection | UX |
|---|---|---|
| Mic permission denied | Catch `SecurityException` (Android) hoặc check `AVAudioSession` (iOS) | Dialog "Cần microphone để chấm điểm. Open Settings → Privacy → Microphone." |
| Recording timeout (> 10s) | `withTimeout(10_000) { recorder.stop() }` | Auto-stop + score luôn |
| Network error | Catch `IOException` từ Ktor | "Không có mạng. Thử lại?" + retry button |
| API timeout (> 8s) | `httpClient.config { install(HttpTimeout) { requestTimeoutMillis = 8000 } }` | "Hơi chậm. Thử lại?" |
| Low confidence < 30 | check `score.score < 30` | Friendly "Try again!" thay vì "Wrong" |
| Gemini parse fail | Json.decodeFromString throws | Fallback `Score(0, "low", "Try again!")`, log Napier.e, retry 1 lần |

---

## Testing Strategy

### Unit test (`core:datasource:testDebugUnitTest`)
- Mock `HttpClient` với MockEngine — trả response JSON known.
- Test parse logic + error handling.

### Integration test (manual)
- Record giọng thật, verify score quality.
- Test mic denied flow.
- Test airplane mode → network error UX.

### Performance
- Verify p95 < 3s round-trip.
- Verify audio buffer không leak (Android Studio Profiler memory dump).

---

## Common Mistakes

❌ Lưu audio file lâu hơn cần thiết (`tmp.deleteOnExit()` chưa đủ — call `tmp.delete()` trong finally).
❌ Quên request mic permission trước `start()` (Android: `ActivityCompat.requestPermissions`).
❌ Hardcode Gemini API key — dùng BuildKonfig.
❌ Send raw PCM thay vì AAC — quá lớn, slow upload.
❌ Forget `temperature: 0.2` → Gemini trả response không nhất quán.
❌ Test với giọng người lớn — kids voice rất khác, MUST test với 3-5 sample giọng trẻ thật ở W0.

✅ Verify Gemini standard tier không dùng data train (Google Cloud Console → Settings → Data usage).
✅ COPPA: discard buffer ngay sau scoring xong.
✅ Show recording indicator UI (waveform animation).
