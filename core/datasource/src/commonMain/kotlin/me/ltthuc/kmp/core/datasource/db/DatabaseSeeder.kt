package me.ltthuc.kmp.core.datasource.db

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity
import me.ltthuc.kmp.core.resource.Res
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
        val existingLessons = database.phonicsLessonDao().count()
        val expectedLevels = entities.levels.size
        val expectedUnits = entities.units.size
        val expectedLessons = entities.lessons.size

        if (existingLevels >= expectedLevels &&
            existingUnits >= expectedUnits &&
            existingLessons >= expectedLessons
        ) {
            return
        }

        if (existingLevels > 0 || existingUnits > 0 || existingLessons > 0) {
            Napier.d(tag = TAG) {
                "Stale curriculum in DB: levels=$existingLevels/$expectedLevels, " +
                    "units=$existingUnits/$expectedUnits, " +
                    "lessons=$existingLessons/$expectedLessons. Re-seeding (REPLACE)."
            }
        }

        database.levelDao().insertAll(entities.levels)
        database.unitDao().insertAll(entities.units)
        database.phonicsLessonDao().insertAll(entities.lessons)
        database.learningProgressDao().upsert(SEED_PROGRESS)
        SEED_USER_PROGRESS.forEach { database.userProgressDao().upsert(it) }

        Napier.d(tag = TAG) {
            "Seeded ${entities.levels.size} levels, ${entities.units.size} units, " +
                "${entities.lessons.size} lessons from $CURRICULUM_RESOURCE_PATH"
        }
    }

    private companion object {
        const val TAG = "DatabaseSeeder"
        const val CURRICULUM_RESOURCE_PATH = "files/curriculum.json"

        private val seedJson = Json { ignoreUnknownKeys = true }

        val SEED_PROGRESS = LearningProgressEntity(
            activeLevelId = "L1",
            activeUnitId = "L1U2",
            activeLessonIndex = 0,
            activeStepIndex = 0,
            unitProgressPercent = 25,
        )

        val SEED_USER_PROGRESS = listOf(
            UserProgressEntity("L1U1", stepIndex = 0, starsEarned = 3, completedAt = 0),
            UserProgressEntity("L1U1", stepIndex = 1, starsEarned = 3, completedAt = 0),
            UserProgressEntity("L1U2", stepIndex = 0, starsEarned = 2, completedAt = 0),
        )
    }
}
