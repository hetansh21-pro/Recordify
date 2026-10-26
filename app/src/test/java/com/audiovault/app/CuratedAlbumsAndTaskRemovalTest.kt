package com.audiovault.app

import com.audiovault.app.core.catalog.CuratedAlbumCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CuratedAlbumsAndTaskRemovalTest {

    @Test
    fun `curated albums collection contains rich 90s and old song albums`() {
        val albums = CuratedAlbumCatalog.curatedAlbums
        assertThat(albums).isNotEmpty()

        // Verify each album has an ID, title, artist, cover art, and internet search query
        for (album in albums) {
            assertThat(album.id).isNotEmpty()
            assertThat(album.title).isNotEmpty()
            assertThat(album.artist).isNotEmpty()
            assertThat(album.coverArtUrl).isNotEmpty()
            assertThat(album.searchQuery).isNotNull()
            assertThat(album.searchQuery).isNotEmpty()
        }
    }

    @Test
    fun `findMatchingAlbums returns 90s albums when querying 90s or 90s songs`() {
        val matches1 = CuratedAlbumCatalog.findMatchingAlbums("90s")
        assertThat(matches1).isNotEmpty()
        val titles1 = matches1.map { it.title }
        assertThat(titles1).contains("Golden 90s Bollywood Hits")
        assertThat(titles1).contains("90s Romantic Melodies")

        val matches2 = CuratedAlbumCatalog.findMatchingAlbums("90s songs")
        assertThat(matches2).isNotEmpty()
        val titles2 = matches2.map { it.title }
        assertThat(titles2).contains("Golden 90s Bollywood Hits")
    }

    @Test
    fun `findMatchingAlbums returns old classics when querying old songs or retro`() {
        val matchesOld = CuratedAlbumCatalog.findMatchingAlbums("old songs")
        assertThat(matchesOld).isNotEmpty()
        val titlesOld = matchesOld.map { it.title }
        assertThat(titlesOld).contains("Evergreen 70s & 80s Classics")

        val matchesOlds = CuratedAlbumCatalog.findMatchingAlbums("olds")
        assertThat(matchesOlds).isNotEmpty()

        val matchesRetro = CuratedAlbumCatalog.findMatchingAlbums("retro")
        assertThat(matchesRetro).isNotEmpty()
    }

    @Test
    fun `findMatchingAlbums matches legendary artists like Kishore Kumar or Lata Mangeshkar`() {
        val matchesKishore = CuratedAlbumCatalog.findMatchingAlbums("kishore kumar")
        assertThat(matchesKishore).isNotEmpty()
        val titlesKishore = matchesKishore.map { it.title }
        assertThat(titlesKishore).contains("Retro Kishore Kumar Golden Hits")

        val matchesLata = CuratedAlbumCatalog.findMatchingAlbums("lata mangeshkar")
        assertThat(matchesLata).isNotEmpty()
        val titlesLata = matchesLata.map { it.title }
        assertThat(titlesLata).contains("Evergreen Lata Mangeshkar Classics")
    }
}
