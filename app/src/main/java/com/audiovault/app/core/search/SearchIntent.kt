package com.audiovault.app.core.search

import com.audiovault.app.core.identity.RecordingVersionType

enum class SearchTargetEntity {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}

enum class SearchQueryType {
    GENERAL_SONG,
    ALBUM_SEARCH,
    ARTIST_DISCOGRAPHY,
    SOUNDTRACK_SEARCH,
    DECADE_SEARCH,
    GENRE_DISCOVERY,
    MOOD_DISCOVERY,
    EXPLICIT_VERSION
}

data class SearchIntent(
    val rawQuery: String,
    val cleanQuery: String,
    val queryType: SearchQueryType,
    val targetEntity: SearchTargetEntity,
    val decadeRange: IntRange? = null,
    val exactYear: Int? = null,
    val detectedArtist: String? = null,
    val detectedAlbum: String? = null,
    val detectedLanguage: String? = null,
    val detectedGenre: String? = null,
    val detectedMood: String? = null,
    val requestedVersion: RecordingVersionType? = null,
    val isOriginalOnly: Boolean = true
)

data class SearchResultContainer(
    val query: String,
    val intent: SearchIntent,
    val songs: List<com.audiovault.app.core.identity.CanonicalTrackIdentity> = emptyList(),
    val albums: List<com.audiovault.app.core.online.OnlineAlbumModel> = emptyList(),
    val artists: List<com.audiovault.app.core.online.ArtistItemModel> = emptyList(),
    val youtubeSongs: List<com.audiovault.app.core.online.OnlineSongModel> = emptyList()
)
