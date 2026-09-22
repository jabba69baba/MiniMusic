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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material baseline schemes are the non-dynamic fallback. On Android 12+
// the system Monet scheme supplies the same semantic roles from wallpaper
// colors; no app-specific amber palette overrides those roles.
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

/**
 * App-wide Material 3 Expressive theme. Android 12+ uses the system's own
 * wallpaper-derived Monet scheme (the wallpaper's Tonal Spot); older devices
 * use the complete warm-neutral fallback above. Every screen receives the
 * same primary/secondary/tertiary and surface-role contract, while the player
 * may add its album-art accent locally — the album-art palette style (Tonal
 * Spot / Vibrant / Expressive / Fruit Salad) is intentionally limited to
 * album-art-derived components so the app chrome stays the device's identity.
 *
 * The motion scheme is the official expressive physics scheme
 * ([MotionScheme.expressive]): spatial springs may overshoot, effects springs
 * (color/alpha) never do. All M3 components and [MiniMusicMotion] custom
 * motion derive from these tokens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    // AMOLED keeps only the deepest app surfaces black. Container roles remain
    // visibly separated so cards, dividers, switches, queue rows, and the mini
    // player do not disappear into one indistinguishable black plane.
    val colorScheme = if (darkTheme && amoledBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF1D1B1E),
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF252326),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0D0C0E),
            surfaceContainer = Color(0xFF121013),
            surfaceContainerHigh = Color(0xFF1A181B),
            surfaceContainerHighest = Color(0xFF242125),
            outlineVariant = Color(0xFF3A363B)
        )
    } else {
        baseColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MiniMusicShapes,
        typography = MiniMusicTypography,
        content = content
    )
}
