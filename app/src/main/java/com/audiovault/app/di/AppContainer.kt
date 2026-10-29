package com.audiovault.app.di

import android.content.Context
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.data.local.AppDatabase
import com.audiovault.app.data.repository.ImportedAlbumRepository
import com.audiovault.app.data.repository.PlaylistRepository
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.data.repository.VideoRepository
import com.audiovault.app.data.storage.FileManager

interface AppContainer {
    val fileManager: FileManager
    val appDatabase: AppDatabase
    val recordingRepository: RecordingRepository
    val videoRepository: VideoRepository
    val userPreferencesRepository: UserPreferencesRepository
    val playlistRepository: PlaylistRepository
    val importedAlbumRepository: ImportedAlbumRepository
    val playerEngine: AudioPlayerEngine
    val onlineSongDownloader: com.audiovault.app.core.online.OnlineSongDownloader
    val recommendationService: com.audiovault.app.core.recommendation.RecommendationService
    val playbackQueueManager: com.audiovault.app.core.queue.PlaybackQueueManager
    val spotifyDataImporter: com.audiovault.app.core.import.SpotifyDataImporter
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val fileManager: FileManager by lazy {
        FileManager(context)
    }

    override val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val recordingRepository: RecordingRepository by lazy {
        RecordingRepository(appDatabase.recordingDao(), fileManager)
    }

    override val videoRepository: VideoRepository by lazy {
        VideoRepository(appDatabase.videoDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(context)
    }

    override val importedAlbumRepository: ImportedAlbumRepository by lazy {
        ImportedAlbumRepository(context)
    }

    override val playerEngine: AudioPlayerEngine by lazy {
        AudioPlayerEngine(context)
    }

    override val onlineSongDownloader: com.audiovault.app.core.online.OnlineSongDownloader by lazy {
        com.audiovault.app.core.online.OnlineSongDownloader(context, recordingRepository, fileManager)
    }

    override val recommendationService: com.audiovault.app.core.recommendation.RecommendationService by lazy {
        com.audiovault.app.core.recommendation.RecommendationService(
            context = context,
            listeningHistoryDao = appDatabase.listeningHistoryDao(),
            playlistRepository = playlistRepository
        )
    }

    override val playbackQueueManager: com.audiovault.app.core.queue.PlaybackQueueManager by lazy {
        com.audiovault.app.core.queue.PlaybackQueueManager(
            context = context,
            playerEngine = playerEngine,
            recommendationService = recommendationService,
            recordingRepository = recordingRepository
        )
    }

    override val spotifyDataImporter: com.audiovault.app.core.import.SpotifyDataImporter by lazy {
        com.audiovault.app.core.import.SpotifyDataImporter(
            listeningHistoryDao = appDatabase.listeningHistoryDao()
        )
    }
}
