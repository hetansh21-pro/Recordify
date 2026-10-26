package com.audiovault.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.audiovault.app.data.local.dao.ListeningHistoryDao
import com.audiovault.app.data.local.dao.RecordingDao
import com.audiovault.app.data.local.dao.VideoDao
import com.audiovault.app.data.local.entity.DownloadedVideoEntity
import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import com.audiovault.app.data.local.entity.RecordingEntity

/**
 * Room Database for AudioVault.
 */
@Database(
    entities = [RecordingEntity::class, DownloadedVideoEntity::class, ListeningHistoryEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao
    abstract fun videoDao(): VideoDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao

    companion object {
        private const val DATABASE_NAME = "audiovault.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
