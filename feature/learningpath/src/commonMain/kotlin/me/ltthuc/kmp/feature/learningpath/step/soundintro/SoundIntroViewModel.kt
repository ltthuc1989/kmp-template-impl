package me.ltthuc.kmp.feature.learningpath.step.soundintro

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
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.step0Refs

internal class SoundIntroViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<SoundIntroUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(SoundIntroUiState(lessons = lessons.toImmutableList()))
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

    /**
     * Bật/tắt phần nghe của lesson.
     *
     * Level 1 là một file dạy dài; Level 2+ là chuỗi 4-6 file (guide vần + từng từ)
     * phát nối tiếp — xem [step0Refs]. Vì cả hai đều là "một danh sách", chỗ này chỉ
     * cần so ref đang phát có thuộc danh sách của lesson hay không, thay vì so bằng
     * một ref duy nhất như trước.
     */
    fun onListenToggle(lesson: PhonicsLesson) {
        val refs = lesson.step0Refs()
        if (refs.isEmpty()) {
            Napier.w(tag = TAG) { "No step-0 audio refs for lesson ${lesson.id}" }
            return
        }
        when (val current = audioRepository.state.value) {
            is AudioState.Playing ->
                if (current.ref in refs) audioRepository.stop() else audioRepository.playAll(refs)
            is AudioState.Paused ->
                if (current.ref in refs) audioRepository.resume() else audioRepository.playAll(refs)
            is AudioState.Loading -> if (current.ref !in refs) audioRepository.playAll(refs)
            else -> audioRepository.playAll(refs)
        }
    }

    fun onLeaveScreen() {
        audioRepository.stop()
    }

    private companion object {
        const val TAG = "SoundIntroViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class SoundIntroUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
