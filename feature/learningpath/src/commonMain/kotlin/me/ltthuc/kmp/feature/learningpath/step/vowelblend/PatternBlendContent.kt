package me.ltthuc.kmp.feature.learningpath.step.vowelblend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.BlendMeta
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.slide_next_cd
import me.ltthuc.kmp.core.resource.slide_previous_cd
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.step.common.BlendPiece
import me.ltthuc.kmp.feature.learningpath.step.common.BlendPieceKind
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.StepChevronButton
import me.ltthuc.kmp.feature.learningpath.step.common.blendParts
import me.ltthuc.kmp.feature.learningpath.step.common.lessonPatterns
import org.jetbrains.compose.resources.stringResource

/**
 * Bước 0 của cấp 3 — dạy nguyên âm dài.
 *
 * Khác hẳn cấp 2 nên viết riêng, KHÔNG cắm thêm nhánh vào [VowelBlendContent]: cấp 2 tách
 * từ thành thẻ rời rồi cộng lại (`t` + `an` = `tan`), còn cấp 3 giữ từ nguyên khối và chỉ
 * phóng to phần đang đọc. Nhồi hai mô hình vào một hàm thì mỗi lần sửa cấp 3 lại rình rập
 * làm hỏng cấp 2 vốn đã ship.
 *
 * Mỗi trang một từ, hai hàng:
 *   hàng 1   vần đang học ("a_e", "ame", "ai")
 *   hàng 2   cả từ, chữ liền nhau
 *
 * Trình tự: đọc vần một lần → đánh vần từng mảnh → đọc cả từ, lặp [SPELL_REPEATS] lượt.
 *
 * HAI ĐƯỜNG CHẠY NHỊP:
 *   có [blendMeta]  một file tiếng cho cả trang, chữ sáng theo VỊ TRÍ PHÁT THẬT
 *   không có        khoảng chờ cố định, câm — giữ lại để bài chưa sinh audio vẫn xem được
 *
 * Cấp 2 lặp bằng cách phát LẠI cả file; cấp 3 thì không, vì hàng vần nằm NGOÀI vòng lặp.
 * Nên cả 2 lượt đánh vần đã được gói sẵn trong một file, màn hình phát đúng một lần rồi
 * bám theo mốc `row`/`slot` để biết sáng chỗ nào.
 */
