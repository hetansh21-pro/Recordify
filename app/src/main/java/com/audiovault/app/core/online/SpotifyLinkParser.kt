package com.audiovault.app.core.online

import java.util.regex.Pattern

enum class SpotifyEntityType {
    ALBUM,
    TRACK,
    PLAYLIST,
    ARTIST
}

data class SpotifyLinkMatch(
    val type: SpotifyEntityType,
    val id: String,
    val originalUrl: String
)

object SpotifyLinkParser {

    private val WEB_LINK_PATTERN = Pattern.compile(
        """(?:https?://)?open\.spotify\.com/(?:[a-zA-Z]{2,4}(?:-[a-zA-Z]{2,4})?/)?(album|track|playlist|artist)/([a-zA-Z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val URI_PATTERN = Pattern.compile(
        """spotify:(album|track|playlist|artist):([a-zA-Z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(input: String): SpotifyLinkMatch? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val webMatcher = WEB_LINK_PATTERN.matcher(trimmed)
        if (webMatcher.find()) {
            val typeStr = webMatcher.group(1)?.lowercase() ?: return null
            val id = webMatcher.group(2) ?: return null
            val type = when (typeStr) {
                "album" -> SpotifyEntityType.ALBUM
                "track" -> SpotifyEntityType.TRACK
                "playlist" -> SpotifyEntityType.PLAYLIST
                "artist" -> SpotifyEntityType.ARTIST
                else -> return null
            }
            return SpotifyLinkMatch(type = type, id = id, originalUrl = trimmed)
        }

        val uriMatcher = URI_PATTERN.matcher(trimmed)
        if (uriMatcher.find()) {
            val typeStr = uriMatcher.group(1)?.lowercase() ?: return null
            val id = uriMatcher.group(2) ?: return null
            val type = when (typeStr) {
                "album" -> SpotifyEntityType.ALBUM
                "track" -> SpotifyEntityType.TRACK
                "playlist" -> SpotifyEntityType.PLAYLIST
                "artist" -> SpotifyEntityType.ARTIST
                else -> return null
            }
            return SpotifyLinkMatch(type = type, id = id, originalUrl = trimmed)
        }

        return null
    }

    fun isSpotifyLink(input: String): Boolean = parse(input) != null
}
