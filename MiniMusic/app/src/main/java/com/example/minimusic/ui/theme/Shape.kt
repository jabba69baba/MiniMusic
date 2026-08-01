package com.example.minimusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Material 3 Expressive leans into bigger, more varied corner rounding than
// classic M3 — and PixelPlayer-style players make that rounding user-adjustable
// rather than fixed. miniMusicShapes() builds the five-tier M3 shape scale
// proportionally from a single base value the user controls in
// Settings > Appearance (see AppSettings.cornerRadiusDp). 20dp is the default
// "medium" reference point the whole scale is derived from.
fun miniMusicShapes(base: Dp): Shapes {
    val ratio = base.value / 20f
    return Shapes(
        extraSmall = RoundedCornerShape((8f * ratio).dp),
        small = RoundedCornerShape((12f * ratio).dp),
        medium = RoundedCornerShape((20f * ratio).dp),
        large = RoundedCornerShape((28f * ratio).dp),
        extraLarge = RoundedCornerShape((36f * ratio).dp)
    )
}

val DefaultCornerRadius = 20.dp
val MiniMusicShapes = miniMusicShapes(DefaultCornerRadius)
