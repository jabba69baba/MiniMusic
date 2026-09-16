package com.example.minimusic.ui.screens

import kotlin.math.roundToInt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import android.content.Context
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.components.performMiniMusicHaptic
import com.example.minimusic.data.AppSettings
import com.example.minimusic.data.PaletteStyle
import com.example.minimusic.data.ThemeMode
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.viewmodel.LibraryUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: AppSettings,
    libraryState: LibraryUiState,
    appVersion: String,
    onBack: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAlbumArtPaletteStyleChange: (PaletteStyle) -> Unit,
    onAmoledBlackModeChange: (Boolean) -> Unit,
    onShowAudioQualityBadgeChange: (Boolean) -> Unit,
    onCenteredTitleChange: (Boolean) -> Unit,
    onResumeOnLaunchChange: (Boolean) -> Unit,
    onStopOnDismissChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeSecondsChange: (Int) -> Unit,
    onMonoAudioChange: (Boolean) -> Unit,
    onMinDurationChange: (Int) -> Unit,
    onRescanLibrary: () -> Unit
) {
    val totalDurationMs = libraryState.allSongs.sumOf { it.durationMs }
    val context = LocalContext.current
    val totalSizeBytes by produceState<Long?>(initialValue = null, libraryState.allSongs) {
        value = withContext(Dispatchers.IO) {
            libraryState.allSongs.sumOf { songSizeBytes(context, it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance", Icons.Filled.Palette)
                SettingsGroup {
                    SettingsChoiceRow(
                        title = "App theme",
                        subtitle = "Choose Light, Dark, or Auto",
                        options = listOf(
                            ThemeMode.SYSTEM to "Auto",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        ),
                        selected = settings.themeMode,
                        onSelect = onThemeModeChange
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        title = "Album art palette",
                        subtitle = "Color style derived from the playing track's art",
                        options = listOf(
                            PaletteStyle.TONAL_SPOT to "Tonal Spot",
                            PaletteStyle.VIBRANT to "Vibrant",
                            PaletteStyle.EXPRESSIVE to "Expressive",
                            PaletteStyle.FRUIT_SALAD to "Fruit Salad"
                        ),
                        selected = settings.albumArtPaletteStyle,
                        onSelect = onAlbumArtPaletteStyleChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "AMOLED dark mode",
                        subtitle = "Use pure-black surfaces in Dark mode",
                        checked = settings.amoledBlackMode,
                        onCheckedChange = onAmoledBlackModeChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Player UI", Icons.Filled.MusicNote)
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Audio quality badge",
                        subtitle = "Show format, bitrate, and sample rate",
                        checked = settings.showAudioQualityBadge,
                        onCheckedChange = onShowAudioQualityBadgeChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Centered title",
                        subtitle = "Center the player title and artist",
                        checked = settings.centeredTitle,
                        onCheckedChange = onCenteredTitleChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Behavior", Icons.Filled.Settings)
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Resume on launch",
                        subtitle = "Restore the last queue and position on launch",
                        checked = settings.resumeOnLaunch,
                        onCheckedChange = onResumeOnLaunchChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Stop on dismiss",
                        subtitle = "Pause playback when the app is dismissed",
                        checked = settings.stopOnDismiss,
                        onCheckedChange = onStopOnDismissChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Haptic feedback",
                        subtitle = "Use haptics for key interactions",
                        checked = settings.hapticFeedback,
                        onCheckedChange = onHapticFeedbackChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Audio", Icons.Filled.GraphicEq)
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Crossfade",
                        subtitle = "Blend adjacent tracks · not active yet",
                        checked = settings.crossfadeEnabled,
                        onCheckedChange = onCrossfadeEnabledChange
                    )
                    SettingsDivider()
                    SettingsSliderRow(
                        title = "Crossfade duration",
                        subtitle = "${settings.crossfadeSeconds} seconds",
                        value = settings.crossfadeSeconds.toFloat(),
                        valueRange = 2f..10f,
                        steps = 7,
                        enabled = settings.crossfadeEnabled,
                        onValueChange = { onCrossfadeSecondsChange(it.toInt()) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Mono Audio",
                        subtitle = "Mix left and right channels to mono",
                        checked = settings.monoAudio,
                        onCheckedChange = onMonoAudioChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Library", Icons.Filled.LibraryMusic)
                SettingsGroup {
                    SettingsSliderRow(
                        title = "Track minimum length",
                        subtitle = if (settings.minDurationSeconds == 0) "No minimum" else "${settings.minDurationSeconds} seconds",
                        value = settings.minDurationSeconds.toFloat(),
                        valueRange = 0f..60f,
                        steps = 3,
                        enabled = true,
                        onValueChange = { onMinDurationChange((it / 15f).toInt() * 15) }
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Rescan library") },
                        trailingContent = {
                            if (libraryState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else {
                                IconButton(onClick = onRescanLibrary) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan library")
                                }
                            }
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader("Library Statistics", Icons.Filled.BarChart)
                SettingsGroup {
                    SettingsValueRow("Songs loaded", libraryState.allSongs.size.toString())
                    SettingsDivider()
                    SettingsValueRow("Albums loaded", libraryState.albums.size.toString())
                    SettingsDivider()
                    SettingsValueRow("Artists loaded", libraryState.artists.size.toString())
                    SettingsDivider()
                    SettingsValueRow("Total duration", formatTotalDuration(totalDurationMs))
                    SettingsDivider()
                    SettingsValueRow(
                        "Total size",
                        totalSizeBytes?.let(::formatTotalSize) ?: "Calculating…"
                    )
                }
            }

            item {
                SettingsSectionHeader("About", Icons.Filled.Info)
                SettingsGroup {
                    ListItem(
                        headlineContent = { Text("The App") },
                        supportingContent = { Text("MiniMusic · Version $appVersion · offline and FOSS") }
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("The Developer") },
                        supportingContent = { Text("MiniMusic is developed as an offline, open-source music player") }
                    )
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Support the dev using") },
                        supportingContent = { Text("Telegram ID: Not configured") },
                        trailingContent = { Text("Not configured", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val view = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = {
            if (hapticsEnabled) view.performMiniMusicHaptic()
            onCheckedChange(it)
        }) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun <T> SettingsChoiceRow(
    title: String,
    subtitle: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    val selectedLabel = options.firstOrNull { it.first == selected }?.second.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hapticsEnabled) view.performMiniMusicHaptic()
                    expanded = true
                },
            headlineContent = { Text(title) },
            trailingContent = {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(180.dp),
            offset = DpOffset((maxWidth - 180.dp).coerceAtLeast(0.dp), 0.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        if (hapticsEnabled) view.performMiniMusicHaptic()
                        onSelect(value)
                    },
                    modifier = if (isSelected) {
                        Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val hapticsEnabled = LocalMiniMusicHaptics.current
    val hapticView = LocalView.current
    var lastHapticStep by remember(value) { mutableStateOf(value.roundToInt()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            modifier = Modifier.height(32.dp),
            value = value,
            onValueChange = {
                val step = it.roundToInt()
                if (hapticsEnabled && step != lastHapticStep) hapticView.performMiniMusicHaptic()
                lastHapticStep = step
                onValueChange(it)
            },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            thumb = {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                )
            }
        )
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

private fun songSizeBytes(context: Context, song: Song): Long {
    val mediaStoreSize = runCatching {
        context.contentResolver.query(
            song.contentUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                cursor.getLong(0).takeIf { it >= 0L }
            } else {
                null
            }
        }
    }.getOrNull()

    return mediaStoreSize ?: runCatching {
        context.contentResolver.openAssetFileDescriptor(song.contentUri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }
        }
    }.getOrNull() ?: 0L
}

private fun formatTotalSize(bytes: Long): String {
    if (bytes < 1024L) return "${bytes.coerceAtLeast(0L)} B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (value >= 100.0 || unitIndex == 0) {
        String.format(java.util.Locale.US, "%.0f %s", value, units[unitIndex])
    } else {
        String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

private fun formatTotalDuration(durationMs: Long): String {
    val totalMinutes = durationMs.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}
