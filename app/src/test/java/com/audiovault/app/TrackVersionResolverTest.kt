package com.audiovault.app

import com.audiovault.app.core.identity.CanonicalTrackIdentity
import com.audiovault.app.core.identity.RecordingVersionType
import com.audiovault.app.core.identity.TrackQualityFilter
import com.audiovault.app.core.identity.TrackVersionResolver
import com.audiovault.app.core.online.OnlineSongModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackVersionResolverTest {

    @Test
    fun `cleanTitle strips promotional, resolution, and format noise tags`() {
        val raw1 = "Kesariya (Official Audio) | Brahmāstra"
        val raw2 = "Tum Hi Ho - Full Video Song [HD] Aashiqui 2"
        val raw3 = "Channa Mereya [Lyrics Video] (4K Audio)"

        assertThat(TrackVersionResolver.cleanTitle(raw1)).contains("Kesariya")
        assertThat(TrackVersionResolver.cleanTitle(raw1)).doesNotContain("Official Audio")

        assertThat(TrackVersionResolver.cleanTitle(raw2)).contains("Tum Hi Ho")
        assertThat(TrackVersionResolver.cleanTitle(raw2)).doesNotContain("Full Video Song")
        assertThat(TrackVersionResolver.cleanTitle(raw2)).doesNotContain("[HD]")

        assertThat(TrackVersionResolver.cleanTitle(raw3)).contains("Channa Mereya")
        assertThat(TrackVersionResolver.cleanTitle(raw3)).doesNotContain("Lyrics Video")
        assertThat(TrackVersionResolver.cleanTitle(raw3)).doesNotContain("4K")
    }

    @Test
    fun `classifyVersion identifies remix, slowed-reverb, and sped-up correctly`() {
        val remix = TrackVersionResolver.classifyVersion("Kesariya - Dance Remix (DJ Mix)", null, "Arijit Singh", 240000L)
        val slowed = TrackVersionResolver.classifyVersion("Tum Hi Ho (Slowed + Reverb)", null, "Arijit Singh", 300000L)
        val spedUp = TrackVersionResolver.classifyVersion("Kala Chashma (Sped Up Nightcore)", null, "Badshah", 180000L)
        val original = TrackVersionResolver.classifyVersion("Kesariya", "Brahmastra", "Arijit Singh", 268000L)
        val soundtrack = TrackVersionResolver.classifyVersion("My Heart Will Go On", "Titanic (Original Motion Picture Soundtrack)", "Celine Dion", 310000L)
        val acoustic = TrackVersionResolver.classifyVersion("Channa Mereya (Acoustic Version)", null, "Arijit Singh", 250000L)
        val live = TrackVersionResolver.classifyVersion("Fix You (Live in Buenos Aires)", "Live in Buenos Aires", "Coldplay", 320000L)
        val karaoke = TrackVersionResolver.classifyVersion("Tum Hi Ho (Karaoke Instrumental)", null, "Arijit Singh", 260000L)

        assertThat(remix.first).isEqualTo(RecordingVersionType.REMIX)
        assertThat(slowed.first).isEqualTo(RecordingVersionType.SLOWED_REVERB)
        assertThat(spedUp.first).isEqualTo(RecordingVersionType.SPED_UP_NIGHTCORE)
        assertThat(original.first).isEqualTo(RecordingVersionType.ORIGINAL_STUDIO)
        assertThat(soundtrack.first).isEqualTo(RecordingVersionType.OFFICIAL_SOUNDTRACK)
        assertThat(acoustic.first).isEqualTo(RecordingVersionType.ACOUSTIC)
        assertThat(live.first).isEqualTo(RecordingVersionType.LIVE)
        assertThat(karaoke.first).isEqualTo(RecordingVersionType.KARAOKE_INSTRUMENTAL)
    }

    @Test
    fun `snippets and ringtones under 55s are classified as snippet clips`() {
        val snippet = TrackVersionResolver.classifyVersion("Kesariya Ringtone", null, "Arijit Singh", 32000L)
        assertThat(snippet.first).isEqualTo(RecordingVersionType.SNIPPET_CLIP)
    }

    @Test
    fun `generateCanonicalId generates identical IDs for equivalent uploads`() {
        val id1 = TrackVersionResolver.generateCanonicalId("Tum Hi Ho", "Arijit Singh")
        val id2 = TrackVersionResolver.generateCanonicalId("Tum Hi Ho - Official Audio", "Arijit Singh")
        val id3 = TrackVersionResolver.generateCanonicalId("Tum Hi Ho (Official Music Video)", "Arijit Singh, Mithoon")

        val clean1 = TrackVersionResolver.cleanTitle("Tum Hi Ho")
        val clean2 = TrackVersionResolver.cleanTitle("Tum Hi Ho - Official Audio")
        val clean3 = TrackVersionResolver.cleanTitle("Tum Hi Ho (Official Music Video)")

        val artist1 = TrackVersionResolver.extractPrimaryArtist("Arijit Singh")
        val artist2 = TrackVersionResolver.extractPrimaryArtist("Arijit Singh")
        val artist3 = TrackVersionResolver.extractPrimaryArtist("Arijit Singh, Mithoon")

        val cId1 = TrackVersionResolver.generateCanonicalId(clean1, artist1)
        val cId2 = TrackVersionResolver.generateCanonicalId(clean2, artist2)
        val cId3 = TrackVersionResolver.generateCanonicalId(clean3, artist3)

        assertThat(cId1).isEqualTo(cId2)
        assertThat(cId1).isEqualTo(cId3)
    }

    @Test
    fun `TrackQualityFilter suppresses remixes and duplicate uploads in default original mode`() {
        val songs = listOf(
            OnlineSongModel(id = "1", title = "Kesariya", artist = "Arijit Singh", album = "Brahmastra", streamUrl = "http://cdn/1.mp3", bitrate = "320kbps", source = "saavn"),
            OnlineSongModel(id = "2", title = "Kesariya - Dance Remix", artist = "Arijit Singh", album = "Remix EP", streamUrl = "http://cdn/2.mp3", bitrate = "320kbps", source = "saavn"),
            OnlineSongModel(id = "3", title = "Kesariya (Slowed + Reverb)", artist = "Arijit Singh", album = "Chill Mix", streamUrl = "http://cdn/3.mp3", bitrate = "160kbps", source = "youtube"),
            OnlineSongModel(id = "4", title = "Kesariya (Sped Up)", artist = "Arijit Singh", album = "TikTok Mix", streamUrl = "http://cdn/4.mp3", bitrate = "160kbps", source = "youtube"),
            OnlineSongModel(id = "5", title = "Kesariya (Official Audio)", artist = "Arijit Singh", album = "Brahmastra", streamUrl = "http://cdn/5.mp3", bitrate = "320kbps", source = "spotify")
        )

        val filtered = TrackQualityFilter.filterAndDeduplicate(songs, isOriginalOnly = true)

        // Only the single canonical "Kesariya" original recording should remain!
        assertThat(filtered).hasSize(1)
        assertThat(filtered.first().normalizedTitle).contains("Kesariya")
        assertThat(filtered.first().recordingVersion).isEqualTo(RecordingVersionType.ORIGINAL_STUDIO)
        // And the duplicate upload from Spotify should be attached as alternate source
        assertThat(filtered.first().alternateSources).isNotEmpty()
    }

    @Test
    fun `TrackQualityFilter permits remixes when explicitly requested`() {
        val songs = listOf(
            OnlineSongModel(id = "1", title = "Kesariya", artist = "Arijit Singh", album = "Brahmastra", streamUrl = "http://cdn/1.mp3", bitrate = "320kbps", source = "saavn"),
            OnlineSongModel(id = "2", title = "Kesariya - Dance Remix", artist = "Arijit Singh", album = "Remix EP", streamUrl = "http://cdn/2.mp3", bitrate = "320kbps", source = "saavn"),
            OnlineSongModel(id = "3", title = "Kesariya (Slowed + Reverb)", artist = "Arijit Singh", album = "Chill Mix", streamUrl = "http://cdn/3.mp3", bitrate = "160kbps", source = "youtube")
        )

        val remixResults = TrackQualityFilter.filterAndDeduplicate(
            songs = songs,
            isOriginalOnly = false,
            requestedVersion = RecordingVersionType.REMIX
        )

        assertThat(remixResults).hasSize(1)
        assertThat(remixResults.first().recordingVersion).isEqualTo(RecordingVersionType.REMIX)
    }
}
