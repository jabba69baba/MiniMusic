package com.example.minimusic.ui.theme

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
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
    val fallback = MaterialTheme.colorScheme.primary

    val target by produceState(initialValue = accentColorCache[albumArtUri] ?: fallback, key1 = albumArtUri) {
        if (albumArtUri == null) {
            value = fallback
            return@produceState
        }

        accentColorCache[albumArtUri]?.let { cached ->
            value = cached
            return@produceState
        }

        value = fallback
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

    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 220),
        label = "artAccentColor"
    )
    return animated
}

@Composable
fun rememberArtColorRoles(albumArtUri: Uri?): ArtColorRoles {
    val wallpaper = MaterialTheme.colorScheme
    val accent = rememberArtAccentColor(albumArtUri)

    return ArtColorRoles(
        primary = accent,
        onPrimary = contrastingOn(accent),
        primaryContainer = accent.copy(alpha = 0.34f).compositeOver(wallpaper.surface),
        onPrimaryContainer = contrastingOn(accent.copy(alpha = 0.34f).compositeOver(wallpaper.surface)),
        secondary = blend(accent, wallpaper.secondary, 0.38f),
        onSecondary = contrastingOn(blend(accent, wallpaper.secondary, 0.38f)),
        secondaryContainer = blend(
            accent.copy(alpha = 0.25f).compositeOver(wallpaper.surface),
            wallpaper.secondaryContainer,
            0.42f
        ),
        onSecondaryContainer = wallpaper.onSurface,
        tertiary = blend(accent, wallpaper.tertiary, 0.55f),
        onTertiary = contrastingOn(blend(accent, wallpaper.tertiary, 0.55f)),
        tertiaryContainer = blend(
            accent.copy(alpha = 0.22f).compositeOver(wallpaper.surface),
            wallpaper.tertiaryContainer,
            0.48f
        ),
        onTertiaryContainer = wallpaper.onSurface,
        background = accent.copy(alpha = 0.14f).compositeOver(wallpaper.background),
        onBackground = wallpaper.onBackground,
        surface = accent.copy(alpha = 0.20f).compositeOver(wallpaper.surface),
        onSurface = wallpaper.onSurface,
        surfaceVariant = blend(
            accent.copy(alpha = 0.18f).compositeOver(wallpaper.surface),
            wallpaper.surfaceVariant,
            0.50f
        ),
        onSurfaceVariant = wallpaper.onSurfaceVariant
    )
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
