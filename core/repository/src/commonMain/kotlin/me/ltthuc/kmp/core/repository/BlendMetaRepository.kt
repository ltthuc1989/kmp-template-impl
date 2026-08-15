package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.model.BlendChain
import me.ltthuc.kmp.core.model.BlendMeta
import me.ltthuc.kmp.core.model.BlendSegment
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads `files/blend_meta/level_<n>.json` — the per-page timing map for the Level 2+ blend screen.
 *
 * Unlike [ChantMetaRepository], which synthesizes an even grid from three tuning numbers, this
 * returns the measured offsets verbatim. They come from ffmpeg `silencedetect` over the generated
 * chain audio, so the animation can track the real sound rather than a nominal tempo.
 *
 * Returns null for a lesson with no entry — callers keep their delay-driven fallback for lessons
 * whose audio has not been generated yet.
 */
class BlendMetaRepository(private val dispatcher: CoroutineDispatcher) {
    private val cache = mutableMapOf<Int, List<BlendMetaDto>>()

    suspend fun metaFor(level: Int, lessonFolder: String): BlendMeta? {
        val list = cache.getOrPut(level) { loadLevel(level) }
        val dto = list.firstOrNull { it.lessonId == lessonFolder } ?: return null
        return BlendMeta(
            lessonId = dto.lessonId,
            chains = dto.chains.map { chain ->
                BlendChain(
                    word = chain.word,
                    audio = chain.audio,
                    durationMs = chain.durationMs,
                    segments = chain.segments.map {
                        BlendSegment(it.card, it.text, it.startMs, it.endMs)
                    },
                )
            },
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadLevel(level: Int): List<BlendMetaDto> = withContext(dispatcher) {
        val path = "files/blend_meta/level_$level.json"
        runCatching {
            val bytes = Res.readBytes(path)
            jsonParser.decodeFromString<List<BlendMetaDto>>(bytes.decodeToString())
        }.onFailure {
            // Missing file is the normal case for levels with no blend audio yet — warn, not throw.
            Napier.w(tag = TAG, throwable = it) { "Failed to load blend meta: $path" }
        }.getOrDefault(emptyList())
    }

    @Serializable
    internal data class BlendMetaDto(
        @SerialName("lesson_id") val lessonId: String,
        val chains: List<BlendChainDto> = emptyList(),
    )

    @Serializable
    internal data class BlendChainDto(
        val word: String,
        val audio: String,
        @SerialName("duration_ms") val durationMs: Int = 0,
        val segments: List<BlendSegmentDto> = emptyList(),
    )

    @Serializable
    internal data class BlendSegmentDto(
        val card: String,
        val text: String = "",
        @SerialName("start_ms") val startMs: Int,
        @SerialName("end_ms") val endMs: Int,
    )

    private companion object {
        const val TAG = "BlendMetaRepository"
        val jsonParser = Json { ignoreUnknownKeys = true }
    }
}
