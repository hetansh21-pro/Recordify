package com.audiovault.app.ui.recordings

import com.audiovault.app.ui.components.SubtleJellyContainer
import androidx.compose.foundation.lazy.rememberLazyListState

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.player.AudioPlayerBottomSheet

@Composable
fun RecordingsScreen(
    viewModel: RecordingsViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForLyrics by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    val listState = rememberLazyListState()

    Scaffold(modifier = modifier) { paddingValues ->
        SubtleJellyContainer(
            isAtTop = { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { jellyModifier ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(jellyModifier)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Title Header
                Text(
                    text = "Song Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search songs, artists, albums...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sort Filter Chips
                SortFilterBar(
                    currentSort = sortOption,
                    onSortSelected = { viewModel.onSortOptionChange(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recordings List
                if (recordings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No recorded songs yet" else "No matching songs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isBlank())
                                        "Play songs in your favorite music app while AudioVault is monitoring to automatically capture and name them."
                                    else
                                        "Try searching with a different song title or artist name.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onNavigateToHome) {
                                        Text("START MONITORING")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = if (playerState.currentRecording != null) 200.dp else 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recordings, key = { it.id }) { recording ->
                            val isPlaying = playerState.isPlaying && playerState.currentRecording?.id == recording.id
                            RecordingItemCard(
                                recording = recording,
                                isPlaying = isPlaying,
                                onPlayPauseClick = { viewModel.playPauseRecording(recording) },
                                onRenameClick = { recordingToRename = recording },
                                onLyricsClick = { recordingForLyrics = recording },
                                onShareClick = {
                                    val shareIntent = viewModel.createShareIntent(recording)
                                    if (shareIntent != null) {
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Share Audio Recording")
                                        )
                                    }
                                },
                                onDeleteClick = { recordingToDelete = recording }
                            )
                        }
                    }
                }
            }

            // Bottom Audio Player sheet when active
            if (playerState.currentRecording != null) {
                AudioPlayerBottomSheet(
                    playerState = playerState,
                    onTogglePlayPause = { viewModel.playerEngine.togglePlayPause() },
                    onSeekTo = { viewModel.playerEngine.seekTo(it) },
                    onSeekRelative = { viewModel.playerEngine.seekRelative(it) },
                    onSpeedSelected = { viewModel.playerEngine.setPlaybackSpeed(it) },
                    onClose = { viewModel.playerEngine.stopAndClose() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }
    }

    // Rename Dialog
    recordingToRename?.let { recording ->
        RenameDialog(
            recording = recording,
            onDismiss = { recordingToRename = null },
            onConfirmRename = { newTitle ->
                viewModel.renameRecording(recording, newTitle)
                recordingToRename = null
            }
        )
    }

    // Lyrics Dialog
    recordingForLyrics?.let { recording ->
        LyricsDialog(
            recording = recording,
            onDismiss = { recordingForLyrics = null },
            onSaveLyrics = { lyrics ->
                viewModel.updateLyrics(recording, lyrics)
                recordingForLyrics = null
            }
        )
    }

    // Delete Confirmation Dialog
    recordingToDelete?.let { recording ->
        DeleteConfirmDialog(
            recording = recording,
            onDismiss = { recordingToDelete = null },
            onConfirmDelete = {
                viewModel.deleteRecording(recording)
                recordingToDelete = null
            }
        )
    }
}
