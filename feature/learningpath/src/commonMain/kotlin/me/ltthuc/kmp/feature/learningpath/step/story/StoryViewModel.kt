package me.ltthuc.kmp.feature.learningpath.step.story

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.Story
import me.ltthuc.kmp.core.model.StoryScene
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.StoryRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.repository.playAndAwait
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

    private val sceneCompletedFlow = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    /** Emits sceneIndex when its narration audio finishes naturally (not user-stopped). */
    val sceneCompleted: SharedFlow<Int> = sceneCompletedFlow.asSharedFlow()

    private var playJob: Job? = null
    private var userInterrupted = false

    fun onPageChange(sceneIndex: Int) {
        _activeSceneIndex.value = sceneIndex
        userInterrupted = false
        val story = storyFlow.value ?: return
        val scene = story.scenes.getOrNull(sceneIndex) ?: return
        val ref = AudioRef.Story(storyId = story.id, sceneNumber = scene.sceneNumber)
        playJob?.cancel()
        playJob = viewModelScope.launch {
            audioRepository.playAndAwait(ref, SCENE_AUDIO_MAX_MS)
            // Only auto-advance if the kid is still on this scene and did not stop manually.
            if (_activeSceneIndex.value == sceneIndex && !userInterrupted) {
                sceneCompletedFlow.emit(sceneIndex)
            }
        }
    }

    fun onListenToggle() {
        val story = storyFlow.value ?: return
        val scene = story.scenes.getOrNull(_activeSceneIndex.value) ?: return
        val ref = AudioRef.Story(storyId = story.id, sceneNumber = scene.sceneNumber)
        when (val current = audioRepository.state.value) {
            is AudioState.Playing -> if (current.ref == ref) {
                userInterrupted = true
                audioRepository.stop()
            } else {
                userInterrupted = false
                audioRepository.play(ref)
            }
            is AudioState.Paused -> if (current.ref == ref) {
                userInterrupted = false
                audioRepository.resume()
            } else {
                userInterrupted = false
                audioRepository.play(ref)
            }
            is AudioState.Loading -> if (current.ref != ref) {
                userInterrupted = false
                audioRepository.play(ref)
            }
            else -> {
                userInterrupted = false
                audioRepository.play(ref)
            }
        }
    }

    fun onLeaveScreen() {
        playJob?.cancel()
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
        const val SCENE_AUDIO_MAX_MS = 30_000L
        val UNIT_ID_REGEX = Regex("""L(\d+)U(\d+)""")
    }
}

@Immutable
internal data class StoryUiState(
    val lessons: ImmutableList<PhonicsLesson>,
    val story: Story,
    val scenes: ImmutableList<StoryScene> = persistentListOf(),
)
