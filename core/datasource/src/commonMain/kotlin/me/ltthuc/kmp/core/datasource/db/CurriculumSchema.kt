package me.ltthuc.kmp.core.datasource.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.PhonicsLessonEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity

/**
 * DTOs for deserializing the Oxford Phonics Words curriculum from
 * `composeResources/files/curriculum.json`. The file is generated offline by
 * `scripts/build_curriculum_json.py` from the OPW CSV source data.
 *
 * Shape: 5 levels → 8 units/level → 3-4 phonics lessons/unit → 4 example words/lesson.
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
    val visibleSteps: List<Int>? = null,
    val units: List<UnitDto>,
)

@Serializable
internal data class UnitDto(
    val id: String,
    val number: Int,
    val title: String,
    val themeChip: String? = null,
    val orderIndex: Int,
    val lessons: List<PhonicsLessonDto>,
)

@Serializable
internal data class PhonicsLessonDto(
    val id: String,
    val letter: String,
    val displayLetter: String,
    val soundSpelling: String,
    val sentence: String,
    val stretchedWord: String,
    val orderIndex: Int,
    val words: List<LessonWordDto>,
    val chantTexts: List<String> = emptyList(),
    val chantOrder: List<Int> = listOf(0, 1, 2, 3),
)

@Serializable
internal data class LessonWordDto(
    val word: String,
    val emoji: String? = null,
)

internal data class CurriculumEntities(
    val levels: List<LevelEntity>,
    val units: List<UnitEntity>,
    val lessons: List<PhonicsLessonEntity>,
)

private val curriculumJson = Json { ignoreUnknownKeys = true }

internal fun CurriculumDto.toEntities(): CurriculumEntities {
    val levels = levels.map { it.toEntity() }
    val units = this.levels.flatMap { level ->
        level.units.map { it.toEntity(levelId = level.id) }
    }
    val lessons = this.levels.flatMap { level ->
        level.units.flatMap { unit ->
            unit.lessons.map { it.toEntity(unitId = unit.id) }
        }
    }
    return CurriculumEntities(levels = levels, units = units, lessons = lessons)
}

private fun LevelDto.toEntity() = LevelEntity(
    id = id,
    number = number,
    title = title,
    totalUnits = units.size,
    isPremium = isPremium,
    orderIndex = orderIndex,
    visibleStepsJson = visibleSteps?.let { curriculumJson.encodeToString(it) },
)

private fun UnitDto.toEntity(levelId: String) = UnitEntity(
    id = id,
    levelId = levelId,
    number = number,
    title = title,
    themeChip = themeChip,
    orderIndex = orderIndex,
)

private fun PhonicsLessonDto.toEntity(unitId: String): PhonicsLessonEntity {
    val wordsJsonStr = curriculumJson.encodeToString(words)
    val chantTextsJsonStr = curriculumJson.encodeToString(chantTexts)
    val chantOrderJsonStr = curriculumJson.encodeToString(chantOrder)
    return PhonicsLessonEntity(
        id = id,
        unitId = unitId,
        letter = letter,
        displayLetter = displayLetter,
        soundSpelling = soundSpelling,
        sentence = sentence,
        stretchedWord = stretchedWord,
        orderIndex = orderIndex,
        wordsJson = wordsJsonStr,
        chantTextsJson = chantTextsJsonStr,
        chantOrderJson = chantOrderJsonStr,
    )
}
