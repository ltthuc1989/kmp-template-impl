package me.ltthuc.kmp.core.audio

/**
 * Local file cache for downloaded audio. Each [cacheKey] maps to one file under the
 * platform-specific cache directory. LRU eviction keeps total cache size under
 * [maxBytes]; oldest-touched files go first.
 */
expect class AudioCacheManager {

    /** Returns absolute file path if [cacheKey] is cached and ready, else null. */
    fun cachedFilePath(cacheKey: String): String?

    /** Writes [bytes] under [cacheKey], returns absolute file path. Triggers LRU eviction. */
    suspend fun put(cacheKey: String, bytes: ByteArray): String

    /** Removes a single entry. No-op if missing. */
    fun evict(cacheKey: String)

    /** Removes all cached audio files. */
    fun clear()
}

const val DEFAULT_AUDIO_CACHE_MAX_BYTES: Long = 50L * 1024 * 1024 // 50MB
