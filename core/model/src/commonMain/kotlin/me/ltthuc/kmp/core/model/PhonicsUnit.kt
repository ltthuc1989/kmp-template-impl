package me.ltthuc.kmp.core.model

data class PhonicsUnit(
    val id: String,
    val levelId: String,
    val number: Int,
    val title: String,
    val themeChip: String?,
    val orderIndex: Int,
)
