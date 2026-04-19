package me.matsumo.grabee.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("unitId")],
)
data class WordEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val text: String,
    val phoneme: String,
    val emoji: String?,
    val imageAsset: String?,
    val wordAudioAsset: String?,
    val sentenceAudioAsset: String?,
    val orderIndex: Int,
)
