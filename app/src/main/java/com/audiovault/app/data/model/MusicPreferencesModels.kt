package com.audiovault.app.data.model

import java.util.Locale

data class CountryInfo(
    val code: String,
    val name: String,
    val flagEmoji: String
)

data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String
)

data class GenreInfo(
    val id: String,
    val name: String,
    val iconName: String = "MusicNote",
    val gradientColors: List<Long> = listOf(0xFF2E7D32, 0xFF1B5E20)
)

data class ArtistRecommendationInfo(
    val name: String,
    val primaryGenre: String,
    val language: String
)

object MusicPreferenceCatalog {

    val COUNTRIES = listOf(
        CountryInfo("IN", "India", "🇮🇳"),
        CountryInfo("US", "United States", "🇺🇸"),
        CountryInfo("GB", "United Kingdom", "🇬🇧"),
        CountryInfo("CA", "Canada", "🇨🇦"),
        CountryInfo("AU", "Australia", "🇦🇺"),
        CountryInfo("DE", "Germany", "🇩🇪"),
        CountryInfo("FR", "France", "🇫🇷"),
        CountryInfo("JP", "Japan", "🇯🇵"),
        CountryInfo("KR", "South Korea", "🇰🇷"),
        CountryInfo("AE", "United Arab Emirates", "🇦🇪"),
        CountryInfo("BR", "Brazil", "🇧🇷"),
        CountryInfo("ES", "Spain", "🇪🇸"),
        CountryInfo("IT", "Italy", "🇮🇹"),
        CountryInfo("MX", "Mexico", "🇲🇽"),
        CountryInfo("NL", "Netherlands", "🇳🇱"),
        CountryInfo("SG", "Singapore", "🇸🇬"),
        CountryInfo("NZ", "New Zealand", "🇳🇿"),
        CountryInfo("ZA", "South Africa", "🇿🇦"),
        CountryInfo("PK", "Pakistan", "🇵🇰"),
        CountryInfo("BD", "Bangladesh", "🇧🇩"),
        CountryInfo("NP", "Nepal", "🇳🇵")
    )

    fun getDefaultCountry(): CountryInfo {
        val deviceCode = Locale.getDefault().country.uppercase()
        return COUNTRIES.firstOrNull { it.code.equals(deviceCode, ignoreCase = true) }
            ?: CountryInfo("IN", "India", "🇮🇳")
    }

    val LANGUAGES = listOf(
        LanguageInfo("hi", "Hindi", "हिन्दी"),
        LanguageInfo("gu", "Gujarati", "ગુજરાતી"),
        LanguageInfo("en", "English", "English"),
        LanguageInfo("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageInfo("ta", "Tamil", "தமிழ்"),
        LanguageInfo("te", "Telugu", "తెలుగు"),
        LanguageInfo("bn", "Bengali", "বাংলা"),
        LanguageInfo("mr", "Marathi", "मराठी"),
        LanguageInfo("ml", "Malayalam", "മലയാളം"),
        LanguageInfo("kn", "Kannada", "ಕನ್ನಡ"),
        LanguageInfo("bho", "Bhojpuri", "भोजपुरी"),
        LanguageInfo("ur", "Urdu", "اردو"),
        LanguageInfo("ja", "Japanese", "日本語"),
        LanguageInfo("ko", "Korean", "한국어"),
        LanguageInfo("es", "Spanish", "Español"),
        LanguageInfo("fr", "French", "Français")
    )

    val GENRES = listOf(
        GenreInfo("bollywood", "Bollywood", "Film", listOf(0xFFE50914, 0xFFB20710)),
        GenreInfo("gujarati", "Gujarati", "LibraryMusic", listOf(0xFFFF6F00, 0xFFE65100)),
        GenreInfo("romantic", "Romantic", "Favorite", listOf(0xFFE91E63, 0xFF880E4F)),
        GenreInfo("pop", "Pop", "MusicNote", listOf(0xFF1DB954, 0xFF191414)),
        GenreInfo("hiphop", "Hip-Hop", "Album", listOf(0xFFFF9800, 0xFFF57C00)),
        GenreInfo("sad", "Sad Songs", "MoodBad", listOf(0xFF1E88E5, 0xFF0D47A1)),
        GenreInfo("chill", "Chill & Lo-Fi", "Headphones", listOf(0xFF00ACC1, 0xFF006064)),
        GenreInfo("devotional", "Devotional", "SelfImprovement", listOf(0xFFFB8C00, 0xFFE65100)),
        GenreInfo("retro", "90s Retro", "History", listOf(0xFF8E24AA, 0xFF4A148C)),
        GenreInfo("rock", "Rock", "GraphicEq", listOf(0xFFD32F2F, 0xFF7B1FA2)),
        GenreInfo("punjabi_pop", "Punjabi Pop", "Speaker", listOf(0xFFFBC02D, 0xFFF57F17)),
        GenreInfo("edm", "EDM / Dance", "AutoAwesome", listOf(0xFF00E676, 0xFF00B0FF)),
        GenreInfo("indie", "Indie", "Radio", listOf(0xFF43A047, 0xFF1B5E20)),
        GenreInfo("ghazal", "Ghazal", "Audiotrack", listOf(0xFF6D4C41, 0xFF3E2723)),
        GenreInfo("classical", "Classical", "FormatListNumbered", listOf(0xFF546E7A, 0xFF263238))
    )

    val POPULAR_ARTISTS = listOf(
        ArtistRecommendationInfo("Arijit Singh", "Romantic", "Hindi"),
        ArtistRecommendationInfo("Sachin-Jigar", "Bollywood", "Gujarati"),
        ArtistRecommendationInfo("Darshan Raval", "Romantic", "Gujarati"),
        ArtistRecommendationInfo("Pritam", "Bollywood", "Hindi"),
        ArtistRecommendationInfo("Shreya Ghoshal", "Romantic", "Hindi"),
        ArtistRecommendationInfo("Badshah", "Hip-Hop", "Hindi"),
        ArtistRecommendationInfo("AP Dhillon", "Punjabi Pop", "Punjabi"),
        ArtistRecommendationInfo("Diljit Dosanjh", "Punjabi Pop", "Punjabi"),
        ArtistRecommendationInfo("Sidhu Moose Wala", "Hip-Hop", "Punjabi"),
        ArtistRecommendationInfo("Atif Aslam", "Romantic", "Hindi"),
        ArtistRecommendationInfo("Kishore Kumar", "Retro", "Hindi"),
        ArtistRecommendationInfo("Lata Mangeshkar", "Retro", "Hindi"),
        ArtistRecommendationInfo("AR Rahman", "Bollywood", "Tamil"),
        ArtistRecommendationInfo("Anirudh Ravichander", "Pop", "Tamil"),
        ArtistRecommendationInfo("Taylor Swift", "Pop", "English"),
        ArtistRecommendationInfo("The Weeknd", "R&B / Pop", "English"),
        ArtistRecommendationInfo("Ed Sheeran", "Pop", "English"),
        ArtistRecommendationInfo("Drake", "Hip-Hop", "English"),
        ArtistRecommendationInfo("Billie Eilish", "Pop", "English"),
        ArtistRecommendationInfo("Sonu Nigam", "Romantic", "Hindi"),
        ArtistRecommendationInfo("Amit Trivedi", "Indie", "Hindi"),
        ArtistRecommendationInfo("Neha Kakkar", "Pop", "Hindi")
    )
}
