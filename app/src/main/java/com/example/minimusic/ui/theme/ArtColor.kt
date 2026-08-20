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
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot

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
        val swatch = bitmap?.let {
            val palette = Palette.from(it).generate()
            // Palette is only a deterministic seed selector. Tonal Spot below
            // creates the actual Material roles and controls chroma/lightness.
            palette.dominantSwatch
                ?: palette.mutedSwatch
                ?: palette.vibrantSwatch
        }
        swatch?.let {
            val resolved = Color(it.rgb)
            value = resolved
            artworkSeedCache[albumArtUri] = resolved
            if (artworkSeedCache.size > ARTWORK_SEED_CACHE_MAX_SIZE) {
                artworkSeedCache.remove(artworkSeedCache.keys.first())
            }
        }
    }
    return seed
}

private fun tonalSpotScheme(seed: Color, isDark: Boolean): DynamicScheme =
    SchemeTonalSpot(Hct.fromInt(seed.toArgb()), isDark, 0.0)

/** Blend a controlled amount of album-art color into an app Material role. */
private fun softenedRole(base: Color, art: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red + (art.red - base.red) * fraction,
        green = base.green + (art.green - base.green) * fraction,
        blue = base.blue + (art.blue - base.blue) * fraction,
        alpha = base.alpha + (art.alpha - base.alpha) * fraction
    )
}

/** The softened art-derived Material primary used by the miniplayer ring. */
@Composable
fun rememberArtAccentColor(albumArtUri: Uri?): Color {
    val seed = rememberArtworkSeedColor(albumArtUri)
    val appScheme = MaterialTheme.colorScheme
    val isDark = appScheme.background.luminance() < 0.5f
    val scheme = remember(seed, isDark) { tonalSpotScheme(seed, isDark) }
    return softenedRole(
        base = appScheme.primary,
        art = Color(scheme.getPrimary()),
        amount = 0.35f
    )
}

@Composable
fun rememberArtColorRoles(albumArtUri: Uri?): ArtColorRoles {
    val seed = rememberArtworkSeedColor(albumArtUri)
    val appScheme = MaterialTheme.colorScheme
    val isDark = appScheme.background.luminance() < 0.5f
    val scheme = remember(seed, isDark) { tonalSpotScheme(seed, isDark) }
    val artPrimary = Color(scheme.getPrimary())
    val artPrimaryContainer = Color(scheme.getPrimaryContainer())
    val artSecondary = Color(scheme.getSecondary())
    val artSecondaryContainer = Color(scheme.getSecondaryContainer())
    val artTertiary = Color(scheme.getTertiary())
    val artTertiaryContainer = Color(scheme.getTertiaryContainer())
    val artSurface = Color(scheme.getSurface())
    val artSurfaceVariant = Color(scheme.getSurfaceVariant())

    return ArtColorRoles(
        primary = softenedRole(appScheme.primary, artPrimary, 0.35f),
        onPrimary = appScheme.onPrimary,
        primaryContainer = softenedRole(appScheme.primaryContainer, artPrimaryContainer, 0.30f),
        onPrimaryContainer = appScheme.onPrimaryContainer,
        secondary = softenedRole(appScheme.secondary, artSecondary, 0.28f),
        onSecondary = appScheme.onSecondary,
        secondaryContainer = softenedRole(appScheme.secondaryContainer, artSecondaryContainer, 0.24f),
        onSecondaryContainer = appScheme.onSecondaryContainer,
        tertiary = softenedRole(appScheme.tertiary, artTertiary, 0.28f),
        onTertiary = appScheme.onTertiary,
        tertiaryContainer = softenedRole(appScheme.tertiaryContainer, artTertiaryContainer, 0.24f),
        onTertiaryContainer = appScheme.onTertiaryContainer,
        background = appScheme.background,
        onBackground = appScheme.onBackground,
        surface = softenedRole(appScheme.surface, artSurface, 0.12f),
        onSurface = appScheme.onSurface,
        surfaceVariant = softenedRole(appScheme.surfaceVariant, artSurfaceVariant, 0.20f),
        onSurfaceVariant = appScheme.onSurfaceVariant
    )
}

/** Convenience: a translucent version of an art-derived role. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
