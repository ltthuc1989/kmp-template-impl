package me.ltthuc.kmp.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks how many times the user has completed a unit (reached UnitCompleteScreen).
 * Drives unit unlock logic: a unit is Completed when [completionCount] > 0; the next
 * unit becomes Unlocked when the previous one is Completed.
 */
@Entity(tableName = "unit_completion")
data class UnitCompletionEntity(
    @PrimaryKey val unitId: String,
    val completionCount: Int,
    val lastCompletedAt: Long,
)
