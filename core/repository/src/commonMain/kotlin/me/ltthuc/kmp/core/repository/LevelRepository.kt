package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.db.DatabaseSeeder
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.LevelDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.model.LevelCard
import me.ltthuc.kmp.core.model.LevelStatus
import me.ltthuc.kmp.core.model.PhonicsUnit

private val visibleStepsJson = Json { ignoreUnknownKeys = true }
private val DEFAULT_VISIBLE_STEPS = (0..6).toList()

class LevelRepository(
    private val levelDao: LevelDao,
    private val unitDao: UnitDao,
    private val learningProgressDao: LearningProgressDao,
    private val seeder: DatabaseSeeder,
    private val appSettingRepository: AppSettingRepository,
) {
    suspend fun getVisibleSteps(levelId: String): List<Int> {
        seeder.seedIfEmpty()
        val raw = levelDao.findById(levelId)?.visibleStepsJson ?: return DEFAULT_VISIBLE_STEPS
        return runCatching {
            visibleStepsJson.decodeFromString<List<Int>>(raw)
                .filter { it in 0..6 }
                .ifEmpty { DEFAULT_VISIBLE_STEPS }
        }.getOrDefault(DEFAULT_VISIBLE_STEPS)
    }

    fun observeLevelCards(): Flow<List<LevelCard>> = combine(
        levelDao.observeAll(),
        unitDao.observeAll(),
        learningProgressDao.observe(),
        appSettingRepository.setting,
    ) { levels, units, progress, setting ->
        buildLevelCards(levels, units, progress, setting.hasPrivilege)
    }.onStart {
        seeder.seedIfEmpty()
    }

    fun observeLevel(levelId: String): Flow<Level?> = levelDao.observeAll().map { all ->
        all.firstOrNull { it.id == levelId }?.toModel()
    }

    /**
     * Returns the next sibling unit in the same level (orderIndex + 1), or `null` if
     * [currentUnitId] is the last unit in its level.
     */
    fun observeNextUnit(currentUnitId: String): Flow<PhonicsUnit?> = unitDao.observeAll().map { all ->
        val current = all.firstOrNull { it.id == currentUnitId } ?: return@map null
        all.filter { it.levelId == current.levelId }
            .sortedBy { it.orderIndex }
            .firstOrNull { it.orderIndex == current.orderIndex + 1 }
            ?.toModel()
    }

    private fun buildLevelCards(
        levels: List<LevelEntity>,
        units: List<UnitEntity>,
        progress: LearningProgressEntity?,
        hasPrivilege: Boolean,
    ): List<LevelCard> {
        val unitsByLevel = units.groupBy { it.levelId }
        val activeOrderIndex = progress?.let { active ->
            levels.firstOrNull { it.id == active.activeLevelId }?.orderIndex
        }

        return levels.sortedBy { it.orderIndex }.map { entity ->
            val level = entity.toModel()
            val prerequisite = levels.firstOrNull { it.orderIndex == entity.orderIndex - 1 }?.toModel()
            val status = when {
                // V1: L2-L5 are flagged isPremium but have no real content yet.
                // Show ComingSoon (not Locked-premium) to avoid selling something we don't ship.
                // Revert to Locked(isPremiumRequired=true) when L2 content ships.
                entity.isPremium -> LevelStatus.ComingSoon
                progress != null && entity.id == progress.activeLevelId -> activeStatus(
                    entity = entity,
                    progress = progress,
                    unitsByLevel = unitsByLevel,
                )
                activeOrderIndex != null && entity.orderIndex == activeOrderIndex + 1 -> {
                    LevelStatus.ReadyToStart
                }
                hasPrivilege -> LevelStatus.ReadyToStart
                else -> LevelStatus.Locked(prerequisiteLevel = prerequisite)
            }
            LevelCard(level = level, status = status)
        }
    }

    private fun activeStatus(
        entity: LevelEntity,
        progress: LearningProgressEntity,
        unitsByLevel: Map<String, List<UnitEntity>>,
    ): LevelStatus {
        val levelUnits = unitsByLevel[entity.id].orEmpty()
        val currentUnit = levelUnits.firstOrNull { it.id == progress.activeUnitId }
            ?: levelUnits.minByOrNull { it.orderIndex }
        return if (currentUnit != null) {
            LevelStatus.Active(
                currentUnit = currentUnit.toModel(),
                progressPercent = progress.unitProgressPercent.coerceIn(0, 100),
            )
        } else {
            LevelStatus.ReadyToStart
        }
    }
}
