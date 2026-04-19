package me.matsumo.grabee.core.model

import kotlinx.serialization.Serializable

@Serializable
data class VocabularyItem(
    val text: String,
    val emoji: String? = null,
    val imageAsset: String? = null,
    val audioAsset: String? = null,
    val orderIndex: Int = 0,
)
