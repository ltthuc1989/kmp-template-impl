package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.PhonicsLessonDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.model.UnitLetterPreview
import me.ltthuc.kmp.core.model.UnitStatus

class UnitRepository(
    private val unitDao: UnitDao,
    private val phonicsLessonDao: PhonicsLessonDao,
    private val unitCompletionRepository: UnitCompletionRepository,
    private val learningProgressDao: LearningProgressDao,
    private val appSettingRepository: AppSettingRepository,
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
                appSettingRepository.setting,
            ) { signals, progress, setting ->
                buildUnitCards(signals, progress?.activeUnitId, setting.developerMode)
            }
        }
    }

    suspend fun resetUnit(unitId: String) {
        unitCompletionRepository.reset(unitId)
    }

    private fun unitSignal(unit: PhonicsUnit): Flow<UnitSignal> = combine(
        unitCompletionRepository.observeCount(unit.id),
        observeLessons(unit.id),
    ) { count, lessons ->
        UnitSignal(
            unit = unit,
            completionCount = count,
            previewLetters = lessons.map { lesson ->
                UnitLetterPreview(
                    letter = lesson.displayLetter,
                    emoji = lesson.words.firstNotNullOfOrNull { it.emoji },
                )
            },
        )
    }

    private fun buildUnitCards(
        signals: List<UnitSignal>,
        activeUnitId: String?,
        developerMode: Boolean,
    ): List<UnitCard> {
        val sorted = signals.sortedBy { it.unit.orderIndex }
        // Sequential gating: first unit in a level is always Unlocked (entry point);
        // each subsequent unit unlocks only when the previous one has been completed
        // (user reached UnitCompleteScreen at least once → completionCount > 0).
        // Developer mode bypasses gating entirely: every unit is forced to Completed
        // so UnitSelectionScreen opens the LessonSelectorSheet on tap, letting QA jump
        // directly to any lesson or Story without playing through prerequisites.
        return sorted.mapIndexed { index, signal ->
            val isActive = signal.unit.id == activeUnitId
            val isCompleted = signal.completionCount > 0
            val prevCompleted = index == 0 || sorted[index - 1].completionCount > 0
            val status = when {
                developerMode -> UnitStatus.Completed
                isCompleted -> UnitStatus.Completed
                isActive -> UnitStatus.Active
                prevCompleted -> UnitStatus.Unlocked
                else -> UnitStatus.Locked
            }
            UnitCard(
                unit = signal.unit,
                status = status,
                completionCount = signal.completionCount,
                previewLetters = signal.previewLetters,
            )
        }
    }

    private data class UnitSignal(
        val unit: PhonicsUnit,
        val completionCount: Int,
        val previewLetters: List<UnitLetterPreview>,
    )
}
