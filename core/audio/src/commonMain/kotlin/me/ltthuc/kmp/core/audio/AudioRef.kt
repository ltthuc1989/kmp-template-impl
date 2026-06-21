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

    /**
     * Single-phoneme letter sound for the Bubble Pop game, bundled at
     * `files/audio/phonemes/<letter>.mp3` (lowercased). The phoneme is spoken once
     * (~1-2s) — distinct from [SoundIntro], which is the long teaching paragraph.
     * Not tied to lesson curriculum: keyed only by [letter] (e.g. "A").
     */
    data class LetterSound(val letter: String) : AudioRef {
        override val lessonFolder: String get() = "phonemes"
    }

    /**
     * Round-start prompt for the Bubble Pop game, bundled at
     * `files/audio/find_sound/<letter>.mp3` (lowercased). Says "Can you find the <phoneme>
     * sound?" — played once when a round begins, before the bubbles appear. Keyed by [letter].
     */
    data class FindSound(val letter: String) : AudioRef {
        override val lessonFolder: String get() = "find_sound"
    }

    /**
     * Short UI sound effects (correct/wrong/etc.) bundled at `files/sfx/<name>.mp3`.
     * Not tied to lesson content. [name] is the SFX identifier (e.g. "correct", "wrong").
     */
    data class Sfx(val name: String) : AudioRef {
        override val lessonFolder: String get() = "sfx"
    }

    /**
     * Voice praise / nudge bundled at `files/sfx/voice/<name>.mp3`. Khan Kids–style
     * warm narrator, fired only when voice toggle enabled. Never gates lesson phonemes.
     */
    data class Voice(val name: String) : AudioRef {
        override val lessonFolder: String get() = "sfx/voice"
    }

    /**
     * Background music loop bundled at `files/sfx/music/<name>.mp3`. Ducks under
     * lesson phoneme audio per the standard mix-bus rules.
     */
    data class Music(val name: String) : AudioRef {
        override val lessonFolder: String get() = "sfx/music"
    }

    /**
     * Spoken UI guidance prompt ("Let's trace the letter!"), bundled at
     * `files/sfx/prompts/<lang>/<promptId>.mp3`. Localized: [lang] is the effective UI
     * language code ("en" / "vi"). Played once on screen entry; gated by the voice toggle
     * like [Voice]. [promptId] is the screen/action key (e.g. "vp_step_trace").
     */
    data class Prompt(val promptId: String, val lang: String) : AudioRef {
        override val lessonFolder: String get() = "prompts"
    }
}
