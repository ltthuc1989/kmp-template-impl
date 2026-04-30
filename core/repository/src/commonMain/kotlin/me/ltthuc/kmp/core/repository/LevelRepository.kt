package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import me.ltthuc.kmp.core.datasource.db.DatabaseSeeder
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.LevelDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity
import me.ltthuc.kmp.core.model.LevelCard
import me.ltthuc.kmp.core.model.LevelStatus

class LevelRepository(
    private val levelDao: LevelDao,
    private val unitDao: UnitDao,
    private val learningProgressDao: LearningProgressDao,
    private val seeder: DatabaseSeeder,
) {
    fun observeLevelCards(): Flow<List<LevelCard>> = combine(
        levelDao.observeAll(),
        unitDao.observeAll(),
        learningProgressDao.observe(),
    ) { levels, units, progress ->
        buildLevelCards(levels, units, progress)
    }.onStart {
        seeder.seedIfEmpty()
    }

    private fun buildLevelCards(
        levels: List<LevelEntity>,
        units: List<UnitEntity>,
        progress: LearningProgressEntity?,
    ): List<LevelCard> {
        val unitsByLevel = units.groupBy { it.levelId }
        val activeOrderIndex = progress?.let { active ->
            levels.firstOrNull { it.id == active.activeLevelId }?.orderIndex
        }

        return levels.sortedBy { it.orderIndex }.map { entity ->
            val level = entity.toModel()
            val status = when {
                progress != null && entity.id == progress.activeLevelId -> activeStatus(
                    entity = entity,
                    progress = progress,
                    unitsByLevel = unitsByLevel,
                )
                activeOrderIndex != null && entity.orderIndex == activeOrderIndex + 1 -> {
                    LevelStatus.ReadyToStart
                }
                else -> LevelStatus.Locked(
                    prerequisiteLevel = levels.firstOrNull { it.orderIndex == entity.orderIndex - 1 }?.toModel(),
                )
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
