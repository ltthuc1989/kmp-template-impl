package me.ltthuc.kmp.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.datasource.db.entity.UnitCompletionEntity

@Dao
interface UnitCompletionDao {
    @Query("SELECT * FROM unit_completion")
    fun observeAll(): Flow<List<UnitCompletionEntity>>

    @Query("SELECT * FROM unit_completion WHERE unitId = :unitId")
    fun observeFor(unitId: String): Flow<UnitCompletionEntity?>

    @Upsert
    suspend fun upsert(entity: UnitCompletionEntity)

    @Query("DELETE FROM unit_completion WHERE unitId = :unitId")
    suspend fun deleteByUnit(unitId: String)
}
