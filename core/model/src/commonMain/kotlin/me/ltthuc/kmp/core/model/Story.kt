package me.ltthuc.kmp.core.model

/**
 * Narrated story shown on Step 7. One [Story] maps 1-1 to a unit (after_unit).
 * Each story has 4 fixed scenes (intro / problem / solution / ending), 5s each,
 * matching the on-disk audio layout `stories/<id>/scene_<N>.mp3`.
 */
data class Story(
    val id: String,
    val title: String,
    val level: Int,
    val afterUnit: Int,
    val phonicsUsed: List<String>,
    val durationSeconds: Int,
    val scenes: List<StoryScene>,
)

data class StoryScene(
    val sceneNumber: Int,
    val name: String,
    val durationSec: Int,
    val text: String,
)
