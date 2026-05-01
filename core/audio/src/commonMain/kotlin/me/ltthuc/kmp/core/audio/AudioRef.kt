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
    data class Word(override val lessonFolder: String, val word: String) : AudioRef
    data class Sentence(override val lessonFolder: String, val word: String) : AudioRef
    data class Phoneme(override val lessonFolder: String) : AudioRef

    /** Story narration lives outside lesson folders (one file per story). */
    data class Story(val storyId: String) : AudioRef {
        override val lessonFolder: String get() = "stories"
    }
}
