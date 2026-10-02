package com.audiovault.app.ui.home

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.core.intelligence.AudioVaultNotificationListener
import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.service.AudioCaptureService
import com.audiovault.app.core.service.MonitoringState
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioVaultApplication).container
    private val recordingRepository: RecordingRepository = container.recordingRepository
    val playerEngine: AudioPlayerEngine = container.playerEngine

    val monitoringState: StateFlow<MonitoringState> = AudioCaptureService.monitoringState
    val currentDetectedSong: StateFlow<SongMetadata?> = AudioCaptureService.currentDetectedSong
    val playerState: StateFlow<PlayerState> = playerEngine.playerState

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    init {
        checkNotificationAccess()
    }

    fun checkNotificationAccess() {
        _isNotificationAccessGranted.value =
            AudioVaultNotificationListener.isNotificationAccessGranted(getApplication())
    }

    val recentRecordings: StateFlow<List<RecordingEntity>> =
        recordingRepository.recordingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val recordingCount: StateFlow<Int> =
        recordingRepository.recordingCountFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    fun startMonitoring(resultCode: Int, data: Intent) {
        AudioCaptureService.startService(getApplication(), resultCode, data)
    }

    fun stopMonitoring() {
        AudioCaptureService.stopService(getApplication())
    }

    fun playPauseRecording(recording: RecordingEntity) {
        if (playerState.value.currentRecording?.id == recording.id) {
            playerEngine.togglePlayPause()
        } else {
            playerEngine.play(recording)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playerState.value.currentRecording?.id == recording.id) {
                playerEngine.stopAndClose()
            }
            recordingRepository.deleteRecording(recording)
        }
    }

    fun createNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}
