package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.first
import me.ltthuc.kmp.core.datasource.db.dao.UnitDao

/**
 * Coordinates a full wipe of local learning progress so the user restarts from Level 1 Unit 1.
 * Clears step scores, lesson completions, and unit completions, then restores the active position
 * to first-install state. Does NOT touch app settings (theme/sound) or the static curriculum.
 */
class ProgressResetRepository(
    private val userProgressRepository: UserProgressRepository,
    private val lessonProgressRepository: LessonProgressRepository,
    private val unitCompletionRepository: UnitCompletionRepository,
    private val learningProgressRepository: LearningProgressRepository,
    private val unitDao: UnitDao,
) {
    suspend fun resetAllProgress() {
        userProgressRepository.resetAll()
        lessonProgressRepository.resetAll()
        unitCompletionRepository.resetAll()
        learningProgressRepository.reset()
    }

    /**
     * Clears unit + lesson completion for every unit in [levelId] so its units re-lock to the
     * sequential gate (used by the parent "Lock all" control after turning off a manual unlock).
     * Does not touch other levels or the active position.
     */
    suspend fun resetLevel(levelId: String) {
        val unitIds = unitDao.observeAll().first()
            .filter { it.levelId == levelId }
            .map { it.id }
        unitIds.forEach { unitId ->
            unitCompletionRepository.reset(unitId)
            lessonProgressRepository.reset(unitId)
        }
    }
}
