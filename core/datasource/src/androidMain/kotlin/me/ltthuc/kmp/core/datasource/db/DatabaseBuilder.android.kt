package me.ltthuc.kmp.core.datasource.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

internal fun createGrabeeDatabaseBuilder(context: Context): RoomDatabase.Builder<GrabeeDatabase> {
    val dbFile = context.applicationContext.getDatabasePath(GRABEE_DB_NAME)
    return Room.databaseBuilder<GrabeeDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
