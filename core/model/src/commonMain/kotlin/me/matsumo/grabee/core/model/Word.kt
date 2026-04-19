package me.matsumo.grabee.core.model

data class Word(
    val id: String,
    val unitId: String,
    val text: String,
    val phoneme: String,
    val emoji: String?,
    val imageAsset: String?,
    val wordAudioAsset: String?,
    val sentenceAudioAsset: String?,
    val orderIndex: Int,
    val vocabulary: List<VocabularyItem> = emptyList(),
)
