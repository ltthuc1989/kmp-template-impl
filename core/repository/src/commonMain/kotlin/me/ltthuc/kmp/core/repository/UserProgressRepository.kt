package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.ltthuc.kmp.core.datasource.db.dao.UserProgressDao
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity
import me.ltthuc.kmp.core.model.UserProgress
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class UserProgressRepository(
    private val userProgressDao: UserProgressDao,
) {
    fun observeByUnit(unitId: String): Flow<List<UserProgress>> =
        userProgressDao.observeByUnit(unitId).map { rows -> rows.map { it.toModel() } }

    fun observeUnitStars(unitId: String): Flow<Int> = userProgressDao.observeUnitStars(unitId)

    fun observeLevelStars(levelId: String): Flow<Int> = userProgressDao.observeLevelStars(levelId)

    @OptIn(ExperimentalTime::class)
    suspend fun markStepCompleted(unitId: String, stepIndex: Int, stars: Int) {
        userProgressDao.upsert(
            UserProgressEntity(
                unitId = unitId,
                stepIndex = stepIndex,
                starsEarned = stars.coerceIn(0, MAX_STARS_PER_STEP),
                completedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private companion object {
        const val MAX_STARS_PER_STEP = 3
    }
}
