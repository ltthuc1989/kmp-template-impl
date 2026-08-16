package me.ltthuc.kmp.core.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which failures are worth offering a retry for.
 *
 * The distinction is the whole reason the download slot has two error looks: a dropped
 * connection gets a retry button, while a full device or a file missing from the CDN gets a
 * warning instead — tapping retry there just walks the user through the same failure again.
 */
class DownloadFailureTest {

    /** The downloader wraps the last attempt's error, and coroutines wrap it again on the way out. */
    private fun wrapped(vararg messages: String): Throwable =
        messages.reversed().fold(null as Throwable?) { cause, message ->
            IllegalStateException(message, cause)
        }!!

    @Test
    fun aDroppedConnectionIsWorthRetrying() {
        assertTrue(isRetryableFailure(wrapped("Unable to download a.mp3 after 3 attempts", "Connection reset")))
    }

    @Test
    fun aTimeoutIsWorthRetrying() {
        assertTrue(isRetryableFailure(wrapped("Request timeout has expired")))
    }

    @Test
    fun aFullDeviceIsNot() {
        assertFalse(isRetryableFailure(wrapped("write failed: No space left on device")))
    }

    @Test
    fun theEnospcErrnoIsRecognisedToo() {
        // Some platforms surface the errno rather than the sentence.
        assertFalse(isRetryableFailure(wrapped("open failed: ENOSPC (No space left on device)")))
    }

    @Test
    fun aFileMissingFromTheCdnIsNot() {
        // Content bug, not a network bug: the fix is republishing, not tapping again.
        assertFalse(isRetryableFailure(wrapped("Unable to download a.mp3 after 3 attempts", "HTTP 404 for https://cdn/x")))
    }

    @Test
    fun aServerErrorIsWorthRetrying() {
        assertTrue(isRetryableFailure(wrapped("Unable to download a.mp3 after 3 attempts", "HTTP 503 for https://cdn/x")))
    }

    @Test
    fun theReasonIsFoundHoweverDeeplyItIsWrapped() {
        // Coroutines re-wrap on the way out of coroutineScope, so the cause can sit several
        // levels down; classifying only the top-level message would call everything retryable.
        assertFalse(isRetryableFailure(wrapped("outer", "middle", "inner: No space left on device")))
    }

    @Test
    fun anUnknownFailureDefaultsToRetryable() {
        // Offering a retry that cannot help is a smaller harm than hiding one that would.
        assertTrue(isRetryableFailure(wrapped("something unexpected")))
        assertTrue(isRetryableFailure(null))
    }
}
