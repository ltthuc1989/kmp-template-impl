package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.ltthuc.kmp.core.datasource.db.dao.UnitCompletionDao
import me.ltthuc.kmp.core.datasource.db.entity.UnitCompletionEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class UnitCompletionRepository(
    private val dao: UnitCompletionDao,
) {
    fun observeAll(): Flow<List<UnitCompletionEntity>> = dao.observeAll()

    fun observeCount(unitId: String): Flow<Int> =
        dao.observeFor(unitId).map { it?.completionCount ?: 0 }

    @OptIn(ExperimentalTime::class)
    suspend fun markCompleted(unitId: String) {
        val current = dao.observeFor(unitId).first()
        dao.upsert(
            UnitCompletionEntity(
                unitId = unitId,
                completionCount = (current?.completionCount ?: 0) + 1,
                lastCompletedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun reset(unitId: String) = dao.deleteByUnit(unitId)

    suspend fun resetAll() = dao.deleteAll()
}
