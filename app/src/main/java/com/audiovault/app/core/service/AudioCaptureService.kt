package com.audiovault.app.core.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.core.audio.AudioCaptureConfig
import com.audiovault.app.core.audio.PlaybackAudioCapture
import com.audiovault.app.core.intelligence.AudioVaultNotificationListener
import com.audiovault.app.core.intelligence.CoverArtFetcher
import com.audiovault.app.core.intelligence.MediaSessionTracker
import com.audiovault.app.core.intelligence.SongMetadata
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.data.storage.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground Service that captures eligible playback audio using MediaProjection,
 * intelligent MediaSession song tracking, and real-time acoustic analysis.
 */
class AudioCaptureService : Service() {

    companion object {
        private const val TAG = "AudioVault/Service"

        const val ACTION_START_CAPTURE = "com.audiovault.app.ACTION_START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.audiovault.app.ACTION_STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _monitoringState = MutableStateFlow<MonitoringState>(MonitoringState.Idle)
        val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

        private val _currentDetectedSong = MutableStateFlow<SongMetadata?>(null)
        val currentDetectedSong: StateFlow<SongMetadata?> = _currentDetectedSong.asStateFlow()

        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_START_CAPTURE
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_STOP_CAPTURE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationManager: ServiceNotificationManager
    private lateinit var fileManager: FileManager
    private lateinit var recordingRepository: RecordingRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private var mediaProjection: MediaProjection? = null
    private var audioCapture: PlaybackAudioCapture? = null
    private var mediaSessionTracker: MediaSessionTracker? = null
    private var isServiceRunning = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(TAG, "MediaProjection session revoked by system.")
            stopCapture(MonitoringState.Error("MediaProjection permission was revoked."))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as AudioVaultApplication
        notificationManager = ServiceNotificationManager(this)
        fileManager = app.container.fileManager
        recordingRepository = app.container.recordingRepository
        userPreferencesRepository = app.container.userPreferencesRepository

        mediaSessionTracker = MediaSessionTracker(this) { prevSong, newSong ->
            _currentDetectedSong.value = newSong
            serviceScope.launch {
                val settings = userPreferencesRepository.userSettingsFlow.first()
                if (settings.isSmartSongSplitEnabled) {
                    audioCapture?.onSongTransition(newSong)
                }
            }
            // Pre-fetch cover art and artist photo in background immediately when song is recognized
            serviceScope.launch(Dispatchers.IO) {
                if (!newSong.title.isNullOrBlank()) {
                    CoverArtFetcher.fetchAndCacheCoverArt(this@AudioCaptureService, newSong.title, newSong.artist)
                    if (!newSong.artist.isNullOrBlank()) {
                        CoverArtFetcher.fetchAndCacheArtistArt(this@AudioCaptureService, newSong.artist, newSong.title)
                    }
                }
            }
        }

        AudioVaultNotificationListener.setOnMetadataListener { song ->
            mediaSessionTracker?.updateFromExternalSource(song)
        }

        Log.d(TAG, "AudioCaptureService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startCapture(resultCode, resultData)
                } else {
                    Log.e(TAG, "Invalid result code or projection data.")
                    _monitoringState.value = MonitoringState.Error("Capture permission was not granted.")
                    stopSelf()
                }
            }

            ACTION_STOP_CAPTURE -> {
                Log.d(TAG, "Received ACTION_STOP_CAPTURE.")
                stopCapture(MonitoringState.Idle)
            }

