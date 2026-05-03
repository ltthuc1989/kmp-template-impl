package me.ltthuc.kmp.feature.learningpath.step.story

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.Story
import me.ltthuc.kmp.core.model.StoryScene
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.StoryRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class StoryViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val storyRepository: StoryRepository,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    private val storyFlow: StateFlow<Story?> = flow {
        emit(loadStoryForUnit())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = null,
    )

    val screenState: StateFlow<ScreenState<StoryUiState>> =
        combine(unitRepository.observeLessons(unitId), storyFlow) { lessons, story ->
            val storyId = story?.id
            if (lessons.isEmpty() || storyId.isNullOrBlank()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                ScreenState.Idle(
                    StoryUiState(
                        lessons = lessons.toImmutableList(),
                        story = story,
                        scenes = story.scenes.sortedBy { it.sceneNumber }.toImmutableList(),
                    ),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    val audioState: StateFlow<AudioState> = audioRepository.state

    private val _activeSceneIndex = MutableStateFlow(0)
    val activeSceneIndex: StateFlow<Int> = _activeSceneIndex.asStateFlow()

    fun onPageChange(sceneIndex: Int) {
        _activeSceneIndex.value = sceneIndex
        val story = storyFlow.value ?: return
        val scene = story.scenes.getOrNull(sceneIndex) ?: return
        audioRepository.play(AudioRef.Story(storyId = story.id, sceneNumber = scene.sceneNumber))
    }

    fun onListenToggle() {
        val story = storyFlow.value ?: return
        val scene = story.scenes.getOrNull(_activeSceneIndex.value) ?: return
        val ref = AudioRef.Story(storyId = story.id, sceneNumber = scene.sceneNumber)
        when (val current = audioRepository.state.value) {
            is AudioState.Playing -> if (current.ref == ref) audioRepository.stop() else audioRepository.play(ref)
            is AudioState.Paused -> if (current.ref == ref) audioRepository.resume() else audioRepository.play(ref)
            is AudioState.Loading -> if (current.ref != ref) audioRepository.play(ref)
            else -> audioRepository.play(ref)
        }
    }

    fun onLeaveScreen() {
        audioRepository.stop()
    }

    private suspend fun loadStoryForUnit(): Story? {
        val match = UNIT_ID_REGEX.matchEntire(unitId) ?: return null
        val (level, unit) = match.destructured
        return runCatching {
            storyRepository.storyForUnit(level = level.toInt(), unitNumber = unit.toInt())
        }.onFailure {
            Napier.e(tag = TAG, throwable = it) { "Failed to load story for $unitId" }
        }.getOrNull()
    }

    private companion object {
        const val TAG = "StoryViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        val UNIT_ID_REGEX = Regex("""L(\d+)U(\d+)""")
    }
}

@Immutable
internal data class StoryUiState(
    val lessons: ImmutableList<PhonicsLesson>,
    val story: Story,
    val scenes: ImmutableList<StoryScene> = persistentListOf(),
)
