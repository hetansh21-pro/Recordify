package com.audiovault.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.audiovault.app.core.intelligence.CoverArtFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, zero-jank in-memory image cache and asynchronous loader.
 * Ensures 120 FPS / 60 FPS buttery smooth scrolling by:
 * 1. Zero disk I/O or bitmap decoding on the Main UI thread.
 * 2. In-memory LRU caching of decoded ImageBitmaps sized for display.
 * 3. In-memory resolution path caching to eliminate filesystem metadata scans.
 * 4. Efficient downsampling via BitmapFactory.Options.inSampleSize.
 */
object ImageMemoryCache {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of the available memory for memory bitmap cache
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16)

    private val bitmapCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // Estimate size in KB: width * height * 4 bytes / 1024
            return (value.width * value.height * 4) / 1024
        }
    }

    // Fast memory cache for resolved cover/artist file paths to avoid repeated disk folder searches
    private val pathCache = ConcurrentHashMap<String, String>()

    fun get(key: String): ImageBitmap? {
        return bitmapCache.get(key)
    }

    fun put(key: String, bitmap: ImageBitmap) {
        bitmapCache.put(key, bitmap)
    }

    fun getResolvedPath(key: String): String? {
        return pathCache[key]
    }

    fun putResolvedPath(key: String, path: String) {
        pathCache[key] = path
    }

    /**
     * Efficiently decodes and downsamples a bitmap from disk on Dispatchers.IO.
     */
    suspend fun decodeSampledBitmap(filePath: String, targetSizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() < 100) return@withContext null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

            // Calculate inSampleSize
            var inSampleSize = 1
            if (targetSizePx > 0) {
                val maxDim = maxOf(options.outWidth, options.outHeight)
                while (maxDim / (inSampleSize * 2) >= targetSizePx) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return@withContext null
            bitmap.asImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * Composable helper to asynchronously load a track cover art bitmap with 0 UI frame drops.
 */
@Composable
fun rememberAsyncCoverArt(
    albumArtPath: String?,
    title: String?,
    artist: String?,
    targetSizeDp: Dp
): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val targetSizePx = remember(targetSizeDp, density) {
        if (targetSizeDp != Dp.Unspecified && targetSizeDp.value > 0) {
            with(density) { targetSizeDp.roundToPx() }
        } else {
            256
        }
    }

    val cacheKey = remember(albumArtPath, title, artist, targetSizePx) {
        "cover_${albumArtPath ?: ""}_${title ?: ""}_${artist ?: ""}_$targetSizePx"
    }

    val imageState = remember(cacheKey) {
        mutableStateOf(ImageMemoryCache.get(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (imageState.value != null) return@LaunchedEffect

        val bitmap = withContext(Dispatchers.IO) {
            // 1. Direct explicit path check or remote URL fetch (prioritize high-res 500x500 / 600x600)
            val cleanArtUrl = albumArtPath
                ?.replace("-150x150.jpg", "-500x500.jpg")
                ?.replace("-50x50.jpg", "-500x500.jpg")
                ?.replace("100x100bb.jpg", "600x600bb.jpg")

            var resolvedPath: String? = null
            if (!cleanArtUrl.isNullOrBlank()) {
                if (cleanArtUrl.startsWith("http://", ignoreCase = true) || cleanArtUrl.startsWith("https://", ignoreCase = true)) {
                    val safeHash = Math.abs(cleanArtUrl.hashCode()).toString()
                    val cachedFile = File(context.cacheDir, "thumb_$safeHash.jpg")
                    if (cachedFile.exists() && cachedFile.length() > 500) {
                        resolvedPath = cachedFile.absolutePath
                    } else {
                        try {
                            val url = URL(cleanArtUrl)
                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                connectTimeout = 8000
                                readTimeout = 10000
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            }
                            if (conn.responseCode == 200) {
                                conn.inputStream.use { input ->
                                    cachedFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (cachedFile.exists() && cachedFile.length() > 500) {
                                    resolvedPath = cachedFile.absolutePath
                                }
                            } else if (cleanArtUrl.contains("maxresdefault.jpg")) {
                                conn.disconnect()
                                val fallbackUrl = URL(cleanArtUrl.replace("maxresdefault.jpg", "hqdefault.jpg"))
                                val fallbackConn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 5000
                                    readTimeout = 5000
                                    setRequestProperty("User-Agent", "Mozilla/5.0")
                                }
                                if (fallbackConn.responseCode == 200) {
                                    fallbackConn.inputStream.use { input ->
                                        cachedFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (cachedFile.exists() && cachedFile.length() > 500) {
                                        resolvedPath = cachedFile.absolutePath
                                    }
                                }
                                fallbackConn.disconnect()
                            }
                            conn.disconnect()
                        } catch (_: Throwable) {}
                    }
                } else {
                    val f = File(cleanArtUrl)
                    if (f.exists() && f.length() > 500) {
                        resolvedPath = cleanArtUrl
                    }
                }
            }

            // 2. Check path cache / fetcher cache if no direct path
            if (resolvedPath == null && !title.isNullOrBlank()) {
                val titleArtistKey = "path_${title}_${artist ?: ""}"
                resolvedPath = ImageMemoryCache.getResolvedPath(titleArtistKey)

                if (resolvedPath == null) {
                    resolvedPath = CoverArtFetcher.getCachedCoverArtPath(context, title, artist)
                    if (resolvedPath == null) {
                        resolvedPath = CoverArtFetcher.fetchAndCacheCoverArt(context, title, artist)
                    }
                    if (resolvedPath != null) {
                        ImageMemoryCache.putResolvedPath(titleArtistKey, resolvedPath)
                    }
                }
            }

            if (resolvedPath != null) {
                val decoded = ImageMemoryCache.decodeSampledBitmap(resolvedPath, targetSizePx)
                if (decoded != null) {
                    ImageMemoryCache.put(cacheKey, decoded)
                    decoded
                } else null
            } else null
        }

        imageState.value = bitmap
    }

    return imageState
}

/**
 * Composable helper to asynchronously load an artist avatar bitmap with 0 UI frame drops.
 */
@Composable
fun rememberAsyncArtistArt(
    artistName: String,
    songHint: String?,
    targetSizeDp: Dp
): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val targetSizePx = remember(targetSizeDp, density) {
        with(density) { targetSizeDp.roundToPx() }
    }

    val cacheKey = remember(artistName, songHint, targetSizePx) {
        "artist_${artistName}_${songHint ?: ""}_$targetSizePx"
    }

    val imageState = remember(cacheKey) {
        mutableStateOf(ImageMemoryCache.get(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (imageState.value != null) return@LaunchedEffect

        val bitmap = withContext(Dispatchers.IO) {
            val pathKey = "artist_path_$artistName"
            var resolvedPath = ImageMemoryCache.getResolvedPath(pathKey)

            if (resolvedPath == null) {
                resolvedPath = CoverArtFetcher.getCachedArtistArtPath(context, artistName)
                if (resolvedPath == null) {
                    resolvedPath = CoverArtFetcher.fetchAndCacheArtistArt(context, artistName, songHint)
                }
                if (resolvedPath != null) {
                    ImageMemoryCache.putResolvedPath(pathKey, resolvedPath)
                }
            }

            if (resolvedPath != null) {
                val decoded = ImageMemoryCache.decodeSampledBitmap(resolvedPath, targetSizePx)
                if (decoded != null) {
                    ImageMemoryCache.put(cacheKey, decoded)
                    decoded
                } else null
            } else null
        }

        imageState.value = bitmap
    }

    return imageState
}
