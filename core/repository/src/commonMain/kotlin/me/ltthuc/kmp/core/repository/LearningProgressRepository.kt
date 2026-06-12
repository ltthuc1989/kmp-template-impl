package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity

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

    /** Restores the active position to first-install state (L1U1, lesson 0, step 0, 0%). */
    suspend fun reset() = learningProgressDao.upsert(LearningProgressEntity.initial())
}
