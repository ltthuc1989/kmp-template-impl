package me.ltthuc.kmp.core.model

/**
 * Timing map for one Level 2+ blend lesson: where each card's sound sits inside the per-page
 * chain audio, so the equation animation can follow real playback position instead of guessing
 * with fixed delays.
 *
 * Authored offline by `opw_audio_project/scripts/assemble_blend.py` into
 * `files/blend_meta/level_<n>.json`, and meant to stay hand-editable — nudging a [start_ms] there
 * re-syncs the animation without regenerating any audio.
 */
data class BlendMeta(
    val lessonId: String,
    val chains: List<BlendChain>,
)

/** One page of the lesson: the whole `a → n → an → f → an → fan` run in a single audio file. */
data class BlendChain(
    val word: String,
    val audio: String,
    val durationMs: Int,
    val segments: List<BlendSegment>,
)

/**
 * One spoken card inside a chain. [card] matches the equation card ids the screen builds
 * (`1o0`/`1o1`/`1r` for the rime row, `2o0`/`2o1`/`2r` for the word row) — matching by id rather
 * than by list position keeps a hand-edited or reordered JSON from silently mis-highlighting.
 */
data class BlendSegment(
    val card: String,
    val text: String,
    val startMs: Int,
    val endMs: Int,
)
