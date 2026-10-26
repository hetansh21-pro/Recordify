package com.audiovault.app.ui.videos

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.audiovault.app.ui.components.SubtleJellyContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.R
import com.audiovault.app.core.downloader.DownloadState
import com.audiovault.app.core.downloader.VideoFormatInfo
import com.audiovault.app.core.downloader.YouTubeVideoInfo
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import com.audiovault.app.ui.components.TrackCoverArt
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideosScreen(
    viewModel: VideosViewModel,
    userName: String = "",
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var videoToDelete by remember { mutableStateOf<DownloadedVideoEntity?>(null) }
    val lazyListState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        SubtleJellyContainer(
            isAtTop = { lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0 },
            modifier = Modifier.fillMaxSize()
        ) { jellyModifier ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(jellyModifier),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenDrawer() }
                    ) {
                        com.audiovault.app.ui.components.ProfileAvatarButton(
                            userName = userName,
                            onClick = onOpenDrawer,
                            size = 38.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Video Downloader",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Save YouTube HD Videos to Device",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFB3B3B3)
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SpotifyCardSurface
                    ) {
                        Text(
                            text = "${downloadedVideos.size} Videos",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreenBright
                            )
                        )
                    }
                }
            }

            // URL Input Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpotifyDarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Paste YouTube Link",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Supports standard videos, shorts, and music videos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF888888)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Input Field with Paste & Clear buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpotifyCardSurface)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(SpotifyGreenBright),
                                decorationBox = { innerTextField ->
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            text = "https://youtube.com/watch?v=...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFFB3B3B3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                                        clipboard.primaryClip?.getItemAt(0)?.text?.let {
                                            urlInput = it.toString()
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = SpotifyGreenBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fetch / Download Action Button
                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    val inputToFetch = urlInput.trim()
                                    urlInput = ""
                                    viewModel.fetchVideoInfo(inputToFetch)
                                }
                            },
                            enabled = urlInput.isNotBlank() && downloadState !is DownloadState.FetchingInfo && downloadState !is DownloadState.Downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreenBright,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF333333),
                                disabledContentColor = Color(0xFF777777)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fetch & Download Video",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Download Status Card (Stateful)
            item {
                AnimatedVisibility(
                    visible = downloadState !is DownloadState.Idle,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    DownloadStatusCard(
                        downloadState = downloadState,
                        onStartDownload = { info, format -> viewModel.startDownload(info, format) },
                        onCancel = { viewModel.cancelDownload() },
                        onDismiss = { viewModel.resetDownloadState() },
                        onPlaySuccess = { video ->
                            viewModel.resetDownloadState()
                            viewModel.playVideo(video)
                        }
                    )
                }
            }

            // Downloaded Videos Section Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloaded Videos",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        if (downloadedVideos.isNotEmpty()) {
                            Text(
                                text = "Saved to Gallery",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Search within downloaded videos
                    if (downloadedVideos.isNotEmpty() || searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(SpotifyCardSurface)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                cursorBrush = SolidColor(SpotifyGreenBright),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search downloaded videos...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF777777)
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFFB3B3B3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Downloaded Videos List or Empty State
            if (downloadedVideos.isEmpty()) {
                item {
                    EmptyVideosPlaceholder()
                }
            } else {
                items(downloadedVideos, key = { it.id }) { video ->
                    DownloadedVideoItem(
                        video = video,
                        onClick = { viewModel.playVideo(video) },
                        onShare = {
                            val shareIntent = viewModel.createShareIntent(video)
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                            }
                        },
                        onDelete = { videoToDelete = video }
                    )
                }
            }
        }
    }

        // Delete Confirmation Dialog
        if (videoToDelete != null) {
            val video = videoToDelete!!
            AlertDialog(
                onDismissRequest = { videoToDelete = null },
                title = {
                    Text(
                        text = "Delete Video?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "This will remove \"${video.title}\" and delete the file from your device.",
                        color = Color(0xFFB3B3B3)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteVideo(video)
                            videoToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = SpotifyDarkSurface
            )
        }
    }
}

