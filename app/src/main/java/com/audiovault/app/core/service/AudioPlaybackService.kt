package com.audiovault.app.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.audiovault.app.AudioVaultApplication

/**
 * Service for audio playback task lifecycle and background continuity.
 * Listens for onTaskRemoved() when the user swipes away Recordify from Recent Apps.
 * If music is currently playing, it keeps playback alive uninterrupted.
 * If music is paused or idle, it dismisses the notification and stops cleanly.
 */
class AudioPlaybackService : Service() {

    companion object {
        private const val TAG = "Recordify/PlaybackService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val app = applicationContext as? AudioVaultApplication
        val isPlaying = app?.container?.playerEngine?.playerState?.value?.isPlaying == true

        if (isPlaying) {
            Log.i(TAG, "Application task removed from Recents, but audio is playing. Keeping playback active.")
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
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up on task removed", e)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = applicationContext as? AudioVaultApplication
        val isPlaying = app?.container?.playerEngine?.playerState?.value?.isPlaying == true
        if (!isPlaying) {
            try {
                PlaybackNotificationManager.activeInstance?.let { manager ->
                    manager.cancelNotification()
                    manager.mediaSession.isActive = false
                }
            } catch (_: Exception) {}
        }
    }
}
