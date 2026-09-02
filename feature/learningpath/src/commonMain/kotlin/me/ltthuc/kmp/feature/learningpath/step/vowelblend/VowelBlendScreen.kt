package me.ltthuc.kmp.feature.learningpath.step.vowelblend

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.BlendMeta
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.slide_next_cd
import me.ltthuc.kmp.core.resource.slide_previous_cd
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.step.common.FillingWordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.StepChevronButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.level
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STEP_INDEX = 0

// When true, the rime-building row (a + n = an) shows on every word. When false (default), it only
// shows on the FIRST word of the family (to guide); words 2+ (man/pan/can) skip straight to onset+rime.
// The first word ALWAYS shows the rime row regardless of this flag.
private const val SHOW_RIME_ROW_ALL_WORDS = false

// Single-letter lessons letter-split a word into cards (c + a + t = cat). Words longer than this or
// containing a space (e.g. "elevator", "ice cream") would overflow the row → equation is skipped,
// leaving just the target-letter card + word card.
private const val MAX_BLEND_LETTERS = 5

/** Cấp đầu tiên dùng bố cục "từ nguyên khối" thay cho các thẻ ghép của cấp 2. */
private const val FIRST_PATTERN_LEVEL = 3

// Per-phoneme colours (short-vowel = magenta, consonant = blue), matching the reference mockup.
internal val VowelColor = Color(0xFFE6007E)
internal val ConsonantColor = Color(0xFF1E88E5)

/** How a card's text is coloured — depends on the card's role, not just its letters. */
private enum class CardStyle { Letter, Rime, Word }

