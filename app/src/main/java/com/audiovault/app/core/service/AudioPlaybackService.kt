package com.audiovault.app.core.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.audiovault.app.AudioVaultApplication

/**
 * Foreground Service for audio playback continuity and lifecycle protection.
 * Ensures Recordify's media playback process is immune to Android Low Memory Killer (LMK),
 * Battery Doze mode, and screen-off kills by declaring FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK.
 */
class AudioPlaybackService : Service() {

    companion object {
        private const val TAG = "Recordify/PlaybackService"
        const val ACTION_START_FOREGROUND = "com.audiovault.app.ACTION_START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.audiovault.app.ACTION_STOP_FOREGROUND"
        const val ACTION_STOP_SERVICE = "com.audiovault.app.ACTION_STOP_SERVICE"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var activeNotification: Notification? = null

        fun startForeground(context: Context, notification: Notification) {
            activeNotification = notification
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START_FOREGROUND
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to startForegroundService", e)
            }
        }

        fun stopForeground(context: Context, removeNotification: Boolean = false) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = if (removeNotification) ACTION_STOP_SERVICE else ACTION_STOP_FOREGROUND
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify service of stop foreground: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_FOREGROUND

        when (action) {
            ACTION_START_FOREGROUND -> {
                val notif = activeNotification
                if (notif != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                PlaybackNotificationManager.NOTIFICATION_ID,
                                notif,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            )
                        } else {
                            startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notif)
                        }
                        isRunning = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to call startForeground", e)
                    }
                }
            }

            ACTION_STOP_FOREGROUND -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(false)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stopForeground(detach)", e)
                }
            }

            ACTION_STOP_SERVICE -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stopForeground(remove)", e)
                }
                isRunning = false
                activeNotification = null
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val app = applicationContext as? AudioVaultApplication
        val isPlaying = app?.container?.playerEngine?.playerState?.value?.isPlaying == true

        if (isPlaying) {
            Log.i(TAG, "Application task removed from Recents, but audio is playing. Keeping foreground playback active.")
        } else {
            Log.i(TAG, "Application task removed from Recents while paused. Dismissing notification and stopping service.")
            try {
                PlaybackNotificationManager.activeInstance?.let { manager ->
                    manager.cancelNotification()
                    manager.mediaSession.isActive = false
                }
                app?.container?.playerEngine?.let { engine ->
                    engine.stopAndClose()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up on task removed", e)
            }
            isRunning = false
            activeNotification = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        activeNotification = null
    }
}
