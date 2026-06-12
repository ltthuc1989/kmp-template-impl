@file:Suppress("ModifierReused")

package me.ltthuc.kmp.core.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.ui.screen.view.ErrorView
import me.ltthuc.kmp.core.ui.screen.view.LoadingView

@Composable
fun <T> AsyncLoadContents(
    screenState: ScreenState<T>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    cornerShape: RoundedCornerShape = RoundedCornerShape(0.dp),
    retryAction: (() -> Unit)? = null,
    terminate: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        modifier = modifier
            .clip(cornerShape)
            .background(containerColor),
        targetState = screenState,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = FADE_DURATION_MS))
                .togetherWith(fadeOut(animationSpec = tween(durationMillis = FADE_DURATION_MS)))
        },
        contentKey = { it::class.simpleName },
        label = "AsyncLoadContents",
    ) { state ->
        when (state) {
            is ScreenState.Idle -> {
                content.invoke(state.data)
            }

            is ScreenState.Loading -> {
                LoadingView(
                    modifier = Modifier.fillMaxWidth(),
                    message = state.message,
                )
            }

            is ScreenState.Error -> {
                ErrorView(
                    modifier = Modifier.fillMaxWidth(),
                    errorState = state,
                    retryAction = retryAction,
                    terminate = terminate,
                )
            }
        }
    }
}

private const val FADE_DURATION_MS = 200
