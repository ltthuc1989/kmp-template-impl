package me.ltthuc.kmp.feature.learningpath.step.vowelblend

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.BlendMeta
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.BlendMetaRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.repository.playAndAwait
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.audioFolderName
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef

/**
 * Level 2 Step 1 (short-vowel blending). Loads the unit's lessons and voices the individual
 * pieces of a blend on demand: single letters (bundled phonemes), the rime as one blended
 * unit (bundled rime clip), and the whole word (lesson vocab audio). Playback suspends until the
 * clip ends so the screen's animation can follow real audio length, with a per-call timeout so a
 * missing asset can never stall the sequence.
 */
internal class VowelBlendViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
    private val blendMetaRepository: BlendMetaRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<VowelBlendUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(VowelBlendUiState(lessons = lessons.toImmutableList()))
                }
            }
            .catch { throwable ->
                Napier.e(tag = TAG, throwable = throwable) { "Failed to load lessons for $unitId" }
                emit(ScreenState.Error(message = Res.string.error_network))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ScreenState.Loading(),
            )

    /** Player position, so the screen can drive its animation off real playback. */
    val audioState: StateFlow<AudioState> = audioRepository.state

    /**
     * Timing map for this lesson's per-page chain audio, or null when it has not been generated
     * yet — the screen then keeps its delay-driven fallback rather than going silent.
     */
    suspend fun loadBlendMeta(lesson: PhonicsLesson): BlendMeta? {
        val level = LEVEL_REGEX.find(lesson.id)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val folder = lesson.audioFolderName() ?: return null
        return blendMetaRepository.metaFor(level, folder)
    }

    /** Starts one page's chain audio. Fire-and-forget: the screen follows [audioState]. */
    fun playChain(lesson: PhonicsLesson, pageIndex: Int, word: String) {
        val folder = lesson.audioFolderName() ?: return
        audioRepository.play(AudioRef.Blend(folder, word, pageIndex))
    }

    /**
     * All three play* functions suspend until the clip actually finishes, so the screen can
     * pace its animation off real audio length instead of a hard-coded delay. Clip lengths
     * vary a lot — phonemes run ~260-710ms, vocab words up to ~2s — and [AudioRepository.play]
     * stops whatever is playing, so a fixed delay shorter than the clip cuts it off mid-sound.
     * [timeoutMs] caps the wait so a missing asset can never stall the sequence.
     */
    suspend fun playLetter(letter: Char, timeoutMs: Long) {
        if (!letter.isLetter()) return
        audioRepository.playAndAwait(AudioRef.LetterSound(letter.toString()), timeoutMs)
    }

    /** The rime spoken as one blended unit, e.g. "an". */
    suspend fun playRime(rime: String, timeoutMs: Long) {
        if (rime.isBlank()) return
        audioRepository.playAndAwait(AudioRef.RimeBlend(rime), timeoutMs)
    }

    /** The whole blended word from the lesson's vocab audio, e.g. "fan". */
    suspend fun playWord(lesson: PhonicsLesson, word: String, timeoutMs: Long) {
        val ref = lesson.wordRef(word) ?: run {
            Napier.w(tag = TAG) { "No Word audio ref for ${lesson.id}/$word" }
            return
        }
        audioRepository.playAndAwait(ref, timeoutMs)
    }

    fun onLeaveScreen() {
        audioRepository.stop()
    }

    private companion object {
        const val TAG = "VowelBlendViewModel"
        val LEVEL_REGEX = Regex("""^L(\d+)U""")
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class VowelBlendUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
