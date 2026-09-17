package com.example.minimusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.minimusic.data.PaletteStyle
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant

// Material baseline schemes are the non-dynamic fallback. On Android 12+
// the system Monet scheme supplies the same semantic roles from wallpaper
// colors; no app-specific amber palette overrides those roles.
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

/**
 * Builds an M3 dynamic scheme from a seed HCT with the selected palette
 * style. Shared by the wallpaper-derived app theme and the
 * album-art-derived player colors so both sides follow the same
 * complementary-palette rules (M3 dynamic color from in-app content).
 */
internal fun dynamicSchemeFromHct(hct: Hct, isDark: Boolean, style: PaletteStyle): DynamicScheme {
    return when (style) {
        PaletteStyle.TONAL_SPOT -> SchemeTonalSpot(hct, isDark, 0.0)
        PaletteStyle.VIBRANT -> SchemeVibrant(hct, isDark, 0.0)
        PaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, isDark, 0.0)
        PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, isDark, 0.0)
    }
}

/**
 * Maps the scheme's tone-generated roles onto a Compose [ColorScheme].
 * Copying the baseline scheme keeps any roles this material3 version adds
 * filled in; only the tone-derived roles are overridden.
 */
private fun DynamicScheme.toComposeColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(getPrimary()),
        onPrimary = Color(getOnPrimary()),
        primaryContainer = Color(getPrimaryContainer()),
        onPrimaryContainer = Color(getOnPrimaryContainer()),
        inversePrimary = Color(getInversePrimary()),
        secondary = Color(getSecondary()),
        onSecondary = Color(getOnSecondary()),
        secondaryContainer = Color(getSecondaryContainer()),
        onSecondaryContainer = Color(getOnSecondaryContainer()),
        tertiary = Color(getTertiary()),
        onTertiary = Color(getOnTertiary()),
        tertiaryContainer = Color(getTertiaryContainer()),
        onTertiaryContainer = Color(getOnTertiaryContainer()),
        background = Color(getBackground()),
        onBackground = Color(getOnBackground()),
        surface = Color(getSurface()),
        onSurface = Color(getOnSurface()),
        surfaceVariant = Color(getSurfaceVariant()),
        onSurfaceVariant = Color(getOnSurfaceVariant()),
        surfaceTint = Color(getSurfaceTint()),
        inverseSurface = Color(getInverseSurface()),
        inverseOnSurface = Color(getInverseOnSurface()),
        error = Color(getError()),
        onError = Color(getOnError()),
        errorContainer = Color(getErrorContainer()),
        onErrorContainer = Color(getOnErrorContainer()),
        outline = Color(getOutline()),
        outlineVariant = Color(getOutlineVariant()),
        scrim = Color(getScrim()),
        surfaceBright = Color(getSurfaceBright()),
        surfaceDim = Color(getSurfaceDim()),
        surfaceContainerLowest = Color(getSurfaceContainerLowest()),
        surfaceContainerLow = Color(getSurfaceContainerLow()),
        surfaceContainer = Color(getSurfaceContainer()),
        surfaceContainerHigh = Color(getSurfaceContainerHigh()),
        surfaceContainerHighest = Color(getSurfaceContainerHighest()),
        primaryFixed = Color(getPrimaryFixed()),
        primaryFixedDim = Color(getPrimaryFixedDim()),
        onPrimaryFixed = Color(getOnPrimaryFixed()),
        onPrimaryFixedVariant = Color(getOnPrimaryFixedVariant()),
        secondaryFixed = Color(getSecondaryFixed()),
        secondaryFixedDim = Color(getSecondaryFixedDim()),
        onSecondaryFixed = Color(getOnSecondaryFixed()),
        onSecondaryFixedVariant = Color(getOnSecondaryFixedVariant()),
        tertiaryFixed = Color(getTertiaryFixed()),
        tertiaryFixedDim = Color(getTertiaryFixedDim()),
        onTertiaryFixed = Color(getOnTertiaryFixed()),
        onTertiaryFixedVariant = Color(getOnTertiaryFixedVariant())
    )
}

/**
 * App-wide Material 3 Expressive theme. Android 12+ uses wallpaper-derived
 * Monet roles; older devices use the complete warm-neutral fallback above.
 * Every screen receives the same primary/secondary/tertiary and surface-role
 * contract, while the player may add its album-art accent locally.
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
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = when {
        // The app-wide scheme is the wallpaper-derived one, re-generated with
        // the selected palette style: the seed is the system dynamic scheme's
        // own primary (the device wallpaper's identity), so non-art
        // components follow the same complementary palette as the
        // album-art-derived player surface. On Android 11- (or with dynamic
        // color off) there is no wallpaper seed — keep the static baseline.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context, darkTheme, paletteStyle) {
                val wallpaperSeed = if (darkTheme) {
                    dynamicDarkColorScheme(context).primary
                } else {
                    dynamicLightColorScheme(context).primary
                }
                dynamicSchemeFromHct(
                    Hct.fromInt(wallpaperSeed.toArgb()),
                    darkTheme,
                    paletteStyle
                ).toComposeColorScheme(darkTheme)
            }
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
