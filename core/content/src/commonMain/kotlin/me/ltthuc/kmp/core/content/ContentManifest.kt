package me.ltthuc.kmp.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Inventory of every asset that is NOT shipped inside the app, generated offline by
 * `scripts/build_content_manifest.py` and bundled at `files/content_manifest.json`.
 *
 * Assets bundled in the app are deliberately absent: they are played in place and never
 * hashed, so listing them would inflate the file 2.5x for nothing. The lookup rule is
 * therefore **present in [assets] → download it; absent → it ships in the app**.
 *
 * A pack is the unit of download — one curriculum unit (`L2U3`), not a whole level,
 * because the first `FREE_UNITS_PER_LEVEL` units of every level are free samples that
 * must work offline straight after install.
 */
@Serializable
data class ContentManifest(
    val version: Int,
    @SerialName("hashLength") val hashLength: Int,
    /** Pack id → totals, including the `core` pack that ships in the app. */
    val packs: Map<String, PackInfo>,
    /** Logical path (e.g. `audio/level_2/unit_03/…/02_chant.mp3`) → where to get it. */
    val assets: Map<String, ContentAsset>,
) {
    fun assetsInPack(packId: String): Map<String, ContentAsset> =
        assets.filterValues { it.pack == packId }

    /**
     * Packs served from the CDN. A pack stays bundled until its files are actually
     * published, so this set grows one publish at a time rather than all at once —
     * claiming a pack is remote before its bytes are up there would break those lessons.
     */
    fun downloadablePackIds(): Set<String> =
        packs.filterValues { !it.bundled }.keys

    fun isPackBundled(packId: String): Boolean = packs[packId]?.bundled ?: true

    companion object {
        const val CORE_PACK = "core"
        const val RESOURCE_PATH = "files/content_manifest.json"

        val EMPTY = ContentManifest(version = 0, hashLength = 0, packs = emptyMap(), assets = emptyMap())
    }
}

@Serializable
data class PackInfo(
    val files: Int,
    val bytes: Long,
    /** True while the pack still ships inside the app — i.e. nothing to download. */
    val bundled: Boolean = true,
)

@Serializable
data class ContentAsset(
    /** First [ContentManifest.hashLength] hex chars of the file's SHA-256. */
    val hash: String,
    val bytes: Long,
    val pack: String,
)
