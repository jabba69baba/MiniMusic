package com.example.minimusic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val dynamicColorEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoShowLyrics: Boolean = false,
    val resumeOnLaunch: Boolean = true,
    val minDurationSeconds: Int = 20,
    val cornerRadiusDp: Int = 20
)

private val Context.dataStore by preferencesDataStore(name = "minimusic_settings")

/**
 * Reads and writes user-facing preferences. Backed by Jetpack DataStore so values
 * survive process death and are exposed as a Flow the UI can collect directly.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SHOW_LYRICS = booleanPreferencesKey("auto_show_lyrics")
        val RESUME_ON_LAUNCH = booleanPreferencesKey("resume_on_launch")
        val MIN_DURATION_SECONDS = intPreferencesKey("min_duration_seconds")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            autoShowLyrics = prefs[Keys.AUTO_SHOW_LYRICS] ?: false,
            resumeOnLaunch = prefs[Keys.RESUME_ON_LAUNCH] ?: true,
            minDurationSeconds = prefs[Keys.MIN_DURATION_SECONDS] ?: 20,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 20
        )
    }

    suspend fun setCornerRadiusDp(dp: Int) {
        context.dataStore.edit { it[Keys.CORNER_RADIUS_DP] = dp }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoShowLyrics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SHOW_LYRICS] = enabled }
    }

    suspend fun setResumeOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_LAUNCH] = enabled }
    }

    suspend fun setMinDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_DURATION_SECONDS] = seconds }
    }
}