@Composable
internal fun VowelBlendScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: VowelBlendViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        val currentLesson = uiState.lessons[safeIndex]
        // Null until loaded, and stays null for lessons whose chain audio has not been generated
        // — those keep the delay-driven fallback below.
        val blendMeta by produceState<BlendMeta?>(initialValue = null, key1 = currentLesson.id) {
            value = viewModel.loadBlendMeta(currentLesson)
        }
        val audioState by viewModel.audioState.collectAsStateWithLifecycle()
        // Cấp 3 trở đi dạy nguyên âm dài nên bố cục khác hẳn — từ giữ nguyên khối, không
        // tách thẻ. Rẽ nhánh ngay ở đây thay vì nhồi thêm chế độ vào [VowelBlendContent],
        // để cấp 2 vốn đã ship không bị đụng tới.
        if ((currentLesson.level() ?: 0) >= FIRST_PATTERN_LEVEL) {
            PatternBlendContent(
                lesson = currentLesson,
                blendMeta = blendMeta,
                audioState = audioState,
                onPlayChain = { page, word -> viewModel.playChain(currentLesson, page, word) },
                onClose = onClose,
                onNext = onNext,
                onStepJump = onStepJump,
                stepSegments = stepSegments,
            )
            return@AsyncLoadContents
        }
        VowelBlendContent(
            lesson = currentLesson,
            blendMeta = blendMeta,
            audioState = audioState,
            onPlayChain = { page, word -> viewModel.playChain(currentLesson, page, word) },
            onPlayLetter = { letter -> viewModel.playLetter(letter, LETTER_MAX_MS) },
            onPlayRime = { rime -> viewModel.playRime(rime, LETTER_MAX_MS) },
            onPlayWord = { word -> viewModel.playWord(currentLesson, word, WORD_MAX_MS) },
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

/** One blend page: onset + rime = word (rime mode), or the whole word (single-letter mode). */
private data class BlendPage(
    val onset: String,
    val rime: String,
    val rimeLetters: List<Char>,
    val word: LessonWord,
)

private fun buildPages(lesson: PhonicsLesson): List<BlendPage> {
    // displayLetter can hold TWO rimes separated by a space ("ad ag", "en ed", "ib id",
    // "it ix", "ud up", "ub um") — six L2 lessons teach a pair. Split into candidates and
    // pick the one each word actually ends with, so "dad" blends as d + ad and "bag" as
    // b + ag. Using the raw string would make endsWith("ad ag") never match, render
    // "d + ad ag = dad", and ask for a rime clip whose filename contains a space.
    val candidates = lesson.displayLetter.lowercase().trim().split(' ').filter { it.isNotEmpty() }
    val fallbackRime = candidates.firstOrNull().orEmpty()
    return lesson.words.map { word ->
        val text = word.text.lowercase()
        val matched = candidates.firstOrNull { it.length >= 2 && text.endsWith(it) && text.length > it.length }
        val rime = matched ?: fallbackRime
        val onset = if (matched != null) text.removeSuffix(matched) else text.take(1)
        BlendPage(onset = onset, rime = rime, rimeLetters = rime.toList(), word = word)
    }
}

/**
 * A card (letter / rime / word) with its colour role, its own voice, and a stable id.
 * [play] suspends until its clip finishes so the sequence can pace off real audio length.
 */
private data class CardModel(
    val id: String,
    val text: String,
    val style: CardStyle,
    val gradient: Boolean = false,
    val play: suspend () -> Unit,
)

/**
 * One equation line: `operands` (+…) = `result`.
 *
 * [result] is null on single-vowel rows (`c a t`), which show only the letters — the word
 * itself lives on the picture card below. [fillCard] names the meta segment that drives the
 * arrow, since without a result card there is no card id to derive it from.
 */
private data class RowSpec(
    val operands: ImmutableList<CardModel>,
    val result: CardModel?,
    val fillCard: String,
)

@Composable
private fun VowelBlendContent(
    lesson: PhonicsLesson,
    blendMeta: BlendMeta?,
    audioState: AudioState,
    onPlayChain: (Int, String) -> Unit,
    onPlayLetter: suspend (Char) -> Unit,
    onPlayRime: suspend (String) -> Unit,
    onPlayWord: suspend (String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val pages = remember(lesson.id) { buildPages(lesson) }
    // displayLetter with 1 char = single-letter lesson (a/e/i/o/u/y/c/g/s) → letter-split each word.
    // 2+ chars = rime lesson (an/am/…) → onset + rime blend.
    val isSingle = remember(lesson.id) { lesson.displayLetter.trim().length == 1 }
    val targetLetter = remember(lesson.id) { lesson.displayLetter.trim().lowercase() }

    var pageIndex by remember(lesson.id) { mutableStateOf(0) }
    var allDone by remember(lesson.id) { mutableStateOf(false) }
    // Bật khi bé tự bấm mũi tên để xem lại. Trang vẫn đọc lại như thường, nhưng KHÔNG tự
    // lật tiếp — nếu vẫn tự lật thì bấm "về trang trước" xong lại bị kéo tới trang cuối,
    // mũi tên hoá ra vô dụng.
    var browsing by remember(lesson.id) { mutableStateOf(false) }
    var replayable by remember(lesson.id) { mutableStateOf(false) }
    var activeId by remember(lesson.id) { mutableStateOf<String?>(null) }
    val row1Fill = remember(lesson.id, pageIndex) { Animatable(0f) }
    val row2Fill = remember(lesson.id, pageIndex) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    if (pages.isEmpty()) {
        VowelBlendScaffold(onClose, onStepJump, stepSegments, nextEnabled = true, onNext = onNext) {}
        return
    }

    val safePage = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safePage]
    // The rime-building row appears on the first page of EACH rime, not just the lesson's
    // first page. Six L2 lessons teach a pair ("ud up", "ad ag", …) and switch rime midway —
    // the second rime has to be introduced too, or the child meets it with no explanation.
    val showRimeRow = SHOW_RIME_ROW_ALL_WORDS || safePage == 0 ||
        pages[safePage].rime != pages[safePage - 1].rime

    // Target-letter card (single-letter lessons only), shown above the equation.
    val targetCard = if (isSingle && targetLetter.isNotEmpty()) {
        CardModel("t", targetLetter, CardStyle.Letter) { onPlayLetter(targetLetter.first()) }
    } else {
        null
    }

    // Equation rows for the current page.
    val rows: List<RowSpec> = if (isSingle) {
        val text = page.word.text
        val blendable = !text.contains(' ') && text.count { it.isLetter() } <= MAX_BLEND_LETTERS
        if (blendable) {
            listOf(
                RowSpec(
                    operands = text.filter { it.isLetter() }.mapIndexed { i, c ->
                        CardModel(
                            "s$i",
                            c.toString(),
                            CardStyle.Letter,
                            gradient = c.toString().equals(targetLetter, ignoreCase = true),
                        ) { onPlayLetter(c) }
                    }.toImmutableList(),
                    result = null,
                    fillCard = "sr",
                ),
            )
        } else {
            emptyList()
        }
    } else {
        buildList {
            if (showRimeRow) {
                add(
                    RowSpec(
                        operands = page.rimeLetters.mapIndexed { i, c ->
                            CardModel("1o$i", c.toString(), CardStyle.Letter) { onPlayLetter(c) }
                        }.toImmutableList(),
                        result = CardModel("1r", page.rime, CardStyle.Rime) { onPlayRime(page.rime) },
                        fillCard = "1r",
                    ),
                )
            }
            add(
                RowSpec(
                    operands = persistentListOf(
                        CardModel("2o0", page.onset, CardStyle.Letter) {
                            page.onset.firstOrNull()?.let { onPlayLetter(it) }
                        },
                        CardModel("2o1", page.rime, CardStyle.Rime) { onPlayRime(page.rime) },
                    ),
                    result = CardModel("2r", page.word.text, CardStyle.Word) { onPlayWord(page.word.text) },
                    fillCard = "2r",
                ),
            )
        }
    }
    val fills = listOf(row1Fill, row2Fill)

    // ---- Chain mode -------------------------------------------------------------------------
    // When this page has generated chain audio, one continuous file speaks the whole equation and
    // the animation follows the player's real position. That removes the two failures the
    // stitched-clip path had: the animation racing ahead of a still-loading clip, and the final
    // word being cut off when the page advanced mid-playback.
    val chain = blendMeta?.chains?.getOrNull(safePage)
    val chainPositionMs = (audioState as? AudioState.Playing)?.positionMs ?: -1L
    // Highlight holds until the NEXT segment starts rather than ending at end_ms — clipped
    // plosives measure as little as 154ms ("puh", "kuh"), which would otherwise blink.
    val chainActiveCard = if (chain != null && chainPositionMs >= 0L) {
        chain.segments.lastOrNull { chainPositionMs >= it.startMs }?.card
    } else {
        null
    }
    val currentAudioState by rememberUpdatedState(audioState)
    // Đang tải hoặc đang phát thì khoá mọi thao tác chạm: bấm chồng lên nhau sẽ cắt ngang
    // tiếng đang đọc dở (AudioRepository chỉ giữ một luồng phát).
    val audioBusy = audioState is AudioState.Loading || audioState is AudioState.Playing
    // Bé tự chạm thẻ hình để nghe lại cả trang. Cần cờ riêng vì highlight chỉ được suy từ
    // vị trí phát khi thứ đang phát ĐÚNG LÀ file chuỗi — chạm một thẻ chữ cũng làm audio
    // chạy, nhưng vị trí lúc đó thuộc file khác nên suy ra thẻ sai.
    var manualChain by remember(lesson.id, safePage) { mutableStateOf(false) }
    LaunchedEffect(audioBusy) { if (!audioBusy) manualChain = false }
    // True from the moment the page's chain has been heard once. Progress bars and ink stay at
    // their finished state while this is set — resetting the instant audio stopped read as the
    // screen "hiding" the child's result. Keyed on the page so the next slide starts clean.
    var chainCompleted by remember(lesson.id, safePage) { mutableStateOf(false) }

    LaunchedEffect(lesson.id, safePage, chain) {
        if (chain == null) return@LaunchedEffect
        replayable = false
        activeId = null
        delay(START_DELAY_MS)

        repeat(REPEAT_COUNT) { pass ->
            if (pass > 0) delay(REPEAT_GAP_MS)
            onPlayChain(safePage, chain.word)
            // Guard with the clip's own length: a missing or broken asset costs one beat instead
            // of parking the page forever with the Next button disabled.
            withTimeoutOrNull(chain.durationMs + CHAIN_TIMEOUT_PAD_MS) {
                snapshotFlow { currentAudioState }.first { it is AudioState.Playing }
                snapshotFlow { currentAudioState }
                    .first { it is AudioState.Idle || it is AudioState.Error }
            }
            chainCompleted = true
        }

        // Hold the finished picture before turning the page — the child needs a beat to look at
        // the completed word after its second repeat.
        delay(PAGE_TURN_DELAY_MS)

        if (safePage >= pages.lastIndex || browsing) {
            allDone = true
            replayable = true
        } else {
            pageIndex = safePage + 1
        }
    }

    // Auto-play state machine: zoom + voice the target letter, then each equation row (operands →
    // fill bar → result), then pop the word. Each card's play() suspends until its clip ends, so
    // the animation follows real audio length; a per-call timeout inside the ViewModel keeps a
    // missing asset from stalling the sequence. The whole chain runs REPEAT_COUNT times per page —
    // one pass is not enough for a 4-year-old to catch the blend.
    LaunchedEffect(lesson.id, safePage, chain) {
        if (chain != null) return@LaunchedEffect
        replayable = false
        activeId = null
        row1Fill.snapTo(0f)
        row2Fill.snapTo(0f)
        delay(START_DELAY_MS)

        repeat(REPEAT_COUNT) { pass ->
            if (pass > 0) {
                // Rewind the arrows so the repeat looks like a fresh run, not a frozen one.
                row1Fill.snapTo(0f)
                row2Fill.snapTo(0f)
                delay(REPEAT_GAP_MS)
            }

            rows.forEachIndexed { rowIndex, row ->
                // Rime lessons spell every operand (a → n). Single-letter lessons don't spell the
                // whole word — they only zoom + voice the target letter once, where it sits inside
                // the word (the big letter above is a static label). Zoom + audio fire together.
                if (isSingle) {
                    row.operands.firstOrNull { it.text.equals(targetLetter, ignoreCase = true) }?.let { op ->
                        activeId = op.id
                        op.play()
                        delay(GAP_MS)
                    }
                } else {
                    row.operands.forEach { op ->
                        activeId = op.id
                        op.play()
                        delay(GAP_MS)
                    }
                }
                // The result pops WHILE the arrow sweeps. play() suspends now, so run it alongside
                // the fill instead of before it, and move on when the slower of the two is done.
                // Single-vowel rows have no result card — the whole word plays under `fillCard`.
                activeId = row.fillCard
                coroutineScope {
                    launch { row.result?.play() ?: onPlayWord(page.word.text) }
                    fills.getOrNull(rowIndex)?.animateTo(1f, tween(FILL_MS, easing = FastOutSlowInEasing))
                        ?: delay(FILL_MS.toLong())
                }
                delay(RESULT_HOLD_MS)
            }
            activeId = "w"
            if (rows.isEmpty()) onPlayWord(page.word.text)
            delay(WORD_MS)
            activeId = null
        }

        // Let the closing word repeat land before the page turns. Without this the slide
        // changes the instant the audio stops, which reads as cutting the child off.
        delay(PAGE_TURN_DELAY_MS)

        // Only after the LAST pass — unlocking early would light up Next mid-sequence and let
        // taps race the auto-play.
        if (safePage >= pages.lastIndex || browsing) {
            allDone = true
            replayable = true
        } else {
            pageIndex = safePage + 1
        }
    }

    fun replay(id: String, action: suspend () -> Unit) {
        // allDone: học xong rồi thì thẻ luôn chạm được. Không có vế này thì sau khi bấm mũi
        // tên, trang mới đang tự đọc lại (replayable = false) sẽ không chạm được — đúng lúc
        // bé muốn nghe lại từ đó nhất.
        if (audioBusy) return
        if (!replayable && !allDone) return
        scope.launch {
            activeId = id
            action()
            delay(REPLAY_POP_MS)
            if (replayable) activeId = null
        }
    }

    // Auto-play highlight comes from the audio in chain mode and from the delay loop otherwise;
    // once the page is replayable, taps take over in both modes.
    val displayActiveId = when {
        chain != null && (!replayable || manualChain) -> chainActiveCard
        else -> activeId
    }

    /** Arrow sweep as a fraction of its own result segment, so it tracks the voice exactly. */
    fun chainFill(resultCardId: String): Float {
        val segs = chain?.segments ?: return 0f
        // After the equation finishes the word is spoken once more (segment `w`). Reuse the
        // word row's own arrow for it — sweeping again from 0 over exactly the repeat's
        // length — rather than adding a second bar the child has to learn to read.
        val repeat = segs.lastOrNull { it.card == WORD_REPEAT_CARD }
        if (resultCardId in WORD_RESULT_CARDS && repeat != null && chainPositionMs >= repeat.startMs) {
            val span = (repeat.endMs - repeat.startMs).coerceAtLeast(1)
            return ((chainPositionMs - repeat.startMs).toFloat() / span).coerceIn(0f, 1f)
        }
        val seg = segs.lastOrNull { it.card == resultCardId } ?: return 0f
        if (chainPositionMs < seg.startMs) return 0f
        val span = (seg.endMs - seg.startMs).coerceAtLeast(1)
        return ((chainPositionMs - seg.startMs).toFloat() / span).coerceIn(0f, 1f)
    }

    VowelBlendScaffold(onClose, onStepJump, stepSegments, nextEnabled = allDone, onNext = onNext) {
        Spacer(Modifier.height(8.dp))
        if (targetCard != null) {
            TargetLetter(
                letter = targetCard.text,
                isActive = displayActiveId == targetCard.id,
                replayable = replayable,
                onClick = { replay(targetCard.id, targetCard.play) },
            )
            Spacer(Modifier.height(16.dp))
        }
        if (rows.isNotEmpty()) {
            EquationPanel {
                rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) Spacer(Modifier.height(18.dp))
                    EquationRow(
                        operands = row.operands,
                        result = row.result,
                        fill = when {
                            chain == null -> fills.getOrNull(rowIndex)?.value ?: 0f
                            chainPositionMs >= 0L -> chainFill(row.fillCard)
                            // Idle after at least one full pass: hold the bar full instead of
                            // snapping back to empty.
                            chainCompleted -> 1f
                            else -> 0f
                        },
                        activeId = displayActiveId,
                        replayable = replayable,
                        showPlus = !isSingle,
                        // Single-vowel rows have no result card, so the whole letter group
                        // inks in together while the word is spoken — and stays inked once done.
                        groupActive = row.result == null &&
                            (displayActiveId == row.fillCard || chainCompleted),
                        holdInk = chainCompleted,
                        onCardTap = { m -> replay(m.id, m.play) },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }
        val wordActive = displayActiveId == WORD_REPEAT_CARD ||
            (chain != null && displayActiveId in WORD_RESULT_CARDS)
        // Mũi tên lật trang, đặt trên hai mép thẻ hình — cùng kiểu StoryScreen dùng.
        // Chỉ hiện sau khi cả lesson chạy xong (lúc nút Next sáng): trong lúc đang dạy mà
        // cho lật trang thì bé bấm lung tung, chuỗi phát đang chạy sẽ bị cắt ngang.
        // contentAlignment = Center: thẻ hình rộng 52% màn, không căn giữa thì Box đẩy nó
        // về mép trái (mặc định TopStart) — trước đây Column cha căn giữa hộ.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            WordCard(
                word = page.word,
                replayable = (replayable || allDone) && !audioBusy,
                // Chạm = nghe lại CẢ TRANG ("f… an… fan… fan"), không phải mỗi từ: thẻ hình
                // là chỗ bé nhìn vào, và cái cần ôn là phép ghép vần chứ không phải từ rời.
                onTap = {
                    activeId = null
                    manualChain = true
                    onPlayChain(safePage, page.word.text)
                },
            )
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
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun VowelBlendScaffold(
    onClose: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    nextEnabled: Boolean,
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                stepSegments = stepSegments,
            )
        },
        bottomBar = {
            StepContinueButton(
                label = stringResource(Res.string.common_next),
                onClick = onNext,
                enabled = nextEnabled,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** Rounded card panel wrapping the equation row(s), matching the app's puffy card chrome. */
@Composable
internal fun EquationPanel(content: @Composable ColumnScope.() -> Unit) {
    PuffySurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.16f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.7f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.05f,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun EquationRow(
    operands: ImmutableList<CardModel>,
    result: CardModel?,
    fill: Float,
    activeId: String?,
    replayable: Boolean,
    showPlus: Boolean,
    groupActive: Boolean,
    holdInk: Boolean,
    onCardTap: (CardModel) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            operands.forEachIndexed { index, model ->
                // Rime lessons blend (a + n); single-letter lessons just spell (c a t) with no "+".
                if (index > 0) {
                    if (showPlus) OperatorGlyph("+") else Spacer(Modifier.width(8.dp))
                }
                // Only the target letter tracks the arrow gradient; the rest keep their fixed colour.
                LetterCard(
                    model,
                    activeId == model.id,
                    replayable,
                    gradientFill = if (model.gradient) fill else null,
                    // Single-vowel rows have no result card: the letters themselves carry the
                    // word, so they ink in together instead of any card bouncing.
                    inkProgress = if (groupActive) fill else null,
                ) { onCardTap(model) }
            }
            // Single-vowel rows drop "= word": the word already lives on the picture card below,
            // and showing it twice invites spelling it out letter by letter.
            if (result != null) {
                OperatorGlyph("=")
                LetterCard(
                    result,
                    activeId == result.id,
                    replayable,
                    // Word cards darken over the audio instead of popping once, and keep
                    // their finished ink until the page turns.
                    inkProgress = if (result.style == CardStyle.Word &&
                        (activeId == result.id || holdInk)
                    ) {
                        fill
                    } else {
                        null
                    },
                ) { onCardTap(result) }
            }
        }
        Spacer(Modifier.height(10.dp))
        FillBar(fraction = fill, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
    }
}

/** The lesson's focus letter shown big above the equation (single-letter lessons). No card frame. */
@Composable
private fun TargetLetter(
    letter: String,
    isActive: Boolean,
    replayable: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "target-zoom",
    )
    Text(
        text = coloredWord(letter, CardStyle.Letter),
        fontFamily = LocalPhonicsFontFamily.current,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .scale(scale)
            .then(if (replayable) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
private fun OperatorGlyph(symbol: String) {
    Text(
        text = symbol,
        fontFamily = LocalPhonicsFontFamily.current,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun LetterCard(
    model: CardModel,
    isActive: Boolean,
    replayable: Boolean,
    gradientFill: Float? = null,
    inkProgress: Float? = null,
    onClick: () -> Unit,
) {
    // Word cards stay put and darken instead of bouncing: [inkProgress] fades the text to black
    // over the word audio's own length, so the ink lands exactly when the voice finishes. A bounce
    // is a single pop that says nothing about how far along the word is.
    val scale by animateFloatAsState(
        targetValue = if (isActive && inkProgress == null) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card-zoom",
    )
    val cardWidth = (model.text.length * CARD_CHAR_WIDTH_DP + CARD_PADDING_DP).dp
    PuffySurface(
        modifier = Modifier
            .scale(scale)
            .width(cardWidth)
            .height(CARD_HEIGHT_DP.dp)
            .then(if (replayable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        shadowElevation = if (isActive) 16.dp else 6.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (isActive) 0.45f else 0.16f,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = 0.85f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = 0.06f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    // Ink in to black across the word's audio.
                    inkProgress != null ->
                        inkedWord(model.text, model.style, inkProgress, MaterialTheme.colorScheme.onSurface)
                    // Target letter colour tracks the arrow: magenta at 0 → blue at 1.
                    gradientFill != null ->
                        solidWord(model.text, lerp(VowelColor, ConsonantColor, gradientFill.coerceIn(0f, 1f)))
                    else -> coloredWord(model.text, model.style)
                },
                fontFamily = LocalPhonicsFontFamily.current,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** Bordered white capsule with a magenta→blue gradient fill ending in an arrowhead. */
@Composable
private fun FillBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(FILL_BAR_HEIGHT.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(VowelColor, ConsonantColor))),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (f > 0.06f) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Thẻ hình của từ, luôn hiện ở dưới cùng. CHỈ có hình, không kèm chữ bên dưới — chữ đã
 * nằm ở hàng ghép phía trên rồi, in thêm lần nữa là bắt bé đọc cùng một từ ở hai chỗ mà
 * chẳng thêm thông tin gì. Bấm vào thẻ để nghe lại cả trang.
 *
 * Bề ngang thẻ giữ nguyên 52% màn; chỉ có ẢNH bên trong là phóng cho kín lòng thẻ thay vì
 * đứng giữa một hình vuông 76dp.
 */
@Composable
internal fun WordCard(
    word: LessonWord,
    replayable: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.52f)
            .then(if (replayable) Modifier.clickable(onClick = onTap) else Modifier),
    ) {
        StoryStyleCard(
            aspectRatio = null,
            whiteInner = true,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FillingWordDisplayView(
                    word = word,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        }
    }
}

/**
 * [text] in its normal phoneme colours, lerped toward black by [progress]. Used while the word
 * is being spoken so the ink darkens in step with the voice and settles exactly as it ends.
 */
private fun inkedWord(text: String, style: CardStyle, progress: Float, base: Color): AnnotatedString {
    val t = progress.coerceIn(0f, 1f)
    return buildAnnotatedString {
        when (style) {
            CardStyle.Word -> withStyle(SpanStyle(color = lerp(base, Color.Black, t))) {
                append(text)
            }
            CardStyle.Rime -> withStyle(SpanStyle(color = lerp(VowelColor, Color.Black, t))) {
                append(text)
            }
            CardStyle.Letter -> text.forEach { ch ->
                val c = if (ch.lowercaseChar() in "aeiou") VowelColor else ConsonantColor
                withStyle(SpanStyle(color = lerp(c, Color.Black, t))) { append(ch.toString()) }
            }
        }
    }
}

/** Whole [text] rendered in a single [color]. */
private fun solidWord(text: String, color: Color): AnnotatedString =
    buildAnnotatedString { withStyle(SpanStyle(color = color)) { append(text) } }

@Composable
private fun coloredWord(text: String, style: CardStyle): AnnotatedString {
    val wordColor = MaterialTheme.colorScheme.onSurface
    return remember(text, style, wordColor) {
        buildAnnotatedString {
            when (style) {
                CardStyle.Word -> withStyle(SpanStyle(color = wordColor)) { append(text) }
                CardStyle.Rime -> withStyle(SpanStyle(color = VowelColor)) { append(text) }
                CardStyle.Letter -> text.forEach { ch ->
                    val color = if (ch.lowercaseChar() in "aeiou") VowelColor else ConsonantColor
                    withStyle(SpanStyle(color = color)) { append(ch.toString()) }
                }
            }
        }
    }
}

private const val START_DELAY_MS = 350L

/**
 * Times each pass runs the whole blend chain (a → n → an → f → an → fan) before the page turns.
 * One pass is too fast for a 4-year-old to catch the blend.
 */
private const val REPEAT_COUNT = 2
private const val REPEAT_GAP_MS = 600L

/** Silence between two sounds. Clip length is no longer padded out by a fixed delay. */
private const val GAP_MS = 180L

// Caps on how long to wait for a clip, not how long to play it. Real clips: phonemes 260-710ms,
// rimes 660-1160ms, vocab words up to ~2s. Generous enough to never truncate, tight enough that a
// missing asset costs one beat instead of hanging the sequence.
private const val LETTER_MAX_MS = 2_500L
private const val WORD_MAX_MS = 4_000L

/** Slack over a chain clip's own length before giving up waiting for it — covers load + decode. */
private const val CHAIN_TIMEOUT_PAD_MS = 3_000L

/**
 * Card ids whose arrow doubles as the closing word repeat's sweep — `2r` on rime lessons
 * (`f + an = fan`), `sr` on single-vowel lessons (`c a t = cat`).
 */
private val WORD_RESULT_CARDS = setOf("2r", "sr")
private const val WORD_REPEAT_CARD = "w"

/** Beat of silence after the closing word repeat before the page turns. */
private const val PAGE_TURN_DELAY_MS = 2_000L

private const val FILL_MS = 550
private const val RESULT_HOLD_MS = 450L
private const val WORD_MS = 900L
private const val REPLAY_POP_MS = 320L
private const val CARD_CHAR_WIDTH_DP = 24
private const val CARD_PADDING_DP = 28
private const val CARD_HEIGHT_DP = 64
private const val FILL_BAR_HEIGHT = 22
