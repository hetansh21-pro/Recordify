package com.audiovault.app

import com.audiovault.app.core.catalog.CountryMusicCatalog
import com.audiovault.app.data.model.MusicPreferenceCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryMusicCatalogTest {

    @Test
    fun `india profile contains authentic regional configuration`() {
        val profile = CountryMusicCatalog.getProfile("IN")
        assertThat(profile.countryCode).isEqualTo("IN")
        assertThat(profile.countryName).isEqualTo("India")
        assertThat(profile.flagEmoji).isEqualTo("🇮🇳")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Hindi")
        assertThat(profile.heroAlbum.title).isEqualTo("Bollywood Mush")
        assertThat(profile.quickAccessTiles).isNotEmpty()
        assertThat(profile.shelves.map { it.category }).containsAtLeast("trending", "bollywood", "romantic", "punjabi")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Arijit Singh", "Shreya Ghoshal", "Diljit Dosanjh")
    }

    @Test
    fun `usa profile contains billboard and american catalog`() {
        val profile = CountryMusicCatalog.getProfile("US")
        assertThat(profile.countryCode).isEqualTo("US")
        assertThat(profile.countryName).isEqualTo("United States")
        assertThat(profile.flagEmoji).isEqualTo("🇺🇸")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("English")
        assertThat(profile.heroAlbum.title).isEqualTo("Today's Top Hits USA")
        assertThat(profile.quickAccessTiles.map { it.title }).containsAtLeast("Taylor Swift", "Drake", "The Weeknd")
        assertThat(profile.shelves.map { it.category }).containsAtLeast("trending", "hiphop", "pop", "rock")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Taylor Swift", "Drake", "The Weeknd", "Kendrick Lamar")
    }

    @Test
    fun `uk profile contains british pop and uk rap`() {
        val profile = CountryMusicCatalog.getProfile("GB")
        assertThat(profile.countryCode).isEqualTo("GB")
        assertThat(profile.countryName).isEqualTo("United Kingdom")
        assertThat(profile.heroAlbum.title).isEqualTo("UK Top 40 Essentials")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Ed Sheeran", "Adele", "Dua Lipa")
    }

    @Test
    fun `south korea profile contains kpop and melon charts`() {
        val profile = CountryMusicCatalog.getProfile("KR")
        assertThat(profile.countryCode).isEqualTo("KR")
        assertThat(profile.countryName).isEqualTo("South Korea")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Korean")
        assertThat(profile.heroAlbum.title).isEqualTo("K-Pop ON! Global")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("BTS", "BLACKPINK", "NewJeans", "IU")
    }

    @Test
    fun `japan profile contains jpop and anime music`() {
        val profile = CountryMusicCatalog.getProfile("JP")
        assertThat(profile.countryCode).isEqualTo("JP")
        assertThat(profile.countryName).isEqualTo("Japan")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Japanese")
        assertThat(profile.heroAlbum.title).isEqualTo("Tokyo Super Hits")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("YOASOBI", "Kenshi Yonezu", "Ado")
    }

    @Test
    fun `brazil profile contains funk brasil and sertanejo`() {
        val profile = CountryMusicCatalog.getProfile("BR")
        assertThat(profile.countryCode).isEqualTo("BR")
        assertThat(profile.countryName).isEqualTo("Brazil")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Portuguese")
        assertThat(profile.heroAlbum.title).isEqualTo("Funk Brasil Hits")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Anitta", "Alok", "Marília Mendonça")
    }

    @Test
    fun `uae profile contains khaleeji and arabic pop`() {
        val profile = CountryMusicCatalog.getProfile("AE")
        assertThat(profile.countryCode).isEqualTo("AE")
        assertThat(profile.countryName).isEqualTo("United Arab Emirates")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Arabic")
        assertThat(profile.heroAlbum.title).isEqualTo("Khaleeji & Arabic Pop")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Hussain Al Jassmi", "Amr Diab", "Nancy Ajram")
    }

    @Test
    fun `spain profile contains top exitos and reggaeton`() {
        val profile = CountryMusicCatalog.getProfile("ES")
        assertThat(profile.countryCode).isEqualTo("ES")
        assertThat(profile.countryName).isEqualTo("Spain")
        assertThat(profile.defaultPrimaryLanguage).isEqualTo("Spanish")
        assertThat(profile.heroAlbum.title).isEqualTo("Top Éxitos España")
        assertThat(profile.popularArtists.map { it.name }).containsAtLeast("Rosalía", "Quevedo", "Aitana")
    }

    @Test
    fun `unknown country falls back to default profile gracefully`() {
        val profile = CountryMusicCatalog.getProfile("XYZ")
        assertThat(profile.countryCode).isEqualTo("IN")
        assertThat(profile.countryName).isEqualTo("India")
    }

    @Test
    fun `music preference catalog delegates languages and artists to active country`() {
        val krLangs = MusicPreferenceCatalog.getLanguagesForCountry("KR")
        assertThat(krLangs.map { it.name }).contains("Korean")

        val usArtists = MusicPreferenceCatalog.getArtistsForCountry("US")
        assertThat(usArtists.map { it.name }).contains("Taylor Swift")
    }
}
