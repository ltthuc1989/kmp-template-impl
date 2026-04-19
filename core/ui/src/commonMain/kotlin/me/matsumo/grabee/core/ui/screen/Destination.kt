package me.matsumo.grabee.core.ui.screen

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
    data class Download(val url: String) : Destination

    @Serializable
    data class Paywall(val source: String) : Destination

    @Serializable
    sealed interface Learning : Destination {
        @Serializable
        data class UnitSelection(val levelId: String) : Learning

        @Serializable
        data class Step(
            val levelId: String,
            val unitId: String,
            val stepIndex: Int,
        ) : Learning

        @Serializable
        data class UnitComplete(
            val levelId: String,
            val unitId: String,
            val starsEarned: Int,
        ) : Learning
    }

    @Serializable
    data object Review : Destination

    @Serializable
    sealed interface Setting : Destination {
        @Serializable
        data object Root : Setting

        @Serializable
        data object License : Setting
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
                    subclass(Learning.UnitComplete::class, Learning.UnitComplete.serializer())
                    subclass(Review::class, Review.serializer())
                    subclass(Setting.Root::class, Setting.Root.serializer())
                    subclass(Setting.License::class, Setting.License.serializer())
                }
            }
        }
    }
}
