package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.lesson_sheet_lesson_label
import me.ltthuc.kmp.core.resource.lesson_sheet_practice_count
import me.ltthuc.kmp.core.resource.lesson_sheet_restart
import me.ltthuc.kmp.core.resource.lesson_sheet_story_label
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet shown when the user taps a Completed unit. Lets the kid pick any of the
 * unit's letters (lessons) to replay independently — replay does NOT reset progress.
 * Story has its own row. Footer "Restart entire unit" wipes completion count and
 * jumps to lesson 0 step 0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonSelectorSheet(
    unit: PhonicsUnit,
    lessons: ImmutableList<PhonicsLesson>,
    completionCount: Int,
    onLessonClick: (lessonIndex: Int) -> Unit,
    onStoryClick: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = unit.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (completionCount > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.lesson_sheet_practice_count, completionCount),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))

            lessons.forEachIndexed { index, lesson ->
                LessonRow(
                    label = stringResource(
                        Res.string.lesson_sheet_lesson_label,
                        index + 1,
                        lesson.displayLetter,
                    ),
                    emoji = lesson.words.firstOrNull()?.emoji.orEmpty().ifEmpty { "📘" },
                    onClick = { onLessonClick(index) },
                )
                Spacer(Modifier.height(8.dp))
            }

            // Story row (1 per unit, after last lesson)
            LessonRow(
                label = stringResource(Res.string.lesson_sheet_story_label),
                emoji = "📖",
                onClick = onStoryClick,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.lesson_sheet_restart))
            }
        }
    }
}

@Composable
private fun LessonRow(
    label: String,
    emoji: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = AccentRedSheet,
            modifier = Modifier.size(28.dp),
        )
    }
}

private val AccentRedSheet = Color(0xFFE63946)
