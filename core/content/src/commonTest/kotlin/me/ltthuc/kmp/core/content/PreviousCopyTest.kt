package me.ltthuc.kmp.core.content

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The scenario this whole mechanism exists for: an app update rewrites an asset's hash while
 * the device is offline. Without the index the lesson goes silent; with it the previous take
 * plays until the new bytes arrive.
 */
class PreviousCopyTest {

    private val path = "audio/level_2/unit_05/chant.mp3"
    private val oldManifest = manifestOf(path to asset("hash-old", "L2U5"))
    private val newManifest = manifestOf(path to asset("hash-new", "L2U5"))

    private fun locator(files: FakePackFiles, index: PackIndex, source: ManifestSource) =
        AssetLocator(source, files, index, "https://cdn.test/content")

    @Test
    fun playsThePreviousCopyWhenTheUpdatedBytesHaveNotArrived() = runTest {
        val files = FakePackFiles()
        val index = PackIndex(files)

        // Yesterday's app: the unit downloaded and was finished.
        ContentPackDownloader(
            FlakyServer().client(),
            FakeManifestSource(oldManifest),
            locator(files, index, FakeManifestSource(oldManifest)),
            files,
            index,
        ).download("L2U5").collect { }

        // Today's app ships a manifest where that asset changed. Nothing has been fetched yet.
        val resolved = locator(files, index, FakeManifestSource(newManifest)).resolve(path)

        assertIs<AssetSource.Local>(resolved, "offline after an update must still find audio")
        assertEquals("/packs/hash-old", resolved.path)
    }

    @Test
    fun prefersTheUpdatedBytesOverThePreviousCopy() = runTest {
        val files = FakePackFiles()
        val index = PackIndex(files)
        index.record(mapOf(path to "hash-old"))
        files.seed("hash-old")
        files.seed("hash-new")

        val resolved = locator(files, index, FakeManifestSource(newManifest)).resolve(path)

        assertIs<AssetSource.Local>(resolved)
        assertEquals("/packs/hash-new", resolved.path, "the current hash always wins")
    }

    @Test
    fun fallsBackToRemoteWhenNoCopyOfAnyVintageIsOnDisk() = runTest {
        val files = FakePackFiles()
        val resolved = locator(files, PackIndex(files), FakeManifestSource(newManifest)).resolve(path)

        assertIs<AssetSource.Remote>(resolved)
    }

    @Test
    fun ignoresAnIndexEntryWhoseFileHasBeenDeleted() = runTest {
        val files = FakePackFiles()
        val index = PackIndex(files)
        index.record(mapOf(path to "hash-old"))
        // Recorded, but the bytes are gone — a stale entry must never produce a dead path.
        val resolved = locator(files, index, FakeManifestSource(newManifest)).resolve(path)

        assertIs<AssetSource.Remote>(resolved)
    }

    @Test
    fun survivesAProcessRestartBecauseTheIndexIsPersisted() = runTest {
        val files = FakePackFiles()
        PackIndex(files).record(mapOf(path to "hash-old"))
        files.seed("hash-old")

        // A brand new instance reads what the previous one wrote, exactly like a cold start.
        assertEquals("hash-old", PackIndex(files).hashFor(path))
    }

    @Test
    fun pruneMissingDropsEntriesWhoseFilesAreGone() = runTest {
        val files = FakePackFiles()
        val index = PackIndex(files)
        index.record(mapOf(path to "hash-old", "other.mp3" to "hash-kept"))
        files.seed("hash-kept")

        index.pruneMissing()

        assertNull(index.hashFor(path))
        assertEquals("hash-kept", index.hashFor("other.mp3"))
    }
}
