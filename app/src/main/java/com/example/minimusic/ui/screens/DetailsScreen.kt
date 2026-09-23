package com.example.minimusic.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.SongDetails
import com.example.minimusic.data.readSongDetails
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.MiniMusicImageLoader
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.MiniMusicType
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The one gap the dialog uses between its three zones — title, identity block,
 * info cards. Keeping it single-sourced is what removes the dead space that
 * used to sit between the artwork and the first card.
 */
private val DetailSectionGap = 24.dp

/** Gap between the info cards themselves. */
private val DetailCardGap = 10.dp

/**
 * Dim behind the card. The dialog draws this itself (see the Dialog call in
 * [DetailsScreen]) so the scrim and the card share one animation instead of the
 * platform dim arriving on its own, un-animated, ahead of the card.
 */
private const val ScrimAlpha = 0.6f

/**
 * Shown in a card whose value has to be read out of the file. The read takes a
 * frame or two on a local file, so the dialog renders every card immediately and
 * these fill in behind the open animation — there is no spinner and no layout
 * change, because the dialog should never announce that it is working.
 */
private const val PendingValue = "—"

/**
 * Reads one value out of the resolved details, falling back to [orElse] and then
 * to "Unknown", or to [PendingValue] while the read is still in flight.
 */
private fun SongDetails?.readOrPending(
    extract: (SongDetails) -> String?,
    orElse: String? = null
): String {
    val snapshot = this ?: return PendingValue
    return extract(snapshot) ?: orElse ?: "Unknown"
}

/** The card's resting size inside that scrim. */
private const val CardWidthFraction = 0.96f
private const val CardHeightFraction = 0.82f

@Composable
fun DetailsScreen(
    song: Song,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // The dialog follows the app's Monet scheme rather than the playing song's
    // art colors: it is a document about the file, not part of the player
    // canvas (see the art-palette scoping note in Theme.kt).
    val scheme = MaterialTheme.colorScheme
    var details by remember(song.id) { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(song.id) {
        details = readSongDetails(context, song)
    }

    // One animation owns both directions: same duration, same easing, same
    // scale pair, so opening and closing are the same motion played forwards
    // and backwards. Closing runs the exit first and only then pops the route,
    // so the route change can never cut the surface off mid-flight.
    val revealed = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }
    val revealSpec = tween<Float>(
        durationMillis = MiniMusicMotion.dialogDurationMillis,
        easing = MiniMusicMotion.dialogEasing
    )
    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            revealed.animateTo(0f, revealSpec)
            onBack()
        }
    }
    LaunchedEffect(Unit) {
        revealed.animateTo(1f, revealSpec)
    }

    Dialog(
        onDismissRequest = { dismiss() },
        // The platform's default width is switched off so this composable owns
        // the whole screen: the dim behind the card is then drawn here and rides
        // the same value as the card's scale, instead of being a platform window
        // fade that only ever plays on the way in. That asymmetry — card
        // animated, dim snapped — is what made the two directions disagree.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Outside taps are handled below so they leave through the same
            // animated exit rather than an instant dismissal.
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            // No window enter/exit animation and no platform dim: this
            // composable is the single owner of both directions.
            dialogWindow?.setWindowAnimations(0)
            dialogWindow?.setDimAmount(0f)
            onDispose { }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = ScrimAlpha * revealed.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(CardWidthFraction)
                    .fillMaxHeight(CardHeightFraction)
                    .graphicsLayer {
                        val progress = revealed.value
                        alpha = progress
                        // Standard M3E dialog motion: a fade plus a small,
                        // UNIFORM scale from 0.85 — the one-axis vertical
                        // expand read as a stretch and double-drove with the
                        // scrim, which is the "weird expanding" feel. Uniform
                        // scale keeps the card's proportions fixed.
                        val scale = 0.85f + 0.15f * progress
                        scaleX = scale
                        scaleY = scale
                    }
                    // Swallow taps on the card so only the scrim dismisses.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface
                    )

                    Spacer(Modifier.height(DetailSectionGap))

                    DetailIdentity(song = song, scheme = scheme)

                    Spacer(Modifier.height(DetailSectionGap))

                    // Every card is rendered from the first frame: the fields
                    // MediaStore already knows (artist, album, duration) are
                    // real values, and the ones that need the file are read
                    // behind the open animation from a neutral placeholder.
                    // No loading indicator — the dialog never looks like it is
                    // waiting on something.
                    val loaded = details
                    Column(verticalArrangement = Arrangement.spacedBy(DetailCardGap)) {
                        DetailCard(Icons.Filled.Person, "Artist", song.artist, scheme)
                        DetailCard(Icons.Filled.Album, "Album", song.album, scheme)
                        DetailCard(
                            Icons.Filled.Badge,
                            "Album artist",
                            loaded.readOrPending({ it.albumArtist }, song.artist),
                            scheme
                        )
                        DetailCard(
                            Icons.Filled.Timer,
                            "Duration",
                            formatDuration(song.durationMs),
                            scheme
                        )
                        DetailCard(Icons.Filled.GraphicEq, "Genre", loaded.readOrPending({ it.genre }), scheme)
                        DetailCard(Icons.Filled.CalendarMonth, "Year", loaded.readOrPending({ it.year }), scheme)
                        DetailCard(
                            Icons.Filled.AudioFile,
                            "Quality",
                            if (loaded == null) PendingValue else audioQualityLabel(loaded),
                            scheme
                        )
                        DetailCard(
                            Icons.Filled.SdCard,
                            "Size",
                            if (loaded == null) PendingValue else formatFileSize(loaded.sizeBytes),
                            scheme
                        )
                        DetailCard(
                            Icons.Filled.Storage,
                            "Path",
                            loaded.readOrPending({ it.path }, song.contentUri.toString()),
                            scheme
                        )
                    }
                }
            }
        }
    }
}

