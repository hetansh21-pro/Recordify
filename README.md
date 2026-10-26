# Recordify 🎵

[![Platform](https://img.shields.io/badge/Platform-Android_10+_(API_29+)-brightgreen?logo=android&logoColor=white)](https://developer.android.com)
[![TV Support](https://img.shields.io/badge/Android_TV-Google_TV_10--Foot_UI-009688?logo=android&logoColor=white)](https://android.com/tv/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Audio_Engine-Media3_ExoPlayer-orange?logo=google&logoColor=white)](https://developer.android.com/guide/topics/media/media3)
[![Build](https://img.shields.io/badge/Build-Gradle_8.14-success?logo=gradle&logoColor=white)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache_2.0-lightgrey)](LICENSE)

> **Recordify** is a next-generation, high-fidelity music streaming, smart audio capture, and personal library application built natively for Android and Android TV. Featuring a sleek Spotify-inspired interface, multi-source search across global music catalogs, real-time AI taste training, zero-pop YouTube ad-cutting, official Android audio playback capture with auto-track splitting, and a dedicated 10-foot Android TV experience with full remote-control navigation.

---

## 📥 Download Public APKs

| Application | Target Platform | Architecture | Min OS | Direct Download |
|---|---|---|---|---|
| **Recordify (Mobile)** | Android Phones & Tablets | ARM64 / x86_64 | Android 10+ (API 29+) | [⬇️ Download `Recordify.apk`](./apks/Recordify.apk) |
| **Recordify TV** | Android TV & Google TV | ARM64 / x86_64 | Android 10+ (API 29+) | [⬇️ Download `Recordify TV.apk`](./apks/Recordify%20TV.apk) |

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
  - [1. Spotify-Inspired UI & Dynamic Player](#1-spotify-inspired-ui--dynamic-player)
  - [2. Multi-Source Music Search & Streaming](#2-multi-source-music-search--streaming)
  - [3. Bandwidth Optimization & Data Saver Mode](#3-bandwidth-optimization--data-saver-mode)
  - [4. Automated Audio Playback Capture](#4-automated-audio-playback-capture)
  - [5. 3-in-1 Precision YouTube Ad Cutter](#5-3-in-1-precision-youtube-ad-cutter)
  - [6. Real-Time Metadata & Cover Art Identification](#6-real-time-metadata--cover-art-identification)
  - [7. Offline AI Taste Training (Spotify Data Import)](#7-offline-ai-taste-training-spotify-data-import)
  - [8. YouTube Video Hub & Offline Downloader](#8-youtube-video-hub--offline-downloader)
  - [9. Quick Settings Media Player & Bluetooth TWS Controls](#9-quick-settings-media-player--bluetooth-tws-controls)
  - [10. Edge-to-Edge Navigation & Smart Focus Management](#10-edge-to-edge-navigation--smart-focus-management)
  - [11. Dedicated Recordify TV Experience](#11-dedicated-recordify-tv-experience)
- [Architecture & Tech Stack](#-architecture--tech-stack)
- [Project Directory Structure](#-project-directory-structure)
- [Permissions & Privacy](#-permissions--privacy)
- [Building & Installation](#-building--installation)
  - [Prerequisites](#prerequisites)
  - [Building from Source](#building-from-source)
- [Physical Device Testing Checklist](#-physical-device-testing-checklist)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

Recordify bridges the gap between internet streaming and offline audio capture. Rather than relying on cloud subscriptions, Recordify allows users to search global music catalogs, stream at studio bitrates or data-saver modes, capture eligible Android device playback into pristine local AAC/M4A tracks, automatically eliminate in-stream ads with micro-crossfades, and train recommendations on their actual Spotify streaming history.

---

## ✨ Key Features

### 1. Spotify-Inspired UI & Dynamic Player
- **Sleek Dark Aesthetic**: Built entirely with Jetpack Compose Material 3, tailored for low battery consumption on OLED displays.
- **Fluid Jelly Motion**: Smooth overscroll dynamics (`SubtleJellyContainer`) and spring-based transitions.
- **Docked MiniPlayer & Full-Screen Player**: Expand seamlessly from a persistent bottom playback bar into a full-screen player with live waveform scrubbing, playback speed adjustment (0.5x – 2.0x), shuffle, and repeat modes.
- **Side Profile Drawer**: Quick access to personalized greeting, user profiles, recents, and system settings.

### 2. Multi-Source Music Search & Streaming
- **Parallel Catalog Search**: Queries Spotify catalog metadata, JioSaavn, and YouTube audio streams concurrently using exact and fuzzy matching.
- **Full Album & Soundtrack Exploration**: Browse entire movie soundtracks and studio albums with complete track listings and instant album playback.
- **Instant Search Caching**: Instant history with single-tap replay, item removal, and bulk clear.
- **Filter Chips**: Filter results dynamically between `All`, `Online`, `Library`, and `Artists`.

### 3. Bandwidth Optimization & Data Saver Mode
- **70% Mobile Data Savings**: Low-bandwidth 96 kbps audio streaming with strict chunked HTTP 206 Partial Content buffering (8s target, 15s max, 0 audio prefetch).
- **10s Smart Skip Protection**: Displays track name and high-res cover art for 10 seconds before downloading stream chunks over the network. If you skip within 10 seconds, **0 MB** of data is wasted.
- **High Fidelity Toggle**: Switch to 320 kbps high-definition playback when connected to Wi-Fi.

### 4. Automated Audio Playback Capture
- **Native Android 10+ API**: Uses official `AudioPlaybackCaptureConfiguration` and `AudioRecord` (16-bit PCM, 44.1 kHz stereo/mono).
- **Intelligent Track Splitting**: Real-time RMS dBFS energy measurement and hysteresis-based silence detection automatically slice consecutive songs into individual audio files.
- **Hardware-Accelerated AAC-LC Encoding**: Encodes raw audio directly via `MediaCodec` and muxes into `.m4a` via `MediaMuxer` at configurable bitrates (192, 256, 320 kbps).
- **Adjustable Silence Timeouts**: Choose between 5s, 7s, 10s, or 15s silence thresholds.

### 5. 3-in-1 Precision YouTube Ad Cutter
- **Accessibility Service Detection**: Real-time in-stream YouTube ad detection via `YouTubeAdAccessibilityService`.
- **50ms Micro-Crossfades**: Automatically mutes capture input during ads with smooth fade-in and fade-out to prevent pops, clicks, or truncated intro notes.
- **SponsorBlock Integration**: Automatically skips sponsored segments, intros, and non-music banter.

### 6. Real-Time Metadata & Cover Art Identification
- **Notification Listener Service**: `AudioVaultNotificationListener` inspects metadata broadcasts and notifications from third-party media players (Spotify, YouTube Music, etc.).
- **Automatic ID3 Tagging**: Slices are automatically tagged with Title, Artist, Album, and Year.
- **High-Resolution Artwork Fetcher**: Downloads cover art from verified music databases (`CoverArtFetcher`) and embeds it into both Room database entities and audio file containers.

### 7. Offline AI Taste Training (Spotify Data Import)
- **Train On Real Data**: Import your official Spotify Extended Streaming History (`.zip` archive or extracted folder containing `endsong_*.json`).
- **Zero Cloud Upload**: 100% on-device parsing of hundreds of thousands of historical streams.
- **Personalized Home Feed**: Dynamic recommendation engine with Vibe Detection (Chill, Workout, Focus, Party, etc.), Smart Queue Engine, and similarity scoring.

### 8. YouTube Video Hub & Offline Downloader
- **Integrated Video Player**: Stream YouTube videos directly within the dedicated **Videos** tab.
- **Built-in Offline Downloader**: Powered by `VidssaveExtractor` and `YouTubeVideoDownloader` for background video downloads.

### 9. Quick Settings Media Player & Bluetooth TWS Controls
- **Android 11+ / HyperOS / OneUI Media Card**: Emits an active `MediaSessionCompat` session token connected to `NotificationCompat.MediaStyle`, rendering the native OS Quick Settings player with track title, artist, seekbar, and headphone icon.
- **Full Bluetooth Earbud & Headset Touch Controls**:
  - **Single Tap**: Play / Pause
  - **Double Tap**: Next Track
  - **Triple Tap**: Previous Track

### 10. Edge-to-Edge Navigation & Smart Focus Management
- **Edge-to-Edge System Bars**: Zero overlap with Android 3-button navigation bars or gesture pills.
- **Smart Keyboard Focus Suppression**: Automatically dismisses the keyboard when scrolling search results or tapping any track/album. Prevents software keyboards from popping up unexpectedly over the full-screen player when returning from recent apps.

### 11. Dedicated Recordify TV Experience
- **10-Foot Television Experience**: Custom-designed for 16:9 widescreen viewing from a sofa, featuring a deep obsidian aesthetic, green focus halos, and scale feedback for standard TV remotes (D-pad Center, Up, Down, Left, Right, Play/Pause, Next, Prev, Back).
- **Intelligent Era & Vibe Auto-Queue Engine**: Playing an old/retro classic (e.g. Kishore Kumar, Lata Mangeshkar, 90s Bollywood) automatically queues matching vintage classics; playing a modern chartbuster (e.g. Arijit Singh, Sachin-Jigar, Karan Aujla) queues current trending hits.
- **Fixed & Docked Bottom Mini-Player**: Stationed firmly at the bottom of the TV screen with zero jumps or layout shifting when playback starts.
- **Interactive Full-Screen TV Player**: Features rotating vinyl disc animation, live progress timeline with fast-forward/rewind, smart queue viewer, and lossless 320 KBPS indicator.
- **1-Click Remote Search & Discovery**: High-efficiency search bar with instant genre pills (*Trending Hits*, *90s Bollywood*, *Romantic Melodies*, *Arijit Singh*, *Punjabi Top 50*, *Lo-Fi Chill*) plus a compact 2-row on-screen keyboard strip.
- **12 Comprehensive TV Music Shelves**: New Releases, Top Regional Chartbusters, Made For You Daily Mix, Romantic Melodies, Relaxing Lo-Fi, 90s Bollywood, Punjabi Party, and complete soundtrack albums with guaranteed zero-latency fallback content.

---

## 🏗 Architecture & Tech Stack

Recordify follows the **Clean Architecture** and **MVVM (Model-View-ViewModel)** design patterns recommended by modern Android standards.

```
                    ┌────────────────────────────────────────┐
                    │               UI Layer                 │
                    │   Jetpack Compose + Material 3         │
                    │   Home | Search | Record | Library     │
                    └───────────────────▲────────────────────┘
                                        │ (StateFlow / UI Events)
                    ┌───────────────────┴────────────────────┐
                    │            ViewModel Layer             │
                    │   RecordingsViewModel | HomeViewModel  │
                    │   VideosViewModel | SettingsViewModel  │
                    └───────────────────▲────────────────────┘
                                        │
        ┌───────────────────────────────┴───────────────────────────────┐
        │                                                               │
┌───────┴─────────────────────────┐           ┌─────────────────────────┴───────┐
│           Core Layer            │           │           Data Layer            │
│ • PlaybackAudioCapture (PCM)   │           │ • Room Database (AppDatabase)   │
│ • MediaCodec AAC Encoder (M4A) │           │ • RecordingDao & Entities       │
│ • Media3 ExoPlayer Engine      │           │ • UserPreferencesRepository     │
│ • NotificationListenerService  │           │   (DataStore Preferences)       │
│ • YouTubeAdAccessibilityService│           │ • PlaylistRepository            │
│ • RecommendationService        │           │ • FileManager (Internal Storage)│
└─────────────────────────────────┘           └─────────────────────────────────┘
```

| Technology | Purpose |
|---|---|
| **Kotlin 2.0+** | Modern, concise, and null-safe core programming language |
| **Jetpack Compose (BOM)** | Declarative, reactive UI toolkit with Material 3 |
| **AndroidX Media3 (ExoPlayer)** | Audio/video playback, caching, and stream resolution |
| **MediaSessionCompat** | Android Quick Settings integration & Bluetooth headset touch gestures |
| **Room Database** | Local persistence for captured songs, playlists, and favorites |
| **Jetpack DataStore** | Reactive key-value storage for user settings and preferences |
| **Kotlin Coroutines & Flow** | Asynchronous execution, background threading, and reactive UI state |
| **Hilt & Custom AppContainer** | Dependency injection |
| **MediaCodec & MediaMuxer** | Low-overhead hardware AAC-LC encoding and container muxing |
| **Accessibility API** | In-stream ad detection for real-time capture muting |

---

## 📂 Project Directory Structure

```
com.audiovault.app/
├── AudioVaultApplication.kt              # App initialization & dependency container
├── MainActivity.kt                       # Single-activity Compose host
├── core/
│   ├── audio/                            # Capture & encoding pipeline
│   │   ├── AudioCaptureConfig.kt         # Bitrate, sample rate, and audio parameters
│   │   ├── PlaybackAudioCapture.kt       # AudioPlaybackCapture & AudioRecord manager
│   │   ├── AudioActivityDetector.kt      # RMS dBFS energy & hysteresis silence detection
│   │   ├── AacAudioEncoder.kt            # MediaCodec AAC-LC + MediaMuxer pipeline
│   │   └── IntelligentAudioAnalyzer.kt   # Dynamic silence calibration
│   ├── downloader/                       # Offline video & audio downloaders
│   │   ├── VidssaveExtractor.kt          # Video stream extractor
│   │   └── YouTubeVideoDownloader.kt     # Multi-threaded download manager
│   ├── import/                           # Spotify history import
│   │   └── SpotifyDataImporter.kt        # Extended streaming history parser
│   ├── intelligence/                     # Smart listeners & ad cutters
│   │   ├── AudioVaultNotificationListener.kt # Metadata listener from third-party players
│   │   ├── YouTubeAdAccessibilityService.kt  # Real-time YouTube ad detection
│   │   ├── CoverArtFetcher.kt            # High-resolution artwork downloader
│   │   └── MediaSessionTracker.kt        # System media session observation
│   ├── online/                           # Online streaming engine
│   │   ├── OnlineMusicSearchService.kt   # Multi-source parallel search engine
│   │   ├── OnlineSongDownloader.kt       # Background music cache manager
│   │   └── OnlineSongModel.kt            # Data contracts for online streams
│   ├── player/                           # Audio playback
│   │   ├── AudioPlayerEngine.kt          # Media3 ExoPlayer wrapper
│   │   └── StreamingConfig.kt            # Buffer sizing & data-saver configurations
│   ├── recommendation/                   # On-device AI recommendation engine
│   │   ├── RecommendationService.kt      # Personalized home feed generator
│   │   ├── SmartQueueEngine.kt           # Autonomous playback queue engine
│   │   └── VibeDetector.kt               # Mood and vibe classification
│   └── service/                          # Background services & notifications
│       ├── AudioCaptureService.kt        # MediaProjection foreground capture service
│       └── PlaybackNotificationManager.kt# MediaStyle Quick Settings notification
├── data/
│   ├── local/                            # Room database
│   │   ├── entity/RecordingEntity.kt     # Track metadata database entity
│   │   ├── dao/RecordingDao.kt           # Room queries & transactions
│   │   └── AppDatabase.kt                # SQLite database definition
│   ├── repository/                       # Repositories & DataStore
│   │   ├── RecordingRepository.kt        # Track & library data repository
│   │   ├── PlaylistRepository.kt         # Custom playlists & liked songs
│   │   └── UserPreferencesRepository.kt  # Settings, Data Saver, and Profile state
│   └── storage/
│       └── FileManager.kt                # App-specific storage & FileProvider
└── ui/
    ├── home/                             # Home feed, vibes, daily mixes
    ├── search/                           # Multi-source search & soundtrack views
    ├── record/                           # Playback capture radar & waveform visualizer
    ├── library/                          # Liked tracks, playlists, and local files
    ├── videos/                           # YouTube video playback & downloader
    ├── player/                           # MiniPlayer & SpotifyFullScreenPlayer
    ├── drawer/                           # Navigation drawer & profile customization
    ├── settings/                         # Data Saver, bitrate, and timeout preferences
    ├── components/                       # Shared Compose widgets
    └── theme/                            # Spotify dark theme palette & typography
```

---

## 🔒 Permissions & Privacy

Recordify is designed with privacy as a foundational principle. **It does not collect, transmit, or sell personal data.**

| Permission | Scope | Rationale |
|---|---|---|
| `INTERNET` | Optional / Online | Required for online music search, streaming, and cover art retrieval. Not used for telemetry. |
| `ACCESS_NETWORK_STATE` | System | Detects Wi-Fi vs. Mobile Data to toggle Data Saver mode automatically. |
| `FOREGROUND_SERVICE` | Background Service | Keeps audio capture and playback active when the app is minimized. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Media Capture | Required by Android 14+ to capture eligible playback audio via `MediaProjection`. |
| `POST_NOTIFICATIONS` | Notifications | Displays ongoing capture status and the interactive Quick Settings media player card. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Optional Intelligence | Allows Recordify to read track names and artist metadata from other music player notifications. |
| `BIND_ACCESSIBILITY_SERVICE` | Optional Ad Cutter | Allows live detection of in-stream YouTube advertisements to mute audio capture during ads. |

> [!NOTE]
> **DRM Restrictions**: Recordify strictly adheres to Android's `AudioPlaybackCaptureConfiguration` platform security. Playback from applications that disallow capture or stream DRM-protected content (e.g. Netflix) cannot and will not be recorded.

---

## 🚀 Building & Installation

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17 (recommended: OpenJDK 17)
- **Android SDK**: Build Tools 34.0.0, Compile SDK 34 (Android 14)
- **Device / Emulator**: Android 10 (API 29) or higher

### Building from Source

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/Re-songs.git
   cd Re-songs
   ```

2. **Set up Local Environment**:
   Ensure your `local.properties` specifies your Android SDK directory:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

3. **Assemble Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be created at:
   ```
   app/build/outputs/apk/debug/Recordify.apk
   ```

4. **Install via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/Recordify.apk
   ```

5. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🧪 Physical Device Testing Checklist

When evaluating Recordify on an Android device:

- [ ] **Data Saver Mode**: Open **Settings** > turn on **Data Saver Mode** and **10s Smart Skip Protection**. Search a song, skip within 10 seconds, and verify that bandwidth consumption is prevented.
- [ ] **Playback Capture**: Navigate to **Record** > tap `START MONITORING` > grant `MediaProjection`. Play an eligible video or track in another app and verify automatic start, waveform visualization, and clean splitting upon silence.
- [ ] **YouTube Ad-Cutter**: Enable the Accessibility Service in **Settings**. Play a monetized YouTube video; observe the smooth micro-crossfade muting during commercials.
- [ ] **Quick Settings Notification**: Play any online or captured song. Pull down the Android notification shade; verify the native media control card with album cover art and playback buttons.
- [ ] **Earbuds Touch Controls**: Connect Bluetooth earbuds. Single-tap to toggle play/pause, double-tap to advance track, and triple-tap to skip backwards.
- [ ] **Spotify Data Training**: In **Settings**, tap **Upload .ZIP** and select your Spotify Extended Streaming History archive. Verify immediate recommendation training on the **Home** tab.

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are welcome!

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/NewAwesomeFeature`).
3. Commit your changes (`git commit -m "feat: Add NewAwesomeFeature"`).
4. Push to the branch (`git push origin feature/NewAwesomeFeature`).
5. Open a Pull Request.

---

## 📄 License

```
Copyright 2026 Recordify Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
