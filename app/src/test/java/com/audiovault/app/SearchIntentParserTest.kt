package com.audiovault.app

import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.search.SearchIntentParser
import com.audiovault.app.core.search.SearchQueryType
import com.audiovault.app.core.search.SearchTargetEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchIntentParserTest {

    @Test
    fun `parses 90s Bollywood albums query correctly`() {
        val intent = SearchIntentParser.parse("90s Bollywood albums")

        assertThat(intent.targetEntity).isEqualTo(SearchTargetEntity.ALBUMS)
        assertThat(intent.queryType).isEqualTo(SearchQueryType.ALBUM_SEARCH)
        assertThat(intent.decadeRange).isEqualTo(1990..1999)
        assertThat(intent.cleanQuery.lowercase()).contains("bollywood")
        assertThat(intent.isOriginalOnly).isTrue()
    }

    @Test
    fun `parses year range query correctly`() {
        val intent = SearchIntentParser.parse("English pop from 1985 to 1995")

        assertThat(intent.decadeRange).isEqualTo(1985..1995)
        assertThat(intent.detectedLanguage).isEqualTo("english")
        assertThat(intent.detectedGenre).isEqualTo("pop")
    }

    @Test
    fun `parses artist discography query correctly`() {
        val intent = SearchIntentParser.parse("All albums by Arijit Singh")

        assertThat(intent.targetEntity).isEqualTo(SearchTargetEntity.ALBUMS)
        assertThat(intent.cleanQuery).contains("Arijit Singh")
    }

    @Test
    fun `parses soundtrack query correctly`() {
        val intent = SearchIntentParser.parse("Songs from the Titanic soundtrack")

        assertThat(intent.targetEntity).isEqualTo(SearchTargetEntity.ALBUMS)
        assertThat(intent.queryType).isEqualTo(SearchQueryType.SOUNDTRACK_SEARCH)
        assertThat(intent.cleanQuery).contains("Titanic")
    }

    @Test
    fun `parses mood and language query correctly`() {
        val intent = SearchIntentParser.parse("Gujarati romantic songs")

        assertThat(intent.targetEntity).isEqualTo(SearchTargetEntity.SONGS)
        assertThat(intent.detectedLanguage).isEqualTo("gujarati")
        assertThat(intent.detectedMood).isEqualTo("romantic")
    }

    @Test
    fun `parses late night sad mood query correctly`() {
        val intent = SearchIntentParser.parse("Sad songs for late night")

        assertThat(intent.targetEntity).isEqualTo(SearchTargetEntity.SONGS)
        assertThat(intent.queryType).isEqualTo(SearchQueryType.MOOD_DISCOVERY)
        assertThat(intent.detectedMood).isEqualTo("sad")
    }

    @Test
    fun `parses explicit remix version query correctly`() {
        val intent = SearchIntentParser.parse("Remixes of Kesariya")

        assertThat(intent.requestedVersion).isEqualTo(RecordingVersionType.REMIX)
        assertThat(intent.isOriginalOnly).isFalse()
        assertThat(intent.cleanQuery).contains("Kesariya")
    }

    @Test
    fun `parses acoustic version query correctly`() {
        val intent = SearchIntentParser.parse("Find acoustic versions of Tum Hi Ho")

        assertThat(intent.requestedVersion).isEqualTo(RecordingVersionType.ACOUSTIC)
        assertThat(intent.isOriginalOnly).isFalse()
        assertThat(intent.cleanQuery).contains("Tum Hi Ho")
    }
}
