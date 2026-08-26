package com.example.minimusic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val AllowedMinimumDurations = listOf(0, 15, 30, 45, 60)

private fun nearestMinimumDuration(seconds: Int): Int =
    AllowedMinimumDurations.minByOrNull { kotlin.math.abs(it - seconds) } ?: 15

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HighRefreshRate(val label: String, val preferredHz: Float) {
    MATCH_DISPLAY_MAX("Match display max", 0f),
    HZ_90("90Hz", 90f),
    HZ_120("120Hz", 120f),
    HZ_144("144Hz", 144f)
}

enum class AlbumArtQuality(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    ORIGINAL("Original")
}

data class AppSettings(
    val dynamicColorEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlackMode: Boolean = false,
    val highRefreshRate: HighRefreshRate = HighRefreshRate.MATCH_DISPLAY_MAX,
    val showAudioQualityBadge: Boolean = true,
    val centeredTitle: Boolean = false,
    val albumArtQuality: AlbumArtQuality = AlbumArtQuality.ORIGINAL,
    val resumeOnLaunch: Boolean = true,
    val stopOnDismiss: Boolean = false,
    val hapticFeedback: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 5,
    val monoAudio: Boolean = false,
    val minDurationSeconds: Int = 15,
    val telegramSupportAddress: String = ""
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
        val AMOLED_BLACK_MODE = booleanPreferencesKey("amoled_black_mode")
        val HIGH_REFRESH_RATE = stringPreferencesKey("high_refresh_rate")
        val SHOW_AUDIO_QUALITY_BADGE = booleanPreferencesKey("show_audio_quality_badge")
        val CENTERED_TITLE = booleanPreferencesKey("centered_title")
        val ALBUM_ART_QUALITY = stringPreferencesKey("album_art_quality")
        val RESUME_ON_LAUNCH = booleanPreferencesKey("resume_on_launch")
        val STOP_ON_DISMISS = booleanPreferencesKey("stop_on_dismiss")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val MONO_AUDIO = booleanPreferencesKey("mono_audio")
        val MIN_DURATION_SECONDS = intPreferencesKey("min_duration_seconds")
        val TELEGRAM_SUPPORT_ADDRESS = stringPreferencesKey("telegram_support_address")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            amoledBlackMode = prefs[Keys.AMOLED_BLACK_MODE] ?: false,
            highRefreshRate = prefs[Keys.HIGH_REFRESH_RATE]?.let {
                runCatching { HighRefreshRate.valueOf(it) }.getOrNull()
            } ?: HighRefreshRate.MATCH_DISPLAY_MAX,
            showAudioQualityBadge = prefs[Keys.SHOW_AUDIO_QUALITY_BADGE] ?: true,
            centeredTitle = prefs[Keys.CENTERED_TITLE] ?: false,
            albumArtQuality = prefs[Keys.ALBUM_ART_QUALITY]?.let {
                runCatching { AlbumArtQuality.valueOf(it) }.getOrNull()
            } ?: AlbumArtQuality.ORIGINAL,
            resumeOnLaunch = prefs[Keys.RESUME_ON_LAUNCH] ?: true,
            stopOnDismiss = prefs[Keys.STOP_ON_DISMISS] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
            crossfadeSeconds = (prefs[Keys.CROSSFADE_SECONDS] ?: 5).coerceIn(2, 10),
            monoAudio = prefs[Keys.MONO_AUDIO] ?: false,
            minDurationSeconds = nearestMinimumDuration(prefs[Keys.MIN_DURATION_SECONDS] ?: 15),
            telegramSupportAddress = prefs[Keys.TELEGRAM_SUPPORT_ADDRESS].orEmpty()
        )
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }


    suspend fun setAmoledBlackMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AMOLED_BLACK_MODE] = enabled }
    }

    suspend fun setHighRefreshRate(rate: HighRefreshRate) {
        context.dataStore.edit { it[Keys.HIGH_REFRESH_RATE] = rate.name }
    }

    suspend fun setShowAudioQualityBadge(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_AUDIO_QUALITY_BADGE] = enabled }
    }

    suspend fun setCenteredTitle(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CENTERED_TITLE] = enabled }
    }

    suspend fun setAlbumArtQuality(quality: AlbumArtQuality) {
        context.dataStore.edit { it[Keys.ALBUM_ART_QUALITY] = quality.name }
    }

    suspend fun setResumeOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_LAUNCH] = enabled }
    }

    suspend fun setStopOnDismiss(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STOP_ON_DISMISS] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(2, 10) }
    }

    suspend fun setMonoAudio(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONO_AUDIO] = enabled }
    }

    suspend fun setMinDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_DURATION_SECONDS] = nearestMinimumDuration(seconds) }
    }

    suspend fun setTelegramSupportAddress(address: String) {
        context.dataStore.edit { it[Keys.TELEGRAM_SUPPORT_ADDRESS] = address.trim() }
    }
}
