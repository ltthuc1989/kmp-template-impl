package me.ltthuc.kmp.core.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the sweep is allowed to delete on the first launch after an app update — the moment
 * the manifest has new hashes and nothing has been downloaded yet.
 *
 * Getting this wrong is not a tidiness bug: deleting a predecessor too eagerly is what left a
 * finished unit silent until the device was next online.
 */
class SweepStaleFilesTest {

    private val chant = "audio/level_2/unit_05/chant.mp3"
    private val word = "audio/level_2/unit_05/word.mp3"

    private fun keep(
        current: Map<String, String>,
        indexed: Map<String, String>,
        onDisk: Set<String>,
    ): Set<String> = hashesToKeep(current, indexed, onDisk::contains)

    @Test
    fun keepsThePredecessorWhileItsReplacementIsMissing() {
        val keep = keep(
            current = mapOf(chant to "hash-new"),
            indexed = mapOf(chant to "hash-old"),
            onDisk = setOf("hash-old"),
        )

        assertTrue("hash-old" in keep, "deleting this is what silenced a finished unit")
    }

    @Test
    fun dropsThePredecessorOnceTheReplacementIsOnDisk() {
        val keep = keep(
            current = mapOf(chant to "hash-new"),
            indexed = mapOf(chant to "hash-old"),
            onDisk = setOf("hash-old", "hash-new"),
        )

        assertFalse("hash-old" in keep, "a predecessor is garbage the moment it is redundant")
        assertTrue("hash-new" in keep)
    }

    @Test
    fun dropsAnAssetRemovedFromTheCurriculumEntirely() {
        val keep = keep(
            current = emptyMap(),
            indexed = mapOf("audio/level_2/unit_05/retired.mp3" to "hash-retired"),
            onDisk = setOf("hash-retired"),
        )

        assertFalse("hash-retired" in keep, "nothing will ever ask for it again")
    }

    @Test
    fun ignoresFilesTheIndexNeverRecorded() {
        // Written before the index existed, or by a download cancelled mid-pack.
        val keep = keep(
            current = mapOf(chant to "hash-new"),
            indexed = emptyMap(),
            onDisk = setOf("hash-orphan"),
        )

        assertEquals(setOf("hash-new"), keep, "an unattributable file is still garbage")
    }

    @Test
    fun anUnchangedAssetNeedsNoExceptionAtAll() {
        val keep = keep(
            current = mapOf(word to "hash-same"),
            indexed = mapOf(word to "hash-same"),
            onDisk = setOf("hash-same"),
        )

        assertEquals(setOf("hash-same"), keep)
    }

    @Test
    fun holdsOneReprievePerChangedAssetAndNoMore() {
        // Two assets changed, one already re-downloaded: only the still-missing one keeps its
        // predecessor, so the reprieve shrinks as the update lands rather than accumulating.
        val keep = keep(
            current = mapOf(chant to "chant-new", word to "word-new"),
            indexed = mapOf(chant to "chant-old", word to "word-old"),
            onDisk = setOf("chant-old", "word-old", "word-new"),
        )

        assertEquals(setOf("chant-new", "word-new", "chant-old"), keep)
    }
}
