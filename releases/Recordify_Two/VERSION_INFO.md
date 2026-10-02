# Recordify Two (Release Checkpoint)

**Version Tag**: `Recordify-Two` / `recordify-two`  
**Date**: October 2, 2026  
**APK Location**: `releases/Recordify_Two/recordify-two.apk`

---

## 🚀 Key Features Included in Recordify Two

1. **Ultra-Smooth 120 FPS / 60 FPS Scrolling Engine**:
   - High-speed in-memory LRU Bitmap cache (`ImageMemoryCache.kt`).
   - Zero disk I/O and zero bitmap decoding on the Main UI thread during scrolling.
   - Asynchronous IO downsampling via `inSampleSize` matching exact UI widget dimensions (48dp / 54dp / 64dp / 90dp).

2. **Hold / Long-Press Song Deletion on Home Screen**:
   - Long-press any track in the **Recent Recordings** list or the **2x2 Quick Grid Tiles** on the Home screen to trigger permanent deletion with a confirmation dialog.
   - Clean, uncluttered UI without any 3-dot buttons.

3. **Original Spring Jelly Overscroll Bounce Effect**:
   - Preserved fluid spring physics (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMediumLow`) with organic multi-axis stretch and snapback.

4. **3-Layer YouTube Ad-Cutter Engine & Post-Recording Audio Cleaner**:
   - Layer 1: Live Accessibility Service scanning for in-stream ads.
   - Layer 2: Real-time 50ms acoustic micro-crossfading for smooth transitions.
   - Layer 3: SponsorBlock trimming.
   - Post-Recording Scanner (`AudioPostProcessor.kt`) for ad removal and auto-trimming.

5. **Guaranteed Mid-Recording Persistence**:
   - Independent coroutine `persistenceScope` ensures songs $\ge$ 50 seconds are saved safely even if monitoring is stopped abruptly.

---

## 📦 Installation
```bash
adb install -r releases/Recordify_Two/recordify-two.apk
```
