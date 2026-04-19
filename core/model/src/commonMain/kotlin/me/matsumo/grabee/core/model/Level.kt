package me.matsumo.grabee.core.model

data class Level(
    val id: String,
    val number: Int,
    val title: String,
    val totalUnits: Int,
    val isPremium: Boolean,
    val orderIndex: Int,
)
