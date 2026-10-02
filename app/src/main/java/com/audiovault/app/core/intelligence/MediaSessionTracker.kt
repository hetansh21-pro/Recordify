package com.audiovault.app.core.intelligence

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks active system MediaSessions, broadcast intents from music players,
 * and status bar notifications to reliably identify song names and artists.
 */
class MediaSessionTracker(
    private val context: Context,
    private val onTrackChanged: (previousSong: SongMetadata?, newSong: SongMetadata) -> Unit
) {
    companion object {
        private const val TAG = "AudioVault/MediaSession"

        private val MUSIC_BROADCAST_ACTIONS = listOf(
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.spotify.music.metadatachanged",
            "com.spotify.music.playbackstatechanged",
            "com.htc.music.metachanged",
            "fm.last.android.metachanged",
            "com.sec.android.app.music.metachanged",
            "com.nullsoft.winamp.metachanged",
            "com.amazon.mp3.metachanged",
            "com.miui.player.metachanged",
            "com.real.Android.metachanged",
            "com.sonyericsson.music.metachanged",
            "com.rdio.android.metachanged",
            "com.samsung.sec.android.MusicPlayer.metachanged",
            "com.andrew.apollo.metachanged"
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers = mutableListOf<MediaController>()
    private var pollJob: Job? = null
    private var isReceiverRegistered = false

    private val _currentSong = MutableStateFlow<SongMetadata?>(null)
    val currentSong: StateFlow<SongMetadata?> = _currentSong.asStateFlow()

    private val musicIntentReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return

            val track = intent.getStringExtra("track")
                ?: intent.getStringExtra("title")
                ?: intent.getStringExtra("song")

            val artist = intent.getStringExtra("artist")
            val album = intent.getStringExtra("album")

            if (!track.isNullOrBlank() && !track.contains("AudioVault", ignoreCase = true)) {
                var cleanTrack = track.trim()
                var cleanArtist = artist?.trim()

                if (cleanArtist.isNullOrBlank() && cleanTrack.contains(" - ")) {
                    val parts = cleanTrack.split(" - ", limit = 2)
                    cleanArtist = parts[0].trim()
                    cleanTrack = parts[1].trim()
                }

                val song = SongMetadata(
                    title = cleanTrack,
                    artist = cleanArtist,
                    album = album?.trim(),
                    sourcePackage = intent.`package`
                )
                updateSong(song)
            }
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateControllers(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            pollActiveSession()
        }
    }

    fun startTracking() {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val notificationListenerComponent = ComponentName(context, AudioVaultNotificationListener::class.java)

            try {
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, notificationListenerComponent)
                val controllers = mediaSessionManager?.getActiveSessions(notificationListenerComponent)
                updateControllers(controllers)
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification listener permission not yet granted: ${e.message}")
            }

            // Register music broadcast actions
            val filter = IntentFilter().apply {
                MUSIC_BROADCAST_ACTIONS.forEach { addAction(it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(musicIntentReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(musicIntentReceiver, filter)
            }
            isReceiverRegistered = true

            // Start poller
            startPoller(notificationListenerComponent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MediaSessionTracker: ${e.message}", e)
        }
    }

    private fun startPoller(component: ComponentName) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val controllers = mediaSessionManager?.getActiveSessions(component)
                    if (controllers != null && controllers.isNotEmpty()) {
                        updateControllers(controllers)
                    }
                } catch (e: Exception) {
                    // Ignore security exception in poller
                }
                delay(2000L)
            }
        }
    }

    fun updateFromExternalSource(metadata: SongMetadata) {
        updateSong(metadata)
    }

    private fun updateSong(newSong: SongMetadata) {
        // Discard invalid/empty song titles or Recordify / AudioVault internal titles
        if (newSong.title.isBlank() ||
            newSong.title.contains("AudioVault", ignoreCase = true) ||
            newSong.title.contains("Recordify", ignoreCase = true) ||
            newSong.title.contains("Recording:", ignoreCase = true) ||
            newSong.title.contains("Capturing", ignoreCase = true)
        ) {
            return
        }

        val prev = _currentSong.value
        if (isDifferentTrack(prev, newSong)) {
            if (newSong.isAdvertisement) {
                Log.i(TAG, "Advertisement detected: '${newSong.displayTitle}'. Skipping audio capture.")
            } else {
                Log.i(TAG, "Song recognized: '${newSong.displayTitle}'")
                // Immediately pre-fetch cover art & artist portrait in background so it's ready when saved
                scope.launch(Dispatchers.IO) {
                    CoverArtFetcher.fetchAndCacheCoverArt(context, newSong.title, newSong.artist)
                    if (!newSong.artist.isNullOrBlank()) {
                        CoverArtFetcher.fetchAndCacheArtistArt(context, newSong.artist, newSong.title)
                    }
                }
            }
            _currentSong.value = newSong
            onTrackChanged(prev, newSong)
        }
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        // Filter out Recordify / AudioVault's own media controllers
        val filtered = controllers?.filter { controller ->
            val pkg = controller.packageName?.lowercase() ?: ""
            pkg != context.packageName.lowercase() && !pkg.contains("audiovault") && !pkg.contains("recordify")
        }

        activeControllers.forEach { it.unregisterCallback(controllerCallback) }
        activeControllers.clear()

        filtered?.forEach { controller ->
            controller.registerCallback(controllerCallback)
            activeControllers.add(controller)
        }

        pollActiveSession()
    }

    private fun pollActiveSession() {
        val playingController = activeControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: activeControllers.firstOrNull()

        playingController?.metadata?.let { metadata ->
            handleMetadata(metadata, playingController.packageName)
        }
    }

    private fun handleMetadata(metadata: MediaMetadata?, packageName: String? = null) {
        if (metadata == null) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return

        var cleanTitle = title.trim()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)

        var cleanArtist = artist?.trim()
        if (cleanArtist.isNullOrBlank() && cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2)
            cleanArtist = parts[0].trim()
            cleanTitle = parts[1].trim()
        }

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).takeIf { it > 0 }

        val artBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        var artPath: String? = null
        if (artBitmap != null) {
            val fileManager = com.audiovault.app.data.storage.FileManager(context)
            artPath = fileManager.saveCoverArtBitmap(artBitmap, cleanTitle, cleanArtist)
        }

        val newSong = SongMetadata(
            title = cleanTitle,
            artist = cleanArtist,
            album = album?.trim(),
            durationMs = durationMs,
            sourcePackage = packageName,
            albumArtPath = artPath
        )

        updateSong(newSong)
    }

    private fun isDifferentTrack(songA: SongMetadata?, songB: SongMetadata): Boolean {
        if (songA == null) return true
        return !songA.title.equals(songB.title, ignoreCase = true) ||
                !songA.artist.equals(songB.artist, ignoreCase = true)
    }

    fun stopTracking() {
        pollJob?.cancel()
        pollJob = null

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(musicIntentReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering music receiver: ${e.message}")
            }
            isReceiverRegistered = false
        }

        try {
            activeControllers.forEach { it.unregisterCallback(controllerCallback) }
            activeControllers.clear()
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaSessionTracker: ${e.message}")
        }
        _currentSong.value = null
    }
}
