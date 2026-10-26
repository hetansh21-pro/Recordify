package com.audiovault.app.core.identity

import com.audiovault.app.core.online.OnlineSongModel
import com.audiovault.app.data.local.entity.RecordingEntity

/**
 * Filters out unwanted edits, slowed/reverb, sped-up/nightcore, duplicate uploads,
 * and groups multi-provider results under one authoritative canonical identity.
 */
object TrackQualityFilter {

    /**
     * Filters and deduplicates a list of OnlineSongModels.
     */
    fun filterAndDeduplicate(
        songs: List<OnlineSongModel>,
        isOriginalOnly: Boolean = true,
        requestedVersion: RecordingVersionType? = null,
        decadeRange: IntRange? = null,
        allowedLanguages: Set<String> = emptySet()
    ): List<CanonicalTrackIdentity> {
        val resolved = songs.map { TrackVersionResolver.resolve(it) }
        return filterAndDeduplicateResolved(resolved, isOriginalOnly, requestedVersion, decadeRange, allowedLanguages)
    }

    /**
     * Filters and deduplicates already resolved CanonicalTrackIdentities.
     */
    fun filterAndDeduplicateResolved(
        candidates: List<CanonicalTrackIdentity>,
        isOriginalOnly: Boolean = true,
        requestedVersion: RecordingVersionType? = null,
        decadeRange: IntRange? = null,
        allowedLanguages: Set<String> = emptySet()
    ): List<CanonicalTrackIdentity> {
        // Step 1: Version & Quality Filtering
        val filtered = candidates.filter { item ->
            // Always suppress snippet/ringtone clips (<55s)
            if (item.recordingVersion == RecordingVersionType.SNIPPET_CLIP) return@filter false

            // If user explicitly requested a specific version type (e.g. Remix, Acoustic, Live)
            if (requestedVersion != null) {
                if (item.recordingVersion != requestedVersion) return@filter false
            } else if (isOriginalOnly) {
                // Default original policy: exclude unofficial edits, remixes, slowed, sped up, karaoke, covers
                when (item.recordingVersion) {
                    RecordingVersionType.REMIX,
                    RecordingVersionType.SLOWED_REVERB,
                    RecordingVersionType.SPED_UP_NIGHTCORE,
                    RecordingVersionType.KARAOKE_INSTRUMENTAL,
                    RecordingVersionType.COVER,
                    RecordingVersionType.LIVE -> return@filter false
                    else -> Unit
                }
            }

            // Decade filter
            if (decadeRange != null && item.releaseYear != null) {
                if (item.releaseYear !in decadeRange) return@filter false
            }

            true
        }

        // Step 2: Canonical Deduplication across providers
        val groupedByCanonical = LinkedHashMap<String, MutableList<CanonicalTrackIdentity>>()
        for (item in filtered) {
            val list = groupedByCanonical.getOrPut(item.canonicalTrackId) { mutableListOf() }
            list.add(item)
        }

        val deduplicated = mutableListOf<CanonicalTrackIdentity>()
        for ((_, versions) in groupedByCanonical) {
            // Pick primary based on: official status, stream readiness, bitrate
            val bestPrimary = versions.maxWithOrNull(
                compareBy<CanonicalTrackIdentity> { if (it.officialStatus) 1 else 0 }
                    .thenBy { if (it.isPlayable) 1 else 0 }
                    .thenBy { if (it.providerName == "saavn") 2 else if (it.providerName == "spotify") 1 else 0 }
                    .thenBy { it.versionConfidence }
            ) ?: versions.first()

            val alternates = versions.filter { it.providerTrackId != bestPrimary.providerTrackId }
            deduplicated.add(bestPrimary.copy(alternateSources = alternates))
        }

        return deduplicated
    }

    /**
     * Determines whether two recordings represent the exact same musical recording.
     */
    fun isSameRecording(a: CanonicalTrackIdentity, b: CanonicalTrackIdentity): Boolean {
        if (a.canonicalTrackId == b.canonicalTrackId) return true
        if (a.isrc != null && b.isrc != null && a.isrc.equals(b.isrc, ignoreCase = true)) return true
        return false
    }
}
