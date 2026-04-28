package me.matsumo.grabee.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.matsumo.grabee.core.datasource.db.dao.LearningProgressDao
import me.matsumo.grabee.core.datasource.db.dao.PhonicsLessonDao
import me.matsumo.grabee.core.datasource.db.dao.UnitDao
import me.matsumo.grabee.core.datasource.db.dao.UserProgressDao
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.model.PhonicsUnit
import me.matsumo.grabee.core.model.UnitCard
import me.matsumo.grabee.core.model.UnitStatus

class UnitRepository(
    private val unitDao: UnitDao,
    private val phonicsLessonDao: PhonicsLessonDao,
    private val userProgressDao: UserProgressDao,
    private val learningProgressDao: LearningProgressDao,
) {
    fun observeUnits(levelId: String): Flow<List<PhonicsUnit>> = unitDao.observeAll().map { all ->
        all.filter { it.levelId == levelId }.sortedBy { it.orderIndex }.map { it.toModel() }
    }

    fun observeUnit(unitId: String): Flow<PhonicsUnit?> = unitDao.observeAll().map { all ->
        all.firstOrNull { it.id == unitId }?.toModel()
    }

    fun observeLessons(unitId: String): Flow<List<PhonicsLesson>> =
        phonicsLessonDao.observeByUnit(unitId).map { lessons -> lessons.map { it.toModel() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUnitCards(levelId: String): Flow<List<UnitCard>> = observeUnits(levelId).flatMapLatest { units ->
        if (units.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                combine(units.map { unit -> unitSignal(unit) }) { it.toList() },
                learningProgressDao.observe(),
            ) { signals, progress ->
                buildUnitCards(signals, progress?.activeUnitId)
            }
        }
    }

    private fun unitSignal(unit: PhonicsUnit): Flow<UnitSignal> = combine(
        userProgressDao.observeUnitStars(unit.id),
        observeLessons(unit.id),
    ) { stars, lessons ->
        UnitSignal(
            unit = unit,
            totalStars = stars,
            previewEmojis = lessons.flatMap { lesson -> lesson.words.mapNotNull { it.emoji } }.take(MAX_PREVIEW_EMOJIS),
        )
    }

    private fun buildUnitCards(signals: List<UnitSignal>, activeUnitId: String?): List<UnitCard> {
        val sorted = signals.sortedBy { it.unit.orderIndex }
        return sorted.mapIndexed { index, signal ->
            val isActive = signal.unit.id == activeUnitId
            val isCompleted = !isActive && signal.totalStars >= UnitCard.UNLOCK_THRESHOLD_STARS
            val prevUnlocked = index == 0 || sorted[index - 1].let { prev ->
                prev.unit.id == activeUnitId || prev.totalStars >= UnitCard.UNLOCK_THRESHOLD_STARS
            }
            val status = when {
                isCompleted -> UnitStatus.Completed
                isActive -> UnitStatus.Active
                prevUnlocked -> UnitStatus.Unlocked
                else -> UnitStatus.Locked
            }
            UnitCard(
                unit = signal.unit,
                status = status,
                totalStars = signal.totalStars,
                previewEmojis = signal.previewEmojis,
            )
        }
    }

    private data class UnitSignal(
        val unit: PhonicsUnit,
        val totalStars: Int,
        val previewEmojis: List<String>,
    )

    private companion object {
        const val MAX_PREVIEW_EMOJIS = 4
    }
}
