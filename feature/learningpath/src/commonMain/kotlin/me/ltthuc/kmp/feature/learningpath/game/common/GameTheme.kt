package me.ltthuc.kmp.feature.learningpath.game.common

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared ocean-themed vertical gradient used as the play-area background across every
 * game in the post-Story flow. Keeping it in one place means visually consistent water
 * + lets all games share the same OceanDecorations layer above it.
 */
internal val OceanBackground: Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0E7C8A),
        Color(0xFF1FA3B8),
        Color(0xFF38C7DC),
    ),
)

/**
 * Warm cream background used by reading-heavy games (PickWord / FillLetter / SpellLetters /
 * DragWords). Pure white is harsh on the eye for long sessions; this soft cream keeps the
 * play-area neutral so text + pastel cards are the focal point, while chrome (header +
 * bottom bar) still stays consistent across all games in the flow.
 */
internal val CreamBackground: Color = Color(0xFFFFFBF5)

/** Deep teal used for dark text on cream — readable contrast without harsh black. */
internal val ReadingTextDark: Color = Color(0xFF0E5562)

/** Light gray dashed outline for empty slots on cream background. */
internal val SlotOutline: Color = Color(0xFFCFD8DC)
