package me.ltthuc.kmp.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.datasource.db.entity.LessonProgressEntity

@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE unitId = :unitId")
    fun observeByUnit(unitId: String): Flow<List<LessonProgressEntity>>

    @Upsert
    suspend fun upsert(entity: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress WHERE unitId = :unitId")
    suspend fun deleteByUnit(unitId: String)
}
