package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.PhonicsLessonDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.model.FREE_UNITS_PER_LEVEL
import me.ltthuc.kmp.core.model.LessonCard
import me.ltthuc.kmp.core.model.LessonStatus
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
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
    private val lessonProgressRepository: LessonProgressRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    fun observeUnits(levelId: String): Flow<List<PhonicsUnit>> = unitDao.observeAll().map { all ->
        all.filter { it.levelId == levelId }.sortedBy { it.orderIndex }.map { it.toModel() }
    }.flowOn(dispatcher)

    fun observeUnit(unitId: String): Flow<PhonicsUnit?> = unitDao.observeAll().map { all ->
        all.firstOrNull { it.id == unitId }?.toModel()
    }.flowOn(dispatcher)

    // toModel() parses each lesson's words/chant JSON — keep it off the UI collector.
    fun observeLessons(unitId: String): Flow<List<PhonicsLesson>> =
        phonicsLessonDao.observeByUnit(unitId).map { lessons -> lessons.map { it.toModel() } }
            .flowOn(dispatcher)

    /**
     * Per-lesson lock state for the Lesson Map. First lesson is always Unlocked; each
     * subsequent lesson unlocks only when the previous one has been completed at least once.
     * Developer mode forces every lesson to Completed (so Story/Mini Games unlock too).
     */
    fun observeLessonCards(unitId: String): Flow<List<LessonCard>> = combine(
        observeLessons(unitId),
        lessonProgressRepository.observeByUnit(unitId),
        learningProgressDao.observe(),
        appSettingRepository.setting,
    ) { lessons, progress, active, setting ->
        val sorted = lessons.sortedBy { it.orderIndex }
        val countById = progress.associateBy({ it.lessonId }, { it.completionCount })
        sorted.mapIndexed { index, lesson ->
            val count = countById[lesson.id] ?: 0
            val isCompleted = count > 0
            val isActive = active?.activeUnitId == unitId && active.activeLessonIndex == lesson.orderIndex
            val prevCompleted = index == 0 || (countById[sorted[index - 1].id] ?: 0) > 0
            val status = when {
                setting.developerMode -> LessonStatus.Completed
                isCompleted -> LessonStatus.Completed
                isActive -> LessonStatus.Active
                prevCompleted -> LessonStatus.Unlocked
                else -> LessonStatus.Locked
            }
            LessonCard(lesson, status, count)
        }
    }

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
                buildUnitCards(
                    signals = signals,
                    activeUnitId = progress?.activeUnitId,
                    developerMode = setting.developerMode,
                    levelOwned = setting.isLevelOwned(levelId),
                    levelOpenedFully = setting.isLevelOpenedFully(levelId),
                )
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
        levelOwned: Boolean,
        levelOpenedFully: Boolean,
    ): List<UnitCard> {
        val sorted = signals.sortedBy { it.unit.orderIndex }
        // Two independent gates compose here:
        //   • sequential  — a unit unlocks only when the previous one is completed
        //   • paywall     — paid units (orderIndex >= FREE_UNITS_PER_LEVEL) require owning the level
        //
        // The first FREE_UNITS_PER_LEVEL unit(s) are free samples → sequential only, never paywalled.
        // For paid units: not owned (with monetization on) → PremiumLocked (paywall wins the display).
        // Owned + parent "open all" (levelOpenedFully) → drop the sequential gate. Owned otherwise →
        // still sequential (buying removes the paywall, not the pedagogy). Developer mode forces every
        // unit to Completed so QA can jump to any lesson/Story via the LessonSelectorSheet.
        return sorted.mapIndexed { index, signal ->
            val status = decideUnitStatus(
                index = index,
                isActive = signal.unit.id == activeUnitId,
                isCompleted = signal.completionCount > 0,
                prevCompleted = index == 0 || sorted[index - 1].completionCount > 0,
                developerMode = developerMode,
                levelOwned = levelOwned,
                levelOpenedFully = levelOpenedFully,
                monetizationEnabled = MONETIZATION_ENABLED,
            )
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

/**
 * Pure per-unit gating decision (extracted for unit testing). [monetizationEnabled] is a parameter
 * — not the global const — so tests can exercise the paywall path. See the two-gate model in
 * [UnitRepository] for the rationale.
 */
internal fun decideUnitStatus(
    index: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    prevCompleted: Boolean,
    developerMode: Boolean,
    levelOwned: Boolean,
    levelOpenedFully: Boolean,
    monetizationEnabled: Boolean,
    freeUnitsPerLevel: Int = FREE_UNITS_PER_LEVEL,
): UnitStatus {
    val sequential = when {
        isCompleted -> UnitStatus.Completed
        isActive -> UnitStatus.Active
        prevCompleted -> UnitStatus.Unlocked
        else -> UnitStatus.Locked
    }
    return when {
        developerMode -> UnitStatus.Completed
        index < freeUnitsPerLevel -> sequential
        !levelOwned && monetizationEnabled -> UnitStatus.PremiumLocked
        levelOpenedFully -> if (isCompleted) UnitStatus.Completed else UnitStatus.Unlocked
        else -> sequential
    }
}
