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
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.PaletteStyle
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.material.color.utilities.Hct
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
    val onSurfaceVariant: Color,
    // Expressive "fixed" tones (Material 3): vivid, contrast-guaranteed
    // container/on pairs used for the transport controls — the same roles
    // PixelPlayer maps its buttons to.
    val primaryFixed: Color,
    val onPrimaryFixed: Color,
    val secondaryFixed: Color,
    val onSecondaryFixed: Color,
    val secondaryFixedDim: Color,
    val tertiaryFixed: Color,
    val onTertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val errorContainer: Color,
    val onErrorContainer: Color
)

private val artworkSeedCache = LinkedHashMap<Uri, Color>()
private const val ARTWORK_SEED_CACHE_MAX_SIZE = 64

/**
 * Last resolved seed, used as the next track's starting point. Without this,
 * every track change would flash the wallpaper fallback first and then snap
 * to the real art color mid-settle — a visible color stomp. Handing off from
 * the previous seed makes cross-track color a continuous glide instead.
 */
private var lastResolvedSeed: Color? = null

/**
 * Extracts one local album-art seed. The seed is only an input to the
 * Material dynamic scheme (see [artScheme]); no raw swatch is ever painted
 * directly into UI.
 */
@Composable
private fun rememberArtworkSeedColor(albumArtUri: Uri?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    val seed by produceState(
        initialValue = artworkSeedCache[albumArtUri] ?: lastResolvedSeed ?: fallback,
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
            lastResolvedSeed = resolved
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

private fun artScheme(seed: Color, isDark: Boolean, style: PaletteStyle): DynamicScheme {
    val hct = Hct.fromInt(seed.toArgb())
    // M3 dynamic color from in-app content: the seed (album art) generates an
    // accessible scheme; role mappings in the UI stay constant across styles.
    // Vibrant/Expressive/Fruit Salad spread the secondary and tertiary hues
    // around the source hue, giving each transport control a distinct vivid
    // tone (PixelPlayer's reference look).
    return when (style) {
        PaletteStyle.TONAL_SPOT -> SchemeTonalSpot(hct, isDark, 0.0)
        PaletteStyle.VIBRANT -> SchemeVibrant(hct, isDark, 0.0)
        PaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, isDark, 0.0)
        PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, isDark, 0.0)
    }
}
/**
 * Normalize a palette seed before it reaches Material scheme generation.
 * Album art can contain nearly-black or highly saturated pixels that are useful
 * in the cover itself but are too aggressive as UI colors. This preserves hue
 * while compressing saturation and keeping the seed in a usable tonal window.
 */
private fun normalizeArtworkSeed(color: Color): Color {
    val hsl = FloatArray(3)
    CoreColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.95f).coerceAtMost(0.92f)
    hsl[2] = hsl[2].coerceIn(0.20f, 0.80f)
    return Color(CoreColorUtils.HSLToColor(hsl))
}

