package com.audiovault.app.core.online

data class OnlineSongModel(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val streamUrl: String = "",
    val highResArtUrl: String? = null,
    val lyrics: String? = null,
    val year: String? = null,
    val language: String? = null,
    val bitrate: String = "320kbps",
    val source: String = "saavn", // "saavn", "youtube", "itunes", "spotify"
    val youtubeVideoId: String? = null
)

data class OnlineAlbumModel(
    val id: String,
    val title: String,
    val year: String? = null,
    val artist: String? = null,
    val coverArtUrl: String? = null,
    val songCount: Int = 0,
    val songs: List<OnlineSongModel> = emptyList(),
    val language: String? = null,
    val type: String = "Album", // "Album", "Compilation", "Single"
    val releaseDate: String? = null,
    val primaryArtists: String? = null,
    val source: String = "saavn",
    val searchQuery: String? = null,
    val trackQueries: List<String> = emptyList()
)

data class RadioMixModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val artists: String,
    val bgHexColor: Long,
    val artistImages: List<String> = emptyList()
)

data class ArtistItemModel(
    val id: String,
    val name: String,
    val imageUrl: String,
    val role: String = "Artist"
)

data class SpotifyHomeContent(
    val recommendedToday: List<OnlineAlbumModel> = emptyList(),
    val popularAlbumsAndSingles: List<OnlineAlbumModel> = emptyList(),
    val popularRadios: List<RadioMixModel> = emptyList(),
    val favouriteArtists: List<ArtistItemModel> = emptyList(),
    val moreOfWhatYouLike: List<OnlineAlbumModel> = emptyList(),
    val latestReleases: List<OnlineSongModel> = emptyList(),
    val trendingSoundtracks: List<OnlineAlbumModel> = emptyList(),
    val featuredPlaylists: List<OnlineSongModel> = emptyList()
)