            else -> {
                if (!isServiceRunning) {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        if (isServiceRunning) return

        try {
            val notification = notificationManager.buildNotification(isRecording = false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ServiceNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(ServiceNotificationManager.NOTIFICATION_ID, notification)
            }

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = projectionManager.getMediaProjection(resultCode, data)

            if (projection == null) {
                _monitoringState.value = MonitoringState.Error("Unable to initialize MediaProjection.")
                stopSelf()
                return
            }

            mediaProjection = projection
            projection.registerCallback(projectionCallback, null)
            mediaSessionTracker?.startTracking()

            serviceScope.launch {
                val settings = userPreferencesRepository.userSettingsFlow.first()
                val captureConfig = AudioCaptureConfig(
                    silenceTimeoutSeconds = settings.silenceTimeoutSeconds,
                    minRecordingDurationSeconds = settings.minRecordingDurationSeconds,
                    bitrateKbps = settings.audioQualityBitrateKbps,
                    isSmartSongSplitEnabled = settings.isSmartSongSplitEnabled,
                    isNoiseAndAdFilterEnabled = settings.isNoiseAndAdFilterEnabled
                )

                var lastUiUpdateTimeMs = 0L
                var lastRecordingState = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val capture = PlaybackAudioCapture(
                        mediaProjection = projection,
                        config = captureConfig,
                        fileProvider = { song -> fileManager.createNewRecordingFile(song) },
                        currentSongProvider = { _currentDetectedSong.value },
                        onRecordingSaved = { file, durationMs, song ->
                            serviceScope.launch(Dispatchers.IO) {
                                val savedId = recordingRepository.saveRecording(
                                    file = file,
                                    durationMs = durationMs,
                                    metadata = song,
                                    bitrateKbps = captureConfig.bitrateKbps
                                )
                                if (savedId > 0 && song != null) {
                                    val art = CoverArtFetcher.fetchAndCacheCoverArt(this@AudioCaptureService, song.title, song.artist)
                                    if (art != null) {
                                        recordingRepository.updateAlbumArtPath(savedId, art)
                                    }
                                    if (!song.artist.isNullOrBlank()) {
                                        CoverArtFetcher.fetchAndCacheArtistArt(this@AudioCaptureService, song.artist, song.title)
                                    }
                                }
                            }
                        },
                        onStateChanged = { isRecording, durationMs, audioLevelDb, isMusic ->
                            val now = System.currentTimeMillis()
                            val stateFlipped = isRecording != lastRecordingState
                            val timeElapsed = (now - lastUiUpdateTimeMs) >= 500L

                            if (stateFlipped || timeElapsed) {
                                lastUiUpdateTimeMs = now
                                lastRecordingState = isRecording

                                if (isRecording) {
                                    _monitoringState.value = MonitoringState.Recording(
                                        durationMs = durationMs,
                                        audioLevelDb = audioLevelDb
                                    )
                                    val songTitle = _currentDetectedSong.value?.displayTitle
                                    notificationManager.updateNotification(
                                        isRecording = true,
                                        recordingDurationMs = durationMs,
                                        songTitle = songTitle
                                    )
                                } else {
                                    if (_monitoringState.value !is MonitoringState.WaitingForAudio) {
                                        _monitoringState.value = MonitoringState.WaitingForAudio
                                        notificationManager.updateNotification(isRecording = false)
                                    }
                                }
                            }
                        },
                        onError = { message ->
                            _monitoringState.value = MonitoringState.Error(message)
                        }
                    )
                    audioCapture = capture
                    capture.start()
                    isServiceRunning = true
                    _monitoringState.value = MonitoringState.Monitoring
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture service", e)
            _monitoringState.value = MonitoringState.Error(e.message ?: "Failed to start capture.")
            stopCapture(MonitoringState.Error(e.message ?: "Failed to start capture."))
        }
    }

    private fun stopCapture(nextState: MonitoringState) {
        if (!isServiceRunning && mediaProjection == null) {
            _monitoringState.value = nextState
            stopSelf()
            return
        }

        _monitoringState.value = MonitoringState.Stopping
        isServiceRunning = false

        try {
            audioCapture?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio capture: ${e.message}")
        }
        audioCapture = null

        try {
            mediaSessionTracker?.stopTracking()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaSessionTracker: ${e.message}")
        }
        _currentDetectedSong.value = null

        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaProjection: ${e.message}")
        }
        mediaProjection = null

        _monitoringState.value = nextState
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "AudioCaptureService stopped cleanly.")
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioVaultNotificationListener.setOnMetadataListener(null)
        stopCapture(MonitoringState.Idle)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
