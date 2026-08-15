package me.ltthuc.kmp.core.datasource.db

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.db.entity.LearningProgressEntity
import me.ltthuc.kmp.core.datasource.db.entity.UserProgressEntity
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DatabaseSeeder(
    private val database: GrabeeDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var syncedThisProcess = false

    /**
     * Pushes `curriculum.json` into the DB, replacing whatever is there.
     *
     * Curriculum is app-owned static content, so the file always wins: editing a lesson in
     * place (new chant text, fixed word, different emoji) has to reach installs that already
     * hold the old row. Counting rows cannot detect that — an edit leaves the counts equal —
     * which is why this replaces unconditionally instead of only filling an empty DB.
     *
     * Runs once per process (~165 rows, REPLACE), so the repeated flow restarts that call
     * this do not re-write on every subscription.
     *
     * Progress rows are seeded ONLY when the DB was empty: they belong to the user, and
     * re-seeding them on a content edit would wipe the child's stars.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun syncCurriculum() = withContext(dispatcher) {
        mutex.withLock {
            if (syncedThisProcess) return@withLock

            val bytes = Res.readBytes(CURRICULUM_RESOURCE_PATH)
            val jsonStr = bytes.decodeToString()
            val curriculum = seedJson.decodeFromString<CurriculumDto>(jsonStr)
            val entities = curriculum.toEntities()

            val wasEmpty = database.levelDao().count() == 0 &&
                database.unitDao().count() == 0 &&
                database.phonicsLessonDao().count() == 0

            // Order matters: `units` cascades onto `phonics_lesson`, so lessons go in last.
            database.levelDao().insertAll(entities.levels)
            database.unitDao().insertAll(entities.units)
            database.phonicsLessonDao().insertAll(entities.lessons)

            if (wasEmpty) {
                database.learningProgressDao().upsert(SEED_PROGRESS)
                SEED_USER_PROGRESS.forEach { database.userProgressDao().upsert(it) }
            }

            syncedThisProcess = true
            Napier.d(tag = TAG) {
                "Synced ${entities.levels.size} levels, ${entities.units.size} units, " +
                    "${entities.lessons.size} lessons from $CURRICULUM_RESOURCE_PATH " +
                    "(freshInstall=$wasEmpty)"
            }
        }
    }

    private companion object {
        const val TAG = "DatabaseSeeder"
        const val CURRICULUM_RESOURCE_PATH = "files/curriculum.json"

        private val seedJson = Json { ignoreUnknownKeys = true }

        // First-install state: user lands on L1 → only L1U1 is Unlocked, no completions yet.
        // Sequential gating in [UnitRepository.buildUnitCards] keeps L1U2+ Locked until
        // L1U1's completionCount > 0 (user reaches UnitCompleteScreen).
        val SEED_PROGRESS = LearningProgressEntity.initial()

        val SEED_USER_PROGRESS = emptyList<UserProgressEntity>()
    }
}
