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
            slideMs = dto.slideMs?.coerceIn(MIN_SLIDE_MS, MAX_SLIDE_MS),
            slideStartsMs = dto.slideStartsMs?.takeIf { it.isValidSlideStarts() },
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
        /** Bỏ trống = màn hình dùng mặc định. Level 1 không khai, giữ nhịp cũ. */
        @SerialName("slide_ms") val slideMs: Long? = null,
        /** 4 mốc kết thúc của 4 thẻ từ, tính từ lúc audio bắt đầu. Ưu tiên hơn slide_ms. */
        @SerialName("slide_starts_ms") val slideStartsMs: List<Long>? = null,
    )

    /**
     * Đủ 4 mốc, tăng dần, và mỗi thẻ nằm trong [MIN_SLIDE_MS]..[MAX_SLIDE_MS].
     * Danh sách hỏng bị bỏ qua để rơi về `slide_ms` — thà nhịp đều còn hơn một thẻ
     * đứng im vô hạn hoặc nhấp nháy vì số ghi sai tay.
     */
    private fun List<Long>.isValidSlideStarts(): Boolean {
        if (size != WORD_SLIDES) return false
        var previous = 0L
        for (mark in this) {
            if (mark - previous !in MIN_SLIDE_MS..MAX_SLIDE_MS) return false
            previous = mark
        }
        return true
    }

    private companion object {
        const val TAG = "ChantMetaRepository"

        // Chặn hai đầu: một con số hỏng trong JSON không được phép làm thẻ đứng im
        // (quá dài) hay nhấp nháy (quá ngắn) — trẻ không có cách nào thoát khỏi đó.
        const val MIN_SLIDE_MS = 800L
        const val MAX_SLIDE_MS = 8_000L
        const val WORD_SLIDES = 4

        val jsonParser = Json { ignoreUnknownKeys = true }
    }
}
