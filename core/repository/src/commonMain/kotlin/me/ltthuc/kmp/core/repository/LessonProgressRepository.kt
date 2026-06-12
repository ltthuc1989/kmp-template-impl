package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.ltthuc.kmp.core.datasource.db.dao.LessonProgressDao
import me.ltthuc.kmp.core.datasource.db.entity.LessonProgressEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LessonProgressRepository(
    private val dao: LessonProgressDao,
) {
    fun observeByUnit(unitId: String): Flow<List<LessonProgressEntity>> = dao.observeByUnit(unitId)

    @OptIn(ExperimentalTime::class)
    suspend fun markCompleted(lessonId: String, unitId: String) {
        val current = dao.observeByUnit(unitId).first().firstOrNull { it.lessonId == lessonId }
        dao.upsert(
            LessonProgressEntity(
                lessonId = lessonId,
                unitId = unitId,
                completionCount = (current?.completionCount ?: 0) + 1,
                lastCompletedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun reset(unitId: String) = dao.deleteByUnit(unitId)

    suspend fun resetAll() = dao.deleteAll()
}
