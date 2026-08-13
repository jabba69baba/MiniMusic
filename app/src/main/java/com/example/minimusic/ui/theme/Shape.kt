package com.example.minimusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 Expressive rounded look: generous corner radii across the shape scale,
// used for cards, containers, and buttons throughout the app.
val MiniMusicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** True stadium/pill shape — corner radius always exactly half the element's height,
 *  so it looks correct at any size. Used for the mini player, capsule controls,
 *  floating nav bars, and anywhere else a fully-rounded pill is needed. */
val PillShape = RoundedCornerShape(percent = 50)