@Composable
internal fun PatternBlendContent(
    lesson: PhonicsLesson,
    blendMeta: BlendMeta?,
    audioState: AudioState,
    onPlayChain: (page: Int, word: String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val pages = remember(lesson.id) { buildPatternPages(lesson) }
    var pageIndex by remember(lesson.id) { mutableStateOf(0) }
    var allDone by remember(lesson.id) { mutableStateOf(false) }
    // Bật khi bé tự bấm mũi tên để xem lại. Trang vẫn đọc lại như thường, nhưng KHÔNG tự
    // lật tiếp — nếu vẫn tự lật thì bấm "về trang trước" xong lại bị kéo tới trang cuối,
    // mũi tên hoá ra vô dụng. Giống hệt cấp 2.
    var browsing by remember(lesson.id) { mutableStateOf(false) }
    val safePage = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val page = pages.getOrNull(safePage)

    // Mảnh đang đọc. null = không mảnh nào — dùng cả lúc nghỉ giữa hai mảnh, nên chữ trở
    // về cỡ thường giữa các nhịp thay vì dính ở cỡ to.
    var active by remember(lesson.id, safePage) { mutableStateOf<ActiveSpan?>(null) }
    // Nhịp đọc cả từ: chữ chuyển sang đen rồi trả lại màu cũ, KHÔNG phóng to.
    var inkAll by remember(lesson.id, safePage) { mutableStateOf(false) }
    // Đếm số lần chạy lại do bé bấm thẻ hình; đổi giá trị là khởi động lại vòng đọc.
    var replayTick by remember(lesson.id, safePage) { mutableStateOf(0) }

    val chain = blendMeta?.chains?.getOrNull(safePage)

    // ---- Có audio: chữ bám theo vị trí phát thật ------------------------------------------
    // Sáng GIỮ tới khi nhịp kế bắt đầu chứ không tắt ở end_ms: mảnh phụ âm cuối cắt ra chỉ
    // 140-180ms, tắt đúng lúc thì chữ chỉ loé lên rồi mất.
    val positionMs = (audioState as? AudioState.Playing)?.positionMs ?: -1L
    val activeSegment = if (chain != null && positionMs >= 0L) {
        chain.segments.lastOrNull { positionMs >= it.startMs }
    } else {
        null
    }
    // SUY RA giá trị, KHÔNG gán vào state: gán state trong thân composable là ghi lúc đang
    // dựng khung hình, Compose cấm và nó đẻ ra vòng recomposition.
    val chainActive = activeSegment
        ?.takeIf { it.slot >= 0 }
        ?.let { seg ->
            val spans = when (seg.row) {
                1 -> page?.patternSteps?.getOrNull(seg.slot)
                else -> page?.pieces?.getOrNull(seg.slot)?.spans
            }
            spans?.let { ActiveSpan(row = seg.row, spans = it) }
        }
    // slot -1 = đang đọc cả từ; màn hình dồn màu về đen thay vì phóng to.
    val chainInk = activeSegment?.slot == WHOLE_WORD_SLOT

    // Bài đã có audio thì nhịp bám theo tiếng; chưa có thì dùng state của đường chờ cố định.
    val shownActive = if (chain != null) chainActive else active
    val shownInk = if (chain != null) chainInk else inkAll

    val currentAudioState by rememberUpdatedState(audioState)

    LaunchedEffect(lesson.id, safePage, replayTick, chain) {
        if (page == null || chain == null) return@LaunchedEffect
        delay(START_DELAY_MS)
        onPlayChain(safePage, chain.word)
        // Chặn bằng chính độ dài của file: thiếu asset thì mất một nhịp, chứ không treo
        // trang lại vĩnh viễn với nút Next tắt.
        withTimeoutOrNull(chain.durationMs + CHAIN_TIMEOUT_PAD_MS) {
            snapshotFlow { currentAudioState }.first { it is AudioState.Playing }
            snapshotFlow { currentAudioState }
                .first { it is AudioState.Idle || it is AudioState.Error }
        }
        delay(PAGE_TURN_DELAY_MS)
        if (safePage >= pages.lastIndex || browsing) allDone = true else pageIndex = safePage + 1
    }

    // ---- Chưa có audio: khoảng chờ cố định, câm -------------------------------------------
    LaunchedEffect(lesson.id, safePage, replayTick, chain) {
        if (page == null || chain != null) return@LaunchedEffect
        active = null
        inkAll = false
        delay(START_DELAY_MS)

        // Vần đọc MỘT lần, không nằm trong vòng lặp — bé cần biết vần trước khi ghép, chứ
        // nghe lại vần ở lượt hai thì chỉ làm loãng phần đánh vần.
        for (step in page.patternSteps) {
            active = ActiveSpan(row = 1, spans = step)
            delay(PIECE_MS)
            active = null
            delay(GAP_MS)
        }

        repeat(SPELL_REPEATS) { pass ->
            if (pass > 0) delay(REPEAT_GAP_MS)
            for (piece in page.pieces) {
                active = ActiveSpan(row = 2, spans = piece.spans)
                delay(PIECE_MS)
                active = null
                delay(GAP_MS)
            }
            inkAll = true
            delay(WHOLE_MS)
            inkAll = false
            delay(GAP_MS)
        }

        delay(PAGE_TURN_DELAY_MS)
        if (safePage >= pages.lastIndex || browsing) allDone = true else pageIndex = safePage + 1
    }

    VowelBlendScaffold(
        onClose = onClose,
        onStepJump = onStepJump,
        stepSegments = stepSegments,
        nextEnabled = allDone,
        onNext = onNext,
    ) {
        if (page == null) return@VowelBlendScaffold
        Spacer(Modifier.height(12.dp))
        // Dùng lại đúng khung thẻ của cấp 2 để hai cấp nhìn cùng một bộ.
        EquationPanel {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpanRow(
                    text = page.pattern,
                    pinkSpans = page.patternPink,
                    active = shownActive?.takeIf { it.row == 1 }?.spans,
                    inkAll = false,
                    fontSize = PATTERN_SP,
                )
                Spacer(Modifier.height(18.dp))
                SpanRow(
                    text = page.word.text,
                    pinkSpans = page.wordPink,
                    active = shownActive?.takeIf { it.row == 2 }?.spans,
                    inkAll = shownInk,
                    fontSize = WORD_SP,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WordCard(
                word = page.word,
                replayable = allDone,
                // Bấm thẻ hình = nghe lại CẢ TRANG, giống cấp 2. Chỉ mở khi trang đã chạy
                // xong, nếu không bé bấm giữa chừng là hai vòng đọc chồng lên nhau.
                onTap = { replayTick++ },
            )
            // Mũi tên lật trang, đặt trên hai mép thẻ hình — y như cấp 2. Chỉ hiện sau khi cả
            // bài chạy xong (lúc nút Next sáng): đang dạy dở mà cho lật thì bé bấm lung tung,
            // chuỗi đọc đang chạy bị cắt ngang.
            if (allDone && pages.size > 1) {
                StepChevronButton(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(Res.string.slide_previous_cd),
                    enabled = safePage > 0,
                    onClick = {
                        browsing = true
                        pageIndex = (safePage - 1).coerceAtLeast(0)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp),
                )
                StepChevronButton(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(Res.string.slide_next_cd),
                    enabled = safePage < pages.lastIndex,
                    onClick = {
                        browsing = true
                        pageIndex = (safePage + 1).coerceAtMost(pages.lastIndex)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PageDotsRow(currentPage = safePage, total = pages.size)
    }
}

/**
 * Hàng chữ: từ vẫn đọc ra như một khối liền, nhưng mỗi ký tự là một phần tử riêng để
 * phóng to được đúng phần đang đọc.
 *
 * Vì sao không dùng một [Text] với `AnnotatedString`: Compose không co giãn được từng
 * đoạn bên trong một Text — `SpanStyle` đổi được màu và nét, nhưng không đổi được kích
 * thước theo kiểu phóng to mà vẫn giữ nguyên chỗ. Xếp từng ký tự ra thì mỗi ký tự có
 * hiệu ứng riêng, đổi lại mất kerning — với chữ dạy đọc thì không tiếc, vì các ký tự
 * vốn đã phải giãn ra cho bé nhìn rõ ranh giới.
 */
@Composable
private fun SpanRow(
    text: String,
    pinkSpans: List<IntRange>,
    active: List<IntRange>?,
    inkAll: Boolean,
    fontSize: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LETTER_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { i, ch ->
            val hot = active?.any { i in it } == true
            val scale by animateFloatAsState(
                targetValue = if (hot) HOT_SCALE else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "char-scale",
            )
            // Vần đang dạy màu hồng, còn lại xanh. KHÔNG tô theo "ký tự có phải nguyên âm
            // không" — chữ `a` của `happy` là nguyên âm nhưng bài đó dạy vần `y`.
            val base = if (pinkSpans.any { i in it }) VowelColor else ConsonantColor
            // Nhịp đọc cả từ dồn màu về đen rồi tự trả lại khi [inkAll] tắt.
            val color by animateColorAsState(
                targetValue = if (inkAll) lerp(base, Color.Black, INK_STRENGTH) else base,
                label = "char-colour",
            )
            Text(
                text = ch.toString(),
                fontFamily = LocalPhonicsFontFamily.current,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                color = color,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        // Bóng lệch chéo cho ra khối 3D, chỉ hiện ở ký tự đang đọc.
                        shadow = if (hot) {
                            Shadow(
                                color = Color.Black.copy(alpha = SHADOW_ALPHA),
                                offset = Offset(SHADOW_DX, SHADOW_DY),
                                blurRadius = SHADOW_BLUR,
                            )
                        } else {
                            null
                        },
                    ),
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

/** Mảnh đang được đọc: nằm ở hàng nào và trùm những khoảng ký tự nào. */
private data class ActiveSpan(val row: Int, val spans: List<IntRange>)

/** Một trang = một từ, kèm mọi thứ đã tính sẵn để lúc chạy không phải tính lại. */
private data class PatternPage(
    val word: me.ltthuc.kmp.core.model.LessonWord,
    val pattern: String,
    val patternPink: List<IntRange>,
    val patternSteps: List<List<IntRange>>,
    val wordPink: List<IntRange>,
    val pieces: List<BlendPiece>,
)

private fun buildPatternPages(lesson: PhonicsLesson): List<PatternPage> {
    val patterns = lesson.lessonPatterns()
    return lesson.words.map { word ->
        val pattern = patternForWord(patterns, word.text)
        val pieces = blendParts(word.text)
        PatternPage(
            word = word,
            pattern = pattern,
            patternPink = patternPinkSpans(pattern),
            patternSteps = patternSteps(pattern),
            wordPink = pinkSpansFor(pieces, pattern),
            pieces = pieces,
        )
    }
}

/**
 * Vần nào hợp với từ này, trong những vần lesson dạy.
 *
 * Bài dạy hai vần thì mỗi từ theo một vần: `game` lấy `ame`, `cake` lấy `ake`. Thử vần DÀI
 * trước — `money` khớp cả `y` lẫn `ey`, mà `ey` mới đúng.
 */
private fun patternForWord(patterns: List<String>, word: String): String {
    val w = word.lowercase()
    val byLength = patterns.sortedByDescending { it.length }
    return byLength.firstOrNull { w.endsWith(it) }
        ?: byLength.firstOrNull { w.contains(it) }
        ?: patterns.firstOrNull().orEmpty()
}

/**
 * Các nhịp đọc vần ở hàng 1.
 *
 * Vần kiểu magic-e có phụ âm ("ame") thì dựng dần: /eɪ/ → /m/ → /eɪm/, để bé thấy vần được
 * lắp từ đâu ra. Vần chỉ có nguyên âm ("a_e", "ai", "ee") thì đọc một nhịp duy nhất — tách
 * `a` với `i` ra đọc rời là sai âm, `ai` là MỘT âm chứ không phải hai.
 */
private fun patternSteps(pattern: String): List<List<IntRange>> {
    val whole = listOf(listOf(pattern.indices))
    // "a_e" cũng theo khuôn magic-e nhưng KHÔNG dựng dần: chỗ `_` để trống, không có phụ
    // âm nào để đọc thành một nhịp riêng.
    if (!isMagicERime(pattern) || pattern.contains('_')) return whole
    return listOf(
        listOf(0..0, MAGIC_E_TAIL..MAGIC_E_TAIL),
        listOf(1..1),
        listOf(pattern.indices),
    )
}

/**
 * Ký tự nào của VẦN ở hàng 1 được tô hồng — chỉ phần nguyên âm, đúng như hàng 2 tô cho từ.
 *
 * Vần magic-e ("ame") viết ra là nguyên âm + phụ âm + `e` câm, mà bài dạy là `a_e`; chữ ở
 * giữa chỉ là phụ âm nên để xanh như mọi phụ âm khác. Tô hồng cả ba chữ thì hàng 1 bảo vần
 * là "ame" trong khi hàng 2 của `game` chỉ tô `a` với `e` — hai hàng dạy hai điều khác nhau.
 * Chữ `_` của "a_e" cũng xanh: nó đứng thay chỗ phụ âm chứ không phải nguyên âm.
 *
 * Tổ hợp nguyên âm ("ai", "igh", "ee") vẫn hồng cả cụm — cả cụm mới là MỘT nguyên âm.
 */
private fun patternPinkSpans(pattern: String): List<IntRange> =
    if (isMagicERime(pattern)) {
        listOf(0..0, MAGIC_E_TAIL..MAGIC_E_TAIL)
    } else {
        listOf(pattern.indices)
    }

/** Vần theo khuôn magic-e: nguyên âm + MỘT phụ âm (hoặc chỗ trống `_`) + `e` cuối. */
private fun isMagicERime(pattern: String): Boolean =
    pattern.length == MAGIC_E_RIME_LENGTH &&
        pattern.endsWith('e') &&
        pattern.first() in VOWELS &&
        pattern[1] !in VOWELS

/**
 * Ký tự nào của từ được tô hồng: đúng phần nguyên âm của VẦN ĐANG DẠY.
 *
 * Từ hai âm tiết có tới hai mảnh nguyên âm (`happy` có `a` và `y`) nên phải chọn theo nhãn
 * khớp vần; không khớp được thì lấy mảnh nguyên âm cuối, vì vần luôn nằm cuối từ.
 */
private fun pinkSpansFor(pieces: List<BlendPiece>, pattern: String): List<IntRange> {
    val vowels = pieces.filter { it.kind == BlendPieceKind.Vowel }
    val matched = vowels.firstOrNull { it.label == pattern } ?: vowels.lastOrNull()
    return matched?.spans.orEmpty()
}

private const val VOWELS = "aeiou"
private const val MAGIC_E_RIME_LENGTH = 3

/** Vị trí chữ `e` câm trong vần magic-e ba chữ. */
private const val MAGIC_E_TAIL = 2

private const val PATTERN_SP = 40
private const val WORD_SP = 46
private const val HOT_SCALE = 1.34f
private const val LETTER_GAP_DP = 1
private const val SHADOW_ALPHA = 0.30f
private const val SHADOW_DX = 5f
private const val SHADOW_DY = 8f
private const val SHADOW_BLUR = 10f
private const val INK_STRENGTH = 0.85f

private const val START_DELAY_MS = 350L
private const val PIECE_MS = 680L
private const val WHOLE_MS = 900L
private const val GAP_MS = 500L
private const val REPEAT_GAP_MS = 500L
private const val PAGE_TURN_DELAY_MS = 1_400L
private const val SPELL_REPEATS = 2
private const val CHAIN_TIMEOUT_PAD_MS = 2_000L

/** `slot` của nhịp đọc CẢ TỪ — khớp `blend_meta`, xem [BlendSegment]. */
private const val WHOLE_WORD_SLOT = -1
