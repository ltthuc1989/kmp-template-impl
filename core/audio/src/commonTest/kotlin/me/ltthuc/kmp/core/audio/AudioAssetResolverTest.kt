package me.ltthuc.kmp.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Multi-word pages must resolve to the file name the generator actually writes. Level 4 has
 * `ice cream` and `cell phone`; before the fix the Blend path kept the space and both pages
 * played nothing — no crash, no log. `yo-yo` (Level 1 vocab) had the same bug with a hyphen.
 */
class AudioAssetResolverTest {

    private val resolver = AudioAssetResolver()

    @Test
    fun blendWordWithSpaceUsesUnderscore() {
        assertEquals(
            "audio/level_4/unit_08/L4U08_C_rice/blend/03_ice_cream.mp3",
            resolver.logicalPath(AudioRef.Blend("L4U08_C_rice", "ice cream", 2)),
        )
    }

    @Test
    fun vocabWordWithHyphenUsesUnderscore() {
        assertEquals(
            "audio/level_1/unit_08/L1U08_Y_yo_yo/vocab/01_yo_yo.mp3",
            resolver.logicalPath(AudioRef.Word("L1U08_Y_yo_yo", "yo-yo", 0)),
        )
    }

    @Test
    fun singleWordIsOnlyLowercased() {
        assertEquals(
            "audio/level_4/unit_01/L4U01_BL-CL_black/blend/01_black.mp3",
            resolver.logicalPath(AudioRef.Blend("L4U01_BL-CL_black", "Black", 0)),
        )
    }
}
