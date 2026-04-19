package me.matsumo.grabee.core.model

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
    ) : LevelStatus
}
