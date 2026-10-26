package com.audiovault.app

import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.online.OnlineMusicSearchService
import com.audiovault.app.core.search.SearchIntentParser
import com.audiovault.app.core.search.SearchRelevanceRanker
import com.audiovault.app.core.search.SearchSuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndianIntentTest {

    @Test
    fun testOldSongsSuggestionsMatchUserScreenshot() {
        val suggestions = SearchSuggestionEngine.getLocalSuggestions("old songs")
        assertEquals(5, suggestions.size)
        assertEquals("old songs hindi 90s", suggestions[0])
        assertEquals("old songs new version", suggestions[1])
        assertEquals("old songs", suggestions[2])
        assertEquals("old songs mashup", suggestions[3])
        assertEquals("old songs mashup hindi", suggestions[4])
    }

    @Test
    fun testSadSongsSuggestionsMatchUserScreenshot() {
        val suggestions = SearchSuggestionEngine.getLocalSuggestions("sad songs")
        assertEquals(5, suggestions.size)
        assertEquals("sad songs hindi", suggestions[0])
        assertEquals("sad songs playlist", suggestions[1])
        assertEquals("sad songs english", suggestions[2])
        assertEquals("sad songs", suggestions[3])
        assertEquals("sad songs slowed and reverb", suggestions[4])
    }

    @Test
    fun testIndianIntentDetectionAndExpansions() {
        assertTrue(OnlineMusicSearchService.isIndianIntentQuery("old songs", "Hindi"))
        assertTrue(OnlineMusicSearchService.isIndianIntentQuery("sad songs", "Hindi"))
        assertTrue(OnlineMusicSearchService.isIndianIntentQuery("purane gane", "Hindi"))
        assertFalse(OnlineMusicSearchService.isIndianIntentQuery("taylor swift 1989", "English"))

        val oldExpansions = OnlineMusicSearchService.getIndianIntentExpansionQueries("old songs")
        assertTrue(oldExpansions.any { it.contains("Likhe Jo Khat Tujhe", ignoreCase = true) })
        assertTrue(oldExpansions.any { it.contains("Pal Pal Dil Ke Paas", ignoreCase = true) })

        val sadExpansions = OnlineMusicSearchService.getIndianIntentExpansionQueries("sad songs")
        assertTrue(sadExpansions.any { it.contains("Thodi Der", ignoreCase = true) })
        assertTrue(sadExpansions.any { it.contains("Jo Tum Mere Ho", ignoreCase = true) })
        assertTrue(sadExpansions.any { it.contains("Tum Hi Aana", ignoreCase = true) })
        assertTrue(sadExpansions.any { it.contains("Zaroorat", ignoreCase = true) })
    }

    @Test
    fun testRelevanceRankerPrioritizesIndianLegendsOverLiteralTitleMatches() {
        val intent = SearchIntentParser.parse("old songs")

        val literalForeignSong = CanonicalTrackIdentity(
            canonicalTrackId = "old songs_indie band",
            normalizedTitle = "Old Songs",
            normalizedPrimaryArtist = "Indie Band",
            officialStatus = true,
            providerTrackId = "foreign_1",
            providerName = "itunes",
            playableSource = "https://audio.example.com/song1.mp3",
            recordingVersion = RecordingVersionType.ORIGINAL_STUDIO
        )

        val mohammedRafiHit = CanonicalTrackIdentity(
            canonicalTrackId = "likhe jo khat tujhe_mohammed rafi",
            normalizedTitle = "Likhe Jo Khat Tujhe",
            normalizedPrimaryArtist = "Mohammed Rafi",
            officialStatus = true,
            providerTrackId = "saavn_1",
            providerName = "saavn",
            releaseYear = 1968,
            language = "Hindi",
            playableSource = "https://audio.example.com/rafi.mp3",
            recordingVersion = RecordingVersionType.ORIGINAL_STUDIO
        )

        val kishoreKumarHit = CanonicalTrackIdentity(
            canonicalTrackId = "pal pal dil ke paas_kishore kumar",
            normalizedTitle = "Pal Pal Dil Ke Paas",
            normalizedPrimaryArtist = "Kishore Kumar",
            officialStatus = true,
            providerTrackId = "saavn_2",
            providerName = "saavn",
            releaseYear = 1973,
            language = "Hindi",
            playableSource = "https://audio.example.com/kishore.mp3",
            recordingVersion = RecordingVersionType.ORIGINAL_STUDIO
        )

        val ranked = SearchRelevanceRanker.rankTracks(
            tracks = listOf(literalForeignSong, mohammedRafiHit, kishoreKumarHit),
            intent = intent,
            primaryLanguage = "Hindi"
        )

        // Mohammed Rafi and Kishore Kumar should be ranked at the top!
        assertTrue(ranked[0].normalizedPrimaryArtist == "Mohammed Rafi" || ranked[0].normalizedPrimaryArtist == "Kishore Kumar")
        assertTrue(ranked[1].normalizedPrimaryArtist == "Mohammed Rafi" || ranked[1].normalizedPrimaryArtist == "Kishore Kumar")
        assertEquals("Indie Band", ranked[2].normalizedPrimaryArtist)
    }
}
