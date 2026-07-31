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
 * NOTE: Material 3 "Expressive" (`MaterialExpressiveTheme`, `MotionScheme`) is still
 * behind `@ExperimentalMaterial3ExpressiveApi` in every material3 release as of this
 * writing — including the 1.5.0-alpha line — and isn't present at all in the stable
 * 1.4.0 release this project depends on. Rather than pin the whole app to a moving
 * alpha artifact for a system-wide motion-curve change, this uses the stable
 * `MaterialTheme` wrapper. The actual "expressive" *look* the app asks for — the
 * blob-shaped album art, the scalloped cookie-shaped play button, the pill-shaped
 * surfaces, the pushed-up corner-radius scale — all comes from this app's own
 * custom shapes (see Shape.kt / ExpressiveShapes.kt) and doesn't depend on that
 * experimental API at all. If you want the extra expressive motion/animation
 * curves later, bump material3 to a current 1.5.0-alpha and swap this back to
 * MaterialExpressiveTheme + MotionScheme.expressive().
 *
 * Dynamic color (Monet) is preferred on Android 12+, matching the wallpaper-driven
 * theming both reference apps use.
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
