package me.ltthuc.kmp.core.datasource.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal fun createGrabeeDatabaseBuilder(): RoomDatabase.Builder<GrabeeDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val dbFilePath = requireNotNull(documentDirectory?.path) {
        "Unable to resolve iOS documents directory for Grabee database"
    } + "/$GRABEE_DB_NAME"
    return Room.databaseBuilder<GrabeeDatabase>(name = dbFilePath)
}
