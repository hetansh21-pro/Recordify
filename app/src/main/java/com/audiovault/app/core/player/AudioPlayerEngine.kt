package com.audiovault.app.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

data class PlayerState(
    val currentRecording: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isCompleted: Boolean = false,
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

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
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
                            durationMs = if (duration > 0) duration else (it.currentRecording?.durationMs ?: 0L),
                            isCompleted = false,
                            error = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _playerState.update {
                        it.copy(
                            isPlaying = false,
                            isCompleted = true,
                            currentPositionMs = it.durationMs
                        )
                    }
                    stopProgressTracker()
                }
                Player.STATE_IDLE -> {
                    // Idle state
                }
                Player.STATE_BUFFERING -> {
                    // Buffering
                }
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
            exoPlayer = this
        }
    }

    fun play(recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Cannot play file; file does not exist or is empty: ${recording.filePath}")
            _playerState.update {
                it.copy(
                    currentRecording = recording,
                    error = "File not found or deleted from storage."
                )
            }
            return
        }

        val player = getOrCreatePlayer()
        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        _playerState.update {
            PlayerState(
                currentRecording = recording,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = recording.durationMs,
                playbackSpeed = player.playbackParameters.speed,
                isCompleted = false,
                error = null
            )
        }
    }

    fun pause() {
        exoPlayer?.pause()
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

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clampedPosition = positionMs.coerceIn(0L, _playerState.value.durationMs)
        player.seekTo(clampedPosition)
        _playerState.update { it.copy(currentPositionMs = clampedPosition) }
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

    fun stopAndClose() {
        stopProgressTracker()
        exoPlayer?.stop()
        _playerState.value = PlayerState()
    }

    fun release() {
        stopProgressTracker()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _playerState.update { it.copy(currentPositionMs = pos) }
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
