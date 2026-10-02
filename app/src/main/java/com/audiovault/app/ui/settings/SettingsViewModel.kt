package com.audiovault.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.data.repository.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioVaultApplication).container
    private val preferencesRepository: UserPreferencesRepository = container.userPreferencesRepository
    private val recordingRepository: RecordingRepository = container.recordingRepository

    val settings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val recordingCount: StateFlow<Int> = recordingRepository.recordingCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalStorageBytes: StateFlow<Long> = recordingRepository.totalStorageBytesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    fun setAutoRecord(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoRecordEnabled(enabled) }
    }

    fun setSilenceTimeout(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setSilenceTimeout(seconds) }
    }

    fun setMinDuration(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setMinRecordingDuration(seconds) }
    }

    fun setAudioBitrate(bitrateKbps: Int) {
        viewModelScope.launch { preferencesRepository.setAudioQualityBitrate(bitrateKbps) }
    }

    fun setSmartSongSplit(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSmartSongSplitEnabled(enabled) }
    }

    fun setNoiseAndAdFilter(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNoiseAndAdFilterEnabled(enabled) }
    }

    fun setAutoCleanupDays(days: Int) {
        viewModelScope.launch { preferencesRepository.setAutoCleanupDays(days) }
    }

    fun clearAllRecordings() {
        viewModelScope.launch {
            recordingRepository.clearAllRecordings()
        }
    }
}
