package me.ltthuc.kmp.core.model

/**
 * Narrated story shown on Step 7. One [Story] maps 1-1 to the unit it closes (unit_number).
 * Each story has 4 fixed scenes (intro / problem / solution / ending), 5s each,
 * matching the on-disk audio layout `stories/<id>/scene_<N>.mp3`.
 */
data class Story(
    val id: String,
    val title: String,
    val level: Int,
    val unitNumber: Int,
    val phonicsUsed: List<String>,
    val durationSeconds: Int,
    val scenes: List<StoryScene>,
)

data class StoryScene(
    val sceneNumber: Int,
    val name: String,
    val durationSec: Int,
    val text: String,
    val imagePathLandscape: String? = null,
    val imagePathPortrait: String? = null,
    val wordTimings: List<WordTiming> = emptyList(),
)

/**
 * Word-level timestamp for karaoke sync. Extracted from MP3 audio via Whisper.
 */
data class WordTiming(
    val word: String,
    val startMs: Int,
    val endMs: Int,
)
