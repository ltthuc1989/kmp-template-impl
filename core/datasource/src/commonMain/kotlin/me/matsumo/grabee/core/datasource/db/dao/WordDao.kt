package me.matsumo.grabee.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.matsumo.grabee.core.datasource.db.entity.WordEntity

@Dao
interface WordDao {
    @Query("SELECT * FROM word WHERE unitId = :unitId ORDER BY orderIndex ASC")
    fun observeByUnit(unitId: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM word")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)
}
