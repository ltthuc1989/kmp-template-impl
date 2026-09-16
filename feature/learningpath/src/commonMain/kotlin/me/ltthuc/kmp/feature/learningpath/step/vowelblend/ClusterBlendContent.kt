package me.ltthuc.kmp.feature.learningpath.step.vowelblend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.BlendMeta
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.slide_next_cd
import me.ltthuc.kmp.core.resource.slide_previous_cd
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.step.common.ClusterLetter
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.StepChevronButton
import me.ltthuc.kmp.feature.learningpath.step.common.clusterLetters
import me.ltthuc.kmp.feature.learningpath.step.common.equationOperands
import me.ltthuc.kmp.feature.learningpath.step.common.lessonPatterns
import me.ltthuc.kmp.feature.learningpath.step.common.patternForChunk
import me.ltthuc.kmp.feature.learningpath.step.common.splitMatchesWord
import me.ltthuc.kmp.feature.learningpath.step.common.wholeWordSplit
import org.jetbrains.compose.resources.stringResource

/**
 * Bước 0 của cấp 4 — dạy cụm phụ âm (`bl`, `spr`, `nd`) và chữ ghép (`sh`, `th`, `ck`).
 *
 * Viết riêng, KHÔNG cắm nhánh vào [PatternBlendContent] hay [VowelBlendContent]: cấp 3 tách
 * từ theo NGUYÊN ÂM (`t·a_e·p`) nên áp lên `black` sẽ ra `bl·a·ck` — sai hẳn bài đang dạy;
 * cấp 2 thì tách thành thẻ rời, mà cấp 4 phải giữ chữ đúng vị trí trong từ.
 *
 * Mỗi trang một từ, hai dòng — đúng hai kiểu panel "Listen and learn" của sách OPW4:
 *
 *     kiểu A (phép cộng)        kiểu B (một chữ ghép = một âm)
 *      b + l = bl                sh
 *      b l a c k                 s h e l l
 *
 * Trình tự một trang (chốt 2026-09-07, xem `~/.claude/plans/level4-step0-plan.md` mục 7):
 *   1. dòng 1 đọc MỘT lần, chỉ ở trang ĐẦU của mỗi cụm — trang sau ẩn hẳn (sửa 2026-09-14)
 *   2. hiện CẢ TỪ màu đen, im lặng ([WORD_PREVIEW_MS]) cho bé nhìn từ trước khi tách
 *   3. ẩn đi, rồi hiện lại từng mảnh theo nhịp đọc, mảnh đang đọc phóng to
 *   4. cả từ hiện lại và dồn màu về đen — lặp bước 3-4 đúng [SPELL_REPEATS] lượt
 *
 * Chữ ở dòng 2 GIỮ CHỖ NGẦM: mỗi ký tự chiếm một ô bề ngang cố định nhưng không viền,
 * không nền. Nhờ vậy `ack` hiện ra đúng chỗ của nó trong `black` thay vì nhảy về giữa dòng
 * — bé thấy mảnh vừa đọc nằm ở đâu trong từ, đó là cả bài học.
 *
 * HAI ĐƯỜNG CHẠY NHỊP, y như cấp 3:
 *   có [blendMeta]  một file tiếng cho cả trang, chữ sáng theo VỊ TRÍ PHÁT THẬT
 *   không có        khoảng chờ cố định, câm — bài chưa sinh audio vẫn xem được (P1)
 */
