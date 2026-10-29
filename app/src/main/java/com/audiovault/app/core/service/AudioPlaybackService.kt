package com.audiovault.app.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.audiovault.app.AudioVaultApplication

/**
 * Foreground Service for persistent audio playback and Android MediaSession lifecycle.
 *
 * Guarantees uninterrupted playback when the screen is locked, the device enters Doze mode,
 * or when Recordify is minimized / removed from the Recent Apps overview.
 */
class AudioPlaybackService : Service() {

    companion object {
        private const val TAG = "Recordify/PlaybackService"

        const val ACTION_PLAY = "com.audiovault.app.ACTION_PLAYBACK_START"
        const val ACTION_PAUSE = "com.audiovault.app.ACTION_PLAYBACK_PAUSE"
        const val ACTION_STOP = "com.audiovault.app.ACTION_PLAYBACK_STOP"

        @Volatile
        var activeService: AudioPlaybackService? = null
            private set

        fun start(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PLAY else ACTION_PAUSE
            }
            try {
                if (isPlaying) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start AudioPlaybackService with action: ${intent.action}", e)
            }
        }

        fun stop(context: Context) {
            try {
                activeService?.let { service ->
                    ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    service.stopSelf()
                } ?: run {
                    val intent = Intent(context, AudioPlaybackService::class.java).apply {
                        action = ACTION_STOP
                    }
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop AudioPlaybackService", e)
            }
        }
    }

    private var isForegroundActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
        Log.i(TAG, "AudioPlaybackService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activeService = this
        val action = intent?.action

        if (action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP. Terminating foreground service.")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = PlaybackNotificationManager.activeInstance?.currentNotification
            ?: PlaybackNotificationManager.buildFallbackNotification(this)

        val isPlaying = action != ACTION_PAUSE
        updateForegroundNotification(notification, isPlaying)

        return START_STICKY
    }

    fun updateForegroundNotification(notification: Notification, isPlaying: Boolean) {
        try {
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        PlaybackNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification)
                }
                isForegroundActive = true
            } else {
                if (isForegroundActive) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                    isForegroundActive = false
                }
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(PlaybackNotificationManager.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating foreground playback notification", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val app = applicationContext as? AudioVaultApplication
        val isPlaying = app?.container?.playerEngine?.playerState?.value?.isPlaying == true

        if (isPlaying) {
            // Music is actively playing -> Keep the Foreground Service running seamlessly
            Log.i(TAG, "Application task removed from Recents, but audio is playing in foreground. Maintaining continuous playback.")
        } else {
            // Audio is paused or idle -> Dismiss notification and terminate service cleanly
            Log.i(TAG, "Application task removed from Recents while paused. Cleaning up notification and service.")
            try {
                PlaybackNotificationManager.activeInstance?.let { manager ->
                    manager.cancelNotification()
                    manager.mediaSession.isActive = false
                }
                app?.container?.playerEngine?.let { engine ->
                    engine.stopAndClose()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up on task removed", e)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeService == this) {
            activeService = null
        }
        isForegroundActive = false
        Log.i(TAG, "AudioPlaybackService destroyed.")
    }
}
