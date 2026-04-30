package me.ltthuc.kmp.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("levelId")],
)
data class UnitEntity(
    @PrimaryKey val id: String,
    val levelId: String,
    val number: Int,
    val title: String,
    val themeChip: String?,
    val orderIndex: Int,
)
