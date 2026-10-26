package com.audiovault.app.ui.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Primary navigation destinations for Recordify TV.
 * Conforms to Spotify TV design patterns: Home, Search, Library, Playlists,
 * Recently Played, Favorites, Now Playing, and Settings.
 */
enum class TvNavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    LIBRARY("Your Library", Icons.Default.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Default.Album),
    RECENTLY_PLAYED("Recently Played", Icons.Default.History),
    FAVORITES("Favorites", Icons.Default.Favorite),
    NOW_PLAYING("Now Playing", Icons.Default.PlayCircle),
    SETTINGS("Settings", Icons.Default.Settings)
}
