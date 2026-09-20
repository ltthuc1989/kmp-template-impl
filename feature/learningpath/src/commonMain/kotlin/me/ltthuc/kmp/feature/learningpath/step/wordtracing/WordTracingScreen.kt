package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.ui.audio.ScreenVoicePrompt
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.ConfettiCanvas
import me.ltthuc.kmp.feature.learningpath.step.common.FillingWordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuide
import me.ltthuc.kmp.feature.learningpath.step.tracing.drawGhostLetter
import me.ltthuc.kmp.feature.learningpath.step.tracing.scaledGuidePaths
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.graphics.lerp as lerpColor

private const val STEP_INDEX = 6

/** How long the burst spreads before the letter starts flying up (they overlap). */
private const val BURST_LEAD_MS = 340L

/** How long the finished letter takes to fly up and shrink into its header cell. */
private const val FLY_MS = 520

/** How long the landed header letter takes to fade from celebration green back to black. */
private const val SETTLE_MS = 380

/** How long the finished word (letters + picture) is held before advancing to the next word. */
private const val WORD_PAUSE_MS = 3000L

/** Per-letter completion sequence: trace done -> [Burst] celebration -> [Fly] into the header. */
private enum class LetterPhase { Idle, Burst, Fly }

@Composable
internal fun WordTracingScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: WordTracingViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    // The letter sound waits for the narrator to finish — two voices at once is noise to a 4-year-old.
    // Flips immediately when voice is off, so a muted parent's child isn't left waiting.
    var guideDone by remember { mutableStateOf(false) }
    ScreenVoicePrompt("vp_step_trace") { guideDone = true }

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
        WordTracingContent(
            lesson = currentLesson,
            guideDone = guideDone,
            onPlayWord = { word -> viewModel.playWord(currentLesson, word) },
            onPlayLetter = viewModel::playLetter,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

/** One word to trace: [source] is the vocab word (audio key + picture); [letters] are the glyphs. */
private data class TraceWord(val source: LessonWord, val letters: String) {
    val audioWord get() = source.word
}

private fun buildWords(lesson: PhonicsLesson): List<TraceWord> =
    lesson.words.mapNotNull { w ->
        val letters = w.text.lowercase().filter(Char::isLetter)
        if (letters.isEmpty()) null else TraceWord(w, letters)
    }

@Composable
private fun WordTracingContent(
    lesson: PhonicsLesson,
    guideDone: Boolean,
    onPlayWord: (String) -> Unit,
    onPlayLetter: (Char, Boolean) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val words = remember(lesson.id) { buildWords(lesson) }

    if (words.isEmpty()) {
        WordTracingScaffold(
            onClose = onClose,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            nextEnabled = true,
            onNext = onNext,
        ) {}
        return
    }

    val sfx = koinInject<SfxController>()

    var wordIndex by remember(lesson.id) { mutableStateOf(0) }
    var letterIndex by remember(lesson.id) { mutableStateOf(0) }
    var allDone by remember(lesson.id) { mutableStateOf(false) }
    // Drives the per-letter celebration: Idle -> Burst -> Fly, then the next letter loads.
    var phase by remember(lesson.id) { mutableStateOf(LetterPhase.Idle) }
    val flyProgress = remember(lesson.id) { Animatable(0f) }
    // The header letter that just landed flashes green then settles to black: index within the
    // current word, plus 0 (green) -> 1 (black) progress.
    var landedIndex by remember(lesson.id) { mutableStateOf(-1) }
    val settle = remember(lesson.id) { Animatable(0f) }
    // After the last letter of a word: focus cleared, word + picture held for a beat before the next.
    var wordPaused by remember(lesson.id) { mutableStateOf(false) }

    // Bounds (in root coords) of the trace canvas (source) and the current header letter's glyph box
    // (target), used to animate the finished letter up into the word above.
    var rootOrigin by remember(lesson.id) { mutableStateOf(Offset.Zero) }
    var canvasBounds by remember(lesson.id) { mutableStateOf<Rect?>(null) }
    var headerGlyphBounds by remember(lesson.id) { mutableStateOf<Rect?>(null) }

    val safeWord = wordIndex.coerceIn(0, words.lastIndex)
    val word = words[safeWord]
    val safeLetter = letterIndex.coerceIn(0, word.letters.lastIndex)
    val currentChar = word.letters[safeLetter]
    val guide = remember(currentChar) { DuolingoGlyphs.get(currentChar) }

    // Every letter is voiced as it becomes the one to trace — the first one included (it used to be
    // skipped, so it was silent). The whole word is not spoken here at all: it is saved for the end,
    // over the picture, once the child has traced all of it.
    //
    // The first letter is voiced as an ONSET ("lơ") and the rest as isolated phonemes — the child is
    // starting a word on that letter, not naming it in the air. See [WordTracingViewModel.playLetter].
    //
    // [guideDone] holds the very first letter back until the screen's spoken guide has finished;
    // after that it stays true, so no later letter ever waits. Re-running on the flip is what makes
    // whichever letter is current then get its sound.
    LaunchedEffect(lesson.id, wordIndex, letterIndex, guideDone) {
        if (guideDone && letterIndex < word.letters.length) {
            onPlayLetter(currentChar, letterIndex == 0)
        }
    }

    // The current letter's celebration is over: it always flashes green in the header, then either
    // the next letter loads or the word closes out (picture + whole-word audio hold).
    val finishLetter = {
        val completed = letterIndex.coerceIn(0, word.letters.lastIndex)
        landedIndex = completed
        if (completed + 1 < word.letters.length) {
            letterIndex = completed + 1
        } else {
            // Word done: clear the focus (no highlighted cell) and hold on the picture.
            letterIndex = word.letters.length
            wordPaused = true
        }
    }

    // A letter was finished: chime + burst, then fly the glyph up into the header, then advance.
    LaunchedEffect(phase) {
        when (phase) {
            LetterPhase.Idle -> Unit
            LetterPhase.Burst -> {
                sfx.playSfx("correct")
                // Start the fly partway through the burst so the letter lifts off while the
                // particles are still spreading.
                delay(BURST_LEAD_MS)
                phase = if (canvasBounds != null && headerGlyphBounds != null) {
                    LetterPhase.Fly
                } else {
                    finishLetter()
                    LetterPhase.Idle
                }
            }
            LetterPhase.Fly -> {
                flyProgress.snapTo(0f)
                flyProgress.animateTo(1f, tween(FLY_MS, easing = FastOutSlowInEasing))
                finishLetter()
                phase = LetterPhase.Idle
            }
        }
    }

    // Fade the just-landed header letter from green back to black.
    LaunchedEffect(landedIndex) {
        if (landedIndex < 0) return@LaunchedEffect
        settle.snapTo(0f)
        settle.animateTo(1f, tween(SETTLE_MS))
        landedIndex = -1
    }

    // End-of-word hold: show the finished word + picture and voice the whole word, so the child
    // hears what they just traced before the next word starts (or the step finishes).
    LaunchedEffect(wordPaused) {
        if (!wordPaused) return@LaunchedEffect
        onPlayWord(word.audioWord)
        delay(WORD_PAUSE_MS)
        val wIdx = wordIndex.coerceIn(0, words.lastIndex)
        if (wIdx + 1 < words.size) {
            wordIndex += 1
            letterIndex = 0
        } else {
            allDone = true
        }
        wordPaused = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.boundsInRoot().topLeft },
    ) {
        WordTracingScaffold(
            onClose = onClose,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            nextEnabled = allDone,
            onNext = onNext,
        ) {
            Spacer(Modifier.height(10.dp))
            WordHeader(
                // Raw letterIndex: during the end-of-word pause it's past the last letter, so no
                // cell is highlighted (the green focus is gone).
                letters = word.letters,
                letterIndex = letterIndex,
                landedIndex = landedIndex,
                landedSettle = settle.value,
                onCurrentGlyphBounds = { headerGlyphBounds = it },
            )
            Spacer(Modifier.height(8.dp))
            // Big middle area: the trace surface while tracing; the word picture once the word is
            // finished (held during the end-of-word pause), then back to tracing for the next word.
            // After the LAST word the picture stays for good — never fall back to the trace surface,
            // which would re-offer the final letter.
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                if (wordPaused || allDone) {
                    WordImageCard(word = word.source, modifier = Modifier.fillMaxSize())
                } else {
                    LetterTraceCanvas(
                        guide = guide,
                        resetKey = "$safeWord:$safeLetter",
                        showGlyph = phase != LetterPhase.Fly,
                        onLetterComplete = { if (phase == LetterPhase.Idle) phase = LetterPhase.Burst },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            // Capture the actual card/glyph area (after padding) as the fly's start rect.
                            .onGloballyPositioned { canvasBounds = it.boundsInRoot() },
                    )
                    // Keep spreading through the fly (kept mounted across Burst -> Fly, not restarted).
                    if (phase == LetterPhase.Burst || phase == LetterPhase.Fly) {
                        LetterCompleteBurst(modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PageDotsRow(currentPage = safeWord, total = words.size)
            Spacer(Modifier.height(8.dp))
        }

        val src = canvasBounds
        val dst = headerGlyphBounds
        if (phase == LetterPhase.Fly && src != null && dst != null) {
            FlyingGlyph(
                guide = guide,
                fraction = flyProgress.value,
                source = src.translate(-rootOrigin.x, -rootOrigin.y),
                target = dst.translate(-rootOrigin.x, -rootOrigin.y),
            )
        }

        if (allDone) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }
    }
}

/** Word picture shown in the trace slot during the end-of-word pause (replaces the trace surface). */
@Composable
private fun WordImageCard(word: LessonWord, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        StoryStyleCard(aspectRatio = 1f, modifier = Modifier.fillMaxWidth(0.55f)) {
            // Thẻ giữ nguyên 55% bề ngang; chỉ ảnh là phóng cho kín lòng thẻ thay vì đứng
            // giữa một hình vuông 96dp.
            FillingWordDisplayView(word = word, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The just-finished glyph, drawn mid-flight from the trace canvas ([source]) into its header letter's
 * glyph box ([target] — the exact box the header draws that letter in, so it lands at the same size).
 */
@Composable
private fun FlyingGlyph(guide: LetterGuide, fraction: Float, source: Rect, target: Rect) {
    val rect = lerp(source, target, fraction)
    // Soft fade over the last stretch so it "merges" rather than snapping.
    val alpha = if (fraction < 0.85f) 1f else ((1f - fraction) / 0.15f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val glyphSize = Size(rect.width, rect.height)
        translate(left = rect.left, top = rect.top) {
            drawGhostLetter(
                guide = guide,
                canvasSize = glyphSize,
                color = TraceDoneGreen.copy(alpha = alpha),
                strokeWidthPx = minOf(glyphSize.width, glyphSize.height) * HEADER_GLYPH_FRACTION,
            )
        }
    }
}

@Composable
private fun WordTracingScaffold(
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

// ---------------- Word header (whole word on 3-line guides) ----------------

private enum class LetterState { Done, Current, Upcoming }

@Composable
private fun WordHeader(
    letters: String,
    letterIndex: Int,
    landedIndex: Int,
    landedSettle: Float,
    onCurrentGlyphBounds: (Rect) -> Unit,
) {
    val guides = remember(letters) { letters.map(DuolingoGlyphs::get) }
    val inkHalfWidth = remember(letters) { guides.maxOf(::inkHalfWidth) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val density = LocalDensity.current
        // A long word ("cellphone" = 9 x 60dp) doesn't fit one line. Room comes out of the gaps first:
        // cells narrow to share the row, letters keep their size. Floored to whole px so the cells
        // add up to no more than the row. Only if the widest letter would then run into its
        // neighbour does the glyph itself shrink.
        val cellWidth = if (constraints.hasBoundedWidth) {
            with(density) { (constraints.maxWidth / letters.length.coerceAtLeast(1)).toDp() }
                .coerceAtMost(HEADER_CELL_W_DP.dp)
        } else {
            HEADER_CELL_W_DP.dp
        }
        val glyphBox = minOf(
            HEADER_GLYPH_BOX_DP.dp,
            (cellWidth - HEADER_MIN_INK_GAP_DP.dp) * (GLYPH_VIEW_BOX / (2f * inkHalfWidth)),
        )
        val glyphBoxPx = with(density) { glyphBox.toPx() }
        val padPx = with(density) { HEADER_CELL_PAD_DP.dp.toPx() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            letters.indices.forEach { index ->
                val letterState = when {
                    index < letterIndex -> LetterState.Done
                    index == letterIndex -> LetterState.Current
                    else -> LetterState.Upcoming
                }
                val isCurrent = letterState == LetterState.Current
                // The just-landed letter flashes green then fades to its normal (dark) color.
                val glyphColor = when {
                    index == landedIndex -> lerpColor(TraceDoneGreen, TraceInkDark, landedSettle)
                    letterState == LetterState.Upcoming -> TraceUpcomingGlyphGray
                    else -> TraceInkDark
                }
                LetterCell(
                    guide = guides[index],
                    cellWidth = cellWidth,
                    glyphBox = glyphBox,
                    highlighted = isCurrent,
                    glyphColor = glyphColor,
                    modifier = if (isCurrent) {
                        Modifier.onGloballyPositioned { coords ->
                            // Report the box the glyph is drawn in, not the cell: the flying
                            // letter must land at exactly this size.
                            val cell = coords.boundsInRoot()
                            onCurrentGlyphBounds(
                                Rect(
                                    left = cell.center.x - glyphBoxPx / 2f,
                                    top = cell.top + padPx,
                                    right = cell.center.x + glyphBoxPx / 2f,
                                    bottom = cell.bottom - padPx,
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun LetterCell(
    guide: LetterGuide,
    cellWidth: Dp,
    glyphBox: Dp,
    highlighted: Boolean,
    glyphColor: Color,
    modifier: Modifier = Modifier,
) {
    val cellBg = if (highlighted) TraceHighlightGreen else Color.Transparent
    Box(
        modifier = modifier
            // Taller than wide on purpose — see [HEADER_CELL_H_DP]. The clip (which rounds the
            // green highlight) is also what cuts a descender off, so the cell has to be tall
            // enough to hold one.
            .size(width = cellWidth, height = HEADER_CELL_H_DP.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellBg),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(HEADER_CELL_PAD_DP.dp)) {
            // The glyph keeps its own [glyphBox]-wide box centered on the cell, even when the cell
            // is narrower: only the empty viewBox margin spills past the edge, never the ink (see
            // [inkHalfWidth]). Guide lines take their Y's from that same box.
            val box = Size(glyphBox.toPx(), size.height)
            drawGuideLines(handwritingLines(box))
            translate(left = (size.width - box.width) / 2f) {
                drawGhostLetter(
                    guide = guide,
                    canvasSize = box,
                    color = glyphColor,
                    strokeWidthPx = minOf(box.width, box.height) * HEADER_GLYPH_FRACTION,
                )
            }
        }
    }
}

/**
 * How far [guide]'s ink reaches either side of the viewBox's center line (x = 50), in viewBox units,
 * stroke thickness included. Every header glyph is centered on its cell, so this — not the glyph's
 * width — is what has to fit in half a cell (`q`'s tail makes it lopsided). Path bounds include
 * Bézier control points, so it can over-estimate slightly, which only errs toward more room.
 */
private fun inkHalfWidth(guide: LetterGuide): Float {
    val center = GLYPH_VIEW_BOX / 2f
    val reach = scaledGuidePaths(guide, Size(GLYPH_VIEW_BOX, GLYPH_VIEW_BOX)).maxOfOrNull { path ->
        val bounds = path.getBounds()
        maxOf(center - bounds.left, bounds.right - center)
    } ?: 0f
    return reach + GLYPH_VIEW_BOX * HEADER_GLYPH_FRACTION / 2f
}

private const val HEADER_CELL_W_DP = 60

/**
 * Cells are taller than wide so descenders survive. The glyphs are authored in a 0..100 viewBox but
 * the tails of `y` and `p` run to y=106 (`g` 104, `j` 101), and GuideLayout scales by
 * `min(width, height) / 100` and centers — so in a square cell everything past the baseline falls
 * outside the canvas and the rounded clip shears it off.
 *
 * With 60x74 and a 4dp pad the inner box is 52x66: scale 0.52, viewBox centered with 7dp above, so
 * the deepest tail lands at 7 + 106*0.52 + half a 5.7dp stroke ≈ 65dp — inside 66dp.
 */
private const val HEADER_CELL_H_DP = 74
private const val HEADER_CELL_PAD_DP = 4
private const val HEADER_GLYPH_FRACTION = 0.11f

/** Full-size glyph box: the viewBox side inside a full-width cell. Narrow cells never enlarge it. */
private const val HEADER_GLYPH_BOX_DP = HEADER_CELL_W_DP - 2 * HEADER_CELL_PAD_DP

/** Least clear space between two neighbouring letters' ink once cells have narrowed. */
private const val HEADER_MIN_INK_GAP_DP = 4

private const val GLYPH_VIEW_BOX = 100f
