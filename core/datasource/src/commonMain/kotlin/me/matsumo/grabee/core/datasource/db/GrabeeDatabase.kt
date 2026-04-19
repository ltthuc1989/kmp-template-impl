package me.matsumo.grabee.core.datasource.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import me.matsumo.grabee.core.datasource.db.dao.LearningProgressDao
import me.matsumo.grabee.core.datasource.db.dao.LevelDao
import me.matsumo.grabee.core.datasource.db.dao.UnitDao
import me.matsumo.grabee.core.datasource.db.dao.UserProgressDao
import me.matsumo.grabee.core.datasource.db.dao.WordDao
import me.matsumo.grabee.core.datasource.db.entity.LearningProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.LevelEntity
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity
import me.matsumo.grabee.core.datasource.db.entity.UserProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.WordEntity

@Database(
    entities = [
        LevelEntity::class,
        UnitEntity::class,
        WordEntity::class,
        LearningProgressEntity::class,
        UserProgressEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@ConstructedBy(GrabeeDatabaseConstructor::class)
abstract class GrabeeDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun unitDao(): UnitDao
    abstract fun wordDao(): WordDao
    abstract fun learningProgressDao(): LearningProgressDao
    abstract fun userProgressDao(): UserProgressDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object GrabeeDatabaseConstructor : RoomDatabaseConstructor<GrabeeDatabase> {
    override fun initialize(): GrabeeDatabase
}

internal const val GRABEE_DB_NAME = "grabee.db"
