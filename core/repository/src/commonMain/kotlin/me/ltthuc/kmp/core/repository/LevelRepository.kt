package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
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

// Step 4 (Blending) was retired and its screen deleted; the index stays reserved so saved
// progress keeps its meaning, but no level may render it. Anything outside this set is dropped.
private val DEFAULT_VISIBLE_STEPS = listOf(0, 1, 2, 3, 5, 6)

// Premium levels whose content has shipped and so are enterable now (per-unit paywall still applies
// in UnitRepository). Other premium levels stay "Coming soon" until their content lands.
private val LAUNCHED_PREMIUM_LEVELS = setOf("L2")

class LevelRepository(
    private val levelDao: LevelDao,
    private val unitDao: UnitDao,
    private val learningProgressDao: LearningProgressDao,
    private val seeder: DatabaseSeeder,
    private val appSettingRepository: AppSettingRepository,
    private val unitCompletionRepository: UnitCompletionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    /** True once every unit of [levelId] has been completed at least once. */
    fun observeIsLevelComplete(levelId: String): Flow<Boolean> = combine(
        unitDao.observeAll(),
        unitCompletionRepository.observeAll(),
    ) { units, completions ->
        val ids = units.filter { it.levelId == levelId }.map { it.id }
        ids.isNotEmpty() && ids.all { id ->
            (completions.firstOrNull { it.unitId == id }?.completionCount ?: 0) > 0
        }
    }.flowOn(dispatcher)

    suspend fun getVisibleSteps(levelId: String): List<Int> = withContext(dispatcher) {
        seeder.syncCurriculum()
        val raw = levelDao.findById(levelId)?.visibleStepsJson ?: return@withContext DEFAULT_VISIBLE_STEPS
        runCatching {
            visibleStepsJson.decodeFromString<List<Int>>(raw)
                .filter { it in DEFAULT_VISIBLE_STEPS }
                .ifEmpty { DEFAULT_VISIBLE_STEPS }
        }.getOrDefault(DEFAULT_VISIBLE_STEPS)
    }

    fun observeLevelCards(): Flow<List<LevelCard>> = combine(
        levelDao.observeAll(),
        unitDao.observeAll(),
        learningProgressDao.observe(),
        appSettingRepository.setting,
    ) { levels, units, progress, _ ->
        buildLevelCards(levels, units, progress)
    }.onStart {
        seeder.syncCurriculum()
    }.flowOn(dispatcher)

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
    ): List<LevelCard> {
        val unitsByLevel = units.groupBy { it.levelId }

        // Per-level monetization model: every level is enterable from the start so its free sample
        // unit(s) can be played. Access to paid units is gated per-unit in UnitRepository, not here —
        // so there is no level-level Locked/ComingSoon/prerequisite anymore. The only distinction is
        // whether a level is the one currently in progress (Active) or not (ReadyToStart).
        return levels.sortedBy { it.orderIndex }.map { entity ->
            val level = entity.toModel()
            val status = when {
                // Premium levels stay ComingSoon until their content ships (see LAUNCHED_PREMIUM_LEVELS).
                // L2 has launched, so it falls through to the normal ReadyToStart/Active flow below.
                entity.isPremium && entity.id !in LAUNCHED_PREMIUM_LEVELS -> LevelStatus.ComingSoon
                progress != null && entity.id == progress.activeLevelId ->
                    activeStatus(entity = entity, progress = progress, unitsByLevel = unitsByLevel)
                else -> LevelStatus.ReadyToStart
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
