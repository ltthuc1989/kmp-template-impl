package me.ltthuc.kmp.feature.learningpath.game.memorymatch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.MemoryCardSpec

/**
 * Centered 2-column grid of memory cards. Six cards (3 pairs) lay out as 3 rows of 2;
 * four cards (2 pairs) become 2 rows of 2. Each card is square (aspectRatio 1f), so the
 * grid's total height comes from card width — bounded by parent.
 *
 * Tap dispatches via [onCardTap]; the host ViewModel decides whether to ignore (matched,
 * already face-up, resolving).
 */
@Composable
internal fun MemoryGrid(
    cards: ImmutableList<MemoryCardSpec>,
    matchedIds: ImmutableSet<Int>,
    selectedIds: ImmutableList<Int>,
    enabled: Boolean,
    onCardTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCardPositioned: (id: Int, centerInWindow: Offset) -> Unit = { _, _ -> },
) {
    val rows = cards.chunked(COLUMN_COUNT)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, alignment = Alignment.CenterVertically),
    ) {
        rows.forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rowCards.forEach { card ->
                    MemoryCard(
                        letter = card.letter,
                        tint = card.tint,
                        isFaceUp = card.id in matchedIds || card.id in selectedIds,
                        isMatched = card.id in matchedIds,
                        enabled = enabled,
                        onTap = { onCardTap(card.id) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .onGloballyPositioned {
                                onCardPositioned(card.id, it.boundsInWindow().center)
                            },
                    )
                }
                // Pad last row if odd card count (e.g. 5 cards → 1 phantom slot).
                val phantomCount = COLUMN_COUNT - rowCards.size
                repeat(phantomCount) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val COLUMN_COUNT = 2
