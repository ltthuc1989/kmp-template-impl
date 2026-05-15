package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.model.Story
import me.ltthuc.kmp.core.model.StoryScene
import me.ltthuc.kmp.core.model.WordTiming
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads story narratives from bundled JSON. Each story has 4 scenes that match
 * `stories/<id>/scene_<N>.mp3` audio files. Stories are static reference data —
 * not stored in Room, parsed on demand and cached by caller if needed.
 */
class StoryRepository {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadStories(level: Int): List<Story> {
        val path = "files/stories/level_$level.json"
        return runCatching {
            val bytes = Res.readBytes(path)
            val list = jsonParser.decodeFromString<List<StoryDto>>(bytes.decodeToString())
            list.map { it.toModel(level) }
        }.onFailure {
            Napier.e(tag = TAG, throwable = it) { "Failed to load stories from $path" }
        }.getOrDefault(emptyList())
    }

    suspend fun storyForUnit(level: Int, unitNumber: Int): Story? =
        loadStories(level).firstOrNull { it.afterUnit == unitNumber }

    @Serializable
    private data class StoryDto(
        val id: String,
        val title: String,
        val level: Int,
        @SerialName("after_unit") val afterUnit: Int,
        @SerialName("phonics_used") val phonicsUsed: List<String> = emptyList(),
        @SerialName("duration_seconds") val durationSeconds: Int = 0,
        val scenes: List<SceneDto> = emptyList(),
    ) {
        fun toModel(loadedLevel: Int) = Story(
            id = id,
            title = title,
            level = level,
            afterUnit = afterUnit,
            phonicsUsed = phonicsUsed,
            durationSeconds = durationSeconds,
            scenes = scenes.map { it.toModel(loadedLevel, id) },
        )
    }

    @Serializable
    private data class SceneDto(
        val scene: Int,
        val name: String,
        @SerialName("duration_sec") val durationSec: Int,
        val text: String,
        @SerialName("image_path_landscape") val imagePathLandscape: String? = null,
        @SerialName("image_path_portrait") val imagePathPortrait: String? = null,
        @SerialName("word_timings") val wordTimings: List<WordTimingDto> = emptyList(),
    ) {
        fun toModel(level: Int, storyId: String) = StoryScene(
            sceneNumber = scene,
            name = name,
            durationSec = durationSec,
            text = text,
            // Fallback to convention path if not in JSON.
            imagePathLandscape = imagePathLandscape
                ?: "files/images/level_$level/stories/$storyId/scene_$scene.webp",
            imagePathPortrait = imagePathPortrait,
            wordTimings = wordTimings.map { it.toModel() },
        )
    }

    @Serializable
    private data class WordTimingDto(
        val word: String,
        @SerialName("start_ms") val startMs: Int,
        @SerialName("end_ms") val endMs: Int,
    ) {
        fun toModel() = WordTiming(word = word, startMs = startMs, endMs = endMs)
    }

    private companion object {
        const val TAG = "StoryRepository"
        val jsonParser = Json { ignoreUnknownKeys = true }
    }
}
