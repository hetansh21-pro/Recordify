package com.audiovault.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.core.online.AlbumLinkImporter
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.theme.SpotifyCardSurface
import com.audiovault.app.ui.theme.SpotifyDarkSurface
import com.audiovault.app.ui.theme.SpotifyGreenBright

@Composable
fun ImportAlbumDialog(
    viewModel: RecordingsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf("") }
    val isImporting by viewModel.isImportingAlbum.collectAsState()
    val importMessage by viewModel.importAlbumMessage.collectAsState()

    // Auto-detect if clipboard already contains a music album link
    LaunchedEffect(Unit) {
        try {
            val clip = clipboardManager.getText()?.text?.trim() ?: ""
            if (clip.isNotBlank() && AlbumLinkImporter.isSupportedLink(clip)) {
                urlInput = clip
            }
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) {
                viewModel.clearImportMessage()
                onDismiss()
            }
        },
        containerColor = SpotifyDarkSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3823),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Paste Album Link",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Spotify • JioSaavn • YouTube",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Paste any album or playlist link. Recordify will fetch all song names, find the full-length audio tracks, and create a complete album for you.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB3B3B3),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            text = "https://open.spotify.com/album/...",
                            color = Color(0xFF6E6E6E),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    enabled = !isImporting,
                    trailingIcon = {
                        if (urlInput.isNotBlank()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text?.trim() ?: ""
                                    if (clip.isNotBlank()) {
                                        urlInput = clip
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = SpotifyGreenBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotifyGreenBright,
                        unfocusedBorderColor = Color(0xFF383838),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = SpotifyGreenBright
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (importMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = importMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (importMessage?.contains("Success", ignoreCase = true) == true) SpotifyGreenBright else Color(0xFFFFB74D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                if (isImporting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = SpotifyGreenBright,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fetching tracklist & matching streams...",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isNotBlank() && !isImporting) {
                        viewModel.importAlbumFromLink(urlInput) {
                            onDismiss()
                        }
                    }
                },
                enabled = urlInput.isNotBlank() && !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreenBright,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color(0xFF777777)
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Import Album",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isImporting) {
                        viewModel.clearImportMessage()
                        onDismiss()
                    }
                },
                enabled = !isImporting
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFFB3B3B3),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        modifier = modifier
    )
}
