package me.ltthuc.kmp.core.datasource.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal fun RoomDatabase.Builder<GrabeeDatabase>.buildGrabeeDatabase(
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
): GrabeeDatabase = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(queryDispatcher)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
