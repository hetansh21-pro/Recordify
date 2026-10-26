package com.audiovault.app.core.catalog

import com.audiovault.app.core.online.OnlineAlbumModel

/**
 * Curated catalog of pre-built internet music albums covering iconic eras
 * (90s Bollywood, 70s/80s Old Classics, Romantic Melodies, Retro Legends, Pop & Rock).
 * These albums dynamically fetch genuine full-fidelity internet streams on demand.
 */
object CuratedAlbumCatalog {

    val curatedAlbums: List<OnlineAlbumModel> = listOf(
        OnlineAlbumModel(
            id = "curated_90s_bollywood",
            title = "Golden 90s Bollywood Hits",
            year = "1990s",
            artist = "Kumar Sanu, Alka Yagnik, Udit Narayan",
            primaryArtists = "Kumar Sanu, Alka Yagnik, Udit Narayan",
            coverArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            type = "Curated Era",
            source = "curated",
            searchQuery = "90s Bollywood evergreen hits",
            songCount = 12,
            trackQueries = listOf(
                "Tujhe Dekha To Kumar Sanu",
                "Dheere Dheere Se Meri Zindagi Kumar Sanu",
                "Kuch Kuch Hota Hai Udit Narayan",
                "Tip Tip Barsa Paani Alka Yagnik",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik",
                "Mera Dil Bhi Kitna Pagal Hai Kumar Sanu",
                "Pehla Nasha Udit Narayan",
                "Bahon Ke Darmiyan Hariharan Alka Yagnik",
                "Dil Hai Ki Manta Nahin Kumar Sanu",
                "Ek Ladki Ko Dekha Kumar Sanu",
                "Main Koi Aisa Geet Gaoon Abhijeet",
                "Chand Chhupa Badal Mein Udit Narayan Alka Yagnik"
            )
        ),
        OnlineAlbumModel(
            id = "curated_70s_80s_classics",
            title = "Evergreen 70s & 80s Classics",
            year = "1970 - 1989",
            artist = "Kishore Kumar, Lata Mangeshkar, R.D. Burman",
            primaryArtists = "Kishore Kumar, Lata Mangeshkar, R.D. Burman",
            coverArtUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&auto=format&fit=crop&q=80",
            type = "Curated Era",
            source = "curated",
            searchQuery = "70s 80s golden hits Kishore Kumar",
            songCount = 12,
            trackQueries = listOf(
                "Pal Pal Dil Ke Paas Kishore Kumar",
                "Likhe Jo Khat Tujhe Mohammed Rafi",
                "Dekha Ek Khwab Lata Mangeshkar Kishore Kumar",
                "Lag Ja Gale Se Phir Lata Mangeshkar",
                "Chura Liya Hai Tumne Jo Dil Ko Asha Bhosle Mohammed Rafi",
                "Yeh Sham Mastani Kishore Kumar",
                "O Mere Dil Ke Chain Kishore Kumar",
                "Tere Bina Zindagi Se Lata Mangeshkar Kishore Kumar",
                "Kya Hua Tera Wada Mohammed Rafi",
                "Roop Tera Mastana Kishore Kumar",
                "Aap Ki Ankhon Mein Kuch Kishore Kumar Lata Mangeshkar",
                "Mere Samne Wali Khidki Mein Kishore Kumar"
            )
        ),
        OnlineAlbumModel(
            id = "curated_90s_romantic",
            title = "90s Romantic Melodies",
            year = "1990s",
            artist = "Udit Narayan, Alka Yagnik, Sonu Nigam",
            primaryArtists = "Udit Narayan, Alka Yagnik, Sonu Nigam",
            coverArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            type = "Curated Era",
            source = "curated",
            searchQuery = "90s romantic love songs hits",
            songCount = 10,
            trackQueries = listOf(
                "Pehla Nasha Udit Narayan",
                "Tujhe Dekha To Kumar Sanu",
                "Bahon Ke Darmiyan Alka Yagnik",
                "Kuch Kuch Hota Hai Udit Narayan",
                "Raja Ko Rani Se Pyar Ho Gaya Udit Narayan",
                "Do Dil Mil Rahe Hain Kumar Sanu",
                "Ae Mere Humsafar Udit Narayan Alka Yagnik",
                "Dheere Dheere Se Meri Zindagi Kumar Sanu",
                "Main Yahaan Hoon Udit Narayan",
                "Humko Humise Chura Lo Udit Narayan Lata Mangeshkar"
            )
        ),
        OnlineAlbumModel(
            id = "curated_kishore_kumar",
            title = "Retro Kishore Kumar Golden Hits",
            year = "Timeless",
            artist = "Kishore Kumar",
            primaryArtists = "Kishore Kumar",
            coverArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            type = "Curated Legend",
            source = "curated",
            searchQuery = "Kishore Kumar hits",
            songCount = 10,
            trackQueries = listOf(
                "Pal Pal Dil Ke Paas Kishore Kumar",
                "Yeh Sham Mastani Kishore Kumar",
                "O Mere Dil Ke Chain Kishore Kumar",
                "Roop Tera Mastana Kishore Kumar",
                "Mere Sapno Ki Rani Kishore Kumar",
                "Khaike Paan Banaras Wala Kishore Kumar",
                "Yeh Dosti Hum Nahi Todenge Kishore Kumar",
                "Ek Ajnabee Haseena Se Kishore Kumar",
                "Zindagi Ek Safar Hai Suhana Kishore Kumar",
                "Chala Jata Hoon Kishore Kumar"
            )
        ),
        OnlineAlbumModel(
            id = "curated_lata_mangeshkar",
            title = "Evergreen Lata Mangeshkar Classics",
            year = "Golden Era",
            artist = "Lata Mangeshkar",
            primaryArtists = "Lata Mangeshkar",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            type = "Curated Legend",
            source = "curated",
            searchQuery = "Lata Mangeshkar hits",
            songCount = 10,
            trackQueries = listOf(
                "Lag Ja Gale Se Phir Lata Mangeshkar",
                "Ajeeb Dastan Hai Yeh Lata Mangeshkar",
                "Tere Bina Zindagi Se Lata Mangeshkar",
                "Tujhse Naraz Nahin Zindagi Lata Mangeshkar",
                "Dekha Ek Khwab Lata Mangeshkar",
                "Hothon Mein Aisi Baat Lata Mangeshkar",
                "Kora Kagaz Tha Yeh Man Mera Lata Mangeshkar",
                "Piya Tose Naina Laage Re Lata Mangeshkar",
                "Tujhe Dekha To Lata Mangeshkar",
                "Didi Tera Devar Deewana Lata Mangeshkar"
            )
        ),
        OnlineAlbumModel(
            id = "curated_mohd_rafi",
            title = "Immortal Mohammed Rafi Hits",
            year = "Golden Era",
            artist = "Mohammed Rafi",
            primaryArtists = "Mohammed Rafi",
            coverArtUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            type = "Curated Legend",
            source = "curated",
            searchQuery = "Mohammed Rafi hits",
            songCount = 10,
            trackQueries = listOf(
                "Likhe Jo Khat Tujhe Mohammed Rafi",
                "Kya Hua Tera Wada Mohammed Rafi",
                "Gulabi Aankhen Mohammed Rafi",
                "Chura Liya Hai Tumne Jo Dil Ko Mohammed Rafi",
                "Badan Pe Sitare Lapete Huye Mohammed Rafi",
                "Parda Hai Parda Mohammed Rafi",
                "Baharon Phool Barsao Mohammed Rafi",
                "Chaudhvin Ka Chand Ho Mohammed Rafi",
                "Ehsan Tera Hoga Mujh Par Mohammed Rafi",
                "Taarif Karoon Kya Uski Mohammed Rafi"
            )
        ),
        OnlineAlbumModel(
            id = "curated_90s_pop_rock",
            title = "90s Pop & Rock Anthems",
            year = "1990 - 1999",
            artist = "Bryan Adams, Queen, Backstreet Boys",
            primaryArtists = "Bryan Adams, Queen, Backstreet Boys",
            coverArtUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            type = "Curated Era",
            source = "curated",
            searchQuery = "90s pop rock classic hits",
            songCount = 8,
            trackQueries = listOf(
                "Everything I Do I Do It for You Bryan Adams",
                "I Want It That Way Backstreet Boys",
                "Wonderwall Oasis",
                "Smells Like Teen Spirit Nirvana",
                "Zombie The Cranberries",
                "Losing My Religion REM",
                "Black or White Michael Jackson",
                "No Rain Blind Melon"
            )
        ),
        OnlineAlbumModel(
            id = "curated_old_ghazals",
            title = "Old Is Gold: Soulful Ghazals",
            year = "Timeless",
            artist = "Jagjit Singh, Pankaj Udhas, Mehdi Hassan",
            primaryArtists = "Jagjit Singh, Pankaj Udhas, Mehdi Hassan",
            coverArtUrl = "https://images.unsplash.com/photo-1445985543469-433ecdd62977?w=600&auto=format&fit=crop&q=80",
            type = "Curated Mood",
            source = "curated",
            searchQuery = "Jagjit Singh Pankaj Udhas ghazals",
            songCount = 8,
            trackQueries = listOf(
                "Woh Kagaz Ki Kashti Jagjit Singh",
                "Chithi Aayi Hai Pankaj Udhas",
                "Hothon Se Chhoo Lo Tum Jagjit Singh",
                "Tum Itna Jo Muskura Rahe Ho Jagjit Singh",
                "Jhuki Jhuki Si Nazar Jagjit Singh",
                "Chandi Jaisa Rang Hai Tera Pankaj Udhas",
                "Ranjish Hi Sahi Mehdi Hassan",
                "Duniya Jise Kahte Hain Jagjit Singh"
            )
        ),
        OnlineAlbumModel(
            id = "curated_90s_dance",
            title = "90s High-Energy Dance Hits",
            year = "1990s",
            artist = "Govinda, Karisma Kapoor Era",
            primaryArtists = "Govinda, Karisma Kapoor Era",
            coverArtUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            type = "Curated Mood",
            source = "curated",
            searchQuery = "90s Bollywood dance hits",
            songCount = 8,
            trackQueries = listOf(
                "Kisi Disco Mein Jaaye Alka Yagnik Udit Narayan",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik",
                "Aankh Marey O Ladka Aankh Mare",
                "Tan Tana Tan Tan Tan Tara Abhijeet",
                "Main Laila Laila Chillaunga",
                "Oonchi Hai Building Anu Malik",
                "Husn Hai Suhana Abhijeet Chandana Dixit",
                "Makhna Govinda Amit Kumar"
            )
        ),
        OnlineAlbumModel(
            id = "curated_90s_sad",
            title = "90s Heartbreak & Sad Melodies",
            year = "1990s",
            artist = "Kumar Sanu, Sonu Nigam, Alka Yagnik",
            primaryArtists = "Kumar Sanu, Sonu Nigam, Alka Yagnik",
            coverArtUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=600&auto=format&fit=crop&q=80",
            type = "Curated Mood",
            source = "curated",
            searchQuery = "90s Bollywood sad songs",
            songCount = 8,
            trackQueries = listOf(
                "Achha Sila Diya Tune Mere Pyar Ka Sonu Nigam",
                "Jeeta Tha Jiske Liye Kumar Sanu Alka Yagnik",
                "Dil Tod Ke Hasti Ho Mera Attaullah Khan",
                "Tere Dard Se Dil Abad Raha Kumar Sanu",
                "Main Duniya Bhula Doonga Kumar Sanu",
                "Bedardi Se Pyar Ka Sahara Na Mila",
                "Is Tarah Aashiqui Ka Asar Chhod Jaunga Kumar Sanu",
                "Wafa Na Raas Aayee Nitin Mukesh"
            )
        ),
        // Iconic Curated Playlists (Recordify Curated Collections)
        OnlineAlbumModel(
            id = "curated_90s_bollywood_playlist",
            title = "90s bollywood",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Bollywood 90s",
            coverArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            type = "Playlist",
            source = "curated",
            searchQuery = "90s bollywood",
            songCount = 10,
            trackQueries = listOf(
                "Tujhe Dekha To Kumar Sanu",
                "Pardesi Pardesi Udit Narayan Alka Yagnik",
                "Dheere Dheere Se Meri Zindagi Kumar Sanu",
                "Kuch Kuch Hota Hai Udit Narayan",
                "Tip Tip Barsa Paani Alka Yagnik",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik",
                "Raja Ko Rani Se Pyar Ho Gaya Udit Narayan",
                "Baazigar O Baazigar Kumar Sanu",
                "Aaye Ho Meri Zindagi Mein Udit Narayan",
                "Mera Dil Bhi Kitna Pagal Hai Kumar Sanu"
            )
        ),
        OnlineAlbumModel(
            id = "curated_old_is_gold_playlist",
            title = "Old is Gold",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Recordify",
            coverArtUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&auto=format&fit=crop&q=80",
            type = "Playlist",
            source = "curated",
            searchQuery = "Old is Gold Hindi hits",
            songCount = 10,
            trackQueries = listOf(
                "Likhe Jo Khat Tujhe Mohammed Rafi",
                "Pal Pal Dil Ke Paas Kishore Kumar",
                "Dekha Ek Khwab Lata Mangeshkar Kishore Kumar",
                "Chura Liya Hai Tumne Jo Dil Ko Asha Bhosle",
                "Yeh Sham Mastani Kishore Kumar",
                "Lag Ja Gale Se Phir Lata Mangeshkar",
                "Mere Samne Wali Khidki Mein Kishore Kumar",
                "Gulabi Aankhen Mohammed Rafi",
                "Yeh Dosti Hum Nahi Todenge Kishore Kumar",
                "Badan Pe Sitare Lapete Huye Mohammed Rafi"
            )
        ),
        OnlineAlbumModel(
            id = "curated_arijit_sad_playlist",
            title = "Arijit Singh All Sad Songs 💔🥀",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Arijit Singh",
            coverArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            type = "Playlist",
            source = "curated",
            searchQuery = "Arijit Singh sad songs",
            songCount = 10,
            trackQueries = listOf(
                "Channa Mereya Arijit Singh",
                "Tum Hi Ho Arijit Singh",
                "Ae Dil Hai Mushkil Arijit Singh",
                "Hamari Adhuri Kahani Arijit Singh",
                "Roke Na Ruke Naina Arijit Singh",
                "Khairiyat Arijit Singh",
                "Tujhe Kitna Chahne Lage Arijit Singh",
                "Pachtaoge Arijit Singh",
                "Agar Tum Saath Ho Arijit Singh Alka Yagnik",
                "Muskurane Arijit Singh"
            )
        ),
        OnlineAlbumModel(
            id = "curated_sad_hindi_songs_playlist",
            title = "Sad Hindi Songs 🥺💔 (Mood Off 🥺)",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Hindi Sad Hits",
            coverArtUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=600&auto=format&fit=crop&q=80",
            type = "Playlist",
            source = "curated",
            searchQuery = "Sad Hindi songs heartbreak",
            songCount = 10,
            trackQueries = listOf(
                "Thodi Der Farhan Saeed Shreya Ghoshal",
                "Tum Hi Aana Jubin Nautiyal",
                "Jo Tum Mere Ho Anuv Jain",
                "Zaroorat Mustafa Zahid",
                "Bewajah Sanam Teri Kasam",
                "Kaise Hua Vishal Mishra",
                "Bekhayali Sachet Tandon",
                "Tera Ban Jaunga Akhil Sachdeva Tulsi Kumar",
                "Hasi Shreya Ghoshal Ami Mishra",
                "Main Dhoondne Ko Zamaane Mein Arijit Singh"
            )
        )
    )

