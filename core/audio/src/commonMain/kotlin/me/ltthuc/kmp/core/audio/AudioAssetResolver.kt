package me.ltthuc.kmp.core.audio

/**
 * Turns an [AudioRef] into its **logical path** — the one identifier every layer agrees on:
 *
 * ```
 * audio/level_1/unit_01/L1U01_A_apple/00_sound_intro.mp3
 * audio/level_2/stories/L2_S03/scene_1.mp3
 * sfx/prompts/en/vp_step_chant.mp3
 * ```
 *
 * The same string is the key in `content_manifest.json`, the path under `composeResources/files/`,
 * and the tail of the CDN URL. Deciding *where* those bytes come from is `AssetLocator`'s job
 * in `core:content`; this class only names them. Layout mirrors `opw_audio_project` output —
 * the generator is the source of truth and code follows it, never the other way round.
 */
class AudioAssetResolver {

    fun logicalPath(ref: AudioRef): String = when (ref) {
        // UI sound layer ships in the app and lives outside the per-level content tree.
        is AudioRef.Sfx -> "sfx/${ref.name}.mp3"
        is AudioRef.Voice -> "sfx/voice/${ref.name}.mp3"
        is AudioRef.Music -> "sfx/music/${ref.name}.mp3"
        is AudioRef.Prompt -> "sfx/prompts/${ref.lang}/${ref.promptId}.mp3"
        else -> "audio/${contentPath(ref)}"
    }

    private fun contentPath(ref: AudioRef): String = when (ref) {
        is AudioRef.SoundIntro -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_SOUND_INTRO"
        is AudioRef.Chant -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_CHANT"
        is AudioRef.Word ->
            "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/vocab/${(ref.index + 1).pad2()}_${ref.word.lowercase()}.mp3"
        is AudioRef.Sentence ->
            "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/sentences/${(ref.index + 1).pad2()}_${ref.word.lowercase()}.mp3"
        is AudioRef.Blend ->
            "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/blend/${(ref.index + 1).pad2()}_${ref.word.lowercase()}.mp3"
        is AudioRef.SoundBlend ->
            "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/blend_intro/${ref.index.pad2()}_${ref.key.lowercase()}.mp3"
        is AudioRef.Phoneme -> "${ref.lessonFolder.toUnitPath()}/${ref.lessonFolder}/$FILE_PHONEME"
        is AudioRef.Story -> "${ref.storyId.toStoryLevelPath()}/stories/${ref.storyId}/scene_${ref.sceneNumber}.mp3"
        is AudioRef.LetterSound -> "phonemes/${ref.letter.lowercase()}.mp3"
        is AudioRef.Rime -> "rimes/${ref.rime.lowercase()}.mp3"
        is AudioRef.RimeBlend -> "rimes_blend/${ref.rime.lowercase()}.mp3"
        is AudioRef.FindSound -> "find_sound/${ref.letter.lowercase()}.mp3"
        is AudioRef.FindRime -> "find_rime/${ref.rime.lowercase()}.mp3"
        is AudioRef.Sfx, is AudioRef.Voice, is AudioRef.Music, is AudioRef.Prompt ->
            error("SFX refs are handled by logicalPath(), not contentPath()")
    }

    /** "L1_S01" -> "level_1" */
    private fun String.toStoryLevelPath(): String {
        val match = STORY_LEVEL_REGEX.find(this)
        val level = match?.groupValues?.get(1)?.toIntOrNull() ?: DEFAULT_LEVEL
        return "level_$level"
    }

    private fun Int.pad2(): String = toString().padStart(2, '0')

    /** "L1U01_A_apple" -> "level_1/unit_01" */
    private fun String.toUnitPath(): String {
        val match = LESSON_FOLDER_REGEX.matchEntire(this)
            ?: error("Invalid lessonFolder '$this'. Expected pattern: L{n}U{nn}_{LETTER}_{word}.")
        val (level, unit) = match.destructured
        return "level_${level.toInt()}/unit_${unit.padStart(2, '0')}"
    }

    private companion object {
        const val FILE_SOUND_INTRO = "00_sound_intro.mp3"
        const val FILE_CHANT = "02_chant.mp3"
        const val FILE_PHONEME = "04_phoneme.mp3"
        const val DEFAULT_LEVEL = 1

        // Letter segment accepts L1 single letters ("A") and L2+ rime codes ("SHORT-A-AN").
        val LESSON_FOLDER_REGEX = Regex("""L(\d+)U(\d+)_[A-Za-z0-9][A-Za-z0-9_-]*_[a-z_]+""")
        val STORY_LEVEL_REGEX = Regex("""L(\d+)_""")
    }
}
