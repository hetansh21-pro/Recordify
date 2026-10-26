package com.audiovault.app

import com.audiovault.app.core.online.SpotifyEntityType
import com.audiovault.app.core.online.SpotifyLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyLinkParserTest {

    @Test
    fun testParseStandardAlbumUrl() {
        val url = "https://open.spotify.com/album/4eLPsYPBmXABThSJ821sqY"
        val match = SpotifyLinkParser.parse(url)
        assertNotNull(match)
        assertEquals(SpotifyEntityType.ALBUM, match!!.type)
        assertEquals("4eLPsYPBmXABThSJ821sqY", match.id)
    }

    @Test
    fun testParseAlbumUrlWithQueryParams() {
        val url = "https://open.spotify.com/album/4eLPsYPBmXABThSJ821sqY?si=abc123xyz&utm_source=copy-link"
        val match = SpotifyLinkParser.parse(url)
        assertNotNull(match)
        assertEquals(SpotifyEntityType.ALBUM, match!!.type)
        assertEquals("4eLPsYPBmXABThSJ821sqY", match.id)
    }

    @Test
    fun testParseIntlLocalizedAlbumUrl() {
        val url = "https://open.spotify.com/intl-ja/album/4eLPsYPBmXABThSJ821sqY"
        val match = SpotifyLinkParser.parse(url)
        assertNotNull(match)
        assertEquals(SpotifyEntityType.ALBUM, match!!.type)
        assertEquals("4eLPsYPBmXABThSJ821sqY", match.id)
    }

    @Test
    fun testParseSpotifyAlbumUri() {
        val uri = "spotify:album:4eLPsYPBmXABThSJ821sqY"
        val match = SpotifyLinkParser.parse(uri)
        assertNotNull(match)
        assertEquals(SpotifyEntityType.ALBUM, match!!.type)
        assertEquals("4eLPsYPBmXABThSJ821sqY", match.id)
    }

    @Test
    fun testParseTrackUrlAndUri() {
        val url = "https://open.spotify.com/track/1n4jwRVXdkK2U34nBDUKKT"
        val matchUrl = SpotifyLinkParser.parse(url)
        assertNotNull(matchUrl)
        assertEquals(SpotifyEntityType.TRACK, matchUrl!!.type)
        assertEquals("1n4jwRVXdkK2U34nBDUKKT", matchUrl.id)

        val uri = "spotify:track:1n4jwRVXdkK2U34nBDUKKT"
        val matchUri = SpotifyLinkParser.parse(uri)
        assertNotNull(matchUri)
        assertEquals(SpotifyEntityType.TRACK, matchUri!!.type)
        assertEquals("1n4jwRVXdkK2U34nBDUKKT", matchUri.id)
    }

    @Test
    fun testParsePlaylistUrl() {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=something"
        val match = SpotifyLinkParser.parse(url)
        assertNotNull(match)
        assertEquals(SpotifyEntityType.PLAYLIST, match!!.type)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", match.id)
    }

    @Test
    fun testPlainSearchQueryReturnsNull() {
        assertNull(SpotifyLinkParser.parse("Arijit Singh"))
        assertNull(SpotifyLinkParser.parse("tum hi ho"))
        assertNull(SpotifyLinkParser.parse("https://youtube.com/watch?v=12345"))
        assertNull(SpotifyLinkParser.parse(""))
    }
}
