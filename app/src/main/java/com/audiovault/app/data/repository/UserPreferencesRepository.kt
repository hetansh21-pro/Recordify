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

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.audiovault.app.data.model.MusicPreferenceCatalog
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class RecentlyPlayedSearchItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtPath: String? = null,
    val isOnline: Boolean = false,
    val streamUrl: String? = null,
    val youtubeVideoId: String? = null,
    val localRecordingId: Long? = null,
    val isAlbum: Boolean = false,
    val albumId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserSettings(
    val isAutoRecordEnabled: Boolean = true,
    val silenceTimeoutSeconds: Int = 7,
    val minRecordingDurationSeconds: Int = 3,
    val audioQualityBitrateKbps: Int = 256, // Studio Grade default
    val autoCleanupDays: Int = 0,
    val isSmartSongSplitEnabled: Boolean = true,
    val isNoiseAndAdFilterEnabled: Boolean = true,
    val isDataSaverEnabled: Boolean = false,
    val isSongPreviewSkipSaverEnabled: Boolean = false,
    val isVibeFlowAutoQueueEnabled: Boolean = true,
    val isPersonalizedRecommendationsEnabled: Boolean = true,
    val isSearchOriginalOnly: Boolean = true,
    val isPersonalizedSearchEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val userCountryCode: String = "IN",
    val userCountryName: String = "India",
    val primaryLanguage: String = "Hindi",
    val preferredLanguages: Set<String> = setOf("Hindi", "Gujarati", "English"),
    val favoriteGenres: Set<String> = setOf("Bollywood", "Gujarati", "Romantic", "Pop"),
    val favoriteArtists: Set<String> = emptySet()
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
        val KEY_DATA_SAVER = booleanPreferencesKey("data_saver_enabled")
        val KEY_SONG_PREVIEW_SKIP_SAVER = booleanPreferencesKey("song_preview_skip_saver_enabled")
        val KEY_VIBE_FLOW_AUTO_QUEUE = booleanPreferencesKey("vibe_flow_auto_queue_enabled")
        val KEY_PERSONALIZED_RECOMMENDATIONS = booleanPreferencesKey("personalized_recommendations_enabled")
        val KEY_SEARCH_ORIGINAL_ONLY = booleanPreferencesKey("search_original_only")
        val KEY_PERSONALIZED_SEARCH = booleanPreferencesKey("personalized_search_enabled")
        val KEY_USER_NAME = stringPreferencesKey("user_profile_name")
        val KEY_HAS_PROMPTED_NAME = booleanPreferencesKey("has_prompted_user_name")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_USER_COUNTRY_CODE = stringPreferencesKey("user_country_code")
        val KEY_USER_COUNTRY_NAME = stringPreferencesKey("user_country_name")
        val KEY_PRIMARY_LANGUAGE = stringPreferencesKey("primary_music_language")
        val KEY_PREFERRED_LANGUAGES = stringSetPreferencesKey("preferred_music_languages")
        val KEY_FAVORITE_GENRES = stringSetPreferencesKey("favorite_music_genres")
        val KEY_FAVORITE_ARTISTS = stringSetPreferencesKey("favorite_music_artists")
    }

    val recentSearchesFlow: Flow<List<RecentlyPlayedSearchItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[PreferencesKeys.KEY_RECENT_SEARCHES] ?: return@map emptyList()
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<RecentlyPlayedSearchItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RecentlyPlayedSearchItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        albumArtPath = obj.optString("art").takeIf { it.isNotBlank() },
                        isOnline = obj.optBoolean("online", false),
                        streamUrl = obj.optString("stream").takeIf { it.isNotBlank() },
                        youtubeVideoId = obj.optString("ytId").takeIf { it.isNotBlank() },
                        localRecordingId = if (obj.has("localId")) obj.getLong("localId") else null,
                        isAlbum = obj.optBoolean("isAlbum", false),
                        albumId = obj.optString("albumId").takeIf { it.isNotBlank() },
                        timestamp = obj.optLong("ts", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addRecentSearchItem(item: RecentlyPlayedSearchItem) {
        context.dataStore.edit { prefs ->
            val currentList = try {
                val jsonStr = prefs[PreferencesKeys.KEY_RECENT_SEARCHES] ?: ""
                val array = JSONArray(jsonStr)
                val list = mutableListOf<RecentlyPlayedSearchItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RecentlyPlayedSearchItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            albumArtPath = obj.optString("art").takeIf { it.isNotBlank() },
                            isOnline = obj.optBoolean("online", false),
                            streamUrl = obj.optString("stream").takeIf { it.isNotBlank() },
                            youtubeVideoId = obj.optString("ytId").takeIf { it.isNotBlank() },
                            localRecordingId = if (obj.has("localId")) obj.getLong("localId") else null,
                            isAlbum = obj.optBoolean("isAlbum", false),
                            albumId = obj.optString("albumId").takeIf { it.isNotBlank() },
                            timestamp = obj.optLong("ts", 0L)
                        )
                    )
                }
                list
            } catch (_: Exception) {
                mutableListOf()
            }

            val updatedList = mutableListOf<RecentlyPlayedSearchItem>()
            updatedList.add(item)
            currentList.filter { !it.title.equals(item.title, ignoreCase = true) }.take(24).forEach {
                updatedList.add(it)
            }

            val outArray = JSONArray()
            for (it in updatedList) {
                val obj = JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("artist", it.artist)
                    put("art", it.albumArtPath ?: "")
                    put("online", it.isOnline)
                    put("stream", it.streamUrl ?: "")
                    put("ytId", it.youtubeVideoId ?: "")
                    it.localRecordingId?.let { lid -> put("localId", lid) }
                    put("isAlbum", it.isAlbum)
                    put("albumId", it.albumId ?: "")
                    put("ts", it.timestamp)
                }
                outArray.put(obj)
            }
            prefs[PreferencesKeys.KEY_RECENT_SEARCHES] = outArray.toString()
        }
    }

    suspend fun removeRecentSearchItem(id: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[PreferencesKeys.KEY_RECENT_SEARCHES] ?: return@edit
            try {
                val array = JSONArray(jsonStr)
                val outArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optString("id") != id) {
                        outArray.put(obj)
                    }
                }
                prefs[PreferencesKeys.KEY_RECENT_SEARCHES] = outArray.toString()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.KEY_RECENT_SEARCHES)
        }
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_USER_NAME] ?: ""
    }

    val hasPromptedNameFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEY_HAS_PROMPTED_NAME] ?: false
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_USER_NAME] = name.trim()
            preferences[PreferencesKeys.KEY_HAS_PROMPTED_NAME] = true
        }
    }

    suspend fun setHasPromptedName(prompted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_HAS_PROMPTED_NAME] = prompted
        }
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val defaultCountry = MusicPreferenceCatalog.getDefaultCountry()
        UserSettings(
            isAutoRecordEnabled = preferences[PreferencesKeys.KEY_AUTO_RECORD] ?: true,
            silenceTimeoutSeconds = preferences[PreferencesKeys.KEY_SILENCE_TIMEOUT] ?: 7,
            minRecordingDurationSeconds = preferences[PreferencesKeys.KEY_MIN_DURATION] ?: 3,
            audioQualityBitrateKbps = preferences[PreferencesKeys.KEY_AUDIO_BITRATE] ?: 256,
            autoCleanupDays = preferences[PreferencesKeys.KEY_AUTO_CLEANUP_DAYS] ?: 0,
            isSmartSongSplitEnabled = preferences[PreferencesKeys.KEY_SMART_SONG_SPLIT] ?: true,
            isNoiseAndAdFilterEnabled = preferences[PreferencesKeys.KEY_NOISE_AD_FILTER] ?: true,
            isDataSaverEnabled = preferences[PreferencesKeys.KEY_DATA_SAVER] ?: false,
            isSongPreviewSkipSaverEnabled = preferences[PreferencesKeys.KEY_SONG_PREVIEW_SKIP_SAVER] ?: false,
            isVibeFlowAutoQueueEnabled = preferences[PreferencesKeys.KEY_VIBE_FLOW_AUTO_QUEUE] ?: true,
            isPersonalizedRecommendationsEnabled = preferences[PreferencesKeys.KEY_PERSONALIZED_RECOMMENDATIONS] ?: true,
            isSearchOriginalOnly = preferences[PreferencesKeys.KEY_SEARCH_ORIGINAL_ONLY] ?: true,
            isPersonalizedSearchEnabled = preferences[PreferencesKeys.KEY_PERSONALIZED_SEARCH] ?: true,
            isOnboardingCompleted = preferences[PreferencesKeys.KEY_ONBOARDING_COMPLETED] ?: false,
            userCountryCode = preferences[PreferencesKeys.KEY_USER_COUNTRY_CODE] ?: defaultCountry.code,
            userCountryName = preferences[PreferencesKeys.KEY_USER_COUNTRY_NAME] ?: defaultCountry.name,
            primaryLanguage = preferences[PreferencesKeys.KEY_PRIMARY_LANGUAGE] ?: "Hindi",
            preferredLanguages = preferences[PreferencesKeys.KEY_PREFERRED_LANGUAGES] ?: setOf("Hindi", "Gujarati", "English"),
            favoriteGenres = preferences[PreferencesKeys.KEY_FAVORITE_GENRES] ?: setOf("Bollywood", "Gujarati", "Romantic", "Pop"),
            favoriteArtists = preferences[PreferencesKeys.KEY_FAVORITE_ARTISTS] ?: emptySet()
        )
    }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_ONBOARDING_COMPLETED] ?: false
    }

    val userCountryFlow: Flow<Pair<String, String>> = context.dataStore.data.map { prefs ->
        val defaultCountry = MusicPreferenceCatalog.getDefaultCountry()
        val code = prefs[PreferencesKeys.KEY_USER_COUNTRY_CODE] ?: defaultCountry.code
        val name = prefs[PreferencesKeys.KEY_USER_COUNTRY_NAME] ?: defaultCountry.name
        Pair(code, name)
    }

    val primaryLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_PRIMARY_LANGUAGE] ?: "Hindi"
    }

    val preferredLanguagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_PREFERRED_LANGUAGES] ?: setOf("Hindi", "Gujarati", "English")
    }

    val favoriteGenresFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_FAVORITE_GENRES] ?: setOf("Bollywood", "Gujarati", "Romantic", "Pop")
    }

    val favoriteArtistsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_FAVORITE_ARTISTS] ?: emptySet()
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setUserCountry(code: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_USER_COUNTRY_CODE] = code
            prefs[PreferencesKeys.KEY_USER_COUNTRY_NAME] = name
        }
    }

    suspend fun setMusicLanguages(primary: String, preferred: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_PRIMARY_LANGUAGE] = primary
            prefs[PreferencesKeys.KEY_PREFERRED_LANGUAGES] = preferred
        }
    }

    suspend fun setFavoriteGenres(genres: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_FAVORITE_GENRES] = genres
        }
    }

    suspend fun setFavoriteArtists(artists: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_FAVORITE_ARTISTS] = artists
        }
    }

    suspend fun setSearchOriginalOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SEARCH_ORIGINAL_ONLY] = enabled
        }
    }

    suspend fun setPersonalizedSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PERSONALIZED_SEARCH] = enabled
        }
    }

    suspend fun setVibeFlowAutoQueueEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_VIBE_FLOW_AUTO_QUEUE] = enabled
        }
    }

    suspend fun setPersonalizedRecommendationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_PERSONALIZED_RECOMMENDATIONS] = enabled
        }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_DATA_SAVER] = enabled
        }
    }

    suspend fun setSongPreviewSkipSaverEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SONG_PREVIEW_SKIP_SAVER] = enabled
        }
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
