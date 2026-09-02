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
 *
 * [row] and [slot] are Level 3+ only, and exist because card ids alone stop being unique there:
 * a Level 3 page speaks the word row TWICE, so `2o0` appears twice in one chain and matching by
 * card would light the wrong beat. They address the beat directly instead —
 *
 *     row 1, slot i   nhịp thứ i của hàng vần        (`patternSteps`)
 *     row 2, slot i   mảnh thứ i của hàng từ         (`blendParts`)
 *     row 2, slot -1  đọc CẢ TỪ, màn hình dồn màu về đen
 *
 * Level 2 JSON has neither field; [row] stays 0 there and the Level 2 screen keeps matching by
 * [card] exactly as before.
 */
data class BlendSegment(
    val card: String,
    val text: String,
    val startMs: Int,
    val endMs: Int,
    val row: Int = 0,
    val slot: Int = 0,
)
