package me.matsumo.grabee.core.datasource.db

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.datasource.db.entity.LearningProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.UserProgressEntity
import me.matsumo.grabee.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DatabaseSeeder(private val database: GrabeeDatabase) {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedIfEmpty() {
        val bytes = Res.readBytes(CURRICULUM_RESOURCE_PATH)
        val jsonStr = bytes.decodeToString()
        val curriculum = seedJson.decodeFromString<CurriculumDto>(jsonStr)
        val entities = curriculum.toEntities()

        val existingLevels = database.levelDao().count()
        val existingUnits = database.unitDao().count()
        val expectedLevels = entities.levels.size
        val expectedUnits = entities.units.size

        // Content already matches current curriculum shipped in JSON — skip re-seed.
        if (existingLevels >= expectedLevels && existingUnits >= expectedUnits) return

        // Stale seed (older build with fewer levels/units). Wipe + re-seed so the app
        // always matches the bundled curriculum.json without requiring users to clear data.
        if (existingLevels > 0 || existingUnits > 0) {
            Napier.d(tag = TAG) {
                "Stale curriculum in DB: levels=$existingLevels/$expectedLevels, " +
                    "units=$existingUnits/$expectedUnits. Wiping + re-seeding."
            }
            database.clearAllTables()
        }

        database.levelDao().insertAll(entities.levels)
        database.unitDao().insertAll(entities.units)
        database.wordDao().insertAll(entities.words)
        database.learningProgressDao().upsert(SEED_PROGRESS)
        SEED_USER_PROGRESS.forEach { database.userProgressDao().upsert(it) }

        Napier.d(tag = TAG) {
            "Seeded ${entities.levels.size} levels, ${entities.units.size} units, " +
                "${entities.words.size} words from $CURRICULUM_RESOURCE_PATH"
        }
    }

    private companion object {
        const val TAG = "DatabaseSeeder"
        const val CURRICULUM_RESOURCE_PATH = "files/curriculum.json"

        private val seedJson = Json { ignoreUnknownKeys = true }

        val SEED_PROGRESS = LearningProgressEntity(
            activeLevelId = "level-1",
            activeUnitId = "level-1-unit-2",
            unitProgressPercent = 25,
        )

        val SEED_USER_PROGRESS = listOf(
            // Unit 1: hoàn thành đủ stars để unlock Unit 2
            UserProgressEntity("level-1-unit-1", stepIndex = 0, starsEarned = 3, completedAt = 0),
            UserProgressEntity("level-1-unit-1", stepIndex = 1, starsEarned = 3, completedAt = 0),
            // Unit 2: đã bắt đầu
            UserProgressEntity("level-1-unit-2", stepIndex = 0, starsEarned = 2, completedAt = 0),
        )
    }
}
