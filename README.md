# AudioVault - Automated Audio Playback Capture for Android

AudioVault is a complete, production-ready Android application built in Kotlin with Jetpack Compose. It monitors eligible device playback audio using Android's official `AudioPlaybackCapture` and `MediaProjection` APIs, automatically creating separated local AAC/M4A audio recordings when media playback starts and stops.

---

## Important Notice: Official Playback Capture Limitations

> **"AudioVault can only capture playback audio that Android and the source application make available through the official playback-capture mechanism. It does not bypass DRM or application restrictions."**

- **Eligibility Policy**: Audio playback capture is restricted by Android platform security (`AudioPlaybackCaptureConfiguration`). It only captures audio from applications that have opted in (`setAllowedCapturePolicy(ALLOW_ALL)`) or targeted Android 10+ without `ALLOW_NONE`.
- **DRM Protection**: Applications containing DRM-protected content (e.g. Spotify, Netflix) or system-protected usages (`USAGE_VOICE_COMMUNICATION`, `USAGE_NOTIFICATION`) are prohibited from capture by Android. AudioVault respects platform boundaries and will not capture DRM streams.
- **Privacy First**: AudioVault runs 100% offline. No audio or usage data is ever collected, uploaded, or transmitted.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3 (Dynamic Color, Dark & Light themes)
- **Minimum SDK**: 29 (Android 10 - required for `AudioPlaybackCaptureConfiguration`)
- **Target SDK**: 35 (Latest stable Android SDK)
- **Architecture**: MVVM / Clean Architecture with StateFlow & Kotlin Coroutines
- **Foreground Service**: `AudioCaptureService` (`FOREGROUND_SERVICE_MEDIA_PROJECTION`)
- **Audio Capture**: `AudioPlaybackCaptureConfiguration` + `AudioRecord` (16-bit PCM, 44.1kHz stereo/mono)
- **Audio Detection**: `AudioActivityDetector` (RMS dBFS energy, hysteresis, silence timeout counter, 500ms pre-roll circular buffer)
- **Audio Encoding**: Hardware AAC-LC via `MediaCodec` muxed into `.m4a` via `MediaMuxer` (96/128/192 kbps)
- **Local Storage**: App-specific external/internal media storage with `FileProvider` for secure Sharesheet
- **Database**: Room Database (`RecordingEntity`, `RecordingDao`, `AppDatabase`)
- **Playback Engine**: Android Media3 / ExoPlayer (Seek, Speed adjustments, Background audio handling)

---

## Application Structure

```
com.audiovault.app/
├── AudioVaultApplication.kt          # Application initialization & DI container
├── MainActivity.kt                   # Single-activity Compose host
├── core/
│   ├── audio/
│   │   ├── AudioCaptureConfig.kt     # Sample rate, channel, thresholds, timeouts
│   │   ├── AudioActivityDetector.kt  # RMS energy, hysteresis & silence detector
│   │   ├── AacAudioEncoder.kt        # MediaCodec AAC-LC + MediaMuxer M4A pipeline
│   │   └── PlaybackAudioCapture.kt   # AudioPlaybackCapture & AudioRecord manager
│   ├── service/
│   │   ├── MonitoringState.kt        # StateFlow representation of capture state
│   │   ├── AudioCaptureService.kt    # Foreground service & MediaProjection lifecycle
│   │   └── ServiceNotificationManager.kt # Low-latency ongoing notification
│   └── player/
│       └── AudioPlayerEngine.kt      # Media3 ExoPlayer integration
├── data/
│   ├── local/
│   │   ├── entity/RecordingEntity.kt # Room database entity
│   │   ├── dao/RecordingDao.kt       # Room DAO (search, sort, CRUD)
│   │   └── AppDatabase.kt            # Room Database definition
│   ├── storage/
│   │   └── FileManager.kt            # Local file storage, filenames, FileProvider
│   └── repository/
│       ├── RecordingRepository.kt    # Data flow repository & sorting
│       └── UserPreferencesRepository.kt # DataStore settings management
├── ui/
│   ├── theme/                        # Material 3 colors, typography, theme
│   ├── navigation/                   # Screen destinations, bottom bar, nav graph
│   ├── home/                         # Main monitoring radar, live visualizer & stats
│   ├── recordings/                   # Library, search, sort, rename, share, delete
│   ├── player/                       # Bottom sheet scrubbable audio player
│   └── settings/                     # Configurable thresholds, cleanup, about card
└── di/
    └── AppContainer.kt               # Clean dependency injection
```

---

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer / Ladybug (2024.2.1+)
- JDK 17 (set in Android Studio or `JAVA_HOME`)
- Android SDK 35 installed via SDK Manager

### 1. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Build Release APK
```bash
./gradlew assembleRelease
```
The output APK will be located at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 3. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## Required Android Permissions Rationale

| Permission | Reason |
|---|---|
| `FOREGROUND_SERVICE` | Required to maintain continuous background audio capture while the app UI is minimized. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Explicit Android 14+ requirement for services handling `MediaProjection` capture. |
| `POST_NOTIFICATIONS` | Required on Android 13+ to display the ongoing monitoring notification with stop controls. |

*Note: No `RECORD_AUDIO` microphone permission or `INTERNET` permission is used or requested.*

---

## Physical Device Testing Checklist

When deploying to a physical Android device:

1. **Launch App**: Open AudioVault. Ensure the Home screen displays the `● Monitoring Inactive` state.
2. **Grant MediaProjection**:
   - Tap `START MONITORING`.
   - On Android 13+, accept notification permission if prompted.
   - On the system Screen/Audio capture dialog, tap "Start now" (or "Entire Screen / Single App").
3. **Verify Foreground Service**:
   - Observe the persistent notification: `AudioVault • Monitoring`.
   - Minimize the app; ensure the service stays active without terminating.
4. **Test Playback Capture**:
   - Open a non-DRM media player (e.g., VLC, YouTube in browser, podcast app).
   - Play an audio track.
   - Check AudioVault: state transitions to `● Recording in Progress` with active timer.
5. **Test Automatic Splitting & Silence Finalization**:
   - Pause or stop the audio.
   - Wait for the configured silence timeout (default 7 seconds).
   - Verify AudioVault finalizes the session and saves `Recording_YYYYMMDD_HHmmss.m4a`.
   - Play another track; verify `Recording_002` is created independently without manual interaction.
6. **Test Library & Playback**:
   - Switch to the **Recordings** tab.
   - Tap Play on the new recording. Verify playback through Media3 ExoPlayer.
   - Test seeking backward (-10s), forward (+10s), scrub bar, and speed options (1.25x, 1.5x).
7. **Test File Management**:
   - **Rename**: Change title and verify instant update in Room.
   - **Share**: Tap Share and select an external app via Android Sharesheet (`FileProvider`).
   - **Delete**: Confirm deletion and verify file is purged from disk and database.
8. **Test Settings**:
   - Adjust silence timeout (e.g. 5s, 10s, 15s), min duration, and quality (96/128/192 kbps).
   - Verify changes take effect in the next monitoring session.
