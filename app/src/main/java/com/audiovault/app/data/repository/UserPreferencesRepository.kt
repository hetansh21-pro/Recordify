package com.audiovault.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val isAutoRecordEnabled: Boolean = true,
    val silenceTimeoutSeconds: Int = 7,
    val minRecordingDurationSeconds: Int = 3,
    val audioQualityBitrateKbps: Int = 256, // Studio Grade default
    val autoCleanupDays: Int = 0,
    val isSmartSongSplitEnabled: Boolean = true,
    val isNoiseAndAdFilterEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_AUTO_RECORD = booleanPreferencesKey("auto_record_enabled")
        val KEY_SILENCE_TIMEOUT = intPreferencesKey("silence_timeout_seconds")
        val KEY_MIN_DURATION = intPreferencesKey("min_duration_seconds")
        val KEY_AUDIO_BITRATE = intPreferencesKey("audio_bitrate_kbps")
        val KEY_AUTO_CLEANUP_DAYS = intPreferencesKey("auto_cleanup_days")
        val KEY_SMART_SONG_SPLIT = booleanPreferencesKey("smart_song_split_enabled")
        val KEY_NOISE_AD_FILTER = booleanPreferencesKey("noise_ad_filter_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            isAutoRecordEnabled = preferences[PreferencesKeys.KEY_AUTO_RECORD] ?: true,
            silenceTimeoutSeconds = preferences[PreferencesKeys.KEY_SILENCE_TIMEOUT] ?: 7,
            minRecordingDurationSeconds = preferences[PreferencesKeys.KEY_MIN_DURATION] ?: 3,
            audioQualityBitrateKbps = preferences[PreferencesKeys.KEY_AUDIO_BITRATE] ?: 256,
            autoCleanupDays = preferences[PreferencesKeys.KEY_AUTO_CLEANUP_DAYS] ?: 0,
            isSmartSongSplitEnabled = preferences[PreferencesKeys.KEY_SMART_SONG_SPLIT] ?: true,
            isNoiseAndAdFilterEnabled = preferences[PreferencesKeys.KEY_NOISE_AD_FILTER] ?: true
        )
    }

    suspend fun setAutoRecordEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUTO_RECORD] = enabled
        }
    }

    suspend fun setSilenceTimeout(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SILENCE_TIMEOUT] = seconds
        }
    }

    suspend fun setMinRecordingDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MIN_DURATION] = seconds
        }
    }

    suspend fun setAudioQualityBitrate(bitrateKbps: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUDIO_BITRATE] = bitrateKbps
        }
    }

    suspend fun setAutoCleanupDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUTO_CLEANUP_DAYS] = days
        }
    }

    suspend fun setSmartSongSplitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SMART_SONG_SPLIT] = enabled
        }
    }

    suspend fun setNoiseAndAdFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_NOISE_AD_FILTER] = enabled
        }
    }
}
