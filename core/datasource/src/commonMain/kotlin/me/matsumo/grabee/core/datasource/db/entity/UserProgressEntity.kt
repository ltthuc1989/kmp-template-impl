package me.matsumo.grabee.core.datasource.db.entity

import androidx.room.Entity

@Entity(
    tableName = "user_progress",
    primaryKeys = ["unitId", "stepIndex"],
)
data class UserProgressEntity(
    val unitId: String,
    val stepIndex: Int,
    val starsEarned: Int,
    val completedAt: Long,
)
