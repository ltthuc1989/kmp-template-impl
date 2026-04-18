package me.matsumo.grabee.core.ui.screen.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
data class FloatingPillNavItem(
    val key: String,
    val label: StringResource,
    val iconInactive: ImageVector,
    val iconActive: ImageVector,
)

@Composable
fun FloatingPillNavBar(
    items: ImmutableList<FloatingPillNavItem>,
    selectedKey: String,
    onItemSelected: (FloatingPillNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(items.size in MIN_ITEMS..MAX_ITEMS) {
        "FloatingPillNavBar supports $MIN_ITEMS..$MAX_ITEMS items, got ${items.size}"
    }

    val containerShape = RoundedCornerShape(percent = 50)
    val primary = MaterialTheme.colorScheme.primary
    val shadowColor = primary.copy(alpha = 0.15f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.92f)
                .widthIn(max = 512.dp)
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = containerShape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor,
                ),
            shape = containerShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .semantics {
                        role = Role.Tab
                        contentDescription = "Navigation"
                    },
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    NavItem(
                        item = item,
                        isSelected = item.key == selectedKey,
                        onClick = { onItemSelected(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    item: FloatingPillNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "NavItemScale",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 12.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "NavItemHorizontalPadding",
    )
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val background by animateColorAsState(
        targetValue = if (isSelected) selectedContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "NavItemBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "NavItemContentColor",
    )

    val label = stringResource(item.label)
    val itemShape = RoundedCornerShape(percent = 50)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(itemShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .sizeIn(minWidth = 64.dp, minHeight = 64.dp)
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .semantics {
                selected = isSelected
                contentDescription = label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp, alignment = Alignment.CenterVertically),
    ) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 150))
            },
            label = "NavItemIconSwap",
        ) { selected ->
            Icon(
                imageVector = if (selected) item.iconActive else item.iconInactive,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor,
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

private const val MIN_ITEMS = 2
private const val MAX_ITEMS = 5
