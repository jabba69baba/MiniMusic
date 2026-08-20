package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.BuildConfig
import com.example.minimusic.MainApplication
import com.example.minimusic.data.AppSettings
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

    fun setAutoShowLyrics(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoShowLyrics(enabled)
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
}
