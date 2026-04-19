package me.matsumo.grabee.core.repository

import me.matsumo.grabee.core.datasource.db.entity.LevelEntity
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity
import me.matsumo.grabee.core.datasource.db.entity.UserProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.WordEntity
import me.matsumo.grabee.core.model.Level
import me.matsumo.grabee.core.model.PhonicsUnit
import me.matsumo.grabee.core.model.UserProgress
import me.matsumo.grabee.core.model.Word

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
    orderIndex = orderIndex,
)

internal fun WordEntity.toModel() = Word(
    id = id,
    unitId = unitId,
    text = text,
    phoneme = phoneme,
    emoji = emoji,
    imageAsset = imageAsset,
    wordAudioAsset = wordAudioAsset,
    sentenceAudioAsset = sentenceAudioAsset,
    orderIndex = orderIndex,
)

internal fun UserProgressEntity.toModel() = UserProgress(
    unitId = unitId,
    stepIndex = stepIndex,
    starsEarned = starsEarned,
    completedAt = completedAt,
)
