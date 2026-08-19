package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuide
import me.ltthuc.kmp.feature.learningpath.step.tracing.drawGhostLetter
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

/** Word picture size inside its card. */
private const val IMAGE_FONT_SP = 96

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
    onPlayLetter: (Char) -> Unit,
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

    // Bounds (in root coords) of the trace canvas (source) and the current header cell (target),
    // used to animate the finished letter up into the word above.
    var rootOrigin by remember(lesson.id) { mutableStateOf(Offset.Zero) }
    var canvasBounds by remember(lesson.id) { mutableStateOf<Rect?>(null) }
    var headerCellBounds by remember(lesson.id) { mutableStateOf<Rect?>(null) }

    val safeWord = wordIndex.coerceIn(0, words.lastIndex)
    val word = words[safeWord]
    val safeLetter = letterIndex.coerceIn(0, word.letters.lastIndex)
    val currentChar = word.letters[safeLetter]
    val guide = remember(currentChar) { DuolingoGlyphs.get(currentChar) }

    // Every letter is voiced as it becomes the one to trace — the first one included (it used to be
    // skipped, so it was silent). The whole word is not spoken here at all: it is saved for the end,
    // over the picture, once the child has traced all of it.
    //
    // [guideDone] holds the very first letter back until the screen's spoken guide has finished;
    // after that it stays true, so no later letter ever waits. Re-running on the flip is what makes
    // whichever letter is current then get its sound.
    LaunchedEffect(lesson.id, wordIndex, letterIndex, guideDone) {
        if (guideDone && letterIndex < word.letters.length) onPlayLetter(currentChar)
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
                phase = if (canvasBounds != null && headerCellBounds != null) {
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
                onCurrentCellBounds = { headerCellBounds = it },
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
        val dst = headerCellBounds
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WordDisplayView(word = word, fontSize = IMAGE_FONT_SP.sp)
            }
        }
    }
}

/** The just-finished glyph, drawn mid-flight from the trace canvas ([source]) into its header cell ([target]). */
@Composable
private fun FlyingGlyph(guide: LetterGuide, fraction: Float, source: Rect, target: Rect) {
    // The header cell paints its glyph inside a 4.dp pad, so shrink the target to that inner area
    // for an exact landing.
    val inset = with(LocalDensity.current) { HEADER_CELL_PAD_DP.dp.toPx() }
    val landing = Rect(
        left = target.left + inset,
        top = target.top + inset,
        right = target.right - inset,
        bottom = target.bottom - inset,
    )
    val rect = lerp(source, landing, fraction)
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
    onCurrentCellBounds: (Rect) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        letters.forEachIndexed { index, ch ->
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
                ch = ch,
                highlighted = isCurrent,
                glyphColor = glyphColor,
                modifier = if (isCurrent) {
                    Modifier.onGloballyPositioned { onCurrentCellBounds(it.boundsInRoot()) }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun LetterCell(
    ch: Char,
    highlighted: Boolean,
    glyphColor: Color,
    modifier: Modifier = Modifier,
) {
    val guide = remember(ch) { DuolingoGlyphs.get(ch) }
    val cellBg = if (highlighted) TraceHighlightGreen else Color.Transparent
    Box(
        modifier = modifier
            // Taller than wide on purpose — see [HEADER_CELL_H_DP]. The clip (which rounds the
            // green highlight) is also what cuts a descender off, so the cell has to be tall
            // enough to hold one.
            .size(width = HEADER_CELL_W_DP.dp, height = HEADER_CELL_H_DP.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellBg),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(HEADER_CELL_PAD_DP.dp)) {
            drawGuideLines(handwritingLines(size))
            drawGhostLetter(
                guide = guide,
                canvasSize = size,
                color = glyphColor,
                strokeWidthPx = minOf(size.width, size.height) * HEADER_GLYPH_FRACTION,
            )
        }
    }
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
