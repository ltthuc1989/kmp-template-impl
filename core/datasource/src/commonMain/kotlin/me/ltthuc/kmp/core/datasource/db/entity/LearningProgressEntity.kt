package me.ltthuc.kmp.core.datasource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeLevelId: String,
    val activeUnitId: String,
    @ColumnInfo(defaultValue = "0")
    val activeLessonIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val activeStepIndex: Int = 0,
    val unitProgressPercent: Int,
) {
    companion object {
        const val SINGLETON_ID = 0

        /**
         * First-install / post-reset state: user lands on L1 → only L1U1 is Unlocked, no
         * completions yet. Used by [DatabaseSeeder] and the global progress reset.
         */
        fun initial() = LearningProgressEntity(
            activeLevelId = "L1",
            activeUnitId = "L1U1",
            activeLessonIndex = 0,
            activeStepIndex = 0,
            unitProgressPercent = 0,
        )
    }
}
