package com.example.minimusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberPrimaryContainerLight,
    onPrimaryContainer = AmberOnPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    onSecondary = AmberOnSecondaryLight,
    secondaryContainer = AmberSecondaryContainerLight,
    onSecondaryContainer = AmberOnSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    background = AmberBackgroundLight,
    onBackground = AmberOnBackgroundLight,
    surface = AmberSurfaceLight,
    onSurface = AmberOnSurfaceLight,
    surfaceVariant = AmberSurfaceVariantLight,
    onSurfaceVariant = AmberOnSurfaceVariantLight,
    outline = AmberOutlineLight,
    outlineVariant = AmberOutlineVariantLight,
    inverseSurface = AmberInverseSurfaceLight,
    inverseOnSurface = AmberInverseOnSurfaceLight,
    inversePrimary = AmberInversePrimaryLight
)

private val DarkColors = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = AmberOnPrimaryDark,
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = AmberOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = AmberOnSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = AmberOnTertiaryContainerDark,
    background = AmberBackgroundDark,
    onBackground = AmberOnBackgroundDark,
    surface = AmberSurfaceDark,
    onSurface = AmberOnSurfaceDark,
    surfaceVariant = AmberSurfaceVariantDark,
    onSurfaceVariant = AmberOnSurfaceVariantDark,
    outline = AmberOutlineDark,
    outlineVariant = AmberOutlineVariantDark,
    inverseSurface = AmberInverseSurfaceDark,
    inverseOnSurface = AmberInverseOnSurfaceDark,
    inversePrimary = AmberInversePrimaryDark
)

/**
 * App-wide Material 3 Expressive theme. Android 12+ uses wallpaper-derived
 * Monet roles; older devices use the complete warm-neutral fallback above.
 * Every screen receives the same primary/secondary/tertiary and surface-role
 * contract, while the player may add its album-art accent locally.
 */
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
    // AMOLED is a dark-theme surface policy only. Override every Material 3 surface
    // container so individual screens cannot fall back to warm/dynamic surfaces.
    val colorScheme = if (darkTheme && amoledBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black
        )
    } else {
        baseColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = MiniMusicShapes,
        typography = MiniMusicTypography,
        content = content
    )
}
