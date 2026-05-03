package me.ltthuc.kmp.core.audio

/**
 * Typesafe reference to an audio asset. Resolver maps this into a Firebase Storage URL
 * and a local cache path. Naming convention matches `opw_audio_project` output:
 * `level_{n}/unit_{nn}/L{n}U{nn}_{LETTER}_{repWord}/{NN}_{step}.mp3`.
 *
 * `lessonFolder` is the per-lesson folder name (e.g. "L1U01_A_apple"). Caller resolves
 * it from PhonicsLesson — keeps this layer free of curriculum coupling.
 */
sealed interface AudioRef {
    val lessonFolder: String

    data class SoundIntro(override val lessonFolder: String) : AudioRef
    data class Chant(override val lessonFolder: String) : AudioRef

    /**
     * [index] is the 0-based position of [word] inside the lesson's vocab list.
     * Resolver uses (index + 1).pad2() as filename prefix to match opw_audio_project
     * output layout `vocab/<NN>_<word>.mp3`.
     */
    data class Word(override val lessonFolder: String, val word: String, val index: Int) : AudioRef
    data class Sentence(override val lessonFolder: String, val word: String, val index: Int) : AudioRef
    data class Phoneme(override val lessonFolder: String) : AudioRef

    /**
     * Story narration is split into 4 scenes (intro / problem / solution / ending),
     * one mp3 per scene at `stories/<storyId>/scene_<sceneNumber>.mp3`. [sceneNumber]
     * is 1-based to match opw_audio_project filenames.
     */
    data class Story(val storyId: String, val sceneNumber: Int) : AudioRef {
        override val lessonFolder: String get() = "stories"
    }
}
