package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Remembers which hash each logical path was last stored under.
 *
 * [PackStore] is keyed by content hash alone, which is what makes a changed file a different
 * file. The cost is that a hash on disk cannot be traced back to the lesson it belongs to —
 * and that matters exactly once: an app update ships a manifest with new hashes, so every
 * changed asset's old copy becomes unattributable garbage the sweep would remove, leaving a
 * finished unit silent until the device is next online. This index is the missing link that
 * lets the old bytes be recognised and kept until the new ones arrive.
 *
 * Best-effort by design. Entries are written when a pack finishes, so a download cancelled
 * halfway leaves its files unindexed — they are then swept like before, which is no worse
 * than the behaviour this class improves on. Nothing reads the index for correctness; it
 * only ever widens what is kept and what can be played.
 */
class PackIndex(private val packFiles: PackFiles) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: MutableMap<String, String>? = null

    /** Hash last stored for [logicalPath], whether or not the manifest still lists it. */
    suspend fun hashFor(logicalPath: String): String? = mutex.withLock { load()[logicalPath] }

    suspend fun snapshot(): Map<String, String> = mutex.withLock { load().toMap() }

    /** Records `logical path → hash` for freshly stored files. No-op when [entries] is empty. */
    suspend fun record(entries: Map<String, String>) {
        if (entries.isEmpty()) return
        mutex.withLock {
            val map = load()
            map.putAll(entries)
            flush(map)
        }
    }

    /**
     * Drops entries whose file is no longer on disk. Run after a sweep so the index shrinks
     * with the store instead of growing forever.
     */
    suspend fun pruneMissing() {
        mutex.withLock {
            val map = load()
            val gone = map.filterValues { !packFiles.has(it) }.keys
            if (gone.isEmpty()) return
            gone.forEach { map.remove(it) }
            flush(map)
        }
    }

    /** Call sites hold [mutex]. */
    private suspend fun load(): MutableMap<String, String> = cache ?: run {
        val loaded = runCatching {
            packFiles.readIndex()
                ?.let { json.decodeFromString<Map<String, String>>(it) }
                ?.toMutableMap()
        }.onFailure {
            Napier.w("Pack index unreadable — starting a fresh one", it)
        }.getOrNull() ?: mutableMapOf()

        loaded.also { cache = it }
    }

    private suspend fun flush(map: Map<String, String>) {
        cache = map.toMutableMap()
        runCatching { packFiles.writeIndex(json.encodeToString(map)) }
            .onFailure { Napier.w("Could not persist the pack index", it) }
    }
}
