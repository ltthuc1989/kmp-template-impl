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

    /**
     * One continuous blend chain for a single Level 2+ page — the whole `a, n, an, f, an, fan`
     * run spoken in one take, at `blend/<NN>_<word>.mp3`. [index] is the 0-based page index.
     *
     * Deliberately one file per page rather than per lesson: the screen repeats each page, and
     * replaying a slice of a longer file would need seek support in `AudioPlayer`. Per-page files
     * make a repeat just another `play()`. Segment offsets inside the file live in
     * `files/blend_meta/level_<n>.json` so the animation can follow real audio position.
     */
    data class Blend(
        override val lessonFolder: String,
        val word: String,
        val index: Int,
    ) : AudioRef

    /**
     * One line of the Level 2+ step-0 blending sequence, at
     * `blend_intro/<NN>_<key>.mp3`. A lesson has two kinds of line:
     *
     *   guide  "a · d · ad"          — teaches the rime, [key] is the rime ("ad")
     *   word   "d · ad · dad ‖ dad"  — builds a word, [key] is the word ("dad")
     *
     * [index] is the 0-based position in the lesson's sequence and matches the
     * filename prefix, so a lesson teaching two rimes runs 00_ad, 01_dad, 02_pad,
     * 03_ag, 04_bag, 05_rag. Build the list with `PhonicsLesson.soundBlendRefs()`
     * rather than by hand — the guide line only appears at the first word of each
     * rime, and single-vowel lessons have no guide at all.
     *
     * Separate from [SoundIntro] (Level 1: one long teaching paragraph per lesson)
     * because the screen plays these per word, so one file per line is what lets
     * playback follow what the child taps.
     */
    data class SoundBlend(
        override val lessonFolder: String,
        val key: String,
        val index: Int,
    ) : AudioRef

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
     * Blended short-vowel rime (onset+rime phonics), bundled at `files/audio/rimes/<rime>.mp3`
     * (lowercased). Says the rime as one blended unit (e.g. "an", "am", "at"). Keyed only by
     * [rime]; not tied to a specific lesson folder. Used by the Bubble Pop game.
     */
    data class Rime(val rime: String) : AudioRef {
        override val lessonFolder: String get() = "rimes"
    }

    /**
     * Same rime, but the take used by the Level 2+ blending screen, at
     * `files/audio/rimes_blend/<rime>.mp3`.
     *
     * Deliberately a separate file set from [Rime]: the blending screen's chain audio is built
     * from the sound bank, so its rime card has to speak the SAME take, or one "an" plays on
     * auto and a different "an" plays on tap. The game keeps the older set, which the user
     * judged better standing alone inside a bubble.
     */
    data class RimeBlend(val rime: String) : AudioRef {
        override val lessonFolder: String get() = "rimes_blend"
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
     * Round-start prompt for the Bubble Pop game at Level 2+, bundled at
     * `files/audio/find_rime/<rime>.mp3`. Says "Can you find the <rime> sound?".
     *
     * Separate from [FindSound]: the rime "a" (lesson L2U1_a) would otherwise overwrite
     * the letter "a" prompt, and the two say different things.
     */
    data class FindRime(val rime: String) : AudioRef {
        override val lessonFolder: String get() = "find_rime"
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
