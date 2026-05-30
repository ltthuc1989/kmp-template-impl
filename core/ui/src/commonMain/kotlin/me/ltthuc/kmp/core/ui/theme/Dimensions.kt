package me.ltthuc.kmp.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppDimensions {
    // Cap content width so tablets / foldables show a centered phone-shaped column
    // instead of stretching gameplay UI across 800dp+. Picked 480dp = large-phone width.
    val ContentMaxWidth: Dp = 480.dp
}
