package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.common_close
import org.jetbrains.compose.resources.stringResource

/**
 * Shared top header for step + game screens.
 *
 * Mockup C layout (single controls row + optional guide row):
 * ```
 *  ✕    ●━●━●━●━●━○━○                →    ← row 1: Close + segments + Next, center-vertical
 *         Tap matching bubble   🔊         ← row 2: guide text + trailing (audio button)
 * ```
 *
 * `stepSegments` lists the canonical step indices to render. `currentStepIndex` is canonical
 * too — highlighting matches by canonical equality, and `onStepJump` receives the canonical idx.
 * Close + Next are plain icons (no circle background); Next pulses once on disabled → enabled.
 */
@Composable
internal fun StepHeader(
    currentStepIndex: Int,
    stepSegments: ImmutableList<Int>,
    onClose: () -> Unit,
    onStepJump: (Int) -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    modifier: Modifier = Modifier,
    guideText: String = "",
    showGuideText: Boolean = true,
    nextContentDescription: String? = null,
    guideTrailing: (@Composable () -> Unit)? = null,
) {
    val resolvedNextCd = nextContentDescription ?: stringResource(Res.string.chant_next)
    val showGuideRow = (showGuideText && guideText.isNotEmpty()) || guideTrailing != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(4.dp))
            StepSegmentRow(
                currentStepIndex = currentStepIndex,
                stepSegments = stepSegments,
                onStepJump = onStepJump,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            PlainNextButton(
                onClick = onNext,
                enabled = nextEnabled,
                contentDescription = resolvedNextCd,
            )
        }
        if (showGuideRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (showGuideText && guideText.isNotEmpty()) {
                    Text(
                        text = guideText,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (guideTrailing != null) {
                    Spacer(Modifier.width(6.dp))
                    guideTrailing()
                }
            }
        }
    }
}

/**
 * Plain icon "Next / advance" button — no background, no shadow. Encodes state via:
 * - enabled = primary alpha 1.0, pulses scale 1.0 → 1.2 → 1.0 once when transitioning
 *   disabled → enabled, so the eye catches "you can move on now".
 * - disabled = primary alpha 0.30, still visible so the kid sees the goal.
 *
 * Tap target is the IconButton's default 48dp circle (invisible), comfortable for small fingers.
 */
