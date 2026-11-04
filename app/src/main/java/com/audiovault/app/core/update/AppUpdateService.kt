package com.audiovault.app.core.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.audiovault.app.MainActivity
import com.audiovault.app.R
import com.audiovault.app.ui.tv.TvMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "Recordify/AppUpdate"
private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/hetansh21-pro/Recordify/releases/latest"
private const val UPDATE_CHANNEL_ID = "recordify_updates"
private const val NOTIFICATION_ID_UPDATE = 9001
private const val PREFS_NAME = "recordify_update_prefs"
private const val KEY_LAST_NOTIFIED_TAG = "last_notified_tag"

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val isUpdateAvailable: Boolean,
    val publishedAt: String
)

object AppUpdateService {
    const val CURRENT_APP_VERSION = "1.0.0"

    private val _updateAvailable = MutableStateFlow<AppUpdateInfo?>(null)
    val updateAvailable: StateFlow<AppUpdateInfo?> = _updateAvailable.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private var downloadCompleteReceiverRegistered = false

    /**
     * Checks for new APK updates from GitHub Releases API asynchronously.
     */
    fun checkForUpdates(
        context: Context,
        isTv: Boolean,
        manualCheck: Boolean = false,
        onResult: ((AppUpdateInfo?) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            _isChecking.value = true
            try {
                val updateInfo = fetchLatestRelease(appContext, isTv)
                withContext(Dispatchers.Main) {
                    _isChecking.value = false
                    if (updateInfo != null && updateInfo.isUpdateAvailable) {
                        _updateAvailable.value = updateInfo

                        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val lastNotifiedTag = prefs.getString(KEY_LAST_NOTIFIED_TAG, null)

                        // Post system notification if new version or if manual check
                        if (manualCheck || lastNotifiedTag != updateInfo.latestVersion) {
                            postUpdateNotification(appContext, updateInfo, isTv)
                            prefs.edit().putString(KEY_LAST_NOTIFIED_TAG, updateInfo.latestVersion).apply()
                        }

                        onResult?.invoke(updateInfo)
                    } else {
                        onResult?.invoke(updateInfo)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for updates", e)
                withContext(Dispatchers.Main) {
                    _isChecking.value = false
                    onResult?.invoke(null)
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        _updateAvailable.value = null
    }

    private fun fetchLatestRelease(context: Context, isTv: Boolean): AppUpdateInfo? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Recordify-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub release check returned status: $responseCode")
                return null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()

            val json = JSONObject(sb.toString())
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "New Update Available")
            val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
            val publishedAt = json.optString("published_at", "")
            val assetsArray = json.optJSONArray("assets")

            if (tagName.isBlank() || assetsArray == null) {
                return null
            }

            var chosenApkUrl = ""
            var chosenApkName = ""

            val targetApkKeyword = if (isTv) "TV" else "Recordify.apk"

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")

                if (isTv) {
                    if (assetName.contains("TV", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true)) {
                        chosenApkUrl = downloadUrl
                        chosenApkName = assetName
                        break
                    }
                } else {
                    if (assetName.equals("Recordify.apk", ignoreCase = true)) {
                        chosenApkUrl = downloadUrl
                        chosenApkName = assetName
                        break
                    }
                }
            }

            // Fallback: choose any apk found in release assets if specific name didn't match
            if (chosenApkUrl.isBlank()) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        chosenApkUrl = downloadUrl
                        chosenApkName = assetName
                        break
                    }
                }
            }

            if (chosenApkUrl.isBlank()) {
                chosenApkUrl = json.optString("html_url", "https://github.com/hetansh21-pro/Recordify/releases")
                chosenApkName = if (isTv) "Recordify.TV.apk" else "Recordify.apk"
            }

            val currentVersion = CURRENT_APP_VERSION
            val isNewer = isNewerVersion(tagName, currentVersion)

            Log.d(TAG, "Update check result: remote=$tagName, current=$currentVersion, isNewer=$isNewer, url=$chosenApkUrl")

            return AppUpdateInfo(
                latestVersion = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkDownloadUrl = chosenApkUrl,
                apkFileName = chosenApkName,
                isUpdateAvailable = isNewer,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching release from GitHub", e)
            return null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Compares version numbers (e.g., v1.0.1 > 1.0.0).
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        return false
    }

    /**
     * Posts a rich system notification to Android's notification bar when an update is available.
     */
    fun postUpdateNotification(context: Context, updateInfo: AppUpdateInfo, isTv: Boolean = false) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Recordify App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when a new version of Recordify is available"
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val targetActivityClass = if (isTv) TvMainActivity::class.java else MainActivity::class.java

        val contentIntent = Intent(context, targetActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_UPDATE_DIALOG", true)
            putExtra("UPDATE_VERSION", updateInfo.latestVersion)
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct browser/download intent for the notification action button
        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkDownloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingDownloadIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE + 1,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recordify Update Available: ${updateInfo.latestVersion}")
            .setContentText("A new version of Recordify is ready. Tap to update.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Recordify ${updateInfo.latestVersion} Available")
                    .bigText(
                        "${updateInfo.releaseTitle}\n\n${updateInfo.releaseNotes}\n\nTap to download and install the latest update."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Download APK",
                pendingDownloadIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
    }

    /**
     * Downloads the APK file using Android's DownloadManager and triggers installation.
     */
    fun downloadAndInstallApk(context: Context, updateInfo: AppUpdateInfo) {
        val appContext = context.applicationContext
        try {
            Toast.makeText(appContext, "Starting download of ${updateInfo.apkFileName}...", Toast.LENGTH_LONG).show()

            val request = DownloadManager.Request(Uri.parse(updateInfo.apkDownloadUrl)).apply {
                setTitle("Downloading Recordify (${updateInfo.latestVersion})")
                setDescription("New version update download in progress")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, updateInfo.apkFileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            registerDownloadReceiver(appContext, downloadId, updateInfo.apkFileName)

            // Also launch browser download link as immediate option
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkDownloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)

        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed, launching browser directly", e)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkDownloadUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(appContext, "Could not open download link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerDownloadReceiver(context: Context, targetDownloadId: Long, apkFileName: String) {
        if (downloadCompleteReceiverRegistered) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == targetDownloadId) {
                        try {
                            val downloadFile = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                apkFileName
                            )
                            if (downloadFile.exists()) {
                                installApk(ctx, downloadFile)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error installing downloaded APK", e)
                        }
                    }
                }
            }
        }

        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            downloadCompleteReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register download receiver", e)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(installIntent)
    }
}
