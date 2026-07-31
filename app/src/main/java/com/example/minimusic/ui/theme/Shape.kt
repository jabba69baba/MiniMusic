package com.example.minimusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Auxio-style "no rounded corners" rational look: every corner bucket in the
// Material shape scale collapses to a sharp rectangle. This is a deliberate
// design choice (see Auxio's own README: "No rounded corners") rather than a
// missing style — flat, plain surfaces with no shape ornamentation anywhere
// in the app, including the album art frame and the play button.
val MiniMusicShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

/** Sharp rectangle — used everywhere a shape reference is needed (mini player, controls, art frame). */
val PillShape = RoundedCornerShape(0.dp)
