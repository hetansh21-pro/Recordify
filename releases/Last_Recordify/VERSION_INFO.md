# Last Recordify (Final Milestone Checkpoint)

**Version Tag**: `Last-Recordify` / `Recordify-Last`  
**Date**: October 24, 2026  
**APK Location**: `releases/Last_Recordify/Recordify.apk` (and `releases/Last_Recordify/recordify-last.apk`)

---

## 🚀 Key Features & Upgrades Included in Last Recordify

1. **Data Saver Mode & 10s Smart Skip Protection**:
   - Progressive Range Architecture: 96 kbps low-bandwidth streaming with strict chunked HTTP 206 Partial Content buffering (8s target, 15s max, 0 audio prefetch), saving up to 70% data.
   - **10s Smart Skip Protection**: Shows song title & high-res artwork for 10 seconds before downloading audio data over the network. If skipped within 10 seconds, **0 MB** of data is used.
   - High-fidelity toggle (320 kbps) when Wi-Fi is active.

2. **Android Quick Settings Media Player & Bluetooth TWS Touch Controls**:
   - Integrated `MediaSessionCompat` and `NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken)` matching native Android 11+, HyperOS, MIUI, and OneUI media cards.
   - Real-time `PlaybackStateCompat` and `MediaMetadataCompat` synchronization with live artwork, seekbar, and transport controls.
   - **Bluetooth Earbuds / Wired Headset Gestures**:
     - **Single Tap**: Play / Pause
     - **Double Tap**: Next Track
     - **Triple Tap**: Previous Track

3. **Smart Focus & Keyboard Auto-Dismissal**:
   - Automatically hides software keyboard and clears focus on search submission, clear-query button, and results list scrolling (`resultsListState.isScrollInProgress`).
   - Clears focus on tapping any song, movie soundtrack album, artist, or recent search history item.
   - Fixed background search focus bug: entering the full-screen player, switching to recent apps, and returning will no longer trigger the keyboard over the player.
   - `android:windowSoftInputMode="adjustResize|stateHidden"` configured on `MainActivity`.

4. **3-in-1 Precision YouTube Ad-Cutter**:
   - Live Accessibility Service (`YouTubeAdAccessibilityService`) detects in-stream video ads in real-time.
   - Acoustic micro-crossfading (50ms) to mute ad audio without clicks or abrupt truncations.
   - SponsorBlock segment skipping for clean non-music banter suppression.

5. **Spotify Extended Streaming History AI Taste Training**:
   - 100% on-device parser for Spotify `.zip` archives or extracted folders containing `endsong_*.json`.
   - Generates personalized recommendations, mood/vibe filtering, and smart playback queue based on user's exact listening history.

6. **Automated Playback Audio Capture & Auto-Splitting**:
   - Native Android 10+ `AudioPlaybackCaptureConfiguration` + `AudioRecord`.
   - RMS dBFS energy measurement and hysteresis-based silence detection automatically split consecutive tracks.
   - Hardware AAC-LC encoding via `MediaCodec` into `.m4a` at 192, 256, or 320 kbps.

7. **Multi-Source Music Search & Offline Downloader**:
   - Parallel multi-catalog search across Spotify, JioSaavn, and YouTube audio streams with fuzzy/exact title matching.
   - Background batch downloading for offline playback with progress indicators.
   - Integrated YouTube video player and downloader in the **Videos** tab.

8. **Edge-to-Edge Navigation Insets**:
   - Full window insets support with zero cut-off or overlap with Android 3-button navigation bars.

---

## 📦 Installation
```bash
adb install -r releases/Last_Recordify/Recordify.apk
```