@Composable
fun rememberArtColorRoles(
    albumArtUri: Uri?,
    style: PaletteStyle = PaletteStyle.TONAL_SPOT
): ArtColorRoles {
    val seed = rememberArtworkSeedColor(albumArtUri)
    val appScheme = MaterialTheme.colorScheme
    val isDark = appScheme.background.luminance() < 0.5f
    val scheme = remember(seed, isDark, style) { artScheme(seed, isDark, style) }
    val artPrimary = Color(scheme.getPrimary())
    val artPrimaryContainer = Color(scheme.getPrimaryContainer())
    val artSecondary = Color(scheme.getSecondary())
    val artSecondaryContainer = Color(scheme.getSecondaryContainer())
    val artTertiary = Color(scheme.getTertiary())
    val artTertiaryContainer = Color(scheme.getTertiaryContainer())
    // PixelPlayer uses the artwork primary-container as the player-area
    // canvas rather than the nearly-neutral Material background role. This
    // preserves the generated tonal palette while making the artwork identity
    // visible across the complete player surface in both light and dark mode.
    val artBackground = artPrimaryContainer
    val artOnBackground = Color(scheme.getOnPrimaryContainer())
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
        onSurfaceVariant = artOnSurfaceVariant,
        primaryFixed = Color(scheme.getPrimaryFixed()),
        onPrimaryFixed = Color(scheme.getOnPrimaryFixed()),
        secondaryFixed = Color(scheme.getSecondaryFixed()),
        onSecondaryFixed = Color(scheme.getOnSecondaryFixed()),
        secondaryFixedDim = Color(scheme.getSecondaryFixedDim()),
        tertiaryFixed = Color(scheme.getTertiaryFixed()),
        onTertiaryFixed = Color(scheme.getOnTertiaryFixed()),
        tertiaryFixedDim = Color(scheme.getTertiaryFixedDim()),
        surfaceContainerLowest = Color(scheme.getSurfaceContainerLowest()),
        surfaceContainer = Color(scheme.getSurfaceContainer()),
        surfaceContainerHigh = Color(scheme.getSurfaceContainerHigh()),
        errorContainer = Color(scheme.getErrorContainer()),
        onErrorContainer = Color(scheme.getOnErrorContainer())
    )
}

/** Interpolates every playback color role together for a coordinated track change. */
fun ArtColorRoles.lerpTo(target: ArtColorRoles, fraction: Float): ArtColorRoles {
    fun blend(from: Color, to: Color): Color =
        Color(ColorUtils.blendARGB(from.toArgb(), to.toArgb(), fraction.coerceIn(0f, 1f)))

    return ArtColorRoles(
        primary = blend(primary, target.primary),
        onPrimary = blend(onPrimary, target.onPrimary),
        primaryContainer = blend(primaryContainer, target.primaryContainer),
        onPrimaryContainer = blend(onPrimaryContainer, target.onPrimaryContainer),
        secondary = blend(secondary, target.secondary),
        onSecondary = blend(onSecondary, target.onSecondary),
        secondaryContainer = blend(secondaryContainer, target.secondaryContainer),
        onSecondaryContainer = blend(onSecondaryContainer, target.onSecondaryContainer),
        tertiary = blend(tertiary, target.tertiary),
        onTertiary = blend(onTertiary, target.onTertiary),
        tertiaryContainer = blend(tertiaryContainer, target.tertiaryContainer),
        onTertiaryContainer = blend(onTertiaryContainer, target.onTertiaryContainer),
        background = blend(background, target.background),
        onBackground = blend(onBackground, target.onBackground),
        surface = blend(surface, target.surface),
        onSurface = blend(onSurface, target.onSurface),
        surfaceVariant = blend(surfaceVariant, target.surfaceVariant),
        onSurfaceVariant = blend(onSurfaceVariant, target.onSurfaceVariant),
        primaryFixed = blend(primaryFixed, target.primaryFixed),
        onPrimaryFixed = blend(onPrimaryFixed, target.onPrimaryFixed),
        secondaryFixed = blend(secondaryFixed, target.secondaryFixed),
        onSecondaryFixed = blend(onSecondaryFixed, target.onSecondaryFixed),
        secondaryFixedDim = blend(secondaryFixedDim, target.secondaryFixedDim),
        tertiaryFixed = blend(tertiaryFixed, target.tertiaryFixed),
        onTertiaryFixed = blend(onTertiaryFixed, target.onTertiaryFixed),
        tertiaryFixedDim = blend(tertiaryFixedDim, target.tertiaryFixedDim),
        surfaceContainerLowest = blend(surfaceContainerLowest, target.surfaceContainerLowest),
        surfaceContainer = blend(surfaceContainer, target.surfaceContainer),
        surfaceContainerHigh = blend(surfaceContainerHigh, target.surfaceContainerHigh),
        errorContainer = blend(errorContainer, target.errorContainer),
        onErrorContainer = blend(onErrorContainer, target.onErrorContainer)
    )
}

/** Convenience: a translucent version of an art-derived role. */
fun Color.atAlpha(alpha: Float): Color = copy(alpha = alpha)
