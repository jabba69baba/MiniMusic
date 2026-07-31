package com.example.minimusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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
 * App-wide theme. Uses [MaterialExpressiveTheme] (Material 3 Expressive) rather than
 * the classic [androidx.compose.material3.MaterialTheme] wrapper, which switches on
 * the expressive motion scheme and unlocks the expressive component variants used
 * throughout the UI. Dynamic color (Monet) is preferred on Android 12+, matching
 * the wallpaper-driven theming both reference apps use.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MiniMusicShapes,
        typography = MiniMusicTypography,
        content = content
    )
}
