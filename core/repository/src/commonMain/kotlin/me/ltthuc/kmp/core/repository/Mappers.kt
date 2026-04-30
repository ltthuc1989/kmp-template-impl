package me.ltthuc.kmp.core.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.PhonicsLessonEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.model.UserProgress

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
    val chantTexts = runCatching {
        jsonParser.decodeFromString<List<String>>(chantTextsJson)
    }.getOrElse { emptyList() }
    val chantOrder = runCatching {
        jsonParser.decodeFromString<List<Int>>(chantOrderJson)
    }.getOrElse { listOf(0, 1, 2, 3) }
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
        chantTexts = chantTexts,
        chantOrder = chantOrder,
    )
}

internal fun UserProgressEntity.toModel() = UserProgress(
    unitId = unitId,
    stepIndex = stepIndex,
    starsEarned = starsEarned,
    completedAt = completedAt,
)
