package me.ltthuc.kmp.core.datasource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks how many times the user has completed an individual lesson (the last visible
 * step of a lesson). Drives per-lesson unlock logic on the Lesson Map: a lesson is
 * Completed when [completionCount] > 0; the next lesson unlocks when the previous one
 * is Completed. Story and Mini Games unlock when every lesson in the unit is Completed.
 */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val unitId: String,
    val completionCount: Int,
    val lastCompletedAt: Long,
)
