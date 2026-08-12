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
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Derives an accent [Color] from a song's album art, for use only on the active
 * Player screen (the rest of the app stays on system Material You / Monet colors,
 * per the app-wide theme in Theme.kt). Falls back to the current Material theme's
 * primary color when there's no artwork or extraction fails.
 *
 * To avoid the old song's hue visibly lingering while the next song's art is
 * still decoding, this keeps a small in-memory cache of already-computed
 * accent colors keyed by URI. A cache hit (e.g. skipping back to a song seen
 * earlier this session, or art Coil already has in its own memory cache)
 * resolves synchronously on the same frame the song changes, so there's
 * nothing to visibly animate from. Only a genuine cache miss — art being
 * decoded and run through Palette for the first time — falls back to a
 * short crossfade, and even then starts from the *fallback* theme color
 * rather than whatever the previous song's accent happened to be.
 */
private val accentColorCache = LinkedHashMap<Uri, Color>()
private const val ACCENT_CACHE_MAX_SIZE = 64

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

        // Not cached yet — show the fallback theme color while this song's art
        // decodes, rather than holding over whatever color the previous song
        // left behind (produceState's initialValue only applied once, on the
        // very first composition, not on every key change).
        value = fallback

        val request = ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false) // Palette needs a software bitmap to read pixels
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

/** Convenience: a translucent version of the art accent, for large background washes. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
