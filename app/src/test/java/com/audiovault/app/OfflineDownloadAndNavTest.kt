package com.audiovault.app

import android.content.Context
import com.audiovault.app.core.player.AudioPlayerEngine
import com.audiovault.app.core.player.PlayerState
import com.audiovault.app.core.queue.PlaybackQueueManager
import com.audiovault.app.core.queue.QueueItem
import com.audiovault.app.core.recommendation.RecommendationService
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.data.repository.RecordingRepository
import com.audiovault.app.ui.navigation.Screen
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadAndNavTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val playerEngine: AudioPlayerEngine = mockk(relaxed = true)
    private val recommendationService: RecommendationService = mockk(relaxed = true)
    private val recordingRepository: RecordingRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playerEngine.playerState } returns MutableStateFlow(PlayerState())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigation bottom bar must contain exactly 4 screens`() {
        // Bottom bar must strictly contain 4 options: Home, Search, Library, Videos
        assertThat(Screen.items).hasSize(4)
        val routes = Screen.items.map { it.route }
        assertThat(routes).containsExactly(
            Screen.Home.route,
            Screen.Search.route,
            Screen.Library.route,
            Screen.Videos.route
        ).inOrder()

        // Record screen must NOT be in the bottom navigation items
        assertThat(routes).doesNotContain(Screen.Record.route)
    }

    @Test
    fun `playback manager prioritizes local downloaded file when offline or available`() = runTest(testDispatcher) {
        val tempFile = File.createTempFile("offline_song_test", ".m4a")
        tempFile.writeBytes(ByteArray(1024) { 1.toByte() })
        tempFile.deleteOnExit()

        val localDownloadedRecording = RecordingEntity(
            id = 42L,
            fileName = tempFile.name,
            filePath = tempFile.absolutePath,
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            durationMs = 210000L,
            fileSizeBytes = 1024L
        )

        coEvery { recordingRepository.getAllRecordings() } returns listOf(localDownloadedRecording)

        val queueManager = PlaybackQueueManager(
            context = context,
            playerEngine = playerEngine,
            recommendationService = recommendationService,
            recordingRepository = recordingRepository
        )

        // Queue item from online search with an unplayable/fake online URL
        val onlineItem = QueueItem(
            id = "online_kesariya_123",
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            streamUrl = "https://unreachable.cdn.stream/kesariya.mp3",
            isOnline = true
        )

        val playedEntitySlot = slot<RecordingEntity>()
        every { playerEngine.play(capture(playedEntitySlot)) } returns 42L

        queueManager.playTrackExplicitly(onlineItem, emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the local downloaded file path was used directly for playback!
        assertThat(playedEntitySlot.isCaptured).isTrue()
        assertThat(playedEntitySlot.captured.filePath).isEqualTo(tempFile.absolutePath)
        assertThat(playedEntitySlot.captured.id).isEqualTo(42L)
    }
}
