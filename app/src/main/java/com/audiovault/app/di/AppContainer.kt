package com.audiovault.app.di

import android.content.Context
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.data.local.AppDatabase
import com.audiovault.app.data.repository.PlaylistRepository
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.data.storage.FileManager

interface AppContainer {
    val fileManager: FileManager
    val appDatabase: AppDatabase
    val recordingRepository: RecordingRepository
    val userPreferencesRepository: UserPreferencesRepository
    val playlistRepository: PlaylistRepository
    val playerEngine: AudioPlayerEngine
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

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(context)
    }

    override val playerEngine: AudioPlayerEngine by lazy {
        AudioPlayerEngine(context)
    }
}
