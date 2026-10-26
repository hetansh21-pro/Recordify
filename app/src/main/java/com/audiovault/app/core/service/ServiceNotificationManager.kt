package com.audiovault.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.audiovault.app.MainActivity
import com.audiovault.app.R

/**
 * Manages clean, non-intrusive foreground service notifications for AudioVault.
 */
class ServiceNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = AppNotificationManager.CHANNEL_CAPTURE_ID
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.audiovault.app.ACTION_STOP_CAPTURE"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        AppNotificationManager.createChannels(context)
    }

    fun buildNotification(
        isRecording: Boolean,
        recordingDurationMs: Long = 0L,
        songTitle: String? = null
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AudioCaptureService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isRecording) {
            if (!songTitle.isNullOrBlank()) songTitle else "Recording Music"
        } else {
            "Recordify Active"
        }

        val contentText = if (isRecording) {
            "Recording: ${formatDuration(recordingDurationMs)}"
        } else {
            "Listening for playback..."
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateNotification(isRecording: Boolean, recordingDurationMs: Long = 0L, songTitle: String? = null) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(isRecording, recordingDurationMs, songTitle)
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * System Notification Manager for heads-up alerts (Song Saved & Video Downloaded).
 */
object AppNotificationManager {

    const val CHANNEL_CAPTURE_ID = "audiovault_capture_channel"
    const val CHANNEL_SONGS_SAVED_ID = "audiovault_song_saved_channel"
    const val CHANNEL_VIDEO_DOWNLOAD_ID = "recordify_video_download_channel"
    const val CHANNEL_PLAYBACK_ID = "recordify_playback_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val captureChannel = NotificationChannel(
                CHANNEL_CAPTURE_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing song with playback controls"
                setShowBadge(false)
            }

            val songSavedChannel = NotificationChannel(
                CHANNEL_SONGS_SAVED_ID,
                "Captured Songs",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a new song is recorded and saved to library"
                setShowBadge(true)
                enableVibration(true)
            }

            val videoDownloadChannel = NotificationChannel(
                CHANNEL_VIDEO_DOWNLOAD_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a video download completes"
                setShowBadge(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(captureChannel, playbackChannel, songSavedChannel, videoDownloadChannel)
            )
        }
    }

    fun postSongSavedNotification(context: Context, song: com.audiovault.app.core.intelligence.SongMetadata?) {
        try {
            createChannels(context)
            val title = song?.title ?: "New Song"
            val artist = song?.artist ?: "Unknown Artist"

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_SONGS_SAVED_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎵 Song Saved to Library")
                .setContentText("$title • $artist")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Saved \"$title\" by $artist to your Library."))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = 2000 + (System.currentTimeMillis() % 10000).toInt()
            notificationManager.notify(notifId, notification)
        } catch (_: Throwable) {}
    }

    fun postVideoDownloadCompleteNotification(context: Context, videoTitle: String) {
        try {
            createChannels(context)
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_VIDEO_DOWNLOAD_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎬 Video Download Complete")
                .setContentText("$videoTitle (Tap to watch)")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Video \"$videoTitle\" is downloaded and ready in your Videos library."))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = 5000 + (System.currentTimeMillis() % 10000).toInt()
            notificationManager.notify(notifId, notification)
        } catch (_: Throwable) {}
    }
}
