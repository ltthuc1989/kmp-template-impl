package me.matsumo.grabee.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY levelId ASC, orderIndex ASC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM units")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)
}
