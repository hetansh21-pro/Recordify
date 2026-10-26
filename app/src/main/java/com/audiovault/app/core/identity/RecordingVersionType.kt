package com.audiovault.app.core.identity

/**
 * Standardized categorization of audio recording versions.
 */
enum class RecordingVersionType(val displayName: String, val badge: String) {
    ORIGINAL_STUDIO("Original Studio", "Original"),
    OFFICIAL_SOUNDTRACK("Official Soundtrack", "OST"),
    OFFICIAL_SINGLE("Official Single", "Single"),
    REMASTERED("Remastered", "Remaster"),
    ACOUSTIC("Acoustic / Unplugged", "Acoustic"),
    LIVE("Live Recording", "Live"),
    REMIX("Remix / Club Edit", "Remix"),
    SLOWED_REVERB("Slowed & Reverb", "Slowed"),
    SPED_UP_NIGHTCORE("Sped Up / Nightcore", "Sped Up"),
    KARAOKE_INSTRUMENTAL("Karaoke / Instrumental", "Instrumental"),
    COVER("Cover Version", "Cover"),
    SNIPPET_CLIP("Short / Ringtone", "Clip"),
    UNCERTAIN("Standard Audio", "")
}
