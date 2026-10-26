package com.audiovault.app.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.audiovault.app.AudioVaultApplication

/**
 * Foreground / task-lifecycle watcher service for audio playback.
 * Specifically listens for onTaskRemoved() when the user swipes away Recordify
 * from the Android Recent Apps (Overview) screen, immediately dismissing the
 * playback notification from the notification bar / shade and stopping playback.
 */
class AudioPlaybackService : Service() {

    companion object {
        private const val TAG = "Recordify/PlaybackService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "Application task removed from Recents overview. Dismissing playback notification.")

        try {
            // 1. Immediately cancel the notification and deactivate media session
            PlaybackNotificationManager.activeInstance?.let { manager ->
                manager.cancelNotification()
                manager.mediaSession.isActive = false
            }

            // 2. Stop player engine to halt audio output immediately
            val app = applicationContext as? AudioVaultApplication
            app?.container?.playerEngine?.let { engine ->
                engine.pause()
                engine.stopAndClose()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up on task removed", e)
        }

        // 3. Terminate this service
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            PlaybackNotificationManager.activeInstance?.let { manager ->
                manager.cancelNotification()
                manager.mediaSession.isActive = false
            }
        } catch (_: Exception) {}
    }
}