    /**
     * Finds curated albums matching search terms such as:
     * "old songs", "olds", "sad songs", "90s", "90s songs", "retro", "classic", "kishore", etc.
     */
    fun findMatchingAlbums(rawQuery: String): List<OnlineAlbumModel> {
        val q = rawQuery.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val isSad = q.contains("sad") || q.contains("dard") || q.contains("heartbreak") || q.contains("emotional")
        val is90s = q.contains("90") || q.contains("nineties")
        val isOldOrRetro = q.contains("old") || q.contains("retro") || q.contains("classic") ||
                q.contains("puran") || q.contains("evergreen") || q.contains("vintage") ||
                q.contains("gold")
        val is70sOr80s = q.contains("70") || q.contains("80")

        return curatedAlbums.filter { album ->
            val titleLower = album.title.lowercase()
            val artistLower = (album.artist ?: "").lowercase()
            val queryLower = (album.searchQuery ?: "").lowercase()

            when {
                // Sad intent
                isSad && (titleLower.contains("sad") || queryLower.contains("sad") || titleLower.contains("heartbreak")) -> true

                // "90s" or "90s songs" intent
                is90s && (titleLower.contains("90") || album.year?.contains("199") == true || queryLower.contains("90")) -> true

                // "70s" or "80s" intent
                is70sOr80s && (titleLower.contains("70") || titleLower.contains("80") || album.year?.contains("197") == true || album.year?.contains("198") == true) -> true

                // "old songs", "olds", "retro", "classic" intent
                isOldOrRetro && (titleLower.contains("old") || titleLower.contains("evergreen") || titleLower.contains("retro") || titleLower.contains("gold") || titleLower.contains("70") || titleLower.contains("80") || titleLower.contains("90") || titleLower.contains("rafi") || titleLower.contains("kishore") || titleLower.contains("lata")) -> true

                // Direct query token matching
                titleLower.contains(q) || artistLower.contains(q) || queryLower.contains(q) -> true

                // Substring token match
                q.split(" ").filter { it.length >= 3 }.any { word ->
                    titleLower.contains(word) || artistLower.contains(word)
                } -> true

                else -> false
            }
        }
    }
}
