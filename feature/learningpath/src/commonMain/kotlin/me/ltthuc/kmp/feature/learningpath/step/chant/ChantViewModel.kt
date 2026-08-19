package me.ltthuc.kmp.feature.learningpath.step.chant

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
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.ChantMeta
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import me.ltthuc.kmp.core.repository.ChantMetaRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.audioFolderName
import me.ltthuc.kmp.feature.learningpath.step.common.chantRef

internal class ChantViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
    private val chantMetaRepository: ChantMetaRepository,
) : ViewModel() {

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    /**
     * ChantMeta JSON keys lessons by [audioFolderName] (e.g. "L1U01_A_apple"), which
     * matches the on-disk audio asset folder. Lesson.id is shorter ("L1U1_A") and only
     * gives us the level number. Repository derives wordTimings from start_ms/speed_ms
     * + the lesson's ordered words (chantOrder respected).
     */
    val screenState: StateFlow<ScreenState<ChantUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(ChantUiState(lessons = lessons.toImmutableList()))
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

    val audioState: StateFlow<AudioState> = audioRepository.state

    suspend fun loadChantMeta(lesson: PhonicsLesson): ChantMeta? {
        val level = parseLevelFromLessonId(lesson.id) ?: return null
        val key = lesson.audioFolderName() ?: return null
        val orderedWords = lesson.chantOrder
            .takeIf { it.size == lesson.words.size }
            ?.map { lesson.words[it].word }
            ?: lesson.words.map { it.word }
        return chantMetaRepository.metaFor(level, key, orderedWords)
    }

    private fun parseLevelFromLessonId(id: String): Int? {
        // Pattern: "L{n}U{n}_{LETTER}" e.g. "L1U1_A" → 1
        if (!id.startsWith("L")) return null
        val uIndex = id.indexOf('U')
        if (uIndex <= 1) return null
        return id.substring(1, uIndex).toIntOrNull()
    }

    fun onChantToggle(lesson: PhonicsLesson) {
        val ref = lesson.chantRef() ?: run {
            Napier.w(tag = TAG) { "No Chant audio ref for lesson ${lesson.id}" }
            return
        }
        when (val current = audioRepository.state.value) {
            is AudioState.Playing -> if (current.ref == ref) audio.stop() else audio.play(ref)
            is AudioState.Paused -> if (current.ref == ref) audio.resume() else audio.play(ref)
            is AudioState.Loading -> if (current.ref != ref) audio.play(ref)
            else -> audio.play(ref)
        }
    }

    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "ChantViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class ChantUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
