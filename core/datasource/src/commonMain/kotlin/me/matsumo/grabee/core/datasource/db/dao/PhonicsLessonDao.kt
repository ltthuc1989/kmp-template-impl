package me.matsumo.grabee.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.matsumo.grabee.core.datasource.db.entity.PhonicsLessonEntity

@Dao
interface PhonicsLessonDao {
    @Query("SELECT * FROM phonics_lesson WHERE unitId = :unitId ORDER BY orderIndex ASC")
    fun observeByUnit(unitId: String): Flow<List<PhonicsLessonEntity>>

    @Query("SELECT COUNT(*) FROM phonics_lesson")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<PhonicsLessonEntity>)
}
