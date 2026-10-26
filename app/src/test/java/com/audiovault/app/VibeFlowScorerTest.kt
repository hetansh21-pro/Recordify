package com.audiovault.app

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.recommendation.UserMusicProfile
import com.audiovault.app.core.recommendation.VibeCategory
import com.audiovault.app.core.vibeflow.MoodLabel
import com.audiovault.app.core.vibeflow.SongMoodProfile
import com.audiovault.app.core.vibeflow.VibeFlowMoodAnalyzer
import com.audiovault.app.core.vibeflow.VibeFlowScorer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VibeFlowScorerTest {

    @Test
    fun `mood analyzer extracts romantic and calm mood from keywords`() {
        val profile = VibeFlowMoodAnalyzer.analyzeFromMetadata(
            trackId = "track_1",
            title = "Tum Hi Ho - Aashiqui 2 (Romantic Love Song)",
            artist = "Arijit Singh",
            album = "Aashiqui 2"
        )

        assertThat(profile.moodLabels).contains(MoodLabel.ROMANTIC)
        assertThat(profile.valence!!).isGreaterThan(0.5f)
        assertThat(profile.energy!!).isLessThan(0.7f)
    }

    @Test
    fun `mood analyzer extracts energetic dance mood from party keywords`() {
        val profile = VibeFlowMoodAnalyzer.analyzeFromMetadata(
            trackId = "track_2",
            title = "Kala Chashma (Club Party Dance Remix)",
            artist = "Badshah",
            album = "Baar Baar Dekho"
        )

        assertThat(profile.moodLabels).contains(MoodLabel.ENERGETIC)
        assertThat(profile.energy!!).isGreaterThan(0.7f)
        assertThat(profile.danceability!!).isGreaterThan(0.7f)
    }

    @Test
    fun `matching romantic mood scores higher than contrasting aggressive mood`() {
        val currentTrack = OnlineSongModel(
            id = "curr",
            title = "Tum Hi Ho",
            artist = "Arijit Singh"
        )
        val currentProfile = SongMoodProfile(
            trackId = "curr",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            genre = "Romantic Bollywood",
            moodLabels = setOf(MoodLabel.ROMANTIC, MoodLabel.CALM),
            valence = 0.65f,
            energy = 0.40f,
            danceability = 0.45f
        )

        val romanticCandidate = OnlineSongModel(
            id = "cand_romantic",
            title = "Channa Mereya",
            artist = "Pritam"
        )
        val romanticProfile = SongMoodProfile(
            trackId = "cand_romantic",
            title = "Channa Mereya",
            artist = "Pritam",
            genre = "Romantic Bollywood",
            moodLabels = setOf(MoodLabel.ROMANTIC, MoodLabel.SAD),
            valence = 0.60f,
            energy = 0.45f,
            danceability = 0.50f
        )

        val aggressiveCandidate = OnlineSongModel(
            id = "cand_aggressive",
            title = "Heavy Metal Rock Assault",
            artist = "Metal Band"
        )
        val aggressiveProfile = SongMoodProfile(
            trackId = "cand_aggressive",
            title = "Heavy Metal Rock Assault",
            artist = "Metal Band",
            genre = "Death Metal",
            moodLabels = setOf(MoodLabel.AGGRESSIVE, MoodLabel.ENERGETIC),
            valence = 0.15f,
            energy = 0.95f,
            danceability = 0.30f
        )

        val userProfile = UserMusicProfile()

        val romanticResult = VibeFlowScorer.scoreCandidate(
            candidate = romanticCandidate,
            candidateProfile = romanticProfile,
            seedProfile = currentProfile,
            userProfile = userProfile
        )

        val aggressiveResult = VibeFlowScorer.scoreCandidate(
            candidate = aggressiveCandidate,
            candidateProfile = aggressiveProfile,
            seedProfile = currentProfile,
            userProfile = userProfile
        )

        assertThat(romanticResult.score).isGreaterThan(aggressiveResult.score)
        assertThat(romanticResult.explanation.isNotEmpty()).isTrue()
    }

    @Test
    fun `missing features do not cause NaN and weights renormalize safely`() {
        val currentProfile = SongMoodProfile(
            trackId = "curr",
            title = "Song A",
            artist = "Artist A"
        )

        val sparseCandidate = OnlineSongModel(
            id = "cand_sparse",
            title = "Sparse Track",
            artist = "Unknown Artist"
        )
        val sparseProfile = SongMoodProfile(
            trackId = "cand_sparse",
            title = "Sparse Track",
            artist = "Unknown Artist",
            genre = null,
            moodLabels = emptySet(),
            valence = null,
            energy = null,
            danceability = null
        )

        val result = VibeFlowScorer.scoreCandidate(
            candidate = sparseCandidate,
            candidateProfile = sparseProfile,
            seedProfile = currentProfile,
            userProfile = UserMusicProfile()
        )

        assertThat(result.score.isNaN()).isFalse()
        assertThat(result.score.isInfinite()).isFalse()
        assertThat(result.score).isGreaterThan(0.0f)
        assertThat(result.score).isLessThan(1.01f)
    }

    @Test
    fun `artist diversity penalizes consecutive same artist`() {
        val currentProfile = SongMoodProfile(trackId = "curr", title = "Song 1", artist = "Arijit Singh")

        val sameArtist = OnlineSongModel(id = "cand_same", title = "Song 2", artist = "Arijit Singh")
        val sameProfile = SongMoodProfile(trackId = "cand_same", title = "Song 2", artist = "Arijit Singh")

        val diffArtist = OnlineSongModel(id = "cand_diff", title = "Song 3", artist = "Mohit Chauhan")
        val diffProfile = SongMoodProfile(trackId = "cand_diff", title = "Song 3", artist = "Mohit Chauhan")

        val userProfile = UserMusicProfile()

        val resultSame = VibeFlowScorer.scoreCandidate(
            candidate = sameArtist,
            candidateProfile = sameProfile,
            seedProfile = currentProfile,
            userProfile = userProfile,
            recentArtistsQueue = setOf("Arijit Singh")
        )

        val resultDiff = VibeFlowScorer.scoreCandidate(
            candidate = diffArtist,
            candidateProfile = diffProfile,
            seedProfile = currentProfile,
            userProfile = userProfile,
            recentArtistsQueue = setOf("Arijit Singh")
        )

        assertThat(resultDiff.score).isGreaterThan(resultSame.score)
    }

    @Test
    fun `liked songs receive recommendation boost and transparent explanation`() {
        val currentProfile = SongMoodProfile(trackId = "curr", title = "Song 1", artist = "Artist 1")
        val candidate = OnlineSongModel(id = "cand_liked", title = "Song 2", artist = "Artist 2")
        val candidateProfile = SongMoodProfile(trackId = "cand_liked", title = "Song 2", artist = "Artist 2")

        val unlikedProfile = UserMusicProfile(likedSongIds = emptySet())
        val likedProfile = UserMusicProfile(likedSongIds = setOf("cand_liked"))

        val unlikedResult = VibeFlowScorer.scoreCandidate(
            candidate = candidate,
            candidateProfile = candidateProfile,
            seedProfile = currentProfile,
            userProfile = unlikedProfile
        )

        val likedResult = VibeFlowScorer.scoreCandidate(
            candidate = candidate,
            candidateProfile = candidateProfile,
            seedProfile = currentProfile,
            userProfile = likedProfile
        )

        assertThat(likedResult.score).isGreaterThan(unlikedResult.score)
        assertThat(likedResult.explanation).contains("Liked Songs")
    }

    @Test
    fun `frequently skipped artists receive negative feedback penalty`() {
        val currentProfile = SongMoodProfile(trackId = "curr", title = "Song 1", artist = "Artist 1")
        val candidate = OnlineSongModel(id = "cand_test", title = "Song 2", artist = "Annoying Artist")
        val candidateProfile = SongMoodProfile(trackId = "cand_test", title = "Song 2", artist = "Annoying Artist")

        val normalProfile = UserMusicProfile()
        val skippedProfile = UserMusicProfile(skippedArtists = mapOf("Annoying Artist" to 1.0f))

        val normalResult = VibeFlowScorer.scoreCandidate(
            candidate = candidate,
            candidateProfile = candidateProfile,
            seedProfile = currentProfile,
            userProfile = normalProfile
        )

        val penalizedResult = VibeFlowScorer.scoreCandidate(
            candidate = candidate,
            candidateProfile = candidateProfile,
            seedProfile = currentProfile,
            userProfile = skippedProfile
        )

        assertThat(penalizedResult.score).isLessThan(normalResult.score)
    }
}