@Composable
private fun DownloadStatusCard(
    downloadState: DownloadState,
    onStartDownload: (YouTubeVideoInfo, VideoFormatInfo?) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onPlaySuccess: (DownloadedVideoEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (downloadState) {
                is DownloadState.FetchingInfo -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SpotifyGreenBright,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = "Extracting Video...",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Fetching stream details and quality options",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888))
                            )
                        }
                    }
                }

                is DownloadState.ReadyToDownload -> {
                    val info = downloadState.videoInfo
                    var selectedFormat by remember { mutableStateOf(info.bestStream) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                        ) {
                            TrackCoverArt(
                                albumArtPath = info.thumbnailUrl,
                                contentDescription = info.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Duration badge
                            if (info.durationSeconds > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = formatSeconds(info.durationSeconds),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = info.author,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quality Selection Chips
                    Text(
                        text = "Select Download Quality:",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF888888))
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val progressiveFormats = info.formats.filter { it.isAudioAndVideo }.distinctBy { it.qualityLabel }
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        progressiveFormats.forEach { format ->
                            val isSelected = selectedFormat?.qualityLabel == format.qualityLabel
                            val sizeMb = if (format.contentLength > 0) {
                                format.contentLength / (1024.0 * 1024.0)
                            } else 0.0
                            val labelText = if (sizeMb > 0.05) {
                                "${format.qualityLabel} • ${String.format(Locale.US, "%.1f MB", sizeMb)}"
                            } else {
                                format.qualityLabel
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFormat = format },
                                label = {
                                    Text(
                                        text = labelText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreenBright,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF282828),
                                    labelColor = Color.White
                                ),
                                border = null,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    selectedFormat?.let { sel ->
                        val selSizeMb = if (sel.contentLength > 0) sel.contentLength / (1024.0 * 1024.0) else 0.0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222222),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${sel.qualityLabel} Resolution",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "Video & Audio stream • MP4 container",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB3B3B3))
                                    )
                                }
                                if (selSizeMb > 0.05) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SpotifyGreenBright.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f MB", selSizeMb),
                                            color = SpotifyGreenBright,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }

                        Button(
                            onClick = { onStartDownload(info, selectedFormat) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreenBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Download",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is DownloadState.Downloading -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Downloading: ${downloadState.videoInfo.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitleText = if (downloadState.bytesDownloaded > 0L) {
                                    String.format(
                                        Locale.US,
                                        "%.1f MB / %.1f MB (%.1f MB/s)",
                                        downloadState.bytesDownloaded / (1024f * 1024f),
                                        downloadState.totalBytes / (1024f * 1024f),
                                        downloadState.speedMbPerSec
                                    )
                                } else {
                                    "Connecting to fast download stream..."
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(color = SpotifyGreenBright)
                                )
                            }

                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (downloadState.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SpotifyGreenBright,
                                trackColor = Color(0xFF282828)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SpotifyGreenBright,
                                trackColor = Color(0xFF282828)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (downloadState.progress > 0f) "${(downloadState.progress * 100).toInt()}% completed" else "Starting download...",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF888888)),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                is DownloadState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Complete!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SpotifyGreenBright
                                )
                            )
                            Text(
                                text = downloadState.video.title,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row {
                            Button(
                                onClick = { onPlaySuccess(downloadState.video) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotifyGreenBright,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                is DownloadState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Failed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            )
                            Text(
                                text = downloadState.message,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun DownloadedVideoItem(
    video: DownloadedVideoEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyDarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay & Duration badge
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF242424))
            ) {
                TrackCoverArt(
                    albumArtPath = video.thumbnailPath,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge
                if (video.durationSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = formatSeconds(video.durationSeconds),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB3B3B3)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SpotifyGreenBright.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = video.resolution,
                            color = SpotifyGreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "•",
                        color = Color(0xFF666666),
                        fontSize = 10.sp
                    )

                    Text(
                        text = formatFileSize(video.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF888888))
                    )
                }
            }

            // Actions: Share & Delete
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyVideosPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SpotifyCardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = SpotifyGreenBright,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Downloaded Videos Yet",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Paste any YouTube link above to download HD videos and watch them offline anytime directly in the app.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun formatSeconds(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    val hours = mins / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins % 60, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format(Locale.US, "%.1f MB", mb)
}
