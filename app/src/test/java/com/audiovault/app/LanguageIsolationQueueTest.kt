package com.audiovault.app

import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.search.SearchIntentParser
import com.audiovault.app.core.search.SearchRelevanceRanker
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageIsolationQueueTest {

    @Test
    fun testInferLanguageGujarati() {
        assertEquals("Gujarati", VibeFlowMoodAnalyzer.inferLanguage("Kinjal Dave - Char Bangdi Vadi"))
        assertEquals("Gujarati", VibeFlowMoodAnalyzer.inferLanguage("Geeta Rabari Tahuko"))
        assertEquals("Gujarati", VibeFlowMoodAnalyzer.inferLanguage("Aditya Gadhvi Khalasi Gotilo"))
        assertEquals("Gujarati", VibeFlowMoodAnalyzer.inferLanguage("ગોટીલો ખલાસી"))
    }

    @Test
    fun testInferLanguageHindi() {
        assertEquals("Hindi", VibeFlowMoodAnalyzer.inferLanguage("Arijit Singh Tum Hi Ho"))
        assertEquals("Hindi", VibeFlowMoodAnalyzer.inferLanguage("Shreya Ghoshal Deewani Mastani"))
        assertEquals("Hindi", VibeFlowMoodAnalyzer.inferLanguage("तुम ही हो आशिकी"))
    }

    @Test
    fun testInferLanguageEnglish() {
        assertEquals("English", VibeFlowMoodAnalyzer.inferLanguage("Taylor Swift Blank Space"))
        assertEquals("English", VibeFlowMoodAnalyzer.inferLanguage("Ed Sheeran Shape of You"))
    }

    @Test
    fun testSingleLetterSearchPrefixRanking() {
        val intent = SearchIntentParser.parse("a")

        val trackArijit = CanonicalTrackIdentity(
            canonicalTrackId = "apnabana_arijit",
            providerTrackId = "1",
            providerName = "saavn",
            normalizedTitle = "Apna Bana Le",
            normalizedPrimaryArtist = "Arijit Singh",
            language = "Hindi"
        )
        val trackEnglish = CanonicalTrackIdentity(
            canonicalTrackId = "antihero_taylor",
            providerTrackId = "2",
            providerName = "saavn",
            normalizedTitle = "Anti-Hero",
            normalizedPrimaryArtist = "Taylor Swift",
            language = "English"
        )
        val trackNonPrefix = CanonicalTrackIdentity(
            canonicalTrackId = "tumhiho_arijit",
            providerTrackId = "3",
            providerName = "saavn",
            normalizedTitle = "Tum Hi Ho",
            normalizedPrimaryArtist = "Arijit Singh",
            language = "Hindi"
        )

        val ranked = SearchRelevanceRanker.rankTracks(
            tracks = listOf(trackNonPrefix, trackEnglish, trackArijit),
            intent = intent,
            userCountryCode = "IN",
            primaryLanguage = "Hindi",
            preferredLanguages = setOf("Hindi", "Gujarati")
        )

        // Apna Bana Le starts with 'A' and is in Hindi -> must rank first!
        assertEquals("Apna Bana Le", ranked.first().normalizedTitle)
    }

    @Test
    fun testLanguageRelevanceBoostsUserPrimaryLanguage() {
        val intent = SearchIntentParser.parse("love")

        val hindiTrack = CanonicalTrackIdentity(
            canonicalTrackId = "love_hindi",
            providerTrackId = "1",
            providerName = "saavn",
            normalizedTitle = "Love Song Hindi",
            normalizedPrimaryArtist = "Arijit Singh",
            language = "Hindi"
        )
        val englishTrack = CanonicalTrackIdentity(
            canonicalTrackId = "love_english",
            providerTrackId = "2",
            providerName = "saavn",
            normalizedTitle = "Love Song English",
            normalizedPrimaryArtist = "Justin Bieber",
            language = "English"
        )

        val ranked = SearchRelevanceRanker.rankTracks(
            tracks = listOf(englishTrack, hindiTrack),
            intent = intent,
            userCountryCode = "IN",
            primaryLanguage = "Hindi",
            preferredLanguages = setOf("Hindi", "Gujarati")
        )

        // Hindi song must rank above English song because user primary language is Hindi and English is unselected!
        assertEquals("love_hindi", ranked.first().canonicalTrackId)
    }

    @Test
    fun testTrendingSongReceivesRelevanceBonus() {
        val intent = SearchIntentParser.parse("khalasi")

        val normalTrack = CanonicalTrackIdentity(
            canonicalTrackId = "khalasi_normal",
            providerTrackId = "1",
            providerName = "saavn",
            normalizedTitle = "Khalasi Studio",
            normalizedPrimaryArtist = "Aditya Gadhvi",
            language = "Gujarati"
        )
        val trendingTrack = CanonicalTrackIdentity(
            canonicalTrackId = "khalasi_viral",
            providerTrackId = "2",
            providerName = "saavn",
            normalizedTitle = "Khalasi Trending Reel Hit",
            normalizedPrimaryArtist = "Aditya Gadhvi",
            language = "Gujarati"
        )

        val ranked = SearchRelevanceRanker.rankTracks(
            tracks = listOf(normalTrack, trendingTrack),
            intent = intent,
            userCountryCode = "IN",
            primaryLanguage = "Gujarati",
            preferredLanguages = setOf("Gujarati", "Hindi")
        )

        assertEquals("khalasi_viral", ranked.first().canonicalTrackId)
    }
}
