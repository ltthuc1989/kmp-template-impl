package me.matsumo.grabee.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.matsumo.grabee.core.datasource.db.entity.LearningProgressEntity

@Dao
interface LearningProgressDao {
    @Query("SELECT * FROM learning_progress WHERE id = ${LearningProgressEntity.SINGLETON_ID} LIMIT 1")
    fun observe(): Flow<LearningProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LearningProgressEntity)
}
