package me.ltthuc.kmp.core.repository

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
) {
    suspend fun resetAllProgress() {
        userProgressRepository.resetAll()
        lessonProgressRepository.resetAll()
        unitCompletionRepository.resetAll()
        learningProgressRepository.reset()
    }
}
