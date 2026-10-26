package com.audiovault.app

import android.content.Context
import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.queue.PlaybackQueueManager
import com.audiovault.app.core.queue.PlaybackRepeatMode
import com.audiovault.app.core.queue.QueueItem
import com.audiovault.app.core.queue.QueueMode
import com.audiovault.app.core.recommendation.RecommendationService
import com.audiovault.app.data.local.entity.RecordingEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackQueueManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val playerEngine: AudioPlayerEngine = mockk(relaxed = true)
    private val recommendationService: RecommendationService = mockk(relaxed = true)

    private lateinit var queueManager: PlaybackQueueManager

    private fun createDummyItem(id: String, title: String, artist: String): QueueItem {
        return QueueItem(
            id = id,
            title = title,
            artist = artist,
            durationMs = 180000L,
            streamUrl = "http://example.com/audio/$id.mp3",
            isOnline = true
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playerEngine.playerState } returns MutableStateFlow(PlayerState())
        queueManager = PlaybackQueueManager(context, playerEngine, recommendationService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playTrackExplicitly initializes current track and upcoming list`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")
        val item3 = createDummyItem("item_3", "Song 3", "Artist 3")

        queueManager.playTrackExplicitly(item1, listOf(item2, item3))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        assertThat(state.currentTrack?.id).isEqualTo(item1.id)
        assertThat(state.upcomingQueue).hasSize(2)
        assertThat(state.upcomingQueue[0].id).isEqualTo(item2.id)
        assertThat(state.upcomingQueue[1].id).isEqualTo(item3.id)
    }

    @Test
    fun `skipToNext advances exactly one position and preserves history`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")
        val item3 = createDummyItem("item_3", "Song 3", "Artist 3")

        queueManager.playTrackExplicitly(item1, listOf(item2, item3))
        testDispatcher.scheduler.advanceUntilIdle()

        queueManager.skipToNext(isUserInitiated = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        assertThat(state.currentTrack?.id).isEqualTo(item2.id)
        assertThat(state.upcomingQueue).hasSize(1)
        assertThat(state.upcomingQueue[0].id).isEqualTo(item3.id)
        assertThat(state.playbackHistory).hasSize(1)
        assertThat(state.playbackHistory[0].id).isEqualTo(item1.id)
    }

    @Test
    fun `repeat-one keeps current track and does not consume upcoming queue on completion`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")

        queueManager.playTrackExplicitly(item1, listOf(item2))
        testDispatcher.scheduler.advanceUntilIdle()

        queueManager.setRepeatMode(PlaybackRepeatMode.ONE)

        // Simulate song completion
        queueManager.handleTrackCompletion(item1.toRecordingEntity())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        assertThat(state.currentTrack?.id).isEqualTo(item1.id)
        assertThat(state.upcomingQueue).hasSize(1)
        assertThat(state.upcomingQueue[0].id).isEqualTo(item2.id)
    }

    @Test
    fun `repeat-all cycles history back to queue when upcoming queue completes`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")

        queueManager.playTrackExplicitly(item1, listOf(item2))
        testDispatcher.scheduler.advanceUntilIdle()

        queueManager.setRepeatMode(PlaybackRepeatMode.ALL)

        // Advance to item2
        queueManager.handleTrackCompletion(item1.toRecordingEntity())
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(queueManager.queueState.value.currentTrack?.id).isEqualTo(item2.id)

        // Sleep past 800ms debounce window
        Thread.sleep(850L)

        // Now item2 completes and upcoming queue is empty; repeat-all wraps history back to queue
        queueManager.handleTrackCompletion(item2.toRecordingEntity())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        // It wraps around to item1
        assertThat(state.currentTrack?.id).isEqualTo(item1.id)
    }

    @Test
    fun `toggleRepeatMode cycles through OFF, ALL, ONE, OFF`() {
        assertThat(queueManager.queueState.value.repeatMode).isEqualTo(PlaybackRepeatMode.OFF)

        val mode1 = queueManager.toggleRepeatMode()
        assertThat(mode1).isEqualTo(PlaybackRepeatMode.ALL)

        val mode2 = queueManager.toggleRepeatMode()
        assertThat(mode2).isEqualTo(PlaybackRepeatMode.ONE)

        val mode3 = queueManager.toggleRepeatMode()
        assertThat(mode3).isEqualTo(PlaybackRepeatMode.OFF)
    }

    @Test
    fun `removeAt removes item at index safely`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")
        val item3 = createDummyItem("item_3", "Song 3", "Artist 3")

        queueManager.playTrackExplicitly(item1, listOf(item2, item3))
        testDispatcher.scheduler.advanceUntilIdle()

        queueManager.removeAt(0) // Remove item2

        val state = queueManager.queueState.value
        assertThat(state.upcomingQueue).hasSize(1)
        assertThat(state.upcomingQueue[0].id).isEqualTo(item3.id)
    }

    @Test
    fun `clearUpcoming empties upcoming queue`() = runTest(testDispatcher) {
        val item1 = createDummyItem("item_1", "Song 1", "Artist 1")
        val item2 = createDummyItem("item_2", "Song 2", "Artist 2")

        queueManager.playTrackExplicitly(item1, listOf(item2))
        testDispatcher.scheduler.advanceUntilIdle()

        queueManager.clearUpcoming()

        val state = queueManager.queueState.value
        assertThat(state.currentTrack?.id).isEqualTo(item1.id)
        assertThat(state.upcomingQueue).isEmpty()
    }

    @Test
    fun `empty queue behaves safely on skipToNext`() = runTest(testDispatcher) {
        queueManager.clearUpcoming()
        queueManager.skipToNext(isUserInitiated = false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        assertThat(state.currentTrack).isNull()
        assertThat(state.upcomingQueue).isEmpty()
    }

    @Test
    fun `playAlbum sets QueueMode ALBUM and preserves album track order without auto-replenishment`() = runTest(testDispatcher) {
        val song1 = OnlineSongModel(id = "online_s1", title = "Track 1", artist = "Artist A", album = "Album X", streamUrl = "http://example.com/s1.mp3", durationMs = 180000L)
        val song2 = OnlineSongModel(id = "online_s2", title = "Track 2", artist = "Artist A", album = "Album X", streamUrl = "http://example.com/s2.mp3", durationMs = 200000L)
        val song3 = OnlineSongModel(id = "online_s3", title = "Track 3", artist = "Artist A", album = "Album X", streamUrl = "http://example.com/s3.mp3", durationMs = 220000L)
        val album = OnlineAlbumModel(
            id = "alb_1",
            title = "Album X",
            artist = "Artist A",
            songCount = 3,
            songs = listOf(song1, song2, song3)
        )

        queueManager.playAlbum(album, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = queueManager.queueState.value
        assertThat(state.queueMode).isEqualTo(QueueMode.ALBUM)
        assertThat(state.currentTrack?.id).isEqualTo("online_s1")
        assertThat(state.upcomingQueue).hasSize(2)
        assertThat(state.upcomingQueue.map { it.id }).containsExactly("online_s2", "online_s3").inOrder()
    }

    @Test
    fun `recentCanonicalIds maintains bounded history and prevents duplicate track IDs`() = runTest(testDispatcher) {
        val items = (1..60).map { i ->
            QueueItem(
                id = "item_$i",
                title = "Song $i",
                artist = "Artist",
                durationMs = 180000L,
                streamUrl = "http://example.com/item_$i.mp3",
                isOnline = true,
                canonicalTrackId = "canon_$i"
            )
        }

        for (item in items) {
            queueManager.playTrackExplicitly(item)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val state = queueManager.queueState.value
        assertThat(state.recentCanonicalIds).hasSize(50)
        // Most recent first
        assertThat(state.recentCanonicalIds.first()).isEqualTo("canon_60")
        assertThat(state.recentCanonicalIds.last()).isEqualTo("canon_11")
        assertThat(state.recentCanonicalIds).doesNotContain("canon_10")
    }
}
