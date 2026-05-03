package me.ltthuc.kmp.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val title: String,
    val totalUnits: Int,
    val isPremium: Boolean,
    val orderIndex: Int,
    val visibleStepsJson: String? = null,
)
