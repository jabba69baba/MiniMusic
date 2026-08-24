package com.example.minimusic.ui.theme

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils as CoreColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.material.color.utilities.DynamicScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeVibrant
import kotlin.math.abs
import kotlin.math.ln

/** Album-art-derived Material roles used by player, queue, lyrics, and miniplayer surfaces. */
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

private val artworkSeedCache = LinkedHashMap<Uri, Color>()
private const val ARTWORK_SEED_CACHE_MAX_SIZE = 64

/**
 * Extracts one local album-art seed. The seed is only an input to Material's
 * Tonal Spot scheme; no raw vibrant swatch is ever painted directly into UI.
 */
@Composable
private fun rememberArtworkSeedColor(albumArtUri: Uri?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    val seed by produceState(
        initialValue = artworkSeedCache[albumArtUri] ?: fallback,
        key1 = albumArtUri,
        key2 = fallback
    ) {
        if (albumArtUri == null) {
            value = fallback
            return@produceState
        }
        artworkSeedCache[albumArtUri]?.let {
            value = it
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
        val swatch = bitmap?.let { artwork ->
            // Palette generation can be relatively expensive for large local
            // album art. Keep it off the UI dispatcher so the player, home
            // screen, and mini-player remain responsive while colors resolve.
            withContext(Dispatchers.Default) {
                val palette = Palette.from(artwork).generate()
                // Score representative swatches rather than trusting the most
                // saturated dominant pixel. Tonal Spot below creates the roles.
                val candidates = listOfNotNull(
                    palette.dominantSwatch,
                    palette.mutedSwatch,
                    palette.darkMutedSwatch,
                    palette.lightMutedSwatch,
                    palette.vibrantSwatch,
                    palette.darkVibrantSwatch,
                    palette.lightVibrantSwatch
                )
                val expressiveCandidates = candidates.filter {
                    Hct.fromInt(it.rgb).chroma >= 14.0
                }
                (expressiveCandidates.ifEmpty { candidates }).maxByOrNull(::artworkSwatchScore)
            }
        }
        swatch?.let {
            val resolved = normalizeArtworkSeed(Color(it.rgb))
            value = resolved
            artworkSeedCache[albumArtUri] = resolved
            if (artworkSeedCache.size > ARTWORK_SEED_CACHE_MAX_SIZE) {
                artworkSeedCache.remove(artworkSeedCache.keys.first())
            }
        }
    }
    return seed
}

private fun artworkSwatchScore(swatch: Palette.Swatch): Double {
    val hct = Hct.fromInt(swatch.rgb)
    val populationScore = ln((swatch.population + 1).toDouble())
    val toneScore = 1.0 - abs(hct.tone - 50.0) / 50.0
    val chromaScore = 1.0 - abs(hct.chroma - 36.0) / 72.0
    return populationScore * 0.28 + toneScore.coerceAtLeast(0.0) * 0.22 +
        chromaScore.coerceAtLeast(0.0) * 0.50
}

private fun artScheme(seed: Color, isDark: Boolean): DynamicScheme =
    SchemeVibrant(Hct.fromInt(seed.toArgb()), isDark, 0.0)

/**
 * Normalize a palette seed before it reaches Material scheme generation.
 * Album art can contain nearly-black or highly saturated pixels that are useful
 * in the cover itself but are too aggressive as UI colors. This preserves hue
 * while compressing saturation and keeping the seed in a usable tonal window.
 */
private fun normalizeArtworkSeed(color: Color): Color {
    val hsl = FloatArray(3)
    CoreColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.78f).coerceAtMost(0.70f)
    hsl[2] = hsl[2].coerceIn(0.24f, 0.74f)
    return Color(CoreColorUtils.HSLToColor(hsl))
}

@Composable
fun rememberArtColorRoles(albumArtUri: Uri?): ArtColorRoles {
    val seed = rememberArtworkSeedColor(albumArtUri)
    val appScheme = MaterialTheme.colorScheme
    val isDark = appScheme.background.luminance() < 0.5f
    val scheme = remember(seed, isDark) { artScheme(seed, isDark) }
    val artPrimary = Color(scheme.getPrimary())
    val artPrimaryContainer = Color(scheme.getPrimaryContainer())
    val artSecondary = Color(scheme.getSecondary())
    val artSecondaryContainer = Color(scheme.getSecondaryContainer())
    val artTertiary = Color(scheme.getTertiary())
    val artTertiaryContainer = Color(scheme.getTertiaryContainer())
    val artBackground = Color(scheme.getBackground())
    val artOnBackground = Color(scheme.getOnBackground())
    val artSurface = Color(scheme.getSurface())
    val artOnSurface = Color(scheme.getOnSurface())
    val artSurfaceVariant = Color(scheme.getSurfaceVariant())
    val artOnSurfaceVariant = Color(scheme.getOnSurfaceVariant())

    return ArtColorRoles(
        primary = artPrimary,
        onPrimary = Color(scheme.getOnPrimary()),
        primaryContainer = artPrimaryContainer,
        onPrimaryContainer = Color(scheme.getOnPrimaryContainer()),
        secondary = artSecondary,
        onSecondary = Color(scheme.getOnSecondary()),
        secondaryContainer = artSecondaryContainer,
        onSecondaryContainer = Color(scheme.getOnSecondaryContainer()),
        tertiary = artTertiary,
        onTertiary = Color(scheme.getOnTertiary()),
        tertiaryContainer = artTertiaryContainer,
        onTertiaryContainer = Color(scheme.getOnTertiaryContainer()),
        background = artBackground,
        onBackground = artOnBackground,
        surface = artSurface,
        onSurface = artOnSurface,
        surfaceVariant = artSurfaceVariant,
        onSurfaceVariant = artOnSurfaceVariant
    )
}

/** Convenience: a translucent version of an art-derived role. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
