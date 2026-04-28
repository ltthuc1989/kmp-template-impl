package me.matsumo.grabee.core.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.datasource.db.entity.LevelEntity
import me.matsumo.grabee.core.datasource.db.entity.PhonicsLessonEntity
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity
import me.matsumo.grabee.core.datasource.db.entity.UserProgressEntity
import me.matsumo.grabee.core.model.LessonWord
import me.matsumo.grabee.core.model.Level
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.model.PhonicsUnit
import me.matsumo.grabee.core.model.UserProgress

private val jsonParser = Json { ignoreUnknownKeys = true }

@Serializable
private data class LessonWordJson(
    val word: String,
    val emoji: String? = null,
)

internal fun LevelEntity.toModel() = Level(
    id = id,
    number = number,
    title = title,
    totalUnits = totalUnits,
    isPremium = isPremium,
    orderIndex = orderIndex,
)

internal fun UnitEntity.toModel() = PhonicsUnit(
    id = id,
    levelId = levelId,
    number = number,
    title = title,
    themeChip = themeChip,
    orderIndex = orderIndex,
)

internal fun PhonicsLessonEntity.toModel(): PhonicsLesson {
    val words = runCatching {
        jsonParser.decodeFromString<List<LessonWordJson>>(wordsJson)
            .map { LessonWord(word = it.word, emoji = it.emoji) }
    }.getOrElse { emptyList() }
    return PhonicsLesson(
        id = id,
        unitId = unitId,
        letter = letter,
        displayLetter = displayLetter,
        soundSpelling = soundSpelling,
        sentence = sentence,
        stretchedWord = stretchedWord,
        orderIndex = orderIndex,
        words = words,
    )
}

internal fun UserProgressEntity.toModel() = UserProgress(
    unitId = unitId,
    stepIndex = stepIndex,
    starsEarned = starsEarned,
    completedAt = completedAt,
)
