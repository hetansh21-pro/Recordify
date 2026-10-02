package com.audiovault.app.data.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.audiovault.app.core.intelligence.SongMetadata
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages audio recording files, directory structures, smart song naming,
 * and secure FileProvider sharing.
 */
class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioVault/FileManager"
        private const val FILE_PREFIX = "Recording_"
        private const val FILE_EXTENSION = ".m4a"
        private val ILLEGAL_CHARACTERS = Regex("[\\\\/:*?\"<>|]")
    }

    val recordingsDirectory: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "recordings")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Generates a smart recording target file based on Song Metadata if available,
     * or timestamp otherwise.
     */
    fun createNewRecordingFile(metadata: SongMetadata? = null): File {
        val baseName = if (metadata != null && metadata.isIdentified) {
            val cleanTitle = sanitizeFilename(metadata.title)
            if (!metadata.artist.isNullOrBlank()) {
                val cleanArtist = sanitizeFilename(metadata.artist)
                "$cleanArtist - $cleanTitle"
            } else {
                cleanTitle
            }
        } else {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            "$FILE_PREFIX$timeStamp"
        }

        var targetFile = File(recordingsDirectory, "$baseName$FILE_EXTENSION")
        var counter = 1

        while (targetFile.exists()) {
            targetFile = File(recordingsDirectory, "$baseName ($counter)$FILE_EXTENSION")
            counter++
        }

        return targetFile
    }

    fun generateDefaultTitle(metadata: SongMetadata? = null, date: Date = Date()): String {
        if (metadata != null && metadata.isIdentified) {
            return metadata.title
        }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return "Recording ${dateFormat.format(date)}"
    }

    fun sanitizeFilename(name: String): String {
        return name.replace(ILLEGAL_CHARACTERS, "_").trim().take(120)
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted file: $filePath (success: $deleted)")
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $filePath", e)
            false
        }
    }

    fun getTotalStorageUsedBytes(): Long {
        return try {
            recordingsDirectory.walkTopDown()
                .filter { it.isFile && it.extension == "m4a" }
                .sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage used", e)
            0L
        }
    }

    fun clearAllRecordingFiles(): Boolean {
        return try {
            recordingsDirectory.listFiles { file -> file.extension == "m4a" }
                ?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing recordings directory", e)
            false
        }
    }

    fun getShareableUri(filePath: String): Uri? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FileProvider URI for $filePath", e)
            null
        }
    }

    fun saveCoverArtBitmap(bitmap: android.graphics.Bitmap, title: String, artist: String?): String? {
        return try {
            val artDir = File(context.filesDir, "album_art").apply { if (!exists()) mkdirs() }
            val name = sanitizeFilename(if (!artist.isNullOrBlank()) "$artist - $title" else title)
            val file = File(artDir, "${name.hashCode()}_art.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover art bitmap", e)
            null
        }
    }
}
