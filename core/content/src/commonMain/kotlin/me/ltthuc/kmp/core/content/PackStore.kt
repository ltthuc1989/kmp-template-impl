package me.ltthuc.kmp.core.content

/**
 * The storage surface the download path actually needs. Split out from [PackStore] because that
 * is an `expect class` and so cannot be substituted in commonTest — everything interesting about
 * downloading (resume, retry, progress) is testable against an in-memory stand-in.
 */
interface PackFiles {

    /** Absolute path of the stored file for [hash], or null when it is not on disk. */
    fun pathFor(hash: String): String?

    fun has(hash: String): Boolean

    /**
     * Writes [bytes] under [hash] and returns the absolute path. Implementations write to a temp
     * file and rename, so a process death mid-write can never leave a truncated file that later
     * looks complete.
     */
    suspend fun put(hash: String, bytes: ByteArray): String
}

/**
 * Pinned on-disk store for downloaded content packs.
 *
 * Deliberately NOT an LRU cache. Pack content is paid-for (or ad-unlocked) curriculum: if
 * eviction could reclaim it, a child would lose a lesson mid-flight on a plane. The LRU
 * audio cache stays for opportunistic single-file fetches; this store only shrinks when
 * the user deletes a pack.
 *
 * Files are keyed by content hash, so two assets with identical bytes share one file and
 * a changed asset lands under a new name — the old one becomes garbage that
 * [deleteUnreferenced] can sweep after a content update.
 *
 * Platform homes: Android `filesDir/content_packs`; iOS Application Support (NOT Caches,
 * which iOS may purge under storage pressure) with the iCloud backup flag cleared, since
 * re-downloadable content must not consume the user's iCloud quota.
 */

expect class PackStore : PackFiles {

    override fun pathFor(hash: String): String?

    override fun has(hash: String): Boolean

    override suspend fun put(hash: String, bytes: ByteArray): String

    /** Total bytes currently stored for [hashes]. */
    fun sizeOf(hashes: Collection<String>): Long

    /** Deletes the files for [hashes]. Missing entries are ignored. */
    fun delete(hashes: Collection<String>)

    /** Deletes every stored file whose hash is not in [keep] — sweeps content-update garbage. */
    fun deleteUnreferenced(keep: Set<String>)

    /** Wipes the whole store. */
    fun clear()
}
