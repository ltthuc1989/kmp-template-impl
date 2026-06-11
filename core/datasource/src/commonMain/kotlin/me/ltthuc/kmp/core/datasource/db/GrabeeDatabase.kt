package me.ltthuc.kmp.core.datasource.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.LessonProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.LevelDao
import me.ltthuc.kmp.core.datasource.db.dao.PhonicsLessonDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitCompletionDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.datasource.db.dao.UserProgressDao
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.LessonProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.PhonicsLessonEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitCompletionEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity

@Database(
    entities = [
        LevelEntity::class,
        UnitEntity::class,
        PhonicsLessonEntity::class,
        LearningProgressEntity::class,
        UserProgressEntity::class,
        UnitCompletionEntity::class,
        LessonProgressEntity::class,
    ],
    version = 13,
    exportSchema = false,
)
@ConstructedBy(GrabeeDatabaseConstructor::class)
abstract class GrabeeDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun unitDao(): UnitDao
    abstract fun phonicsLessonDao(): PhonicsLessonDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun unitCompletionDao(): UnitCompletionDao
    abstract fun lessonProgressDao(): LessonProgressDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object GrabeeDatabaseConstructor : RoomDatabaseConstructor<GrabeeDatabase> {
    override fun initialize(): GrabeeDatabase
}

internal const val GRABEE_DB_NAME = "grabee.db"
