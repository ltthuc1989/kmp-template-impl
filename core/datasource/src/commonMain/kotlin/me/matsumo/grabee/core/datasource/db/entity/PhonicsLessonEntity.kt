package me.matsumo.grabee.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phonics_lesson",
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
data class PhonicsLessonEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val letter: String,
    val displayLetter: String,
    val soundSpelling: String,
    val sentence: String,
    val stretchedWord: String,
    val orderIndex: Int,
    val wordsJson: String,
)
