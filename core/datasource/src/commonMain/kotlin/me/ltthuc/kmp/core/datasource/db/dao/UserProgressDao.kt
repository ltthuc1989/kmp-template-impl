package me.ltthuc.kmp.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE unitId = :unitId ORDER BY stepIndex ASC")
    fun observeByUnit(unitId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT COALESCE(SUM(starsEarned), 0) FROM user_progress WHERE unitId = :unitId")
    fun observeUnitStars(unitId: String): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(up.starsEarned), 0) FROM user_progress up " +
            "INNER JOIN units u ON u.id = up.unitId WHERE u.levelId = :levelId",
    )
    fun observeLevelStars(levelId: String): Flow<Int>

    @Upsert
    suspend fun upsert(progress: UserProgressEntity)

    @Query("DELETE FROM user_progress WHERE unitId = :unitId")
    suspend fun deleteByUnit(unitId: String)
}
