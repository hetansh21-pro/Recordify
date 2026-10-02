package com.audiovault.app.core.intelligence

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * NotificationListenerService that extracts live song metadata from music and media player notifications,
 * strictly filtering out system UI and AudioVault's own notifications.
 */
class AudioVaultNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "AudioVault/NotifListener"

        var instance: AudioVaultNotificationListener? = null
            private set

        private var metadataCallback: ((SongMetadata) -> Unit)? = null

        fun setOnMetadataListener(callback: ((SongMetadata) -> Unit)?) {
            metadataCallback = callback
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val componentName = "${context.packageName}/${AudioVaultNotificationListener::class.java.name}"
            return enabledListeners?.contains(componentName) == true
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Notification listener connected.")
        try {
            activeNotifications?.forEach { extractMediaInfo(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning active notifications: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.i(TAG, "Notification listener disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        extractMediaInfo(sbn)
    }

    private fun extractMediaInfo(sbn: StatusBarNotification) {
        val pkg = sbn.packageName?.lowercase() ?: return

        // CRITICAL: Filter out Recordify / AudioVault itself, system packages, and downloaders to prevent self-triggering loops
        if (pkg == packageName.lowercase() ||
            pkg.contains("audiovault") ||
            pkg.contains("recordify") ||
            pkg == "android" ||
            pkg == "com.android.systemui" ||
            pkg == "com.google.android.gms" ||
            pkg == "com.android.vending"
        ) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank()) return

        // Reject internal service status notifications
        val titleLower = title.lowercase()
        if (titleLower.contains("audiovault") ||
            titleLower.contains("recordify") ||
            titleLower.contains("monitoring active") ||
            titleLower.contains("capturing music") ||
            titleLower.contains("recording:") ||
            titleLower.contains("downloading") ||
            titleLower.contains("connected") ||
            titleLower.contains("usb") ||
            titleLower.contains("battery")
        ) {
            return
        }

        val isMediaCategory = notification.category == Notification.CATEGORY_TRANSPORT ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

        val hasMediaActions = notification.actions?.any { action ->
            val actionTitle = action.title?.toString()?.lowercase() ?: ""
            actionTitle.contains("pause") || actionTitle.contains("play") || actionTitle.contains("next") || actionTitle.contains("previous")
        } == true

        if (isMediaCategory || hasMediaActions || isMusicPackage(pkg)) {
            // Clean up artist and title if title contains " - "
            var songTitle = title
            var songArtist = text?.takeIf { it.isNotBlank() }

            if (songArtist.isNullOrBlank() && title.contains(" - ")) {
                val parts = title.split(" - ", limit = 2)
                songArtist = parts[0].trim()
                songTitle = parts[1].trim()
            }

            val song = SongMetadata(
                title = songTitle,
                artist = songArtist,
                album = subText?.takeIf { it.isNotBlank() },
                sourcePackage = sbn.packageName
            )

            metadataCallback?.invoke(song)
        }
    }

    private fun isMusicPackage(pkg: String): Boolean {
        return pkg.contains("music") || pkg.contains("spotify") || pkg.contains("audio") ||
                pkg.contains("player") || pkg.contains("youtube") || pkg.contains("sound") ||
                pkg.contains("vlc") || pkg.contains("saavn") || pkg.contains("gaana") ||
                pkg.contains("wynk") || pkg.contains("podcast") || pkg.contains("deezer") ||
                pkg.contains("tidal") || pkg.contains("pandora") || pkg.contains("apple.android.music")
    }
}
