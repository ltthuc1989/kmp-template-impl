package me.ltthuc.kmp.core.ui.screen

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Immutable
@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    /** Content-pack download for a level, e.g. after unlocking it. */
    data class Download(val levelId: String) : Destination

    @Serializable
    data class Paywall(
        val source: String,
        val levelId: String? = null,
        // True if the parental gate was already passed before navigating here (e.g. via the Pro
        // icon) → the purchase button skips a second gate. False (default, e.g. tapping a locked
        // unit) → the purchase button shows the parental gate first.
        val gatedAlready: Boolean = false,
    ) : Destination {
        object Source {
            const val SETTINGS = "settings"
            const val LEVEL_LOCKED = "level_locked"
            const val LEVEL1_COMPLETE = "level1_complete"
            const val UNIT_LOCKED = "unit_locked"
        }
    }

    @Serializable
    sealed interface Learning : Destination {
        @Serializable
        data class UnitSelection(val levelId: String) : Learning

        @Serializable
        data class Step(
            val levelId: String,
            val unitId: String,
            val lessonIndex: Int,
            val stepIndex: Int,
        ) : Learning

        @Serializable
        data class LessonComplete(
            val levelId: String,
            val unitId: String,
            val lessonIndex: Int,
        ) : Learning

        @Serializable
        data class UnitStory(
            val levelId: String,
            val unitId: String,
        ) : Learning

        @Serializable
        data class UnitGame(
            val levelId: String,
            val unitId: String,
            val gameIndex: Int,
        ) : Learning

        @Serializable
        data class LessonMap(
            val levelId: String,
            val unitId: String,
        ) : Learning

        @Serializable
        data class LevelComplete(val levelId: String) : Learning

        @Serializable
        data object TracingGuideDebug : Learning

        @Serializable
        data object BubblePopPreview : Learning

        @Serializable
        data object MemoryMatchPreview : Learning
    }

    @Serializable
    data object Review : Destination

    @Serializable
    sealed interface Setting : Destination {
        @Serializable
        data object Root : Setting

        @Serializable
        data object License : Setting

        @Serializable
        data object Gate : Setting
    }

    companion object {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Home::class, Home.serializer())
                    subclass(Onboarding::class, Onboarding.serializer())
                    subclass(Download::class, Download.serializer())
                    subclass(Paywall::class, Paywall.serializer())
                    subclass(Learning.UnitSelection::class, Learning.UnitSelection.serializer())
                    subclass(Learning.Step::class, Learning.Step.serializer())
                    subclass(Learning.LessonComplete::class, Learning.LessonComplete.serializer())
                    subclass(Learning.UnitStory::class, Learning.UnitStory.serializer())
                    subclass(Learning.UnitGame::class, Learning.UnitGame.serializer())
                    subclass(Learning.LessonMap::class, Learning.LessonMap.serializer())
                    subclass(Learning.LevelComplete::class, Learning.LevelComplete.serializer())
                    subclass(Learning.TracingGuideDebug::class, Learning.TracingGuideDebug.serializer())
                    subclass(Learning.BubblePopPreview::class, Learning.BubblePopPreview.serializer())
                    subclass(Learning.MemoryMatchPreview::class, Learning.MemoryMatchPreview.serializer())
                    subclass(Review::class, Review.serializer())
                    subclass(Setting.Root::class, Setting.Root.serializer())
                    subclass(Setting.License::class, Setting.License.serializer())
                    subclass(Setting.Gate::class, Setting.Gate.serializer())
                }
            }
        }
    }
}
