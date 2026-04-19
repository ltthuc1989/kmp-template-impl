package me.matsumo.grabee.core.model

data class LearningProgress(
    val activeLevelId: String,
    val activeUnitId: String,
    val unitProgressPercent: Int,
)
