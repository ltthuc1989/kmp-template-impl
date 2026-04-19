package me.matsumo.grabee.core.model

data class UserProgress(
    val unitId: String,
    val stepIndex: Int,
    val starsEarned: Int,
    val completedAt: Long,
)
