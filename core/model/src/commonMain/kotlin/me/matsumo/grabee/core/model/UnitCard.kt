package me.matsumo.grabee.core.model

data class UnitCard(
    val unit: PhonicsUnit,
    val status: UnitStatus,
    val totalStars: Int,
    val previewEmojis: List<String>,
) {
    companion object {
        const val MAX_STARS_PER_UNIT = 24
        const val UNLOCK_THRESHOLD_STARS = 6
    }
}

enum class UnitStatus {
    Completed,
    Active,
    Unlocked,
    Locked,
}
