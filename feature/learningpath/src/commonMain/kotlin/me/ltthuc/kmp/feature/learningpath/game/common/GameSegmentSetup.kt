package me.ltthuc.kmp.feature.learningpath.game.common

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the segment indices for the game flow header. We reuse [StepHeader] which expects
 * an arbitrary `ImmutableList<Int>` and highlights the entry matching `currentStepIndex`.
 * For games, indices are simply 0..(totalGames-1).
 */
internal fun gameSegmentsFor(totalGames: Int): ImmutableList<Int> =
    (0 until totalGames.coerceAtLeast(1)).toList().toImmutableList()
