package com.audiovault.app

import com.audiovault.app.core.intelligence.TrackMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatcherTest {

    @Test
    fun testNormalizationStripsBracketsAndNoise() {
        val rawTitle = "Tum Hi Ho (From \"Aashiqui 2\") [Remastered] (feat. Mithoon)"
        val normalized = TrackMatcher.normalize(rawTitle)
        assertTrue("Normalized title should contain 'tum hi ho', got: $normalized", normalized.contains("tum hi ho"))
        assertFalse("Should strip 'remastered'", normalized.contains("remastered"))
        assertFalse("Should strip 'feat.'", normalized.contains("feat"))
    }

    @Test
    fun testArtistMatchingHandlesCollaborations() {
        // Expected primary artist matches multi-artist candidate
        assertTrue(TrackMatcher.artistMatches("Arijit Singh", "Arijit Singh, Mithoon, Pritam"))
        assertTrue(TrackMatcher.artistMatches("Arijit Singh, Mithoon", "Arijit Singh"))
        assertTrue(TrackMatcher.artistMatches("Kishore Kumar", "Kishore Kumar & Lata Mangeshkar"))

        // Unrelated artist should not match
        assertFalse(TrackMatcher.artistMatches("Arijit Singh", "Taylor Swift"))
        assertFalse(TrackMatcher.artistMatches("Kumar Sanu", "Ed Sheeran"))
    }

    @Test
    fun testConfidentMatchAcceptsBollywoodSoundtrackMatches() {
        // Same song with slight formatting difference
        val isMatch = TrackMatcher.isConfidentMatch(
            expectedTitle = "Tum Hi Ho",
            expectedArtist = "Arijit Singh",
            candidateTitle = "Tum Hi Ho (From Aashiqui 2)",
            candidateArtist = "Mithoon & Arijit Singh"
        )
        assertTrue("Should confidently match authentic Bollywood track", isMatch)
    }

    @Test
    fun testConfidentMatchRejectsUnrelatedEnglishTrack() {
        // English track with same or random title
        val isMatch = TrackMatcher.isConfidentMatch(
            expectedTitle = "Hero",
            expectedArtist = "Arijit Singh",
            candidateTitle = "Hero",
            candidateArtist = "Enrique Iglesias"
        )
        assertFalse("Should reject English track with mismatched artist", isMatch)
    }

    @Test
    fun testCuratedPlaylistsHaveAuthenticTracks() {
        val curatedAlbums = com.audiovault.app.core.catalog.CuratedAlbumCatalog.curatedAlbums
        val playlists = curatedAlbums.filter { it.type.equals("Playlist", ignoreCase = true) }
        assertTrue("Curated playlists must not be empty", playlists.isNotEmpty())

        for (pl in playlists) {
            assertTrue("Playlist '${pl.title}' must have trackQueries", pl.trackQueries.isNotEmpty())
            assertTrue("Playlist '${pl.title}' must have high-res cover art", pl.coverArtUrl?.contains("500x500") == true)
            // Ensure no track in playlist contains 'remix' unless explicitly requested
            for (track in pl.trackQueries) {
                assertFalse("Track '$track' in playlist '${pl.title}' should not be a remix", track.lowercase().contains("remix"))
            }
        }
    }
}
