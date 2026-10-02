package com.audiovault.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.audiovault.app.R
import com.audiovault.app.core.intelligence.CoverArtFetcher
import com.audiovault.app.ui.theme.SpotifyCardSurface
import java.io.File

/**
 * Reusable cover art image loader.
 * Displays official high-res album artwork when available with smooth scaling,
 * or falls back cleanly to the circular Recordify logo.
 */
@Composable
fun TrackCoverArt(
    albumArtPath: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    artist: String? = null,
    size: Dp = Dp.Unspecified,
    shape: Shape = RoundedCornerShape(8.dp),
    placeholderIconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val resolvedModifier = if (size != Dp.Unspecified) modifier.size(size) else modifier

    var currentPath by remember(albumArtPath, title, artist) {
        mutableStateOf(
            albumArtPath?.takeIf { File(it).exists() && File(it).length() > 500 }
                ?: if (!title.isNullOrBlank()) CoverArtFetcher.getCachedCoverArtPath(context, title, artist) else null
        )
    }

    LaunchedEffect(albumArtPath, title, artist) {
        if (currentPath == null && !title.isNullOrBlank()) {
            val cached = CoverArtFetcher.getCachedCoverArtPath(context, title, artist)
            if (cached != null) {
                currentPath = cached
            } else {
                val fetched = CoverArtFetcher.fetchAndCacheCoverArt(context, title, artist)
                if (fetched != null) {
                    currentPath = fetched
                }
            }
        }
    }

    val imageBitmap = remember(currentPath) {
        if (!currentPath.isNullOrBlank()) {
            val file = File(currentPath!!)
            if (file.exists() && file.length() > 0) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = resolvedModifier.clip(shape)
        )
    } else {
        Box(
            modifier = resolvedModifier
                .clip(shape)
                .background(SpotifyCardSurface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_recordify_logo),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(placeholderIconSize)
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Reusable circular artist profile photo component.
 * Fetches the real artist photo once from Deezer / iTunes and caches it permanently on disk.
 * Renders the photo with smooth circle clipping, or falls back to a Spotify-style initial badge.
 */
@Composable
fun ArtistAvatar(
    artistName: String,
    modifier: Modifier = Modifier,
    songHint: String? = null,
    size: Dp = 64.dp,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    var cachedPath by remember(artistName) {
        mutableStateOf<String?>(CoverArtFetcher.getCachedArtistArtPath(context, artistName))
    }

    LaunchedEffect(artistName, songHint) {
        val existing = CoverArtFetcher.getCachedArtistArtPath(context, artistName)
        if (existing != null) {
            cachedPath = existing
        } else {
            val fetched = CoverArtFetcher.fetchAndCacheArtistArt(context, artistName, songHint)
            if (fetched != null) {
                cachedPath = fetched
            }
        }
    }

    val currentPath = cachedPath
    val imageBitmap = remember(currentPath) {
        if (!currentPath.isNullOrBlank()) {
            val file = File(currentPath)
            if (file.exists() && file.length() > 0) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription ?: artistName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2E7D32),
                            Color(0xFF1B5E20),
                            Color(0xFF003300)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artistName.take(1).uppercase(),
                style = if (size > 70.dp) {
                    MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            )
        }
    }
}
