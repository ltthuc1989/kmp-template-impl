package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.model.ChantMeta
import me.ltthuc.kmp.core.model.WordTiming
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val NUM_REPS = 3

class ChantMetaRepository(private val dispatcher: CoroutineDispatcher) {
    private val cache = mutableMapOf<Int, List<ChantMetaDto>>()

    suspend fun metaFor(level: Int, lessonId: String, orderedWords: List<String>): ChantMeta? {
        val list = cache.getOrPut(level) { loadLevel(level) }
        val dto = list.firstOrNull { it.lessonId == lessonId } ?: return null
        if (orderedWords.isEmpty()) return null

        val cardCount = orderedWords.size
        val edgeSpeed = dto.edgeSpeedMs ?: dto.speedMs
        val perRepMs = 2 * edgeSpeed + (cardCount - 1) * dto.speedMs

        val timings = (0 until NUM_REPS * cardCount).map { idx ->
            val rep = idx / cardCount
            val posInRep = idx % cardCount
            val s = dto.startMs + rep * perRepMs + edgeSpeed + posInRep * dto.speedMs
            WordTiming(
                word = orderedWords[posInRep],
                startMs = s.toInt(),
                endMs = (s + (dto.speedMs * 0.7).toLong()).toInt(),
            )
        }
        return ChantMeta(
            lessonId = dto.lessonId,
            startMs = dto.startMs,
            speedMs = dto.speedMs,
            edgeSpeedMs = edgeSpeed,
            wordTimings = timings,
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadLevel(level: Int): List<ChantMetaDto> = withContext(dispatcher) {
        val path = "files/chant_meta/level_$level.json"
        runCatching {
            val bytes = Res.readBytes(path)
            jsonParser.decodeFromString<List<ChantMetaDto>>(bytes.decodeToString())
        }.onFailure {
            Napier.w(tag = TAG, throwable = it) { "Failed to load chant meta: $path" }
        }.getOrDefault(emptyList())
    }

    @Serializable
    internal data class ChantMetaDto(
        @SerialName("lesson_id") val lessonId: String,
        @SerialName("start_ms") val startMs: Long = 10000,
        @SerialName("speed_ms") val speedMs: Long = 800,
        @SerialName("edge_speed_ms") val edgeSpeedMs: Long? = null,
    )

    private companion object {
        const val TAG = "ChantMetaRepository"
        val jsonParser = Json { ignoreUnknownKeys = true }
    }
}