@Composable
private fun PlainNextButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            scale.animateTo(1.20f, animationSpec = tween(150, easing = FastOutSlowInEasing))
            scale.animateTo(
                1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        }
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(44.dp)
            .scale(scale.value)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.30f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun StepSegmentRow(
    currentStepIndex: Int,
    stepSegments: ImmutableList<Int>,
    onStepJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stepSegments.forEach { canonicalIdx ->
            StepSegment(
                isCurrent = canonicalIdx == currentStepIndex,
                isPast = canonicalIdx < currentStepIndex,
                onClick = { if (canonicalIdx != currentStepIndex) onStepJump(canonicalIdx) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepSegment(
    isCurrent: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val targetColor by animateColorAsState(
        targetValue = when {
            isCurrent -> primary
            isPast -> primary.copy(alpha = 0.65f)
            else -> primaryContainer
        },
        animationSpec = tween(durationMillis = 500),
        label = "segmentColor",
    )
    val targetHeight by animateDpAsState(
        targetValue = if (isCurrent) 12.dp else 8.dp,
        animationSpec = tween(durationMillis = 500),
        label = "segmentHeight",
    )
    val ringPadding by animateDpAsState(
        targetValue = if (isCurrent) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "segmentRing",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(52.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = primary, bounded = true),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val ringWrapper = if (isCurrent) {
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = primary.copy(alpha = 0.30f),
                    spotColor = primary.copy(alpha = 0.30f),
                )
                .clip(CircleShape)
                .background(primaryContainer)
                .padding(ringPadding)
        } else {
            Modifier.fillMaxWidth()
        }
        Box(
            modifier = ringWrapper,
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetHeight)
                    .shadow(
                        elevation = 1.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f),
                    )
                    .clip(CircleShape)
                    .background(targetColor),
                contentAlignment = Alignment.Center,
            ) {
                if (isPast) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}

/**
 * Shared floating pill bottom bar showing letter progress in the unit.
 * Fixed across all 8 steps of a letter — only `currentIndex` updates when letter advances.
 */
@Composable
internal fun LetterStepperBar(
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        PuffySurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(48.dp),
            containerColor = Color.White.copy(alpha = 0.85f),
            shadowElevation = 28.dp,
            shadowTint = MaterialTheme.colorScheme.primary,
            shadowAlpha = 0.40f,
            topHighlightHeight = 12.dp,
            topHighlightAlpha = 0.6f,
            bottomShadeHeight = 12.dp,
            bottomShadeAlpha = 0.10f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                lessons.forEachIndexed { index, lesson ->
                    val isDone = index < currentIndex
                    val isCurrent = index == currentIndex
                    StepperLetter(
                        letterPair = lesson.letterPair(),
                        isHighlighted = isDone || isCurrent,
                    )
                    if (index < lessons.lastIndex) {
                        StepperConnector(
                            isActive = isDone || isCurrent,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperLetter(
    letterPair: String,
    isHighlighted: Boolean,
) {
    PuffySurface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        containerColor = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        shadowElevation = if (isHighlighted) 8.dp else 4.dp,
        shadowTint = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Black,
        shadowAlpha = if (isHighlighted) 0.55f else 0.18f,
        topHighlightHeight = 5.dp,
        topHighlightAlpha = if (isHighlighted) 0.3f else 0.8f,
        bottomShadeHeight = 5.dp,
        bottomShadeAlpha = if (isHighlighted) 0.25f else 0.10f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letterPair,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
    }
}

@Composable
private fun StepperConnector(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(CircleShape)
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ),
    )
}

/**
 * Replicates CSS multi-layer puffy box-shadow:
 * - Outer drop shadow via Modifier.shadow
 * - Inner top highlight: gradient strip (white → transparent)
 * - Inner bottom shade: gradient strip (transparent → black)
 */
@Composable
internal fun PuffySurface(
    shape: Shape,
    containerColor: Color,
    shadowElevation: Dp,
    shadowTint: Color,
    shadowAlpha: Float,
    topHighlightHeight: Dp,
    topHighlightAlpha: Float,
    bottomShadeHeight: Dp,
    bottomShadeAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = shadowTint.copy(alpha = shadowAlpha * 0.6f),
                spotColor = shadowTint.copy(alpha = shadowAlpha),
            )
            .clip(shape)
            .background(containerColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topHighlightHeight)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = topHighlightAlpha),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomShadeHeight)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = bottomShadeAlpha),
                        ),
                    ),
                ),
        )
        content()
    }
}

internal fun PhonicsLesson.letterPair(): String =
    displayLetter.substringBefore(' ').ifEmpty { "?" }

/**
 * Shared step navigation row (Previous + Next). Used by Chant, Vocabulary, ...
 * Previous = outlined puffy button; Next = filled primary puffy button.
 */
@Composable
internal fun StepNavRow(
    previousLabel: String,
    nextLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepPreviousButton(
            label = previousLabel,
            onClick = onPrevious,
            modifier = Modifier.weight(1f),
        )
        StepNextButton(
            label = nextLabel,
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StepPreviousButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 10.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.25f,
        topHighlightHeight = 8.dp,
        topHighlightAlpha = 0.8f,
        bottomShadeHeight = 8.dp,
        bottomShadeAlpha = 0.08f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StepNextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    PuffySurface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = containerColor,
        shadowElevation = if (enabled) 14.dp else 6.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (enabled) 0.55f else 0.20f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = if (enabled) 0.3f else 0.15f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = if (enabled) 0.30f else 0.15f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Single contextual continue button for step screens — replaces StepNavRow's Previous+Next pair.
 * Full width, primary red filled (puffy 3D). Disabled state shows reduced shadow + gray.
 *
 * For game screens (Matching/Identify/Blending), pass `enabled = activityComplete && overlay == null`
 * to enforce: user must complete activity AND dismiss celebration overlay before tapping Continue.
 */
@Composable
internal fun StepContinueButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    PuffySurface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = containerColor,
        shadowElevation = if (enabled) 14.dp else 6.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (enabled) 0.55f else 0.20f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = if (enabled) 0.3f else 0.15f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = if (enabled) 0.30f else 0.15f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