/**
 * The dialog's identity block: artwork at a third of the available width, the
 * remaining two thirds carrying the song name above its artist. Both lines
 * marquee when they don't fit, so a long name is read completely instead of
 * being truncated to a different font size.
 */
@Composable
private fun DetailIdentity(
    song: Song,
    scheme: ColorScheme
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val artSize = maxWidth / 3f
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val artRequest = song.albumArtUri?.let { uri ->
                    remember(uri) {
                        ImageRequest.Builder(context)
                            .data(uri)
                            // No bitmap fade: the tile swaps the instant it
                            // decodes. On decode failure the note icon below
                            // takes over, never an empty tile.
                            .crossfade(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                }
                var artFailed by remember(song.albumArtUri) { mutableStateOf(false) }
                if (artRequest == null || artFailed) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    AsyncImage(
                        model = artRequest,
                        imageLoader = MiniMusicImageLoader.get(context),
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Error) artFailed = true
                        }
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 900,
                            initialDelayMillis = 700,
                            velocity = 19.dp
                        )
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 900,
                            initialDelayMillis = 700,
                            velocity = 19.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    scheme: ColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant
    ) {            Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                // A card whose value is still being read from the file shows the
                // skeleton pattern in miniature: the placeholder pulses, and the
                // real value then replaces it. Nothing moves and no spinner
                // appears — the card is complete from the first frame, it is
                // only its value that settles. The pulse rides the shared tone
                // rather than a per-card animation so every pending card breathes
                // together.
                val isPending = value == PendingValue
                val pendingPulse: State<Float>? = if (isPending) {
                    val transition = rememberInfiniteTransition(label = "detailCardPending")
                    transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                MiniMusicMotion.skeletonPulseMillis,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "detailCardPendingPulse"
                    )
                } else {
                    null
                }
                Text(
                    value,
                    // Tabular figures across the whole card: duration, sample
                    // rate, bitrate and file size are a column of values read
                    // against each other, which is the spec's case for
                    // monospaced digits. Non-numeric values are unaffected —
                    // the feature only equalises digit advances.
                    style = MiniMusicType.tabular(MaterialTheme.typography.bodyLarge),
                    color = scheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .then(
                            if (pendingPulse != null) {
                                // Read in the graphics layer, so the pulse never
                                // recomposes the dialog.
                                Modifier.graphicsLayer {
                                    alpha = 0.3f + 0.5f * pendingPulse.value
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

private fun audioQualityLabel(loaded: SongDetails): String {
    val format = loaded.formatInfo
    return buildList {
        format?.sampleRateHz?.let {
            add("${String.format(Locale.US, "%.1f", it / 1000f)} kHz")
        }
        format?.bitrateKbps?.let { add("$it kbps") }
        format?.mimeLabel?.let { add(it) }
    }.joinToString(" • ").ifBlank { "Unknown" }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatFileSize(sizeBytes: Long?): String {
    val bytes = sizeBytes ?: return "Unknown"
    if (bytes < 0L) return "Unknown"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (value >= 100.0 || unitIndex == 0) {
        "%.0f %s".format(Locale.US, value, units[unitIndex])
    } else {
        "%.1f %s".format(Locale.US, value, units[unitIndex])
    }
}
