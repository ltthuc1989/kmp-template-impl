package me.matsumo.grabee.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.matsumo.grabee.core.datasource.db.dao.LearningProgressDao
import me.matsumo.grabee.core.datasource.db.entity.LearningProgressEntity

class LearningProgressRepository(
    private val learningProgressDao: LearningProgressDao,
) {
    fun observe(): Flow<LearningProgressEntity?> = learningProgressDao.observe()

    suspend fun current(): LearningProgressEntity? = learningProgressDao.observe().first()

    suspend fun setActivePosition(
        levelId: String,
        unitId: String,
        lessonIndex: Int,
        stepIndex: Int,
        progressPercent: Int,
    ) {
        learningProgressDao.upsert(
            LearningProgressEntity(
                activeLevelId = levelId,
                activeUnitId = unitId,
                activeLessonIndex = lessonIndex,
                activeStepIndex = stepIndex,
                unitProgressPercent = progressPercent.coerceIn(0, 100),
            ),
        )
    }
}
