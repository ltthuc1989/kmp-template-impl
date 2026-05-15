package me.ltthuc.kmp.core.model

data class LevelCard(
    val level: Level,
    val status: LevelStatus,
)

sealed interface LevelStatus {
    data class Active(
        val currentUnit: PhonicsUnit,
        val progressPercent: Int,
    ) : LevelStatus

    data object ReadyToStart : LevelStatus

    data class Locked(
        val prerequisiteLevel: Level?,
        val isPremiumRequired: Boolean = false,
    ) : LevelStatus

    data object ComingSoon : LevelStatus
}
