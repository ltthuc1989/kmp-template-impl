package me.matsumo.grabee.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.matsumo.grabee.core.datasource.db.entity.LevelEntity

@Dao
interface LevelDao {
    @Query("SELECT * FROM levels ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<LevelEntity>>

    @Query("SELECT COUNT(*) FROM levels")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(levels: List<LevelEntity>)
}
