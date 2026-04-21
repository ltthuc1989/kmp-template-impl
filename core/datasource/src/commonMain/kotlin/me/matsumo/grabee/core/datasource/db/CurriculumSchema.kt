package me.matsumo.grabee.core.datasource.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.datasource.db.entity.LevelEntity
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity
import me.matsumo.grabee.core.datasource.db.entity.WordEntity
import me.matsumo.grabee.core.model.VocabularyItem

/**
 * DTOs for deserializing the full 5-level phonics curriculum from
 * `composeResources/files/curriculum.json`. Kept separate from Room entities so content editors
 * can extend/version the JSON without touching the database schema.
 *
 * Maps to Room entities via [CurriculumDto.toEntities] — ordering preserved by top-to-bottom
 * list iteration; `orderIndex` inside each DTO is authored-canonical.
 */
@Serializable
internal data class CurriculumDto(
    val levels: List<LevelDto>,
)

@Serializable
internal data class LevelDto(
    val id: String,
    val number: Int,
    val title: String,
    val isPremium: Boolean,
    val ageRange: String,
    val orderIndex: Int,
    val units: List<UnitDto>,
)

@Serializable
internal data class UnitDto(
    val id: String,
    val number: Int,
    val title: String,
    val orderIndex: Int,
    val words: List<WordDto>,
)

@Serializable
internal data class WordDto(
    val id: String,
    val text: String,
    val phoneme: String,
    val emoji: String? = null,
    val orderIndex: Int,
    val vocabulary: List<VocabularyDto> = emptyList(),
)

@Serializable
internal data class VocabularyDto(
    val text: String,
    val emoji: String? = null,
    val orderIndex: Int = 0,
)

internal data class CurriculumEntities(
    val levels: List<LevelEntity>,
    val units: List<UnitEntity>,
    val words: List<WordEntity>,
)

private val curriculumJson = Json { ignoreUnknownKeys = true }

internal fun CurriculumDto.toEntities(): CurriculumEntities {
    val levels = levels.map { it.toEntity() }
    val units = this.levels.flatMap { level ->
        level.units.map { it.toEntity(levelId = level.id) }
    }
    val words = this.levels.flatMap { level ->
        level.units.flatMap { unit ->
            unit.words.map { it.toEntity(unitId = unit.id) }
        }
    }
    return CurriculumEntities(levels = levels, units = units, words = words)
}

private fun LevelDto.toEntity() = LevelEntity(
    id = id,
    number = number,
    title = title,
    totalUnits = units.size,
    isPremium = isPremium,
    orderIndex = orderIndex,
)

private fun UnitDto.toEntity(levelId: String) = UnitEntity(
    id = id,
    levelId = levelId,
    number = number,
    title = title,
    orderIndex = orderIndex,
)

private fun WordDto.toEntity(unitId: String): WordEntity {
    val vocab = vocabulary.map { it.toModel() }
    val json = if (vocab.isEmpty()) "[]" else curriculumJson.encodeToString(vocab)
    return WordEntity(
        id = id,
        unitId = unitId,
        text = text,
        phoneme = phoneme,
        emoji = emoji,
        imageAsset = null,
        wordAudioAsset = null,
        sentenceAudioAsset = null,
        orderIndex = orderIndex,
        vocabularyJson = json,
    )
}

private fun VocabularyDto.toModel() = VocabularyItem(
    text = text,
    emoji = emoji,
    orderIndex = orderIndex,
)
