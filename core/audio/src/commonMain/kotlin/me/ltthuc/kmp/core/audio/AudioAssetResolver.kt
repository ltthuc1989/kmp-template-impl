package me.ltthuc.kmp.core.audio

/**
 * Builds Firebase Storage download URLs and stable cache keys from [AudioRef].
 *
 * Firebase REST URL: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{percent_encoded_path}?alt=media
 * Storage path layout (matches opw_audio_project output):
 *   content/level_1/unit_01/L1U01_A_apple/00_sound_intro.mp3
 *   content/stories/L1_S01.mp3
 *
 * Cache key is a flat filename (no directory) — easy LRU eviction.
 */
class AudioAssetResolver(
    private val storageBucket: String,
    private val contentRoot: String = "content",
) {
    fun downloadUrl(ref: AudioRef): String {
        val path = "$contentRoot/${storagePath(ref)}"
        return "https://firebasestorage.googleapis.com/v0/b/$storageBucket/o/${path.percentEncode()}?alt=media"
    }

    fun cacheKey(ref: AudioRef): String = storagePath(ref).replace('/', '_')

    /**
     * Path under Compose Resources (`core:resource/composeResources/`). Use as
     * `Res.readBytes(bundledResourcePath(ref))` to load files bundled into the app
     * before falling back to network. Returns null when this asset is definitely
     * not bundled. Returning a path doesn't guarantee the file exists — caller must
     * still handle the IO miss (falls back to Firebase). Current bundle policy:
     * any L1 Unit 1 lesson audio (sound_intro / chant / word / sentence / phoneme).
     */
    fun bundledResourcePath(ref: AudioRef): String? = when (ref) {
        is AudioRef.Story -> null
        else -> "files/audio/${storagePath(ref)}".takeIf { ref.lessonFolder.startsWith(L1_U1_PREFIX) }
    }

    private fun storagePath(ref: AudioRef): String = when (ref) {
        is AudioRef.SoundIntro -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_SOUND_INTRO"
        is AudioRef.Chant -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_CHANT"
        is AudioRef.Word -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/02_word_${ref.word.lowercase()}.mp3"
        is AudioRef.Sentence -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/03_sentence_${ref.word.lowercase()}.mp3"
        is AudioRef.Phoneme -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_PHONEME"
        is AudioRef.Story -> "stories/${ref.storyId}.mp3"
    }

    /** "L1U01_A_apple" -> "level_1/unit_01" */
    private fun String.toUnitPath(): String {
        val match = LESSON_FOLDER_REGEX.matchEntire(this)
            ?: error("Invalid lessonFolder '$this'. Expected pattern: L{n}U{nn}_{LETTER}_{word}.")
        val (level, unit) = match.destructured
        return "level_${level.toInt()}/unit_${unit.padStart(2, '0')}"
    }

    private fun String.percentEncode(): String = buildString {
        for (ch in this@percentEncode) {
            when {
                ch.isLetterOrDigit() || ch in UNRESERVED -> append(ch)
                else -> append('%').append(ch.code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

    private companion object {
        const val FILE_SOUND_INTRO = "00_sound_intro.mp3"
        const val FILE_CHANT = "01_chant.mp3"
        const val FILE_PHONEME = "04_phoneme.mp3"
        const val UNRESERVED = "-._~"
        const val L1_U1_PREFIX = "L1U01_"
        val LESSON_FOLDER_REGEX = Regex("""L(\d+)U(\d+)_[A-Z]_[a-z_]+""")
    }
}
