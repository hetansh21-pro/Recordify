package com.audiovault.app.core.recommendation

import com.audiovault.app.data.local.entity.ListeningHistoryEntity
import java.util.Calendar

enum class VibeCategory(
    val displayName: String,
    val searchKeyword: String,
    val description: String,
    val emoji: String
) {
    CHILL("Chill Vibe", "chill acoustic lofi", "Laid-back, mellow, and acoustic grooves", "🌿"),
    ROMANTIC("Romantic Vibe", "romantic love songs", "Heartfelt melodies and love ballads", "❤️"),
    ENERGETIC("Energetic Vibe", "energetic dance pop", "Upbeat rhythms to keep you moving", "⚡"),
    LOFI("Lo-fi & Chill", "lofi beats relax", "Calm soothing lo-fi study and sleep vibes", "☕"),
    BOLLYWOOD("Bollywood Hits", "bollywood top hits", "Chart-topping Hindi film tracks & classics", "🎬"),
    FOCUS("Focus & Relax", "peaceful instrumental focus", "Deep ambient focus for work & study", "🎧"),
    PARTY("Party Hits", "party dance remix", "High-octane club & celebration tracks", "🎉"),
    SAD("Moody & Melancholy", "sad acoustic emotional", "Soulful and emotional melodies", "🌧️"),
    WORKOUT("Workout Fuel", "gym workout power motivation", "High BPM tracks to crush your fitness goals", "🔥"),
    TRENDING("Trending Worldwide", "top viral songs 2024", "Currently trending on global charts", "📈"),
    DISCOVERY("Fresh Discovery", "fresh indie pop discovery", "New releases and hidden gems tailored for you", "✨"),
    MORNING("Morning Vibe", "morning acoustic feel good", "Bright and uplifting tracks to start your day", "☀️"),
    NIGHT("Late Night Vibe", "late night slow calm", "Intimate, smooth tunes for the night", "🌙"),
    MIXED("Your Custom Vibe", "top hits mix", "A balanced personalized blend of your favorite genres", "🎶")
}

data class UserVibe(
    val category: VibeCategory,
    val confidence: Float,
    val greeting: String,
    val subtitle: String
) {
    val vibeName: String get() = category.displayName
}

object VibeDetector {

    fun getTimeGreeting(userName: String = ""): Pair<String, VibeCategory> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val nameSuffix = if (userName.isNotBlank()) ", $userName" else ""

        return when (hour) {
            in 5..11 -> Pair("Good morning$nameSuffix", VibeCategory.MORNING)
            in 12..16 -> Pair("Good afternoon$nameSuffix", VibeCategory.CHILL)
            in 17..21 -> Pair("Good evening$nameSuffix", VibeCategory.ROMANTIC)
            else -> Pair("Good night$nameSuffix", VibeCategory.NIGHT)
        }
    }

    fun detectCurrentVibe(
        recentHistory: List<ListeningHistoryEntity>,
        timeCategory: VibeCategory? = null
    ): UserVibe {
        val (greeting, defaultTimeVibe) = getTimeGreeting()
        val baseTimeVibe = timeCategory ?: defaultTimeVibe

        if (recentHistory.isEmpty()) {
            return UserVibe(
                category = baseTimeVibe,
                confidence = 0.65f,
                greeting = greeting,
                subtitle = "Your ${baseTimeVibe.displayName}"
            )
        }

        // Analyze the recent 10 events
        val recentSample = recentHistory.take(10)
        val vibeScores = mutableMapOf<VibeCategory, Float>()

        for (event in recentSample) {
            val text = "${event.title} ${event.artist}".lowercase()
            when {
                text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || text.contains("slowed") ->
                    vibeScores[VibeCategory.LOFI] = (vibeScores[VibeCategory.LOFI] ?: 0f) + 1.2f
                text.contains("romantic") || text.contains("love") || text.contains("ishq") || text.contains("dil") ->
                    vibeScores[VibeCategory.ROMANTIC] = (vibeScores[VibeCategory.ROMANTIC] ?: 0f) + 1.2f
                text.contains("party") || text.contains("dance") || text.contains("remix") || text.contains("dj") ->
                    vibeScores[VibeCategory.PARTY] = (vibeScores[VibeCategory.PARTY] ?: 0f) + 1.2f
                text.contains("gym") || text.contains("workout") || text.contains("bass") || text.contains("power") ->
                    vibeScores[VibeCategory.WORKOUT] = (vibeScores[VibeCategory.WORKOUT] ?: 0f) + 1.2f
                text.contains("sad") || text.contains("dard") || text.contains("broken") || text.contains("judaai") ->
                    vibeScores[VibeCategory.SAD] = (vibeScores[VibeCategory.SAD] ?: 0f) + 1.2f
                text.contains("acoustic") || text.contains("unplugged") || text.contains("peace") ->
                    vibeScores[VibeCategory.CHILL] = (vibeScores[VibeCategory.CHILL] ?: 0f) + 1.0f
                else ->
                    vibeScores[VibeCategory.BOLLYWOOD] = (vibeScores[VibeCategory.BOLLYWOOD] ?: 0f) + 0.8f
            }
        }

        val topVibeEntry = vibeScores.maxByOrNull { it.value }
        if (topVibeEntry != null && topVibeEntry.value >= 2.0f) {
            val confidence = (topVibeEntry.value / recentSample.size.toFloat()).coerceIn(0.5f, 0.95f)
            return UserVibe(
                category = topVibeEntry.key,
                confidence = confidence,
                greeting = greeting,
                subtitle = "Your ${topVibeEntry.key.displayName}"
            )
        }

        return UserVibe(
            category = baseTimeVibe,
            confidence = 0.70f,
            greeting = greeting,
            subtitle = "Your ${baseTimeVibe.displayName}"
        )
    }

    fun getNextVibe(current: VibeCategory): VibeCategory {
        val rotation = listOf(
            VibeCategory.CHILL,
            VibeCategory.ROMANTIC,
            VibeCategory.LOFI,
            VibeCategory.BOLLYWOOD,
            VibeCategory.DISCOVERY,
            VibeCategory.ENERGETIC,
            VibeCategory.FOCUS,
            VibeCategory.PARTY
        )
        val currentIndex = rotation.indexOf(current)
        return if (currentIndex >= 0 && currentIndex < rotation.size - 1) {
            rotation[currentIndex + 1]
        } else {
            rotation.first()
        }
    }
}
