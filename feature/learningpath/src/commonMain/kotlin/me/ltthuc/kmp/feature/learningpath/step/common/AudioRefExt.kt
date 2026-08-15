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
    val match = LESSON_ID_REGEX.find(id) ?: return null
    val (level, unit) = match.destructured
    // Use the lesson's `letter` code verbatim: L1 = single letter ("A"), L2+ = rime code
    // ("SHORT-A-AN"). Matches the opw_audio_project folder layout `L{n}U{nn}_{LETTER}_{word}`.
    val letterCode = letter.trim().takeIf { it.isNotEmpty() } ?: return null
    val repWord = words.firstOrNull()?.word?.lowercase()?.replace(' ', '_')?.replace('-', '_')
        ?: return null
    return "L${level}U${unit.padStart(2, '0')}_${letterCode}_$repWord"
}

internal fun PhonicsLesson.soundIntroRef(): AudioRef.SoundIntro? =
    audioFolderName()?.let(AudioRef::SoundIntro)

internal fun PhonicsLesson.chantRef(): AudioRef.Chant? =
    audioFolderName()?.let(AudioRef::Chant)

internal fun PhonicsLesson.wordRef(word: String): AudioRef.Word? {
    val folder = audioFolderName() ?: return null
    val idx = words.indexOfFirst { it.word == word }
    if (idx < 0) return null
    return AudioRef.Word(folder, word, idx)
}

internal fun PhonicsLesson.sentenceRef(word: String): AudioRef.Sentence? {
    val folder = audioFolderName() ?: return null
    val idx = words.indexOfFirst { it.word == word }
    if (idx < 0) return null
    return AudioRef.Sentence(folder, word, idx)
}

internal fun PhonicsLesson.phonemeRef(): AudioRef.Phoneme? =
    audioFolderName()?.let(AudioRef::Phoneme)

/**
 * The Level 2+ step-0 blending sequence, in play order.
 *
 * Mirrors how `opw_audio_project` names the generated files, so the order here IS the
 * filename order (00_ad, 01_dad, 02_pad, 03_ag, ...). Two shapes:
 *
 *   rime lessons ("SHORT-A-AD-AG")  guide line at the FIRST word of each rime, then one
 *                                   line per word: ad, dad, pad, ag, bag, rag
 *   vowel lessons ("SHORT-A")       no rime to teach, so one line per word: cat, ant, ...
 *
 * The guide sits at each rime's first word, not only at the start, because a lesson that
 * teaches two rimes must introduce the second one too.
 *
 * Returns empty when the lesson has no audio folder (see [audioFolderName]).
 */
/**
 * Chuỗi audio cho step 0, tự chọn theo cấp độ:
 *
 *   Level 1    một file dạy dài  → [soundIntroRef]
 *   Level 2+   4-6 file rời      → [soundBlendRefs]
 *
 * Phân biệt bằng cách CÓ chuỗi blend hay không, chứ không đọc số cấp độ: lesson
 * nào đã có bộ file blend thì dùng bộ đó, chưa có thì rơi về file dạy dài. Nhờ vậy
 * cấp độ mới không cần sửa hàm này, và cấp độ chưa sinh audio blend vẫn chạy.
 */
internal fun PhonicsLesson.step0Refs(): List<AudioRef> =
    soundBlendRefs().ifEmpty { listOfNotNull(soundIntroRef()) }

internal fun PhonicsLesson.soundBlendRefs(): List<AudioRef.SoundBlend> {
    // Chặn ở Level 1: lesson L1 có `letter` là một chữ cái ("A") nên không tách ra
    // vần nào, và nhánh "lesson nguyên âm đơn" bên dưới sẽ vô tình sinh ref cho từng
    // từ — trỏ vào file blend_intro không hề tồn tại. L1 dùng file dạy dài
    // (00_sound_intro.mp3), không có bộ blend.
    val level = LESSON_ID_REGEX.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: return emptyList()
    if (level < FIRST_BLEND_LEVEL) return emptyList()

    val folder = audioFolderName() ?: return emptyList()
    // "SHORT-A-AD-AG" -> ["ad", "ag"]; "SHORT-A" -> []. Segment 0 is "SHORT", 1 is the vowel.
    val rimes = letter.uppercase().split('-').drop(2).map { it.lowercase() }
    val refs = mutableListOf<AudioRef.SoundBlend>()

    if (rimes.isEmpty()) {
        words.forEachIndexed { index, lessonWord ->
            refs += AudioRef.SoundBlend(folder, lessonWord.word.lowercase(), index)
        }
        return refs
    }

    val taught = mutableSetOf<String>()
    for (lessonWord in words) {
        val word = lessonWord.word.lowercase()
        val rime = rimes.firstOrNull { word.endsWith(it) && word.length > it.length } ?: rimes.first()
        if (taught.add(rime)) {
            refs += AudioRef.SoundBlend(folder, rime, refs.size)
        }
        refs += AudioRef.SoundBlend(folder, word, refs.size)
    }
    return refs
}

private val LESSON_ID_REGEX = Regex("""^L(\d+)U(\d+)_""")

/** Cấp độ đầu tiên dạy ghép vần, tức cấp đầu tiên có bộ file `blend_intro/`. */
private const val FIRST_BLEND_LEVEL = 2
