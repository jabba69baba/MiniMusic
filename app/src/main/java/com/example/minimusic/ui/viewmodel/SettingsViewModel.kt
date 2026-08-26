package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.BuildConfig
import com.example.minimusic.MainApplication
import com.example.minimusic.data.AlbumArtQuality
import com.example.minimusic.data.AppSettings
import com.example.minimusic.data.HighRefreshRate
import com.example.minimusic.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MainApplication).settingsRepository

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val appVersion: String = BuildConfig.VERSION_NAME

    fun setDynamicColorEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColorEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.setThemeMode(mode)
    }

    fun setAmoledBlackMode(enabled: Boolean) = viewModelScope.launch {
        repository.setAmoledBlackMode(enabled)
    }

    fun setHighRefreshRate(rate: HighRefreshRate) = viewModelScope.launch {
        repository.setHighRefreshRate(rate)
    }

    fun setResumeOnLaunch(enabled: Boolean) = viewModelScope.launch {
        repository.setResumeOnLaunch(enabled)
    }

    fun setMinDurationSeconds(seconds: Int) = viewModelScope.launch {
        repository.setMinDurationSeconds(seconds)
    }

    fun setShowAudioQualityBadge(enabled: Boolean) = viewModelScope.launch {
        repository.setShowAudioQualityBadge(enabled)
    }

    fun setCenteredTitle(enabled: Boolean) = viewModelScope.launch {
        repository.setCenteredTitle(enabled)
    }

    fun setAlbumArtQuality(quality: AlbumArtQuality) = viewModelScope.launch {
        repository.setAlbumArtQuality(quality)
    }

    fun setStopOnDismiss(enabled: Boolean) = viewModelScope.launch {
        repository.setStopOnDismiss(enabled)
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        repository.setHapticFeedback(enabled)
    }

    fun setCrossfadeEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setCrossfadeEnabled(enabled)
    }

    fun setCrossfadeSeconds(seconds: Int) = viewModelScope.launch {
        repository.setCrossfadeSeconds(seconds)
    }

    fun setMonoAudio(enabled: Boolean) = viewModelScope.launch {
        repository.setMonoAudio(enabled)
    }
}
