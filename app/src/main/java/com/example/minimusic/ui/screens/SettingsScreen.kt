package com.example.minimusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalContext
import com.example.minimusic.data.AppSettings
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
                        subtitle = "Dark, Light, or Auto (system)",
                        options = listOf(
                            ThemeMode.DARK to "Dark",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.SYSTEM to "Auto"
                        ),
                        selected = settings.themeMode,
                        onSelect = onThemeModeChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "AMOLED black mode",
                        subtitle = "Use true-black surfaces when dark theme is active",
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
                        subtitle = "Show sample rate, bitrate, and format on the player",
                        checked = settings.showAudioQualityBadge,
                        onCheckedChange = onShowAudioQualityBadgeChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Centered title",
                        subtitle = "Center the current song title and artist in the player",
                        checked = settings.centeredTitle,
                        onCheckedChange = onCenteredTitleChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Behavior", Icons.Filled.Info)
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Resume on launch",
                        subtitle = "Retain queue order on launch; queue restoration is not active yet",
                        checked = settings.resumeOnLaunch,
                        onCheckedChange = onResumeOnLaunchChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Stop on dismiss",
                        subtitle = "Pause playback when the app is cleared from Recents; service hook pending",
                        checked = settings.stopOnDismiss,
                        onCheckedChange = onStopOnDismissChange
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Haptic feedback",
                        subtitle = "Vibrate on touches and drags; interaction hooks pending",
                        checked = settings.hapticFeedback,
                        onCheckedChange = onHapticFeedbackChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Audio", Icons.Filled.MusicNote)
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Crossfade",
                        subtitle = "Preference saved; Media3 crossfade is not active in the current player service",
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
                        subtitle = "Preference saved; channel mixing will be wired into playback next",
                        checked = settings.monoAudio,
                        onCheckedChange = onMonoAudioChange
                    )
                }
            }

            item {
                SettingsSectionHeader("Library", Icons.Filled.LibraryMusic)
                SettingsGroup {
                    SettingsSliderRow(
                        title = "Song minimum length",
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
                        supportingContent = { Text("Clear the current library view and reload local MediaStore files") },
                        leadingContent = { Icon(Icons.Filled.Refresh, contentDescription = null) },
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
                SettingsSectionHeader("Library Statistics", Icons.Filled.LibraryMusic)
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
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    TextButton(onClick = { onSelect(value) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = label,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps, enabled = enabled)
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
