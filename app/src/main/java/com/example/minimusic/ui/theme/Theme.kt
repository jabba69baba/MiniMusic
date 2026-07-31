package com.example.minimusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberPrimaryContainerLight,
    onPrimaryContainer = AmberOnPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    background = AmberBackgroundLight,
    surface = AmberSurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = AmberOnPrimaryDark,
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = AmberOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    background = AmberBackgroundDark,
    surface = AmberSurfaceDark
)

/**
 * App-wide theme.
 *
 * The app's shape language is intentionally flat — see Shape.kt: every corner
 * bucket collapses to a sharp rectangle, following Auxio's "no rounded corners"
 * design philosophy, so there's no dependency on Material 3 Expressive's
 * experimental shape/motion APIs here.
 *
 * Dynamic color (Monet) is preferred on Android 12+ and applies app-wide. The
 * one exception is the Player screen, which additionally tints itself with a
 * hue extracted from the current song's album art on top of this base theme
 * (see ArtColor.kt) — everywhere else in the app stays on pure system color.
 */
@Composable
fun MiniMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MiniMusicShapes,
        typography = MiniMusicTypography,
        content = content
    )
}
