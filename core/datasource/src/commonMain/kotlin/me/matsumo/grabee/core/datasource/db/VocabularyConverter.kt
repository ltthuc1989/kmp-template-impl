package me.matsumo.grabee.core.datasource.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.model.VocabularyItem

class VocabularyConverter {
    @TypeConverter
    fun fromJson(value: String?): List<VocabularyItem> {
        if (value.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<VocabularyItem>>(value) }.getOrElse { emptyList() }
    }

    @TypeConverter
    fun toJson(items: List<VocabularyItem>): String = json.encodeToString(items)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
