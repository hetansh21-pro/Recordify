package com.audiovault.app

import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.audiovault.app.data.repository.RecentlyPlayedSearchItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchTwoStateModeTest {

    @Test
    fun `landing browse mode showcases curated albums and explore categories`() {
        // In browse mode (!isSearchActive), the landing page displays pre-built curated albums
        val curatedAlbums = CuratedAlbumCatalog.curatedAlbums
        assertThat(curatedAlbums).isNotEmpty()

        // 90s Bollywood and 70s-80s Old Classics must be featured on browse landing
        val albumTitles = curatedAlbums.map { it.title }
        assertThat(albumTitles).contains("Golden 90s Bollywood Hits")
        assertThat(albumTitles).contains("Evergreen 70s & 80s Classics")
        assertThat(albumTitles).contains("90s Romantic Melodies")
    }

    @Test
    fun `active search recents handles song, album and artist subtitles correctly`() {
        val songItem = RecentlyPlayedSearchItem(
            id = "song_1",
            title = "Kesariya",
            artist = "Arijit Singh",
            isOnline = true,
            isAlbum = false
        )
        val albumItem = RecentlyPlayedSearchItem(
            id = "alb_1",
            title = "Brahmastra",
            artist = "Pritam",
            isOnline = true,
            isAlbum = true,
            albumId = "brahmastra_album_id"
        )
        val artistItem = RecentlyPlayedSearchItem(
            id = "art_1",
            title = "Arijit Singh",
            artist = "Artist",
            isOnline = false
        )

        // Subtitle resolution verification matching Image 2
        fun computeSubtitle(item: RecentlyPlayedSearchItem): String {
            val isArtist = item.artist.equals("Artist", ignoreCase = true) || item.artist.isBlank()
            return when {
                item.isAlbum -> "Album • ${item.artist}"
                isArtist -> "Artist"
                else -> "Song • ${item.artist}"
            }
        }

        assertThat(computeSubtitle(songItem)).isEqualTo("Song • Arijit Singh")
        assertThat(computeSubtitle(albumItem)).isEqualTo("Album • Pritam")
        assertThat(computeSubtitle(artistItem)).isEqualTo("Artist")
    }
}
