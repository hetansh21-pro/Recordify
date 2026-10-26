package com.audiovault.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiovault.app.AudioVaultApplication
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.data.repository.UserPreferencesRepository
import com.audiovault.app.data.repository.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AudioVaultApplication).container
    private val preferencesRepository: UserPreferencesRepository = container.userPreferencesRepository
    private val recordingRepository: RecordingRepository = container.recordingRepository

    val settings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val recordingCount: StateFlow<Int> = recordingRepository.recordingCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalStorageBytes: StateFlow<Long> = recordingRepository.totalStorageBytesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    private val spotifyDataImporter = container.spotifyDataImporter
    private val recommendationService = container.recommendationService
    private val appDatabase = container.appDatabase

    private val _spotifyImportState = kotlinx.coroutines.flow.MutableStateFlow<SpotifyImportUiState>(SpotifyImportUiState.Idle)
    val spotifyImportState: StateFlow<SpotifyImportUiState> = _spotifyImportState.asStateFlow()

    private val _importedSpotifyCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val importedSpotifyCount: StateFlow<Int> = _importedSpotifyCount.asStateFlow()

    init {
        loadSpotifyImportCount()
    }

    fun loadSpotifyImportCount() {
        viewModelScope.launch {
            try {
                _importedSpotifyCount.value = appDatabase.listeningHistoryDao().getImportedSpotifyCount()
            } catch (_: Exception) {}
        }
    }

    fun importSpotifyZip(uri: android.net.Uri) {
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing(0f, 0)
            try {
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open selected ZIP file")

                val result = inputStream.use { stream ->
                    spotifyDataImporter.importFromZip(stream) { progress, count ->
                        _spotifyImportState.value = SpotifyImportUiState.Importing(progress, count)
                    }
                }

                if (result.success) {
                    recommendationService.reloadProfileAfterImport()
                    _importedSpotifyCount.value = result.stats.importedCount
                    _spotifyImportState.value = SpotifyImportUiState.Success(result.stats)
                } else {
                    _spotifyImportState.value = SpotifyImportUiState.Error(result.errorMessage ?: "Failed to import history ZIP")
                }
            } catch (e: Exception) {
                _spotifyImportState.value = SpotifyImportUiState.Error(e.localizedMessage ?: "Import error")
            }
        }
    }

    fun importSpotifyFolder(treeUri: android.net.Uri) {
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing(0f, 0)
            try {
                val context = getApplication<Application>().applicationContext
                val result = spotifyDataImporter.importFromDocumentTree(context, treeUri) { progress, count ->
                    _spotifyImportState.value = SpotifyImportUiState.Importing(progress, count)
                }

                if (result.success) {
                    recommendationService.reloadProfileAfterImport()
                    _importedSpotifyCount.value = result.stats.importedCount
                    _spotifyImportState.value = SpotifyImportUiState.Success(result.stats)
                } else {
                    _spotifyImportState.value = SpotifyImportUiState.Error(result.errorMessage ?: "Failed to import folder")
                }
            } catch (e: Exception) {
                _spotifyImportState.value = SpotifyImportUiState.Error(e.localizedMessage ?: "Import error")
            }
        }
    }

    fun importSpotifyFromPath(path: String) {
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing(0f, 0)
            try {
                val dir = java.io.File(path)
                val result = spotifyDataImporter.importFromDirectory(dir) { progress, count ->
                    _spotifyImportState.value = SpotifyImportUiState.Importing(progress, count)
                }

                if (result.success) {
                    recommendationService.reloadProfileAfterImport()
                    _importedSpotifyCount.value = result.stats.importedCount
                    _spotifyImportState.value = SpotifyImportUiState.Success(result.stats)
                } else {
                    _spotifyImportState.value = SpotifyImportUiState.Error(result.errorMessage ?: "Failed to import directory")
                }
            } catch (e: Exception) {
                _spotifyImportState.value = SpotifyImportUiState.Error(e.localizedMessage ?: "Import error")
            }
        }
    }

    fun clearSpotifyImportedData() {
        viewModelScope.launch {
            try {
                appDatabase.listeningHistoryDao().deleteImportedSpotifyHistory()
                recommendationService.reloadProfileAfterImport()
                _importedSpotifyCount.value = 0
                _spotifyImportState.value = SpotifyImportUiState.Idle
            } catch (_: Exception) {}
        }
    }

    fun resetSpotifyImportState() {
        _spotifyImportState.value = SpotifyImportUiState.Idle
    }

    fun setAutoRecord(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoRecordEnabled(enabled) }
    }

    fun setSilenceTimeout(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setSilenceTimeout(seconds) }
    }

    fun setMinDuration(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setMinRecordingDuration(seconds) }
    }

    fun setAudioBitrate(bitrateKbps: Int) {
        viewModelScope.launch { preferencesRepository.setAudioQualityBitrate(bitrateKbps) }
    }

    fun setSmartSongSplit(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSmartSongSplitEnabled(enabled) }
    }

    fun setNoiseAndAdFilter(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNoiseAndAdFilterEnabled(enabled) }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataSaverEnabled(enabled) }
    }

    fun setSongPreviewSkipSaver(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSongPreviewSkipSaverEnabled(enabled) }
    }

    fun setAutoCleanupDays(days: Int) {
        viewModelScope.launch { preferencesRepository.setAutoCleanupDays(days) }
    }

    fun clearAllRecordings() {
        viewModelScope.launch {
            recordingRepository.clearAllRecordings()
        }
    }

    fun setUserCountry(code: String, name: String) {
        viewModelScope.launch {
            preferencesRepository.setUserCountry(code, name)
            recommendationService.loadHomeFeed(forceRefresh = true)
        }
    }

    fun setMusicLanguages(primary: String, preferred: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setMusicLanguages(primary, preferred)
            recommendationService.loadHomeFeed(forceRefresh = true)
        }
    }

    fun setFavoriteGenres(genres: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setFavoriteGenres(genres)
            recommendationService.loadHomeFeed(forceRefresh = true)
        }
    }

    fun setFavoriteArtists(artists: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setFavoriteArtists(artists)
            recommendationService.loadHomeFeed(forceRefresh = true)
        }
    }

    fun clearListeningHistoryAndReset() {
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
            recommendationService.clearHistoryAndResetRecommendations()
        }
    }
}

sealed class SpotifyImportUiState {
    object Idle : SpotifyImportUiState()
    data class Importing(val progress: Float, val streamCount: Int) : SpotifyImportUiState()
    data class Success(val stats: com.audiovault.app.core.import.SpotifyImportStats) : SpotifyImportUiState()
    data class Error(val message: String) : SpotifyImportUiState()
}
