package me.ltthuc.kmp.feature.learningpath.step.common

import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.model.PhonicsLesson

/**
 * Maps a [PhonicsLesson] to the audio asset folder name produced by `opw_audio_project`,
 * e.g. lesson `L1U1_A` (with first word `apple`) → `L1U01_A_apple`. Unit number is
 * zero-padded to two digits to match the on-disk layout.
 *
 * Returns null when the lesson id does not match the expected `L{n}U{n}_{LETTER}`
 * pattern or when the lesson has no vocabulary words to source the rep word from.
 */
internal fun PhonicsLesson.audioFolderName(): String? {
    val match = LESSON_ID_REGEX.matchEntire(id) ?: return null
    val (level, unit, letter) = match.destructured
    val repWord = words.firstOrNull()?.word?.lowercase()?.replace(' ', '_')?.replace('-', '_')
        ?: return null
    return "L${level}U${unit.padStart(2, '0')}_${letter}_$repWord"
}

internal fun PhonicsLesson.soundIntroRef(): AudioRef.SoundIntro? =
    audioFolderName()?.let(AudioRef::SoundIntro)

internal fun PhonicsLesson.chantRef(): AudioRef.Chant? =
    audioFolderName()?.let(AudioRef::Chant)

internal fun PhonicsLesson.wordRef(word: String): AudioRef.Word? =
    audioFolderName()?.let { AudioRef.Word(it, word) }

internal fun PhonicsLesson.sentenceRef(word: String): AudioRef.Sentence? =
    audioFolderName()?.let { AudioRef.Sentence(it, word) }

internal fun PhonicsLesson.phonemeRef(): AudioRef.Phoneme? =
    audioFolderName()?.let(AudioRef::Phoneme)

private val LESSON_ID_REGEX = Regex("""L(\d+)U(\d+)_([A-Z])""")
