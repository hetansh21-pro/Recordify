package com.audiovault.app

import com.audiovault.app.data.model.MusicPreferenceCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MusicPreferenceCatalogTest {

    @Test
    fun `default country resolves valid country info`() {
        val defaultCountry = MusicPreferenceCatalog.getDefaultCountry()
        assertThat(defaultCountry.code).isNotEmpty()
        assertThat(defaultCountry.name).isNotEmpty()
        assertThat(defaultCountry.flagEmoji).isNotEmpty()
    }

    @Test
    fun `countries catalog contains key regions`() {
        val codes = MusicPreferenceCatalog.COUNTRIES.map { it.code }
        assertThat(codes).containsAtLeast("IN", "US", "GB", "CA", "AU", "DE", "FR", "JP", "AE")
    }

    @Test
    fun `languages catalog contains Indian and international music languages`() {
        val names = MusicPreferenceCatalog.LANGUAGES.map { it.name }
        assertThat(names).containsAtLeast("Hindi", "Gujarati", "English", "Punjabi", "Tamil", "Telugu", "Spanish", "Korean")
    }

    @Test
    fun `genres catalog contains major music genres`() {
        val genres = MusicPreferenceCatalog.GENRES.map { it.name }
        assertThat(genres).containsAtLeast("Bollywood", "Gujarati", "Romantic", "Pop", "Hip-Hop", "Sad Songs", "Chill & Lo-Fi", "90s Retro")
    }

    @Test
    fun `popular artists catalog contains leading artists across languages`() {
        val artists = MusicPreferenceCatalog.POPULAR_ARTISTS.map { it.name }
        assertThat(artists).containsAtLeast("Arijit Singh", "Sachin-Jigar", "Darshan Raval", "Pritam", "Shreya Ghoshal", "Taylor Swift")
    }
}
