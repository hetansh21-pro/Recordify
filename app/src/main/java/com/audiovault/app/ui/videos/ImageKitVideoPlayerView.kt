package com.audiovault.app.ui.videos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile

private const val TAG = "ImageKitPlayer"
private const val BASE_URL = "https://recordify.local/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImageKitVideoPlayerView(
    video: DownloadedVideoEntity,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val file = remember(video.filePath) { File(video.filePath) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.BLACK)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onClose() {
                                onClose()
                            }

                            @JavascriptInterface
                            fun onError(message: String) {
                                Log.e(TAG, "ImageKit Video Player JS Error: $message")
                                onError(message)
                            }
                        },
                        "AndroidBridge"
                    )

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customView = null
                            customViewCallback = null
                        }

                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            if (consoleMessage != null) {
                                Log.d(TAG, "[JS] ${consoleMessage.message()} (line ${consoleMessage.lineNumber()})")
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val uri = request?.url ?: return null
                            val path = uri.path ?: ""

                            return when {
                                path.endsWith("styles.css") -> {
                                    try {
                                        val stream = ctx.assets.open("imagekit/styles.css")
                                        val headers = mapOf(
                                            "Content-Type" to "text/css",
                                            "Access-Control-Allow-Origin" to "*"
                                        )
                                        WebResourceResponse("text/css", "UTF-8", 200, "OK", headers, stream)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error loading styles.css from assets", e)
                                        null
                                    }
                                }

                                path.endsWith("index.global.js") -> {
                                    try {
                                        val stream = ctx.assets.open("imagekit/index.global.js")
                                        val headers = mapOf(
                                            "Content-Type" to "application/javascript",
                                            "Access-Control-Allow-Origin" to "*"
                                        )
                                        WebResourceResponse("application/javascript", "UTF-8", 200, "OK", headers, stream)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error loading index.global.js from assets", e)
                                        null
                                    }
                                }

                                path.endsWith("video.mp4") -> {
                                    handleVideoByteRangeRequest(file, request)
                                }

                                else -> super.shouldInterceptRequest(view, request)
                            }
                        }
                    }

                    // Build and load player HTML page
                    val htmlContent = buildPlayerHtml(video)
                    loadDataWithBaseURL(BASE_URL, htmlContent, "text/html", "UTF-8", null)

                    webViewRef = this
                }
            },
            update = { /* Updates handled via player lifecycle */ },
            modifier = Modifier.fillMaxSize()
        )

        // Custom View for native full-screen video overlay
        if (customView != null) {
            AndroidView(
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        addView(
                            customView,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Handles HTTP Byte-Range requests (RFC 7233) for HTML5 video playback in WebView.
 * Allows seeking, scrubbing, and smooth playback without downloading or buffering entire file into memory.
 */
private fun handleVideoByteRangeRequest(file: File, request: WebResourceRequest): WebResourceResponse? {
    if (!file.exists()) {
        Log.w(TAG, "Video file does not exist: ${file.absolutePath}")
        return null
    }

    val totalLength = file.length()
    if (totalLength <= 0L) return null

    val rangeHeader = request.requestHeaders?.get("Range")
        ?: request.requestHeaders?.get("range")

    return try {
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val rangeVal = rangeHeader.removePrefix("bytes=").trim()
            val parts = rangeVal.split("-")
            val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val end = if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].toLongOrNull() ?: (totalLength - 1)
            } else {
                totalLength - 1
            }.coerceAtMost(totalLength - 1)

            val contentLength = (end - start + 1).coerceAtLeast(0L)
            val raf = RandomAccessFile(file, "r")
            raf.seek(start)

            val limitedInputStream = object : InputStream() {
                private var bytesLeft = contentLength

                override fun read(): Int {
                    if (bytesLeft <= 0L) return -1
                    val b = raf.read()
                    if (b != -1) bytesLeft--
                    return b
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (bytesLeft <= 0L) return -1
                    val toRead = len.toLong().coerceAtMost(bytesLeft).toInt()
                    val bytesRead = raf.read(b, off, toRead)
                    if (bytesRead != -1) bytesLeft -= bytesRead
                    return bytesRead
                }

                override fun close() {
                    raf.close()
                }
            }

            val headers = mapOf(
                "Content-Type" to "video/mp4",
                "Content-Range" to "bytes $start-$end/$totalLength",
                "Accept-Ranges" to "bytes",
                "Content-Length" to contentLength.toString(),
                "Access-Control-Allow-Origin" to "*"
            )
            WebResourceResponse("video/mp4", "UTF-8", 206, "Partial Content", headers, limitedInputStream)
        } else {
            val headers = mapOf(
                "Content-Type" to "video/mp4",
                "Accept-Ranges" to "bytes",
                "Content-Length" to totalLength.toString(),
                "Access-Control-Allow-Origin" to "*"
            )
            WebResourceResponse("video/mp4", "UTF-8", 200, "OK", headers, FileInputStream(file))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error streaming video byte range", e)
        null
    }
}

private fun buildPlayerHtml(video: DownloadedVideoEntity): String {
    val videoSrc = if (video.filePath.startsWith("http://") || video.filePath.startsWith("https://")) {
        video.filePath
    } else {
        "${BASE_URL}video.mp4"
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>${escapeHtml(video.title)}</title>
            <link rel="stylesheet" href="${BASE_URL}imagekit/styles.css">
            <style>
                * {
                    box-sizing: border-box;
                    user-select: none;
                    -webkit-user-select: none;
                    margin: 0;
                    padding: 0;
                }
                html, body {
                    width: 100vw;
                    height: 100vh;
                    background-color: #000;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .player-container {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    position: relative;
                }
                .video-js {
                    width: 100% !important;
                    height: 100% !important;
                    background-color: #000 !important;
                }
                .video-js .vjs-big-play-button {
                    border-color: #00E676 !important;
                    background-color: rgba(0, 0, 0, 0.75) !important;
                    border-radius: 50% !important;
                    width: 72px !important;
                    height: 72px !important;
                    line-height: 68px !important;
                    font-size: 38px !important;
                    margin-top: -36px !important;
                    margin-left: -36px !important;
                    color: #00E676 !important;
                }
                .video-js .vjs-play-progress,
                .video-js .vjs-volume-level,
                .video-js .vjs-slider-bar {
                    background-color: #00E676 !important;
                }
                .video-js .vjs-load-progress {
                    background: rgba(255, 255, 255, 0.25) !important;
                }
                .video-js .vjs-control-bar {
                    background: linear-gradient(to top, rgba(0,0,0,0.92) 0%, rgba(0,0,0,0.55) 60%, transparent 100%) !important;
                    height: 54px !important;
                }
                .vjs-button > .vjs-icon-placeholder:before {
                    font-size: 22px !important;
                    line-height: 54px !important;
                }
                .vjs-time-control {
                    line-height: 54px !important;
                    font-size: 13px !important;
                    font-weight: 600 !important;
                    color: #fff !important;
                }
            </style>
        </head>
        <body>
            <div class="player-container">
                <video
                    id="ik-video"
                    class="video-js vjs-default-skin vjs-big-play-centered"
                    controls
                    autoplay
                    playsinline
                    preload="auto">
                </video>
            </div>

            <script src="${BASE_URL}imagekit/index.global.js"></script>
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    try {
                        var player;
                        if (window.ImageKitVideoPlayer && typeof window.ImageKitVideoPlayer.videoPlayer === 'function') {
                            player = window.ImageKitVideoPlayer.videoPlayer('ik-video', {
                                imagekitId: 'recordify',
                                seekThumbnails: false,
                                hideContextMenu: false
                            }, {
                                autoplay: true,
                                controls: true,
                                responsive: true,
                                fluid: false,
                                playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2]
                            });
                            player.src({
                                src: '$videoSrc',
                                type: 'video/mp4'
                            });
                            player.ready(function() {
                                player.play().catch(function(e) { console.log('Autoplay deferred:', e); });
                            });
                        } else if (window.videojs) {
                            player = window.videojs('ik-video', {
                                autoplay: true,
                                controls: true,
                                playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2]
                            });
                            player.src({
                                src: '$videoSrc',
                                type: 'video/mp4'
                            });
                        }
                    } catch (err) {
                        console.error('Player initialization error:', err);
                        if (window.AndroidBridge && window.AndroidBridge.onError) {
                            window.AndroidBridge.onError(err.message || 'Player init failed');
                        }
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
