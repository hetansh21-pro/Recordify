package com.audiovault.app.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.audiovault.app.data.local.entity.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong

enum class PlayerPlaybackStatus {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    BUFFERING,
    SEEKING,
    ENDED,
    ERROR,
    STOPPED
}

data class PlayerState(
    val currentRecording: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val status: PlayerPlaybackStatus = PlayerPlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isCompleted: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)

class AudioPlayerEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioVault/Player"
        private const val PROGRESS_UPDATE_INTERVAL_MS = 200L
    }

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // Rapid song switching request ID tracking
    private val activePlaybackRequestId = AtomicLong(0L)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _streamingMetrics = MutableStateFlow(StreamingMetrics())
    val streamingMetrics: StateFlow<StreamingMetrics> = _streamingMetrics.asStateFlow()

    private var isDataSaverActive: Boolean = false
    private var currentBufferConfig: StreamingBufferConfig = StreamingBufferConfig.NORMAL

    var onSongCompleted: ((RecordingEntity) -> Unit)? = null
    var onProgressMilestone: ((RecordingEntity, Float) -> Unit)? = null
    var onPlaybackError: ((RecordingEntity?, String) -> Unit)? = null

    private var hasPassed25 = false
    private var hasPassed50 = false
    private var hasPassed75 = false

    // Real-time network byte-transfer listener on ExoPlayer HTTP DataSource
    private val networkTransferListener = object : TransferListener {
        override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}

        override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
            if (isNetwork) {
                _streamingMetrics.update { it.copy(activeRangeRequestsCount = it.activeRangeRequestsCount + 1) }
            }
        }

        override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
            if (isNetwork && bytesTransferred > 0) {
                _streamingMetrics.update {
                    it.copy(
                        sessionBytesStreamed = it.sessionBytesStreamed + bytesTransferred,
                        currentSongBytesStreamed = it.currentSongBytesStreamed + bytesTransferred
                    )
                }
            }
        }

        override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update {
                it.copy(
                    isPlaying = isPlaying,
                    status = when {
                        isPlaying -> PlayerPlaybackStatus.PLAYING
                        it.status == PlayerPlaybackStatus.BUFFERING -> PlayerPlaybackStatus.BUFFERING
                        it.status == PlayerPlaybackStatus.ENDED -> PlayerPlaybackStatus.ENDED
                        else -> PlayerPlaybackStatus.PAUSED
                    }
                )
            }
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    _playerState.update {
                        it.copy(
                            status = if (it.isPlaying) PlayerPlaybackStatus.PLAYING else PlayerPlaybackStatus.PAUSED,
                            durationMs = if (duration > 0) duration else (it.currentRecording?.durationMs ?: 0L),
                            bufferedPositionMs = exoPlayer?.bufferedPosition ?: 0L,
                            isBuffering = false,
                            isCompleted = false,
                            error = null
                        )
                    }
                }
                Player.STATE_BUFFERING -> {
                    _playerState.update {
                        it.copy(
                            status = PlayerPlaybackStatus.BUFFERING,
                            isBuffering = true,
                            bufferedPositionMs = exoPlayer?.bufferedPosition ?: 0L
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    val finishedRecording = _playerState.value.currentRecording
                    _playerState.update {
                        it.copy(
                            isPlaying = false,
                            status = PlayerPlaybackStatus.ENDED,
                            isCompleted = true,
                            isBuffering = false,
                            currentPositionMs = it.durationMs
                        )
                    }
                    stopProgressTracker()
                    if (finishedRecording != null) {
                        onSongCompleted?.invoke(finishedRecording)
                    }
                }
                Player.STATE_IDLE -> {
                    _playerState.update {
                        it.copy(
                            status = if (it.error != null) PlayerPlaybackStatus.ERROR else PlayerPlaybackStatus.IDLE,
                            isBuffering = false
                        )
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer error occurred: ${error.message}", error)
            val current = _playerState.value.currentRecording
            _playerState.update {
                it.copy(
                    status = PlayerPlaybackStatus.ERROR,
                    isPlaying = false,
                    isBuffering = false,
                    error = error.localizedMessage ?: "Playback error"
                )
            }
            onPlaybackError?.invoke(current, error.localizedMessage ?: "Playback error")
        }
    }

    /**
     * Reconfigures LoadControl and HTTP DataSource based on current Data Saver policy.
     */
    fun updateDataSaverMode(isDataSaver: Boolean) {
        if (this.isDataSaverActive == isDataSaver && exoPlayer != null) return
        this.isDataSaverActive = isDataSaver
        this.currentBufferConfig = if (isDataSaver) StreamingBufferConfig.DATA_SAVER else StreamingBufferConfig.NORMAL

        _streamingMetrics.update {
            it.copy(
                isDataSaverActive = isDataSaver,
                currentBitrateKbps = if (isDataSaver) 96 else 320
            )
        }

        // Recreate player with updated buffer parameters
        val currentRec = _playerState.value.currentRecording
        val currentPos = _playerState.value.currentPositionMs
        val wasPlaying = _playerState.value.isPlaying

        release()
        getOrCreatePlayer()

        if (currentRec != null && wasPlaying) {
            play(currentRec)
            if (currentPos > 1000L) {
                seekTo(currentPos)
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val bufferConfig = currentBufferConfig

        // Controlled Buffering: Strict sliding window and small initial playback buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ bufferConfig.targetBufferMs,
                /* maxBufferMs = */ bufferConfig.maximumBufferMs,
                /* bufferForPlaybackMs = */ bufferConfig.initialBufferMs,
                /* bufferForPlaybackAfterRebufferMs = */ bufferConfig.bufferForPlaybackAfterRebufferMs
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ bufferConfig.backBufferMs,
                /* retainBackBufferFromKeyframe = */ false
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Progressive HTTP Range Request Data Source with local file support
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Recordify/2.0 (Linux; Android) ProgressiveStreaming")
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setTransferListener(networkTransferListener)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(defaultDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player.addListener(playerListener)
        exoPlayer = player
        return player
    }

    /**
     * Starts progressive range-based playback of the given recording or stream URL.
     * Cancels any in-flight download requests of previous songs immediately.
     */
    fun play(recording: RecordingEntity): Long {
        val requestId = activePlaybackRequestId.incrementAndGet()

        hasPassed25 = false
        hasPassed50 = false
        hasPassed75 = false

        val isStreaming = recording.filePath.startsWith("http://") || recording.filePath.startsWith("https://")
        val mediaItem: MediaItem

        if (isStreaming) {
            mediaItem = MediaItem.fromUri(Uri.parse(recording.filePath))
        } else {
            val file = File(recording.filePath)
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "Cannot play file; file does not exist or is empty: ${recording.filePath}")
                _playerState.update {
                    it.copy(
                        currentRecording = recording,
                        status = PlayerPlaybackStatus.ERROR,
                        error = "File not found or deleted from storage."
                    )
                }
                return requestId
            }
            mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        }

        // Estimate full file size: duration (sec) * bitrate (bps) / 8
        val durationSec = (recording.durationMs / 1000L).coerceAtLeast(1L)
        val bitrateKbps = if (isDataSaverActive) 96 else 320
        val estimatedTotalBytes = if (isStreaming) (durationSec * bitrateKbps * 1024L) / 8L else File(recording.filePath).length()

        _streamingMetrics.update {
            it.copy(
                currentSongBytesStreamed = 0L,
                currentSongFullSizeBytes = estimatedTotalBytes,
                currentBitrateKbps = bitrateKbps
            )
        }

        val player = getOrCreatePlayer()

        // Interrupt & cancel any existing network requests on previous media
        player.stop()
        player.clearMediaItems()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        _playerState.update {
            PlayerState(
                currentRecording = recording,
                isPlaying = true,
                status = PlayerPlaybackStatus.BUFFERING,
                currentPositionMs = 0L,
                durationMs = recording.durationMs,
                bufferedPositionMs = 0L,
                playbackSpeed = player.playbackParameters.speed,
                isCompleted = false,
                isBuffering = true,
                error = null
            )
        }

        return requestId
    }

    fun isCurrentRequestId(requestId: Long): Boolean {
        return activePlaybackRequestId.get() == requestId
    }

    fun setPlaceholderPlaying(recording: RecordingEntity) {
        hasPassed25 = false
        hasPassed50 = false
        hasPassed75 = false

        // Stop any currently playing audio so it is completely silent during inspection countdown
        activePlaybackRequestId.incrementAndGet()
        stopProgressTracker()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()

        _playerState.update {
            it.copy(
                currentRecording = recording,
                isPlaying = true,
                status = PlayerPlaybackStatus.LOADING,
                currentPositionMs = 0L,
                durationMs = recording.durationMs,
                bufferedPositionMs = 0L,
                isCompleted = false,
                isBuffering = true,
                error = null
            )
        }
    }

    fun updateCurrentRecording(recording: RecordingEntity) {
        _playerState.update { it.copy(currentRecording = recording) }
    }

    fun pause() {
        exoPlayer?.pause()
        _playerState.update { it.copy(isPlaying = false, status = PlayerPlaybackStatus.PAUSED) }
    }

    fun resume() {
        val player = exoPlayer ?: return
        if (_playerState.value.isCompleted) {
            player.seekTo(0)
            _playerState.update { it.copy(isCompleted = false, currentPositionMs = 0) }
        }
        player.play()
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Range-based seeking:
     * ExoPlayer aborts the current range transfer and issues a new HTTP byte-range request
     * directly at the new seek location, without downloading intermediate data.
     */
    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clampedPosition = positionMs.coerceIn(0L, _playerState.value.durationMs)
        player.seekTo(clampedPosition)
        _playerState.update {
            it.copy(
                currentPositionMs = clampedPosition,
                status = PlayerPlaybackStatus.SEEKING
            )
        }
    }

    fun seekRelative(offsetMs: Long) {
        val current = _playerState.value.currentPositionMs
        seekTo(current + offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.playbackParameters = PlaybackParameters(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun setRepeatMode(repeatMode: Int) {
        exoPlayer?.repeatMode = repeatMode
    }

    /**
     * Closes the player and immediately aborts all active network socket connections.
     */
    fun stopAndClose() {
        activePlaybackRequestId.incrementAndGet()
        stopProgressTracker()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _playerState.value = PlayerState(status = PlayerPlaybackStatus.STOPPED)
    }

    fun release() {
        activePlaybackRequestId.incrementAndGet()
        stopProgressTracker()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState(status = PlayerPlaybackStatus.IDLE)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)
                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            bufferedPositionMs = buf
                        )
                    }

                    val current = _playerState.value.currentRecording
                    if (current != null && dur > 0L) {
                        val fraction = pos.toFloat() / dur.toFloat()
                        if (fraction >= 0.25f && !hasPassed25) {
                            hasPassed25 = true
                            onProgressMilestone?.invoke(current, 0.25f)
                        }
                        if (fraction >= 0.50f && !hasPassed50) {
                            hasPassed50 = true
                            onProgressMilestone?.invoke(current, 0.50f)
                        }
                        if (fraction >= 0.75f && !hasPassed75) {
                            hasPassed75 = true
                            onProgressMilestone?.invoke(current, 0.75f)
                        }
                    }
                }
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
