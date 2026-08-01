package com.example.minimusic.ui.theme

import androidx.compose.ui.graphics.Color

// Warm amber seed, used only as a fallback on API < 31 where dynamic color
// (Monet / Material You) isn't available. On API 31+ we derive the scheme
// from the user's wallpaper instead.
val AmberPrimaryLight = Color(0xFF8C5000)
val AmberOnPrimaryLight = Color(0xFFFFFFFF)
val AmberPrimaryContainerLight = Color(0xFFFFDDB3)
val AmberOnPrimaryContainerLight = Color(0xFF2C1600)
val AmberSecondaryLight = Color(0xFF6F5B40)
val AmberBackgroundLight = Color(0xFFFFFBFF)
val AmberSurfaceLight = Color(0xFFFFFBFF)

val AmberPrimaryDark = Color(0xFFFFB876)
val AmberOnPrimaryDark = Color(0xFF4A2900)
val AmberPrimaryContainerDark = Color(0xFF693C00)
val AmberOnPrimaryContainerDark = Color(0xFFFFDDB3)
val AmberSecondaryDark = Color(0xFFDDC3A2)
val AmberBackgroundDark = Color(0xFF1F1B16)
val AmberSurfaceDark = Color(0xFF1F1B16)