@Composable
internal fun ClusterBlendContent(
    lesson: PhonicsLesson,
    blendMeta: BlendMeta?,
    audioState: AudioState,
    onPlayChain: (page: Int, word: String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val pages = remember(lesson.id) { buildClusterPages(lesson) }
    var pageIndex by remember(lesson.id) { mutableStateOf(0) }
    var allDone by remember(lesson.id) { mutableStateOf(false) }
    // Bật khi bé tự bấm mũi tên để xem lại: trang vẫn đọc lại, nhưng KHÔNG tự lật tiếp —
    // nếu vẫn tự lật thì bấm "về trang trước" xong lại bị kéo tới trang cuối. Giống cấp 2/3.
    var browsing by remember(lesson.id) { mutableStateOf(false) }
    val safePage = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val page = pages.getOrNull(safePage)

    // Thẻ đang đọc ở dòng 1 (null = không thẻ nào), và nhịp đang chạy ở dòng 2.
    var equationSlot by remember(lesson.id, safePage) { mutableStateOf<Int?>(null) }
    var wordBeat by remember(lesson.id, safePage) { mutableStateOf<WordBeat?>(null) }
    // Đếm số lần bé bấm thẻ hình để nghe lại; đổi giá trị là khởi động lại vòng đọc.
    var replayTick by remember(lesson.id, safePage) { mutableStateOf(0) }
    // Tiếng của trang đã bắt đầu phát chưa — để biết lúc về Idle là "đọc xong" chứ không phải "chưa đọc".
    var chainStarted by remember(lesson.id, safePage, replayTick) { mutableStateOf(false) }

    val chain = blendMeta?.chains?.getOrNull(safePage)

    // ---- Có audio: chữ bám theo vị trí phát thật ------------------------------------------
    // Sáng GIỮ tới khi nhịp kế bắt đầu chứ không tắt ở end_ms: mảnh phụ âm cắt ra chỉ
    // 140-180ms, tắt đúng lúc thì chữ chỉ loé lên rồi mất.
    val positionMs = (audioState as? AudioState.Playing)?.positionMs ?: -1L
    val segment = if (chain != null && positionMs >= 0L) {
        chain.segments.lastOrNull { positionMs >= it.startMs }
    } else {
        null
    }
    // SUY RA giá trị, KHÔNG gán vào state trong thân composable — Compose cấm ghi state lúc
    // đang dựng khung hình, và nó đẻ ra vòng recomposition.
    val chainSlot = segment?.takeIf { it.row == ROW_EQUATION && it.slot >= 0 }?.slot
    val chainBeat = segment?.takeIf { it.row == ROW_WORD }?.let { wordBeatOf(it.slot) }

    val shownSlot = if (chain != null) chainSlot else equationSlot
    // Đọc xong thì GIỮ cả từ trên màn. Hết tiếng thì `positionMs` về -1, nhịp suy ra thành null
    // và dòng 2 trống trơn — trang cuối không lật tiếp nên bé nhìn một khung rỗng (user bắt 2026-09-14).
    val chainEnded = chainStarted && audioState !is AudioState.Playing && audioState !is AudioState.Loading
    val shownBeat = if (chain != null) chainBeat ?: WordBeat.Whole.takeIf { chainEnded } else wordBeat

    val currentAudioState by rememberUpdatedState(audioState)

    LaunchedEffect(lesson.id, safePage, replayTick, chain) {
        if (page == null || chain == null) return@LaunchedEffect
        delay(START_DELAY_MS)
        onPlayChain(safePage, chain.word)
        // Chặn bằng chính độ dài của file: thiếu asset thì mất một nhịp, chứ không treo
        // trang lại vĩnh viễn với nút Next tắt.
        withTimeoutOrNull(chain.durationMs + CHAIN_TIMEOUT_PAD_MS) {
            snapshotFlow { currentAudioState }.first { it is AudioState.Playing }
            chainStarted = true
            snapshotFlow { currentAudioState }
                .first { it is AudioState.Idle || it is AudioState.Error }
        }
        delay(PAGE_TURN_DELAY_MS)
        if (safePage >= pages.lastIndex || browsing) allDone = true else pageIndex = safePage + 1
    }

    // ---- Chưa có audio: khoảng chờ cố định, câm (đường của P1) -----------------------------
    LaunchedEffect(lesson.id, safePage, replayTick, chain) {
        if (page == null || chain != null) return@LaunchedEffect
        equationSlot = null
        wordBeat = null
        delay(START_DELAY_MS)

        // Dòng 1 đọc MỘT lần và chỉ ở trang đầu của mỗi cụm — trang sau bé đã biết cụm rồi,
        // đọc lại chỉ làm loãng phần ghép từ (chốt câu 8).
        if (page.readsEquation) {
            // Kiểu B không có toán hạng nào: vòng lặp chạy đúng một nhịp cho thẻ cụm.
            for (slot in 0..page.operands.size) {
                equationSlot = slot
                // Giữ sáng hết cả khoảng nghỉ, tới khi nhịp sau bắt đầu — chớp tắt giữa hai
                // nhịp làm hàng thẻ nhấp nháy.
                delay(PIECE_MS + GAP_MS)
            }
            equationSlot = null
        }

        wordBeat = WordBeat.Preview
        delay(WORD_PREVIEW_MS + GAP_MS)

        repeat(SPELL_REPEATS) { pass ->
            if (pass > 0) delay(REPEAT_GAP_MS)
            for (index in 0 until page.chunkCount) {
                wordBeat = WordBeat.Chunk(index)
                delay(PIECE_MS + GAP_MS)
            }
            wordBeat = WordBeat.Whole
            delay(WHOLE_MS + GAP_MS)
        }

        delay(PAGE_TURN_DELAY_MS)
        if (safePage >= pages.lastIndex || browsing) allDone = true else pageIndex = safePage + 1
    }

    VowelBlendScaffold(
        onClose = onClose,
        onStepJump = onStepJump,
        stepSegments = stepSegments,
        // Bài không có từ nào là lỗi dữ liệu; mở sẵn nút Next để bé không bị kẹt trong màn trống.
        nextEnabled = allDone || page == null,
        onNext = onNext,
    ) {
        if (page == null) return@VowelBlendScaffold
        Spacer(Modifier.height(12.dp))
        // Dùng lại đúng khung thẻ của cấp 2/3 để ba cấp nhìn cùng một bộ.
        EquationPanel {
            // Dòng 1 chỉ có mặt ở trang đầu của mỗi cụm. Từ thứ hai trong cùng cụm thì ẩn hẳn
            // (sửa 2026-09-14): để hiện mà im thì bé nhìn hàng thẻ đứng yên, tưởng màn bị treo.
            if (page.readsEquation) {
                EquationCards(page = page, activeSlot = shownSlot)
                Spacer(Modifier.height(20.dp))
            }
            WordRow(letters = page.letters, beat = shownBeat)
        }

        Spacer(Modifier.height(22.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WordCard(
                word = page.word,
                replayable = allDone,
                // Bấm thẻ hình = nghe lại CẢ TRANG, giống cấp 2/3. Chỉ mở khi trang đã chạy
                // xong, nếu không bé bấm giữa chừng là hai vòng đọc chồng lên nhau.
                onTap = { replayTick++ },
            )
            // Mũi tên lật trang trên hai mép thẻ hình, chỉ hiện khi cả bài đã chạy xong.
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
 * Dòng 1: `b + l = bl` (kiểu A) hoặc một thẻ `sh` (kiểu B).
 *
 * Cả hàng thu nhỏ lại khi không đủ chỗ thay vì bị cắt: `s + p + r = spr` cần ~364dp mà máy
 * hẹp chỉ còn ~300dp trong khung thẻ. Thu nhỏ đều cả hàng vẫn đọc được; để Row tự co thì
 * thẻ cuối bị đẩy ra ngoài mép và biến mất.
 */
@Composable
private fun EquationCards(page: ClusterPage, activeSlot: Int?) {
    val resultSlot = page.operands.size
    val needed = remember(page.pattern) {
        val cards = page.operands.sumOf { cardWidthDp(it.length) } + cardWidthDp(page.pattern.length)
        val glyphs = if (page.operands.isEmpty()) 0 else page.operands.size * OPERATOR_WIDTH_DP
        (cards + glyphs).dp
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val scale = if (needed > maxWidth) maxWidth / needed else 1f
        Row(
            modifier = Modifier
                // Đo ở bề ngang THẬT rồi mới thu nhỏ khi vẽ: bị bó theo bề ngang khung thì
                // Row ép các thẻ chồng lên nhau trước khi phép thu nhỏ kịp có tác dụng.
                .wrapContentWidth(unbounded = true)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            page.operands.forEachIndexed { index, operand ->
                if (index > 0) OperatorGlyph("+")
                ClusterCard(text = operand, isActive = activeSlot == index)
            }
            if (page.operands.isNotEmpty()) OperatorGlyph("=")
            ClusterCard(text = page.pattern, isActive = activeSlot == resultSlot)
        }
    }
}

/** Thẻ chữ của dòng 1. Cả hàng đều là cụm đang dạy nên chữ luôn hồng. */
@Composable
private fun ClusterCard(text: String, isActive: Boolean) {
    BlendCardSurface(charCount = text.length, isActive = isActive) {
        Text(
            text = text,
            color = VowelColor,
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = CARD_TEXT_SP.sp,
            lineHeight = (CARD_TEXT_SP + 2).sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * Dòng 2: hàng ký tự giữ chỗ ngầm.
 *
 * Mỗi ô rộng đúng bằng bề ngang THẬT của chữ trong ô (đo bằng [rememberTextMeasurer]), rồi
 * cả hàng phóng/thu một hệ số cho vừa chỗ. Bản trước chia ô đều và đặt cỡ chữ = 1.34 × ô, nên
 * `m` `w` rộng hơn ô bị xén mép phải, còn `g` `y` `p` bị xén đáy vì line height 24sp của
 * LocalTextStyle ngắn hơn chữ (user bắt 2026-09-14).
 *
 * Cỡ chữ suy từ chỗ còn lại, không theo cỡ chữ hệ thống — hàng chữ này là hình vẽ dạy đọc,
 * phóng to theo cài đặt hệ thống chỉ làm từ tràn ra ngoài màn.
 */
@Composable
private fun WordRow(letters: ImmutableList<ClusterLetter>, beat: WordBeat?) {
    val measurer = rememberTextMeasurer()
    val fontFamily = LocalPhonicsFontFamily.current
    val density = LocalDensity.current
    val glyphs = remember(letters, fontFamily, density) {
        val style = letterStyle(fontFamily, REF_FONT_SP.sp)
        val n = measurer.measure("n", style).size
        GlyphMetrics(
            // Dấu cách chỉ chiếm nửa chữ `n` — `ice cream` mà để rộng thì hai chữ rời hẳn ra
            // như hai từ không liên quan.
            widths = letters.map {
                if (it.char == ' ') n.width * SPACE_WEIGHT else measurer.measure(it.char.toString(), style).size.width.toFloat()
            },
            height = n.height.toFloat(),
        )
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gaps = with(density) { LETTER_GAP_DP.dp.toPx() } * (letters.size - 1).coerceAtLeast(0)
        // Chừa chỗ cho chữ đang đọc phóng to ở hai mép hàng, nếu không nó lại bị xén.
        val needed = glyphs.widths.sum() + (glyphs.widths.maxOrNull() ?: 0f) * (HOT_SCALE - 1f)
        val refFontPx = with(density) { REF_FONT_SP.sp.toPx() }
        val capScale = with(density) { MAX_FONT_DP.dp.toPx() } / refFontPx
        val scale = if (needed > 0f) minOf((constraints.maxWidth - gaps) / needed, capScale) else capScale
        val fontSize = (REF_FONT_SP * scale).sp
        val slotHeight = with(density) { (glyphs.height * scale).toDp() }
        Row(
            horizontalArrangement = Arrangement.spacedBy(LETTER_GAP_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            letters.forEachIndexed { index, letter ->
                LetterSlot(
                    letter = letter,
                    slotWidth = with(density) { (glyphs.widths[index] * scale).toDp() },
                    slotHeight = slotHeight,
                    fontSize = fontSize,
                    shown = beat.shows(letter),
                    hot = beat is WordBeat.Chunk && letter.chunkIndex == beat.index,
                    inked = beat == WordBeat.Preview || beat == WordBeat.Whole,
                    inkInstant = beat == WordBeat.Preview,
                )
            }
        }
    }
}

@Composable
private fun LetterSlot(
    letter: ClusterLetter,
    slotWidth: Dp,
    slotHeight: Dp,
    fontSize: TextUnit,
    shown: Boolean,
    hot: Boolean,
    inked: Boolean,
    inkInstant: Boolean,
) {
    if (letter.char == ' ') {
        Spacer(Modifier.width(slotWidth))
        return
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(LETTER_FADE_MS),
        label = "letter-alpha",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            hot -> HOT_SCALE
            shown -> 1f
            else -> HIDDEN_SCALE
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "letter-scale",
    )
    // Cụm đang dạy màu hồng, còn lại xanh — theo bảng tách đã chốt, KHÔNG theo "ký tự có
    // phải nguyên âm không": `a` trong `black` là nguyên âm nhưng bài đang dạy `bl`.
    val base = if (letter.isPink) VowelColor else ConsonantColor
    val color by animateColorAsState(
        targetValue = if (inked) lerp(base, Color.Black, INK_STRENGTH) else base,
        // Nhịp "nhìn từ trước" phải ĐEN NGAY: nó là ảnh chụp cả từ, không phải nhịp đọc dần.
        animationSpec = if (inkInstant) snap() else tween(INK_MS),
        label = "letter-colour",
    )
    Box(
        modifier = Modifier.width(slotWidth).height(slotHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.char.toString(),
            color = color,
            // Style riêng, KHÔNG merge LocalTextStyle: bản đó mang lineHeight 24sp, ngắn hơn chữ
            // nên đáy `g` `y` `p` bị xén. Cùng style với lúc đo để ô và chữ khớp nhau.
            style = letterStyle(LocalPhonicsFontFamily.current, fontSize).copy(
                // Bóng lệch chéo cho ra khối 3D, chỉ hiện ở mảnh đang đọc.
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
            maxLines = 1,
            softWrap = false,
            // Lưới an toàn: đo lệch vài px thì chữ tràn ra ngoài ô chứ không bị xén.
            overflow = TextOverflow.Visible,
            modifier = Modifier.wrapContentSize(unbounded = true).graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

/** Bề ngang từng chữ và chiều cao dòng, đo ở cỡ [REF_FONT_SP]. */
private class GlyphMetrics(val widths: List<Float>, val height: Float)

private fun letterStyle(fontFamily: FontFamily?, fontSize: TextUnit) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = fontSize,
)

/** Nhịp đang chạy ở dòng 2. */
private sealed interface WordBeat {
    /** Cả từ, màu đen, IM LẶNG — cho bé nhìn từ trước khi tách ra đọc (chốt câu 7). */
    data object Preview : WordBeat

    /** Đang đọc mảnh thứ [index]; chỉ chữ của mảnh đó hiện, phóng to. */
    data class Chunk(val index: Int) : WordBeat

    /** Đọc cả từ: mọi chữ hiện và dồn màu về đen. */
    data object Whole : WordBeat
}

/** Ký tự này có hiện ở nhịp hiện tại không. Chưa tới nhịp nào thì dòng 2 để trống. */
private fun WordBeat?.shows(letter: ClusterLetter): Boolean = when (this) {
    null -> false
    WordBeat.Preview, WordBeat.Whole -> true
    is WordBeat.Chunk -> letter.chunkIndex == index
}

/**
 * `slot` của `blend_meta` → nhịp dòng 2. Hợp đồng với bộ lắp audio (`assemble_blend_pages`
 * cấp 4), phải khớp cả hai phía:
 *
 *     row 1, slot i        toán hạng thứ i của dòng 1 (kiểu B không có toán hạng nào)
 *     row 1, slot n        thẻ KẾT QUẢ, với n = số toán hạng (kiểu B: n = 0)
 *     row 2, slot -2       nhịp nhìn cả từ, im lặng — file phải chèn đúng khoảng lặng này
 *     row 2, slot i        mảnh thứ i của bảng tách
 *     row 2, slot -1       đọc cả từ
 */
private fun wordBeatOf(slot: Int): WordBeat = when (slot) {
    PREVIEW_SLOT -> WordBeat.Preview
    WHOLE_WORD_SLOT -> WordBeat.Whole
    else -> WordBeat.Chunk(slot)
}

/** Một trang = một từ, mọi thứ tính sẵn để lúc chạy không phải tính lại. */
private data class ClusterPage(
    val word: LessonWord,
    val pattern: String,
    val operands: ImmutableList<String>,
    val letters: ImmutableList<ClusterLetter>,
    val chunkCount: Int,
    /** Trang đầu của mỗi cụm mới có dòng 1; các trang sau ẩn hẳn dòng 1. */
    val readsEquation: Boolean,
)

private fun buildClusterPages(lesson: PhonicsLesson): List<ClusterPage> {
    val patterns = lesson.lessonPatterns()
    var previousPattern: String? = null
    return lesson.words.map { word ->
        // Bảng tách là NGUỒN DUY NHẤT, và phải khớp với từ. Thiếu hoặc lệch thì hiện cả từ
        // như một mảnh — vẫn xem được, nhưng LOG, vì đây là loại lỗi bé không kêu được:
        // màn hình vẫn chạy mượt, chỉ là không còn dạy tách cụm nữa.
        val split = word.blendSplit?.takeIf { splitMatchesWord(word.text, it) }
            ?: run {
                Napier.w(tag = TAG) {
                    "Bảng tách thiếu hoặc lệch: ${lesson.id}/${word.text} (${word.blendSplit}) — hiện cả từ làm một mảnh"
                }
                wholeWordSplit(word.text)
            }
        val pattern = patternForChunk(patterns, split.patternChunk)
        val page = ClusterPage(
            word = word,
            pattern = pattern,
            // Kiểu B trả rỗng → dòng 1 chỉ có thẻ cụm, không có phép cộng.
            operands = equationOperands(pattern).toImmutableList(),
            letters = clusterLetters(word.text, split, pattern).toImmutableList(),
            chunkCount = split.chunks.size,
            readsEquation = pattern != previousPattern,
        )
        previousPattern = pattern
        page
    }
}

/** Bề ngang một thẻ chữ, cùng công thức [BlendCardSurface] dùng để đặt bề ngang thật. */
private fun cardWidthDp(charCount: Int): Int = charCount * CARD_CHAR_WIDTH_DP + CARD_PADDING_DP

private const val TAG = "ClusterBlendContent"

/** `row` của hai dòng trong `blend_meta`. */
private const val ROW_EQUATION = 1
private const val ROW_WORD = 2

/** `slot` của nhịp đọc CẢ TỪ và nhịp nhìn cả từ. Xem [wordBeatOf]. */
private const val WHOLE_WORD_SLOT = -1
private const val PREVIEW_SLOT = -2

/** Bề ngang tạm tính của dấu `+` / `=` (chữ 30sp đậm + hai bên 8dp) để biết khi nào phải thu nhỏ. */
private const val OPERATOR_WIDTH_DP = 38

private const val LETTER_GAP_DP = 4
private const val SPACE_WEIGHT = 0.5f

/** Cỡ mẫu để đo bề ngang chữ; cỡ thật suy từ nó theo chỗ còn lại. */
private const val REF_FONT_SP = 100f

/** Trần cỡ chữ, bằng bản cũ (ô 34dp × 1.34) — từ ngắn như `sh` không phình to quá. */
private const val MAX_FONT_DP = 46f
private const val HOT_SCALE = 1.34f
private const val HIDDEN_SCALE = 0.8f
private const val INK_STRENGTH = 0.85f
private const val SHADOW_ALPHA = 0.30f
private const val SHADOW_DX = 5f
private const val SHADOW_DY = 8f
private const val SHADOW_BLUR = 10f

private const val START_DELAY_MS = 350L

/** Nhịp câm cho bé nhìn cả từ trước khi tách (chốt câu 7 — mặc định 1s). */
private const val WORD_PREVIEW_MS = 1_000L
private const val PIECE_MS = 680L
private const val WHOLE_MS = 900L
private const val GAP_MS = 500L
private const val REPEAT_GAP_MS = 500L
private const val PAGE_TURN_DELAY_MS = 1_400L
private const val SPELL_REPEATS = 2
private const val CHAIN_TIMEOUT_PAD_MS = 2_000L
private const val LETTER_FADE_MS = 160
private const val INK_MS = 260
