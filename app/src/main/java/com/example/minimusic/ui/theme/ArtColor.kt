package com.example.minimusic.ui.theme

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/** Album-art-derived Material roles used only by PlayerScreen and LyricsScreen. */
data class ArtColorRoles(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color
)

private val accentColorCache = LinkedHashMap<Uri, Color>()
private const val ACCENT_CACHE_MAX_SIZE = 64

/**
 * Derives one stable accent from local album art using Palette. The image is
 * read from the device's local MediaStore URI; no network source is involved.
 */
@Composable
fun rememberArtAccentColor(albumArtUri: Uri?): Color {
    val context = LocalContext.current
    val wallpaperPrimary = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    val target by produceState(initialValue = accentColorCache[albumArtUri] ?: wallpaperPrimary, key1 = albumArtUri) {
        if (albumArtUri == null) {
            value = wallpaperPrimary
            return@produceState
        }

        accentColorCache[albumArtUri]?.let { cached ->
            value = cached
            return@produceState
        }

        value = wallpaperPrimary
        val request = ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap: Bitmap? = result.drawable?.let { drawable ->
            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        }

        if (bitmap != null) {
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch
            if (swatch != null) {
                val resolved = Color(swatch.rgb)
                value = resolved
                accentColorCache[albumArtUri] = resolved
                if (accentColorCache.size > ACCENT_CACHE_MAX_SIZE) {
                    accentColorCache.remove(accentColorCache.keys.first())
                }
            }
        }
    }

    val normalizedTarget = if (albumArtUri == null) {
        wallpaperPrimary
    } else {
        normalizeAccent(target, wallpaperPrimary, isDark)
    }

    return animateColorAsState(
        targetValue = normalizedTarget,
        animationSpec = tween(durationMillis = 220),
        label = "artAccentColor"
    ).value
}

@Composable
fun rememberArtColorRoles(albumArtUri: Uri?): ArtColorRoles {
    val wallpaper = MaterialTheme.colorScheme
    val accent = rememberArtAccentColor(albumArtUri)
    val primaryContainer = blend(wallpaper.surface, accent, 0.24f)
    val secondary = blend(wallpaper.secondary, accent, 0.24f)
    val secondaryContainer = blend(wallpaper.secondaryContainer, accent, 0.22f)
    val tertiary = blend(wallpaper.tertiary, accent, 0.24f)
    val tertiaryContainer = blend(wallpaper.tertiaryContainer, accent, 0.20f)

    return ArtColorRoles(
        primary = accent,
        onPrimary = contrastingOn(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = wallpaper.onSurface,
        secondary = secondary,
        onSecondary = contrastingOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = wallpaper.onSurface,
        tertiary = tertiary,
        onTertiary = contrastingOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = wallpaper.onSurface,
        background = blend(wallpaper.background, accent, 0.08f),
        onBackground = wallpaper.onBackground,
        surface = blend(wallpaper.surface, accent, 0.12f),
        onSurface = wallpaper.onSurface,
        surfaceVariant = blend(wallpaper.surfaceVariant, accent, 0.18f),
        onSurfaceVariant = wallpaper.onSurfaceVariant
    )
}

private fun normalizeAccent(raw: Color, wallpaperPrimary: Color, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgb(), hsl)
    val isNearNeutral = hsl[1] < 0.08f
    if (isNearNeutral) {
        // Neutral art needs a small amount of the wallpaper primary to remain
        // legible without inventing a saturated hue unrelated to the artwork.
        return blend(raw, wallpaperPrimary, 0.34f)
    }

    hsl[1] = hsl[1].coerceAtMost(0.54f)
    hsl[2] = if (isDark) {
        hsl[2].coerceIn(0.48f, 0.70f)
    } else {
        hsl[2].coerceIn(0.30f, 0.56f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun contrastingOn(color: Color): Color =
    if (color.luminance() > 0.52f) Color(0xFF171717) else Color.White

private fun blend(start: Color, end: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

/** Convenience: a translucent version of the art accent, for large background washes. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
