package me.ltthuc.kmp.core.content

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The download path is what stands between a paying parent and a silent lesson, so these cover
 * the awkward cases rather than the happy one: a half-finished pack, a flaky connection, a file
 * missing from the CDN, a full device.
 */
class ContentPackDownloaderTest {

    private val manifest = manifestOf(
        "audio/level_1/unit_03/a.mp3" to asset("hash-a", "L1U3", bytes = 100),
        "audio/level_1/unit_03/b.mp3" to asset("hash-b", "L1U3", bytes = 200),
        "images/level_1/stories/L1_S03/scene_1.webp" to asset("hash-c", "L1U3", bytes = 300),
        "audio/level_1/unit_04/d.mp3" to asset("hash-d", "L1U4", bytes = 400),
    )

    private fun downloader(
        files: FakePackFiles = FakePackFiles(),
        server: FlakyServer = FlakyServer(),
        source: ManifestSource = FakeManifestSource(manifest),
        index: PackIndex = PackIndex(files),
    ): ContentPackDownloader {
        val locator = AssetLocator(source, files, index, "https://cdn.test/content")
        return ContentPackDownloader(server.client(), source, locator, files, index)
    }

    @Test
    fun downloadsEveryFileInThePackAndNothingFromAnother() = runTest {
        val files = FakePackFiles()
        downloader(files).download("L1U3").toList()

        assertEquals(setOf("hash-a", "hash-b", "hash-c"), files.storedHashes)
        assertFalse(files.has("hash-d"), "L1U4 belongs to a different pack")
    }

    @Test
    fun audioAndStoryImagesTravelTogether() = runTest {
        // A story with narration but no picture is a broken lesson, so the image must be in the
        // same pack and arrive with it.
        val files = FakePackFiles()
        downloader(files).download("L1U3").toList()

        assertTrue(files.has("hash-c"))
    }

    @Test
    fun skipsFilesAlreadyOnDiskSoAnInterruptedDownloadResumes() = runTest {
        val files = FakePackFiles().apply { seed("hash-a") }
        val server = FlakyServer()

        downloader(files, server).download("L1U3").toList()

        assertEquals(2, server.requestCount, "the file already on disk must not be fetched again")
        assertEquals(setOf("hash-a", "hash-b", "hash-c"), files.storedHashes)
    }

    @Test
    fun aFullyDownloadedPackCompletesWithoutTouchingTheNetwork() = runTest {
        val files = FakePackFiles().apply {
            seed("hash-a")
            seed("hash-b")
            seed("hash-c")
        }
        val server = FlakyServer()

        val progress = downloader(files, server).download("L1U3").toList()

        assertEquals(0, server.requestCount)
        assertTrue(progress.last().isComplete)
    }

    @Test
    fun anUnknownPackIsANoOpRatherThanAnError() = runTest {
        val progress = downloader().download("L9U9").toList()
        assertTrue(progress.last().isComplete)
    }

    @Test
    fun progressEndsAtEveryFileAndEveryByte() = runTest {
        val progress = downloader().download("L1U3").toList()
        val last = progress.last()

        assertEquals(3, last.filesDone)
        assertEquals(3, last.filesTotal)
        assertEquals(600L, last.bytesDone)
        assertEquals(600L, last.bytesTotal)
        assertEquals(1f, last.fraction)
    }

    @Test
    fun progressCountsOnlyWhatIsStillMissing() = runTest {
        // Resuming must not report "1 of 3" when two files are already here — the bar would
        // crawl through work that is already done.
        val files = FakePackFiles().apply {
            seed("hash-a")
            seed("hash-b")
        }

        val last = downloader(files).download("L1U3").toList().last()

        assertEquals(1, last.filesTotal)
        assertEquals(300L, last.bytesTotal)
    }

    @Test
    fun aFlakyConnectionIsRetriedAndSucceeds() = runTest {
        val files = FakePackFiles()
        val server = FlakyServer(failuresPerUrl = 2)

        downloader(files, server).download("L1U3").toList()

        assertEquals(setOf("hash-a", "hash-b", "hash-c"), files.storedHashes)
        assertEquals(3, server.attemptsFor("hash-a"), "two failures then a success")
    }

    @Test
    fun givesUpAfterThreeAttemptsPerFile() = runTest {
        // Single-file pack on purpose: with several files in flight the first permanent failure
        // cancels its siblings mid-retry, so only a pack of one can pin the exact attempt count.
        val server = FlakyServer(failuresPerUrl = Int.MAX_VALUE)

        val failure = assertFails { downloader(server = server).download("L1U4").toList() }

        assertContains(failure.chainMessages(), "after 3 attempts")
        assertEquals(3, server.attemptsFor("hash-d"))
    }

    @Test
    fun oneDeadFileFailsThePackButItsSiblingsAreKept() = runTest {
        // A pack missing one clip is a lesson that breaks mid-way, so the pack as a whole fails.
        // What already landed still stays on disk — that is what makes the retry cheap.
        val files = FakePackFiles()
        val server = DeadUrlServer(deadHash = "hash-c")

        assertFails { downloader(files, server).download("L1U3").toList() }

        assertEquals(setOf("hash-a", "hash-b"), files.storedHashes)
    }

    @Test
    fun aMissingFileOnTheCdnSurfacesItsStatusForClassification() = runTest {
        // The repository decides "retry" vs "do not offer retry" by reading this message, so the
        // 404 has to survive into the thrown cause.
        val server = FlakyServer(failuresPerUrl = Int.MAX_VALUE, status = HttpStatusCode.NotFound)

        val failure = assertFails { downloader(server = server).download("L1U4").toList() }

        assertContains(failure.chainMessages(), "HTTP 404")
    }

    @Test
    fun aFullDeviceFailsTheDownloadRatherThanSilentlyDroppingAFile() = runTest {
        val files = FakePackFiles(failOnPut = "hash-b")

        val failure = assertFails { downloader(files).download("L1U3").toList() }

        assertContains(failure.chainMessages(), "No space left")
    }

    @Test
    fun pendingBytesCountsOnlyWhatIsMissing() = runTest {
        val files = FakePackFiles().apply { seed("hash-a") }
        val subject = downloader(files)

        assertEquals(500L, subject.pendingBytes("L1U3"))
        assertEquals(0L, subject.pendingBytes("L9U9"))
    }

    @Test
    fun fetchOneStoresTheFileAndReturnsItsPath() = runTest {
        val files = FakePackFiles()
        val path = downloader(files).fetchOne("audio/level_1/unit_04/d.mp3", asset("hash-d", "L1U4"))

        assertEquals("/packs/hash-d", path)
        assertTrue(files.has("hash-d"))
    }
}
