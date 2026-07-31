package com.example.minimusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.AppSettings
import com.example.minimusic.data.ThemeMode

private data class SettingsSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    appVersion: String,
    onBack: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutoShowLyricsChange: (Boolean) -> Unit,
    onResumeOnLaunchChange: (Boolean) -> Unit,
    onMinDurationChange: (Int) -> Unit,
    onRescanLibrary: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {

            item { SectionHeader("Appearance", Icons.Filled.Palette) }
            item {
                SettingsSwitchRow(
                    title = "Dynamic color",
                    subtitle = "Match colors to your wallpaper (Android 12+)",
                    checked = settings.dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange
                )
            }
            item {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            item {
                ThemeModeSelector(current = settings.themeMode, onSelect = onThemeModeChange)
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

            item { SectionHeader("Player", Icons.Filled.MusicNote) }
            item {
                SettingsSwitchRow(
                    title = "Open lyrics automatically",
                    subtitle = "Show the lyrics panel by default when opening a track",
                    checked = settings.autoShowLyrics,
                    onCheckedChange = onAutoShowLyricsChange
                )
            }
            item {
                SettingsSwitchRow(
                    title = "Resume on launch",
                    subtitle = "Reopen your last queue when you start the app",
                    checked = settings.resumeOnLaunch,
                    onCheckedChange = onResumeOnLaunchChange
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

            item { SectionHeader("Content", Icons.Filled.LibraryMusic) }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Minimum track length", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Hide clips shorter than ${settings.minDurationSeconds}s (ringtones, voice memos)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.minDurationSeconds.toFloat(),
                        onValueChange = { onMinDurationChange(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 11
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Rescan library") },
                    supportingContent = { Text("Pick up newly added or removed files") },
                    leadingContent = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    trailingContent = { TextButton(onClick = onRescanLibrary) { Text("Scan") } }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

            item { SectionHeader("About", Icons.Filled.Info) }
            item {
                ListItem(
                    headlineContent = { Text("MiniMusic") },
                    supportingContent = { Text("Version $appVersion") },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Built with") },
                    supportingContent = { Text("Jetpack Compose, Media3 (ExoPlayer) and Material 3 Expressive. 100% offline — no analytics, no network access.") },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun ThemeModeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = current == mode
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f)
            ) {
                TextButton(onClick = { onSelect(mode) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
