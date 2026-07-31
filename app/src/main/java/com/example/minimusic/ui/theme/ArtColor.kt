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
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Derives an accent [Color] from a song's album art, for use only on the active
 * Player screen (the rest of the app stays on system Material You / Monet colors,
 * per the app-wide theme in Theme.kt). Falls back to the current Material theme's
 * primary color when there's no artwork or extraction fails.
 *
 * The color is animated between songs so the Player screen's background eases
 * from one track's hue to the next rather than snapping.
 */
@Composable
fun rememberArtAccentColor(albumArtUri: Uri?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary

    val target by produceState(initialValue = fallback, key1 = albumArtUri) {
        value = fallback
        if (albumArtUri == null) return@produceState

        val request = ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false) // Palette needs a software bitmap to read pixels
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
                value = Color(swatch.rgb)
            }
        }
    }

    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 500),
        label = "artAccentColor"
    )
    return animated
}

/** Convenience: a translucent version of the art accent, for large background washes. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
