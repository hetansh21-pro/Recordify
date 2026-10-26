package com.audiovault.app.ui.record

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.audiovault.app.core.service.MonitoringState
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.components.ProfileAvatarButton
import com.audiovault.app.ui.components.SubtleJellyContainer
import com.audiovault.app.ui.home.HomeViewModel
import com.audiovault.app.ui.home.MonitoringStateCard
import com.audiovault.app.ui.home.RecentRecordingsList
import com.audiovault.app.ui.recordings.DeleteConfirmDialog
import com.audiovault.app.ui.theme.SpotifyBlack
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright
import com.audiovault.app.ui.theme.StateRecordingRed
import kotlinx.coroutines.launch

@Composable
fun RecordScreen(
    viewModel: HomeViewModel,
    onNavigateToRecordings: () -> Unit,
    userName: String = "",
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val monitoringState by viewModel.monitoringState.collectAsState()
    val detectedSong by viewModel.currentDetectedSong.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val recentRecordings by viewModel.recentRecordings.collectAsState()

    var permissionErrorMessage by remember { mutableStateOf<String?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    val isCapturingActive = monitoringState !is MonitoringState.Idle

    LaunchedEffect(Unit) {
        viewModel.checkNotificationAccess()
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            permissionErrorMessage = null
            viewModel.startMonitoring(result.resultCode, result.data!!)
        } else {
            permissionErrorMessage = "Audio capture permission is required to monitor playback."
            scope.launch {
                snackbarHostState.showSnackbar("Audio capture permission was denied.")
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun requestStartMonitoring() {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val projectionManager =
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SpotifyBlack,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
    ) { paddingValues ->
        SubtleJellyContainer(
            isAtTop = { scrollState.value == 0 },
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .padding(paddingValues)
        ) { jellyModifier ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(jellyModifier)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenDrawer() }
                    ) {
                        ProfileAvatarButton(
                            userName = userName,
                            onClick = onOpenDrawer,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Smart Capture",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Access Banner if not granted
                AnimatedVisibility(
                    visible = !isNotificationAccessGranted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SpotifyDarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = SpotifyGreenBright,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Metadata Listener",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Enable notification access to auto-tag title & artist from active playback.",
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(viewModel.createNotificationListenerSettingsIntent())
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotifyGreenBright)
                            ) {
                                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Monitoring Card
                MonitoringStateCard(
                    state = monitoringState,
                    detectedSong = detectedSong
                )

                if (permissionErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = permissionErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big Primary Action Button (Start / Stop Monitoring)
                Button(
                    onClick = {
                        if (isCapturingActive) {
                            viewModel.stopMonitoring()
                        } else {
                            requestStartMonitoring()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturingActive) StateRecordingRed else SpotifyGreenBright,
                        contentColor = if (isCapturingActive) Color.White else Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isCapturingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCapturingActive) "STOP RECORDING & MONITORING" else "START AUTO-RECORDING",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Recent Captures Section
                RecentRecordingsList(
                    recentRecordings = recentRecordings,
                    currentPlayingId = playerState.currentRecording?.id,
                    isPlaying = playerState.isPlaying,
                    onRecordingClick = { viewModel.playPauseRecording(it) },
                    onDeleteClick = { recordingToDelete = it },
                    onViewAllClick = onNavigateToRecordings
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (recordingToDelete != null) {
        DeleteConfirmDialog(
            recording = recordingToDelete!!,
            onDismiss = { recordingToDelete = null },
            onConfirmDelete = {
                viewModel.deleteRecording(recordingToDelete!!)
                recordingToDelete = null
            }
        )
    }
}
