package com.audiovault.app.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.audiovault.app.MainActivity
import com.audiovault.app.R
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.data.local.entity.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Manages a persistent Spotify-style playback notification and MediaSession in Android's notification shade.
 * Enables Android Quick Settings Media Player card and Bluetooth Earbud touch controls.
 */
class PlaybackNotificationManager(
    private val context: Context,
    private val playerEngine: AudioPlayerEngine,
    private val onPlayNext: () -> Unit = {},
    private val onPlayPrevious: () -> Unit = {}
) {

    companion object {
        const val NOTIFICATION_ID = 4004
        const val ACTION_PLAY_PAUSE = "com.audiovault.app.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.audiovault.app.ACTION_NEXT"
        const val ACTION_PREV = "com.audiovault.app.ACTION_PREV"
        const val ACTION_CLOSE = "com.audiovault.app.ACTION_CLOSE_NOTIFICATION"

        // Singleton instance hook for BroadcastReceiver and AudioPlaybackService
        var activeInstance: PlaybackNotificationManager? = null

        fun buildFallbackNotification(context: Context): Notification {
            AppNotificationManager.createChannels(context)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_PLAYBACK_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Recordify")
                .setContentText("Playing audio...")
                .setContentIntent(openAppPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        }
    }

    @Volatile
    var currentNotification: Notification? = null
        private set

    val isCurrentlyPlaying: Boolean
        get() = playerEngine.playerState.value.isPlaying

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastCoverPath: String? = null
    private var lastBitmap: Bitmap? = null
    private var fetchJob: Job? = null

    // MediaSessionCompat enables Bluetooth earbud single/double tap controls and the native Quick Settings Media Card
    val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "RecordifyMediaSession").apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playerEngine.resume()
            }

            override fun onPause() {
                playerEngine.pause()
            }

            override fun onSkipToNext() {
                onPlayNext()
            }

            override fun onSkipToPrevious() {
                onPlayPrevious()
            }

            override fun onStop() {
                playerEngine.pause()
                cancelNotification()
            }

            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            playerEngine.togglePlayPause()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            playerEngine.resume()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            playerEngine.pause()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onPlayNext()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            onPlayPrevious()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonEvent)
            }
        })

        isActive = true
    }

    init {
        activeInstance = this
        AppNotificationManager.createChannels(context)
    }

    fun updateNotification(state: PlayerState) {
        val recording = state.currentRecording
        if (recording == null) {
            updateMediaSessionState(state = PlaybackStateCompat.STATE_NONE, position = 0L, isPlaying = false)
            cancelNotification()
            return
        }

        // Update MediaSession PlaybackState
        val sessionState = if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        updateMediaSessionState(
            state = sessionState,
            position = state.currentPositionMs,
            isPlaying = state.isPlaying
        )

        // Check if artwork needs to be loaded
        val artPath = recording.albumArtPath
        if (artPath != lastCoverPath) {
            lastCoverPath = artPath
            lastBitmap = null
            fetchJob?.cancel()
            if (!artPath.isNullOrBlank()) {
                fetchJob = scope.launch {
                    val bmp = loadBitmap(artPath)
                    withContext(Dispatchers.Main) {
                        lastBitmap = bmp
                        updateMediaSessionMetadata(recording, bmp)
                        postNotification(recording, state.isPlaying, bmp)
                    }
                }
            } else {
                updateMediaSessionMetadata(recording, null)
            }
        } else {
            updateMediaSessionMetadata(recording, lastBitmap)
        }

        postNotification(recording, state.isPlaying, lastBitmap)
    }

    private fun updateMediaSessionState(state: Int, position: Long, isPlaying: Boolean) {
        try {
            val playbackSpeed = if (isPlaying) 1.0f else 0.0f
            val actions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO

            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, playbackSpeed)

            mediaSession.setPlaybackState(stateBuilder.build())
        } catch (_: Throwable) {}
    }

    private fun updateMediaSessionMetadata(recording: RecordingEntity, artBitmap: Bitmap?) {
        try {
            val metaBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, recording.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, recording.artist ?: "Recordify")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, recording.album ?: "Playing")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, recording.durationMs)

            if (artBitmap != null) {
                metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artBitmap)
                metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artBitmap)
            }

            mediaSession.setMetadata(metaBuilder.build())
        } catch (_: Throwable) {}
    }

    fun cancelNotification() {
        try {
            currentNotification = null
            updateMediaSessionState(PlaybackStateCompat.STATE_STOPPED, 0L, false)
            mediaSession.isActive = false
            notificationManager.cancel(NOTIFICATION_ID)
            AudioPlaybackService.stop(context)
        } catch (_: Throwable) {}
    }

    private fun postNotification(recording: RecordingEntity, isPlaying: Boolean, artBitmap: Bitmap?) {
        try {
            val notification = buildNotification(recording, isPlaying, artBitmap)
            currentNotification = notification

            val service = AudioPlaybackService.activeService
            if (service != null) {
                service.updateForegroundNotification(notification, isPlaying)
            } else {
                AudioPlaybackService.start(context, isPlaying)
            }
        } catch (_: Throwable) {}
    }

    private fun buildNotification(
        recording: RecordingEntity,
        isPlaying: Boolean,
        artBitmap: Bitmap?
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(context, PlaybackActionReceiver::class.java).apply {
            action = ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play / Pause Action
        val playPauseIntent = Intent(context, PlaybackActionReceiver::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            12,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next Action
        val nextIntent = Intent(context, PlaybackActionReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            13,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_PLAYBACK_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(recording.title)
            .setContentText(recording.artist ?: "Recordify")
            .setSubText(recording.album ?: "Playing")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (artBitmap != null) {
            builder.setLargeIcon(artBitmap)
        }

        return builder.build()
    }

    private fun loadBitmap(pathOrUrl: String): Bitmap? {
        return try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                val stream = URL(pathOrUrl).openStream()
                BitmapFactory.decodeStream(stream)
            } else {
                val file = File(pathOrUrl)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun handleAction(action: String?) {
        when (action) {
            ACTION_PLAY_PAUSE -> {
                playerEngine.togglePlayPause()
            }
            ACTION_NEXT -> {
                onPlayNext()
            }
            ACTION_PREV -> {
                onPlayPrevious()
            }
            ACTION_CLOSE -> {
                cancelNotification()
            }
        }
    }
}

/**
 * BroadcastReceiver for handling media notification button taps.
 */
class PlaybackActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        PlaybackNotificationManager.activeInstance?.handleAction(action)
    }
}
