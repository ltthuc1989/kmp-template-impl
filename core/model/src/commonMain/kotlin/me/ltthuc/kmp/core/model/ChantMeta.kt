package me.ltthuc.kmp.core.model

data class ChantMeta(
    val lessonId: String,
    val startMs: Long,
    val speedMs: Long,
    val edgeSpeedMs: Long,
    val wordTimings: List<WordTiming>,
)
