package me.ltthuc.kmp.core.datasource.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import me.ltthuc.kmp.core.datasource.db.dao.LearningProgressDao
import me.ltthuc.kmp.core.datasource.db.dao.LevelDao
import me.ltthuc.kmp.core.datasource.db.dao.PhonicsLessonDao
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao
import me.ltthuc.kmp.core.datasource.db.dao.UserProgressDao
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.LevelEntity
import me.ltthuc.kmp.core.datasource.db.entity.PhonicsLessonEntity
import me.ltthuc.kmp.core.datasource.db.entity.UnitEntity
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity

@Database(
    entities = [
        LevelEntity::class,
        UnitEntity::class,
        PhonicsLessonEntity::class,
        LearningProgressEntity::class,
        UserProgressEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@ConstructedBy(GrabeeDatabaseConstructor::class)
abstract class GrabeeDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun unitDao(): UnitDao
    abstract fun phonicsLessonDao(): PhonicsLessonDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun userProgressDao(): UserProgressDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object GrabeeDatabaseConstructor : RoomDatabaseConstructor<GrabeeDatabase> {
    override fun initialize(): GrabeeDatabase
}

internal const val GRABEE_DB_NAME = "grabee.db"
