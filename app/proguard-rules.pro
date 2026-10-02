# Room Proguard Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer Proguard Rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coroutines Proguard Rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Data Classes
-keepclassmembers class com.audiovault.app.data.local.entity.** { *; }
