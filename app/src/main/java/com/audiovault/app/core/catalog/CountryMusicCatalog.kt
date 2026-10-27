package com.audiovault.app.core.catalog

import com.audiovault.app.core.online.OnlineAlbumModel
import com.audiovault.app.data.model.ArtistRecommendationInfo
import com.audiovault.app.data.model.CountryInfo
import com.audiovault.app.data.model.LanguageInfo
import com.audiovault.app.data.model.MusicPreferenceCatalog

/**
 * Country-specific shelf configuration for dynamic home discovery.
 */
data class CountryShelfConfig(
    val title: String,
    val subtitle: String,
    val searchQuery: String,
    val category: String = "general"
)

/**
 * Complete regional music profile defining local taste, curated hero, quick access tiles,
 * native languages, popular artists, and home shelves.
 */
data class CountryMusicProfile(
    val countryCode: String,
    val countryName: String,
    val flagEmoji: String,
    val defaultPrimaryLanguage: String,
    val supportedLanguages: List<LanguageInfo>,
    val popularArtists: List<ArtistRecommendationInfo>,
    val heroAlbum: OnlineAlbumModel,
    val quickAccessTiles: List<OnlineAlbumModel>,
    val shelves: List<CountryShelfConfig>
)

object CountryMusicCatalog {

    val PROFILES: Map<String, CountryMusicProfile> by lazy {
        listOf(
            createIndiaProfile(),
            createUsaProfile(),
            createUkProfile(),
            createSouthKoreaProfile(),
            createJapanProfile(),
            createBrazilProfile(),
            createUaeProfile(),
            createSpainProfile()
        ).associateBy { it.countryCode.uppercase() }
    }

    fun getProfile(countryCode: String?): CountryMusicProfile {
        val code = countryCode?.trim()?.uppercase() ?: "IN"
        return PROFILES[code] ?: PROFILES["IN"] ?: createIndiaProfile()
    }

    fun getHeroAlbum(countryCode: String?): OnlineAlbumModel {
        return getProfile(countryCode).heroAlbum
    }

    fun getQuickAccessTiles(countryCode: String?): List<OnlineAlbumModel> {
        return getProfile(countryCode).quickAccessTiles
    }

    fun getShelves(countryCode: String?): List<CountryShelfConfig> {
        return getProfile(countryCode).shelves
    }

    fun getSupportedLanguages(countryCode: String?): List<LanguageInfo> {
        return getProfile(countryCode).supportedLanguages
    }

    fun getPopularArtists(countryCode: String?): List<ArtistRecommendationInfo> {
        return getProfile(countryCode).popularArtists
    }

    // -------------------------------------------------------------
    // 1. 🇮🇳 INDIA (Bollywood, Punjabi, Gujarati, Tamil, Telugu, Indie, 90s)
    // -------------------------------------------------------------
    private fun createIndiaProfile(): CountryMusicProfile {
        val hero = CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_bollywood_mush" }
            ?: OnlineAlbumModel(
                id = "curated_bollywood_mush",
                title = "Bollywood Mush",
                year = "Playlist",
                artist = "Playlist • Recordify",
                primaryArtists = "Arijit Singh, Mithoon, Tanishk Bagchi",
                coverArtUrl = "https://c.saavncdn.com/598/Saiyaara-Hindi-2025-20250703061754-500x500.jpg",
                type = "Playlist",
                source = "curated",
                searchQuery = "Bollywood Mush love songs",
                songCount = 12
            )

        val quickTiles = listOf(
            hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_arijit_singh" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_aditya_gadhvi" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_kumar_sanu_all_time_hits" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_padmaavat" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_love_songs_emoji" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_90s_pop_rock" } ?: hero,
            CuratedAlbumCatalog.curatedAlbums.find { it.id == "curated_90s_bollywood" } ?: hero
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in India", "Top viral hits streaming across India right now", "top trending hindi bollywood songs 2024", "trending"),
            CountryShelfConfig("🎬 Latest Bollywood Hits", "Fresh chartbusters from blockbuster cinema", "latest bollywood movie songs 2024", "bollywood"),
            CountryShelfConfig("❤️ Romantic Hindi Melodies", "Soulful love ballads for the heart", "best hindi romantic songs love hits", "romantic"),
            CountryShelfConfig("🎤 Punjabi Trending", "High-energy beats from Punjab's finest", "punjabi viral hits trending 2024", "punjabi"),
            CountryShelfConfig("🎵 Gujarati Folk & Urban Hits", "Timeless garba, folk and urban melodies", "gujarati garba urban hits", "regional"),
            CountryShelfConfig("✨ Indian Indie & Acoustic", "Original independent music and raw acoustics", "indian indie folk acoustic unplugged", "indie"),
            CountryShelfConfig("🕰️ 90s Bollywood Classics", "Nostalgic golden era melodies of the 90s", "90s evergreen bollywood hits", "retro")
        )

        val languages = listOf(
            LanguageInfo("hi", "Hindi", "हिन्दी"),
            LanguageInfo("gu", "Gujarati", "ગુજરાતી"),
            LanguageInfo("pa", "Punjabi", "ਪੰਜਾਬੀ"),
            LanguageInfo("ta", "Tamil", "தமிழ்"),
            LanguageInfo("te", "Telugu", "తెలుగు"),
            LanguageInfo("bn", "Bengali", "বাংলা"),
            LanguageInfo("mr", "Marathi", "मराठी"),
            LanguageInfo("ml", "Malayalam", "മലയാളം"),
            LanguageInfo("en", "English", "English")
        )

        val artists = listOf(
            ArtistRecommendationInfo("Arijit Singh", "Romantic", "Hindi"),
            ArtistRecommendationInfo("Shreya Ghoshal", "Romantic", "Hindi"),
            ArtistRecommendationInfo("Diljit Dosanjh", "Punjabi Pop", "Punjabi"),
            ArtistRecommendationInfo("A.R. Rahman", "Bollywood", "Tamil"),
            ArtistRecommendationInfo("Pritam", "Bollywood", "Hindi"),
            ArtistRecommendationInfo("KK", "Romantic", "Hindi"),
            ArtistRecommendationInfo("Atif Aslam", "Romantic", "Hindi"),
            ArtistRecommendationInfo("Sachin-Jigar", "Bollywood", "Gujarati"),
            ArtistRecommendationInfo("Darshan Raval", "Romantic", "Gujarati"),
            ArtistRecommendationInfo("Anirudh Ravichander", "Pop", "Tamil"),
            ArtistRecommendationInfo("Sidhu Moose Wala", "Hip-Hop", "Punjabi"),
            ArtistRecommendationInfo("Sonu Nigam", "Romantic", "Hindi")
        )

        return CountryMusicProfile("IN", "India", "🇮🇳", "Hindi", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 2. 🇺🇸 USA (American Pop, Hip-Hop, Rock, R&B, Country, New Releases)
    // -------------------------------------------------------------
    private fun createUsaProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_todays_top_hits_us",
            title = "Today's Top Hits USA",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Taylor Swift, Drake, The Weeknd, Billie Eilish",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/c3/8c/d7/c38cd731-0498-8ec1-ea40-39efcbbd424b/23UM1IM48974.rgb.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "today top hits pop music usa",
            songCount = 10,
            trackQueries = listOf(
                "Cruel Summer Taylor Swift",
                "Espresso Sabrina Carpenter",
                "greedy Tate McRae",
                "vampire Olivia Rodrigo",
                "Paint The Town Red Doja Cat",
                "Lose Control Teddy Swims",
                "Lovin On Me Jack Harlow",
                "Stick Season Noah Kahan",
                "Water Tyla",
                "I Had Some Help Post Malone Morgan Wallen"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_taylor_swift", "Taylor Swift", "Artist • Taylor Swift", "Taylor Swift", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/58/b0/02/58b0022d-3cb8-3162-8eec-4c6e9389241b/23UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Cruel Summer Taylor Swift", "Anti-Hero Taylor Swift", "Blank Space Taylor Swift")),
            createTile("curated_drake", "Drake", "Artist • Drake", "Drake", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/71/e6/65/71e665ba-9f20-9907-7e61-8b839aa354f9/23UM1IM48974.rgb.jpg/600x600bb.jpg", listOf("God's Plan Drake", "One Dance Drake", "Rich Baby Daddy Drake")),
            createTile("curated_the_weeknd", "The Weeknd", "Artist • The Weeknd", "The Weeknd", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/57/0d/1c/570d1cfa-bca9-04fa-555e-bc8e2d41a7d1/20UMGIM08709.rgb.jpg/600x600bb.jpg", listOf("Blinding Lights The Weeknd", "Starboy The Weeknd", "Save Your Tears The Weeknd")),
            createTile("curated_billie_eilish", "Billie Eilish", "Artist • Billie Eilish", "Billie Eilish", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg", listOf("BIRDS OF A FEATHER Billie Eilish", "bad guy Billie Eilish", "lovely Billie Eilish Khalid")),
            createTile("curated_kendrick_lamar", "Kendrick Lamar", "Artist • Kendrick Lamar", "Kendrick Lamar", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/44/e9/87/44e98782-b7e1-88df-7598-a83d3ff0cb7e/22UMGIM45902.rgb.jpg/600x600bb.jpg", listOf("Not Like Us Kendrick Lamar", "HUMBLE Kendrick Lamar", "All The Stars Kendrick Lamar SZA")),
            createTile("curated_morgan_wallen", "Morgan Wallen", "Artist • Morgan Wallen", "Morgan Wallen", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/33/c0/81/33c081e5-8f64-41d3-6ff9-a78c9bb759dc/23UMGIM09090.rgb.jpg/600x600bb.jpg", listOf("Last Night Morgan Wallen", "Wasted On You Morgan Wallen", "Thinkin' Bout Me Morgan Wallen")),
            createTile("curated_post_malone", "Post Malone", "Artist • Post Malone", "Post Malone", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/b4/0f/c6/b40fc6fe-f5e9-847e-4050-89ef488fa8e2/23UMGIM43243.rgb.jpg/600x600bb.jpg", listOf("Sunflower Post Malone Swae Lee", "Circles Post Malone", "Congratulations Post Malone"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in USA", "Billboard Hot 100 & viral hits streaming in America", "billboard hot 100 top hits usa 2024", "trending"),
            CountryShelfConfig("🎤 American Hip-Hop & Rap", "The biggest trap, drill, and lyrical hip-hop hits", "top american hip hop rap hits 2024", "hiphop"),
            CountryShelfConfig("🎵 Today's Pop Hits", "Massive melodic pop anthems defining the airwaves", "pop music hits usa chartbusters", "pop"),
            CountryShelfConfig("🎸 Rock Essentials", "From timeless stadium classics to modern indie rock", "classic and modern rock essentials usa", "rock"),
            CountryShelfConfig("🌙 Late Night R&B", "Velvety smooth contemporary R&B and neo-soul", "late night chill rb soul usa", "rnb"),
            CountryShelfConfig("🤠 American Country Hits", "Modern Nashville storytellers and country radio favorites", "modern country hits radio usa", "country"),
            CountryShelfConfig("✨ New Releases USA", "Brand-new singles and album drops this week", "new english song releases 2024 usa", "new_releases")
        )

        val languages = listOf(
            LanguageInfo("en", "English", "English"),
            LanguageInfo("es", "Spanish", "Español"),
            LanguageInfo("fr", "French", "Français")
        )

        val artists = listOf(
            ArtistRecommendationInfo("Taylor Swift", "Pop", "English"),
            ArtistRecommendationInfo("Drake", "Hip-Hop", "English"),
            ArtistRecommendationInfo("The Weeknd", "R&B / Pop", "English"),
            ArtistRecommendationInfo("Billie Eilish", "Pop", "English"),
            ArtistRecommendationInfo("Kendrick Lamar", "Hip-Hop", "English"),
            ArtistRecommendationInfo("Morgan Wallen", "Country", "English"),
            ArtistRecommendationInfo("Post Malone", "Pop / Hip-Hop", "English"),
            ArtistRecommendationInfo("Bruno Mars", "Pop / R&B", "English"),
            ArtistRecommendationInfo("Ariana Grande", "Pop", "English"),
            ArtistRecommendationInfo("Eminem", "Hip-Hop", "English")
        )

        return CountryMusicProfile("US", "United States", "🇺🇸", "English", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 3. 🇬🇧 UK (British Pop, UK Rap & Grime, Indie Rock, London Late Night)
    // -------------------------------------------------------------
    private fun createUkProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_uk_top_40",
            title = "UK Top 40 Essentials",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Ed Sheeran, Adele, Dua Lipa, Harry Styles",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/97/81/49/978149e7-578f-6fa1-f09d-cbcf57662cf4/21UMGIM81958.rgb.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "uk top 40 singles chart hits",
            songCount = 10,
            trackQueries = listOf(
                "Bad Habits Ed Sheeran",
                "Easy On Me Adele",
                "As It Was Harry Styles",
                "Cold Heart Elton John Dua Lipa",
                "Sprinter Central Cee Dave",
                "Forget Me Lewis Capaldi",
                "Stay With Me Sam Smith",
                "Heat Waves Glass Animals",
                "Miracle Calvin Harris Ellie Goulding",
                "Doja Central Cee"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_ed_sheeran", "Ed Sheeran", "Artist • Ed Sheeran", "Ed Sheeran", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/8e/3c/63/8e3c631c-b5f7-f0a5-f5be-58ea2d70fa72/075679758781.jpg/600x600bb.jpg", listOf("Shape of You Ed Sheeran", "Perfect Ed Sheeran", "Bad Habits Ed Sheeran")),
            createTile("curated_adele", "Adele", "Artist • Adele", "Adele", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/7b/72/78/7b72783d-e43f-b3be-fb23-747d96bbf451/191404118824.png/600x600bb.jpg", listOf("Rolling in the Deep Adele", "Someone Like You Adele", "Hello Adele")),
            createTile("curated_dua_lipa", "Dua Lipa", "Artist • Dua Lipa", "Dua Lipa", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/85/3c/44/853c4424-9cb6-c0c1-be54-9ee7eb192d6e/24UMGIM22367.rgb.jpg/600x600bb.jpg", listOf("Levitating Dua Lipa", "Don't Start Now Dua Lipa", "Houdini Dua Lipa")),
            createTile("curated_harry_styles", "Harry Styles", "Artist • Harry Styles", "Harry Styles", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/4a/c3/91/4ac39149-a226-f7f1-e4ea-b9e382d62118/196589088734.jpg/600x600bb.jpg", listOf("As It Was Harry Styles", "Watermelon Sugar Harry Styles", "Sign of the Times Harry Styles")),
            createTile("curated_coldplay", "Coldplay", "Artist • Coldplay", "Coldplay", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/71/24/4d/71244d2d-59aa-5ea9-5e0f-2114bc21ceaa/190296614132.jpg/600x600bb.jpg", listOf("Viva La Vida Coldplay", "Yellow Coldplay", "Fix You Coldplay")),
            createTile("curated_stormzy", "Stormzy", "Artist • Stormzy", "Stormzy", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/3b/68/70/3b687002-c8d7-7ec0-ca19-74e2d3bb9706/075679872586.jpg/600x600bb.jpg", listOf("Vossi Bop Stormzy", "Own It Stormzy Ed Sheeran", "Hide & Seek Stormzy")),
            createTile("curated_arctic_monkeys", "Arctic Monkeys", "Artist • Arctic Monkeys", "Arctic Monkeys", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/4a/ca/84/4aca84ef-6111-e435-0816-ec8c81881792/887830031737.png/600x600bb.jpg", listOf("Do I Wanna Know? Arctic Monkeys", "505 Arctic Monkeys", "R U Mine? Arctic Monkeys"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in UK", "The UK Official Top 40 singles streaming right now", "official uk top 40 singles chart 2024", "trending"),
            CountryShelfConfig("✨ British Pop Essentials", "Infectious hooks and iconic British songwriting", "british pop hits radio uk", "pop"),
            CountryShelfConfig("🎤 UK Rap, Drill & Grime", "London streets to global stages — gritty UK rap anthems", "uk drill rap grime stormzy central cee", "hiphop"),
            CountryShelfConfig("🎸 British Indie & Britpop", "Timeless anthems from Manchester, Sheffield and beyond", "british indie rock britpop oasis arctic monkeys", "rock"),
            CountryShelfConfig("☕ London Acoustic & Chill", "Rainy day acoustic folk and reflective London melodies", "london acoustic chill indie coffee", "chill")
        )

        val languages = listOf(LanguageInfo("en", "English", "English"))
        val artists = listOf(
            ArtistRecommendationInfo("Ed Sheeran", "Pop", "English"),
            ArtistRecommendationInfo("Adele", "Soul / Pop", "English"),
            ArtistRecommendationInfo("Dua Lipa", "Pop / Disco", "English"),
            ArtistRecommendationInfo("Harry Styles", "Pop / Rock", "English"),
            ArtistRecommendationInfo("Coldplay", "Rock / Pop", "English"),
            ArtistRecommendationInfo("Central Cee", "UK Rap", "English"),
            ArtistRecommendationInfo("Arctic Monkeys", "Indie Rock", "English"),
            ArtistRecommendationInfo("Lewis Capaldi", "Pop", "English")
        )

        return CountryMusicProfile("GB", "United Kingdom", "🇬🇧", "English", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 4. 🇰🇷 SOUTH KOREA (K-Pop Global, Korean R&B, K-Hip-Hop, OST)
    // -------------------------------------------------------------
    private fun createSouthKoreaProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_kpop_on",
            title = "K-Pop ON! Global",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "BTS, BLACKPINK, NewJeans, Stray Kids",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/66/41/55/6641551c-6d55-be2d-f954-46c59b21f92e/196922438848_Cover.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "kpop on global hits",
            songCount = 10,
            trackQueries = listOf(
                "Seven Jungkook Latto",
                "Super Shy NewJeans",
                "Shut Down BLACKPINK",
                "Dynamite BTS",
                "S-Class Stray Kids",
                "Queencard (G)I-DLE",
                "Love Lee AKMU",
                "Spicy aespa",
                "Love wins all IU",
                "Standing Next to You Jungkook"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_bts", "BTS", "Artist • BTS", "BTS", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/05/cf/40/05cf40ee-fb0a-9d62-159c-851752b5d444/196922066874_Cover.jpg/600x600bb.jpg", listOf("Butter BTS", "Dynamite BTS", "Boy With Luv BTS")),
            createTile("curated_blackpink", "BLACKPINK", "Artist • BLACKPINK", "BLACKPINK", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/4b/3e/20/4b3e20e2-7634-19c2-5503-490ba781878f/22UMGIM86065.rgb.jpg/600x600bb.jpg", listOf("Pink Venom BLACKPINK", "How You Like That BLACKPINK", "Kill This Love BLACKPINK")),
            createTile("curated_newjeans", "NewJeans", "Artist • NewJeans", "NewJeans", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/37/10/89/37108920-569b-325b-381c-164741639d6d/196922474921_Cover.jpg/600x600bb.jpg", listOf("Hype Boy NewJeans", "OMG NewJeans", "Ditto NewJeans")),
            createTile("curated_iu", "IU", "Artist • IU", "IU", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/77/80/77/778077c5-59fe-923f-e54d-fc45d65d491c/8804775369650.jpg/600x600bb.jpg", listOf("eight IU SUGA", "Celebrity IU", "Blueming IU")),
            createTile("curated_stray_kids", "Stray Kids", "Artist • Stray Kids", "Stray Kids", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/19/2d/cb/192dcb52-45e0-8fa3-eb12-32a2155e8c15/196922616871_Cover.jpg/600x600bb.jpg", listOf("God's Menu Stray Kids", "Maniac Stray Kids", "LALALALA Stray Kids")),
            createTile("curated_seventeen", "SEVENTEEN", "Artist • SEVENTEEN", "SEVENTEEN", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/bf/16/e0/bf16e036-79cf-d54b-d106-93d3957297e6/196922485590_Cover.jpg/600x600bb.jpg", listOf("Super SEVENTEEN", "HOT SEVENTEEN", "Don't Wanna Cry SEVENTEEN")),
            createTile("curated_twice", "TWICE", "Artist • TWICE", "TWICE", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/ce/eb/b1/ceebb126-c178-e5a9-f000-dcbaaa50a803/196922765876_Cover.jpg/600x600bb.jpg", listOf("What is Love? TWICE", "The Feels TWICE", "FANCY TWICE"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in South Korea", "Melon Top 100 & viral K-Pop streaming in Seoul", "melon top 100 kpop trending hits 2024", "trending"),
            CountryShelfConfig("✨ K-Pop Global Chartbusters", "Worldwide chart-toppers and dazzling choreography anthems", "kpop viral hits blackpink bts newjeans", "kpop"),
            CountryShelfConfig("🌙 Korean R&B & Krnb Chill", "Moody late-night vibes and smooth Korean neo-soul", "krnb korean chill rb dpr ian dean", "rnb"),
            CountryShelfConfig("🎤 Korean Hip-Hop (K-Hip-Hop)", "Hard-hitting flows from Seoul's underground & rap shows", "korean hip hop show me the money khop", "hiphop"),
            CountryShelfConfig("🎬 K-Drama OST Favorites", "Emotional ballads and unforgettable drama soundtracks", "korean drama ost emotional ballad hits", "ost")
        )

        val languages = listOf(
            LanguageInfo("ko", "Korean", "한국어"),
            LanguageInfo("en", "English", "English"),
            LanguageInfo("ja", "Japanese", "日本語")
        )

        val artists = listOf(
            ArtistRecommendationInfo("BTS", "K-Pop", "Korean"),
            ArtistRecommendationInfo("BLACKPINK", "K-Pop", "Korean"),
            ArtistRecommendationInfo("NewJeans", "K-Pop", "Korean"),
            ArtistRecommendationInfo("IU", "K-Pop / Ballad", "Korean"),
            ArtistRecommendationInfo("Stray Kids", "K-Pop", "Korean"),
            ArtistRecommendationInfo("SEVENTEEN", "K-Pop", "Korean"),
            ArtistRecommendationInfo("TWICE", "K-Pop", "Korean"),
            ArtistRecommendationInfo("Jung Kook", "K-Pop / Pop", "Korean")
        )

        return CountryMusicProfile("KR", "South Korea", "🇰🇷", "Korean", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 5. 🇯🇵 JAPAN (J-Pop, Anime Music & OST, J-Rock, City Pop)
    // -------------------------------------------------------------
    private fun createJapanProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_tokyo_super_hits",
            title = "Tokyo Super Hits",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "YOASOBI, Kenshi Yonezu, Ado, Fujii Kaze",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/d5/36/41/d5364115-bb8a-115f-d232-a1dc02bc1463/cover.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "tokyo super hits jpop",
            songCount = 10,
            trackQueries = listOf(
                "Idol YOASOBI",
                "Shinunoga E-Wa Fujii Kaze",
                "Kick Back Kenshi Yonezu",
                "New Genesis Ado",
                "Subtitle Official HIGE DANdism",
                "Gurenge LiSA",
                "Kaikai Kitan Eve",
                "Bling-Bang-Bang-Born Creepy Nuts",
                "Marunouchi Sadistic Sheena Ringo",
                "Matsuri Fujii Kaze"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_yoasobi", "YOASOBI", "Artist • YOASOBI", "YOASOBI", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/c3/8c/d7/c38cd731-0498-8ec1-ea40-39efcbbd424b/23UM1IM48974.rgb.jpg/600x600bb.jpg", listOf("Idol YOASOBI", "Yoru ni Kakeru YOASOBI", "Monster YOASOBI")),
            createTile("curated_kenshi_yonezu", "Kenshi Yonezu", "Artist • Kenshi Yonezu", "Kenshi Yonezu", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg", listOf("Lemon Kenshi Yonezu", "Kick Back Kenshi Yonezu", "Chikyugi Kenshi Yonezu")),
            createTile("curated_ado", "Ado", "Artist • Ado", "Ado", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/4b/3e/20/4b3e20e2-7634-19c2-5503-490ba781878f/22UMGIM86065.rgb.jpg/600x600bb.jpg", listOf("Usseewa Ado", "Show Ado", "New Genesis Ado")),
            createTile("curated_fujii_kaze", "Fujii Kaze", "Artist • Fujii Kaze", "Fujii Kaze", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/44/e9/87/44e98782-b7e1-88df-7598-a83d3ff0cb7e/22UMGIM45902.rgb.jpg/600x600bb.jpg", listOf("Shinunoga E-Wa Fujii Kaze", "Matsuri Fujii Kaze", "Kirari Fujii Kaze")),
            createTile("curated_hige_dan", "Official HIGE DANdism", "Artist • HIGE DAN", "Official HIGE DANdism", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/57/0d/1c/570d1cfa-bca9-04fa-555e-bc8e2d41a7d1/20UMGIM08709.rgb.jpg/600x600bb.jpg", listOf("Pretender Official HIGE DANdism", "Subtitle Official HIGE DANdism", "I LOVE... Official HIGE DANdism")),
            createTile("curated_lisa", "LiSA", "Artist • LiSA", "LiSA", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/97/81/49/978149e7-578f-6fa1-f09d-cbcf57662cf4/21UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Gurenge LiSA", "Homura LiSA", "Crossing Field LiSA")),
            createTile("curated_radwimps", "RADWIMPS", "Artist • RADWIMPS", "RADWIMPS", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/71/24/4d/71244d2d-59aa-5ea9-5e0f-2114bc21ceaa/190296614132.jpg/600x600bb.jpg", listOf("Zenzenzense RADWIMPS", "Sparkle RADWIMPS", "Suzume RADWIMPS"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in Japan", "Oricon & Billboard Japan top charting hits", "oricon billboard japan top 100 hits 2024", "trending"),
            CountryShelfConfig("✨ J-Pop Essentials", "The defining melodies and creative brilliance of modern J-Pop", "jpop top hits yoasobi kenshi yonezu ado", "jpop"),
            CountryShelfConfig("🎬 Anime Music & OST Anthems", "Iconic openings, endings, and movie theme anthems", "anime opening ending ost viral themes", "anime"),
            CountryShelfConfig("🎸 J-Rock Energy", "Electrifying Japanese rock bands with unmatched stage passion", "jrock one ok rock radwimps king gnu", "rock"),
            CountryShelfConfig("🌆 Tokyo City Pop & Neo-Retro", "Sun-soaked 80s groove and nostalgic urban Tokyo nightscapes", "japanese city pop plastic love stay with me", "citypop")
        )

        val languages = listOf(
            LanguageInfo("ja", "Japanese", "日本語"),
            LanguageInfo("en", "English", "English")
        )

        val artists = listOf(
            ArtistRecommendationInfo("YOASOBI", "J-Pop", "Japanese"),
            ArtistRecommendationInfo("Kenshi Yonezu", "J-Pop / Rock", "Japanese"),
            ArtistRecommendationInfo("Ado", "J-Pop", "Japanese"),
            ArtistRecommendationInfo("Fujii Kaze", "R&B / Pop", "Japanese"),
            ArtistRecommendationInfo("Official HIGE DANdism", "J-Pop", "Japanese"),
            ArtistRecommendationInfo("LiSA", "Anime / Rock", "Japanese"),
            ArtistRecommendationInfo("RADWIMPS", "J-Rock", "Japanese"),
            ArtistRecommendationInfo("ONE OK ROCK", "Rock", "Japanese")
        )

        return CountryMusicProfile("JP", "Japan", "🇯🇵", "Japanese", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 6. 🇧🇷 BRAZIL (Funk Brasil, Sertanejo, Samba & Pagode, MPB)
    // -------------------------------------------------------------
    private fun createBrazilProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_funk_brasil",
            title = "Funk Brasil Hits",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Anitta, Alok, Ludmilla, Pedro Sampaio",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/64/00/c7/6400c732-d81a-6df3-1fcf-8ee71be3fe46/0.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "funk brasil hits top",
            songCount = 8,
            trackQueries = listOf(
                "Envolver Anitta",
                "MTG Quero Te Encontrar DJ Japa",
                "Havaianas MC Pedrinho",
                "Ai Papai Anitta MC Danny",
                "Dança Kuduro Don Omar",
                "PocPoc Pedro Sampaio",
                "Leão Marília Mendonça",
                "Coração Cigano Luan Santana Luísa Sonza"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_anitta", "Anitta", "Artist • Anitta", "Anitta", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/64/00/c7/6400c732-d81a-6df3-1fcf-8ee71be3fe46/0.jpg/600x600bb.jpg", listOf("Envolver Anitta", "Bellakeo Peso Pluma Anitta", "Downtown Anitta J Balvin")),
            createTile("curated_alok", "Alok", "Artist • Alok", "Alok", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/c3/8c/d7/c38cd731-0498-8ec1-ea40-39efcbbd424b/23UM1IM48974.rgb.jpg/600x600bb.jpg", listOf("Hear Me Now Alok", "Deep Down Alok", "Don't Say Goodbye Alok")),
            createTile("curated_marilia", "Marília Mendonça", "Artist • Marília Mendonça", "Marília Mendonça", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/58/b0/02/58b0022d-3cb8-3162-8eec-4c6e9389241b/23UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Infiel Marília Mendonça", "Leão Marília Mendonça", "Eu Sei De Cor Marília Mendonça")),
            createTile("curated_gusttavo", "Gusttavo Lima", "Artist • Gusttavo Lima", "Gusttavo Lima", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/97/81/49/978149e7-578f-6fa1-f09d-cbcf57662cf4/21UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Balada Gusttavo Lima", "Bloqueado Gusttavo Lima", "Desejo Imortal Gusttavo Lima")),
            createTile("curated_ludmilla", "Ludmilla", "Artist • Ludmilla", "Ludmilla", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg", listOf("Maldivas Ludmilla", "Sintomas de Prazer Ludmilla", "Verdinha Ludmilla")),
            createTile("curated_luisa_sonza", "Luísa Sonza", "Artist • Luísa Sonza", "Luísa Sonza", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/57/0d/1c/570d1cfa-bca9-04fa-555e-bc8e2d41a7d1/20UMGIM08709.rgb.jpg/600x600bb.jpg", listOf("Chico Luísa Sonza", "Penhasco Luísa Sonza", "Modo Turbo Luísa Sonza")),
            createTile("curated_luan_santana", "Luan Santana", "Artist • Luan Santana", "Luan Santana", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/44/e9/87/44e98782-b7e1-88df-7598-a83d3ff0cb7e/22UMGIM45902.rgb.jpg/600x600bb.jpg", listOf("Meteoro Luan Santana", "Morena Luan Santana", "Meio Termo Luan Santana"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in Brazil", "Top 50 Brasil hits dominating Spotify and TikTok", "top brasil spotify funk sertanejo 2024", "trending"),
            CountryShelfConfig("⚡ Funk Brasil & Baile", "O som dos bailes do Rio e São Paulo no talo", "funk rj sp baile funk viral tiktok brasil", "funk"),
            CountryShelfConfig("🤠 Sertanejo Universitário", "As maiores vozes do sertanejo que movem multidões", "sertanejo hits gusttavo lima marilia mendonca", "sertanejo"),
            CountryShelfConfig("🥁 Samba & Pagode Raiz", "Alegria, cavaquinho e o melhor pagode de mesa", "pagode samba brasil churrasco hits", "pagode"),
            CountryShelfConfig("🌿 MPB & Brazilian Chill", "Poesia cantada e violão das lendas da MPB", "mpb bossa nova caetano veloso gilberto gil", "mpb")
        )

        val languages = listOf(
            LanguageInfo("pt", "Portuguese", "Português"),
            LanguageInfo("en", "English", "English"),
            LanguageInfo("es", "Spanish", "Español")
        )

        val artists = listOf(
            ArtistRecommendationInfo("Anitta", "Funk / Pop", "Portuguese"),
            ArtistRecommendationInfo("Alok", "Electronic / Dance", "English"),
            ArtistRecommendationInfo("Marília Mendonça", "Sertanejo", "Portuguese"),
            ArtistRecommendationInfo("Gusttavo Lima", "Sertanejo", "Portuguese"),
            ArtistRecommendationInfo("Ludmilla", "Pagode / Funk", "Portuguese"),
            ArtistRecommendationInfo("Luísa Sonza", "Pop", "Portuguese"),
            ArtistRecommendationInfo("Luan Santana", "Sertanejo / Pop", "Portuguese"),
            ArtistRecommendationInfo("Jorge & Mateus", "Sertanejo", "Portuguese")
        )

        return CountryMusicProfile("BR", "Brazil", "🇧🇷", "Portuguese", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 7. 🇦🇪 UAE (Khaleeji Pop, Arabic Melodies, Dubai Lounge, Bollywood Fusion)
    // -------------------------------------------------------------
    private fun createUaeProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_khaleeji_arabic_pop",
            title = "Khaleeji & Arabic Pop",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Hussain Al Jassmi, Amr Diab, Nancy Ajram, Balqees",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e0/f8/ee/e0f8ee96-c6eb-8e56-11f8-d4218a5ae7a8/190296711756.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "khaleeji arabic pop hits",
            songCount = 8,
            trackQueries = listOf(
                "Boshret Kheir Hussain Al Jassmi",
                "Tamally Maak Amr Diab",
                "Ah W Noss Nancy Ajram",
                "Enta Eih Nancy Ajram",
                "El Ghazala Rayka Karim Mahmoud",
                "Enta El Haz Amr Diab",
                "Albi Ya Albi Nancy Ajram",
                "Sabran Balqees"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_hussain_al_jassmi", "Hussain Al Jassmi", "Artist • Hussain Al Jassmi", "Hussain Al Jassmi", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e0/f8/ee/e0f8ee96-c6eb-8e56-11f8-d4218a5ae7a8/190296711756.jpg/600x600bb.jpg", listOf("Boshret Kheir Hussain Al Jassmi", "Bel Bonto El Areed Hussain Al Jassmi")),
            createTile("curated_amr_diab", "Amr Diab", "Artist • Amr Diab", "Amr Diab", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/97/81/49/978149e7-578f-6fa1-f09d-cbcf57662cf4/21UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Tamally Maak Amr Diab", "Nour El Ain Amr Diab", "Ya Ana Ya La Amr Diab")),
            createTile("curated_nancy_ajram", "Nancy Ajram", "Artist • Nancy Ajram", "Nancy Ajram", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/58/b0/02/58b0022d-3cb8-3162-8eec-4c6e9389241b/23UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Ah W Noss Nancy Ajram", "Salamat Nancy Ajram", "Enta Eih Nancy Ajram")),
            createTile("curated_balqees", "Balqees", "Artist • Balqees", "Balqees", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg", listOf("Momken Balqees Saif Nabeel", "Sabran Balqees", "Entaha Balqees")),
            createTile("curated_mohamed_hamaki", "Mohamed Hamaki", "Artist • Mohamed Hamaki", "Mohamed Hamaki", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/57/0d/1c/570d1cfa-bca9-04fa-555e-bc8e2d41a7d1/20UMGIM08709.rgb.jpg/600x600bb.jpg", listOf("Ya Sattar Mohamed Hamaki", "La Malama Mohamed Hamaki")),
            createTile("curated_elissa", "Elissa", "Artist • Elissa", "Elissa", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/44/e9/87/44e98782-b7e1-88df-7598-a83d3ff0cb7e/22UMGIM45902.rgb.jpg/600x600bb.jpg", listOf("Betmoun Elissa", "Krahni Elissa", "Aa Baly Habibi Elissa")),
            createTile("curated_saad_lamjarred", "Saad Lamjarred", "Artist • Saad Lamjarred", "Saad Lamjarred", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/c3/8c/d7/c38cd731-0498-8ec1-ea40-39efcbbd424b/23UM1IM48974.rgb.jpg/600x600bb.jpg", listOf("Lm3allem Saad Lamjarred", "Ghaltana Saad Lamjarred"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in UAE", "The top streaming chartbusters across Dubai & Abu Dhabi", "top trending uae dubai arabic khaleeji 2024", "trending"),
            CountryShelfConfig("✨ Khaleeji Top Hits", "Golden authentic rhythms and Gulf poetry", "khaleeji songs hussain al jassmi balqees", "regional"),
            CountryShelfConfig("🌙 Arabic Pop & Melodies", "Sensational modern Arabic pop from Cairo, Beirut & Dubai", "arabic pop amr diab mohamed hamaki elissa", "pop"),
            CountryShelfConfig("🏙️ Dubai Late Night Lounge", "Atmospheric deep lounge, oriental chill & rooftop beats", "dubai luxury lounge oriental chill deep house", "chill"),
            CountryShelfConfig("🎬 Bollywood & Desi in Dubai", "Massive South Asian movie hits loved across the Emirates", "top bollywood hindi hits in uae dubai", "bollywood")
        )

        val languages = listOf(
            LanguageInfo("ar", "Arabic", "العربية"),
            LanguageInfo("en", "English", "English"),
            LanguageInfo("hi", "Hindi", "हिन्दी"),
            LanguageInfo("ur", "Urdu", "اردو")
        )

        val artists = listOf(
            ArtistRecommendationInfo("Hussain Al Jassmi", "Khaleeji / Pop", "Arabic"),
            ArtistRecommendationInfo("Amr Diab", "Arabic Pop", "Arabic"),
            ArtistRecommendationInfo("Nancy Ajram", "Arabic Pop", "Arabic"),
            ArtistRecommendationInfo("Balqees", "Khaleeji", "Arabic"),
            ArtistRecommendationInfo("Mohamed Hamaki", "Arabic Pop", "Arabic"),
            ArtistRecommendationInfo("Elissa", "Romantic Ballad", "Arabic"),
            ArtistRecommendationInfo("Saad Lamjarred", "Pop", "Arabic")
        )

        return CountryMusicProfile("AE", "United Arab Emirates", "🇦🇪", "Arabic", languages, artists, hero, quickTiles, shelves)
    }

    // -------------------------------------------------------------
    // 8. 🇪🇸 SPAIN (Top España, Reggaeton Latino, Flamenco Pop, Pop Urbano)
    // -------------------------------------------------------------
    private fun createSpainProfile(): CountryMusicProfile {
        val hero = OnlineAlbumModel(
            id = "curated_top_exitos_espana",
            title = "Top Éxitos España",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Rosalía, Quevedo, Aitana, C. Tangana",
            coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "top exitos espana reggaeton",
            songCount = 8,
            trackQueries = listOf(
                "Despechá Rosalía",
                "Quevedo: Bzrp Music Sessions, Vol. 52",
                "Mon Amour Remix zzoilo Aitana",
                "Demasiadas Mujeres C. Tangana",
                "Columbia Quevedo",
                "Las Babys Aitana",
                "Tacones Rojos Sebastián Yatra",
                "La Fama Rosalía The Weeknd"
            )
        )

        val quickTiles = listOf(
            hero,
            createTile("curated_rosalia", "Rosalía", "Artist • Rosalía", "Rosalía", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/b8/b1/7b/b8b17b6d-a773-ee10-a249-14a938c114f6/196589139801.jpg/600x600bb.jpg", listOf("Despechá Rosalía", "Malamente Rosalía", "Saoko Rosalía")),
            createTile("curated_quevedo", "Quevedo", "Artist • Quevedo", "Quevedo", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/58/b0/02/58b0022d-3cb8-3162-8eec-4c6e9389241b/23UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Columbia Quevedo", "Punto G Quevedo", "Playa Del Inglés Quevedo")),
            createTile("curated_aitana", "Aitana", "Artist • Aitana", "Aitana", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/57/0d/1c/570d1cfa-bca9-04fa-555e-bc8e2d41a7d1/20UMGIM08709.rgb.jpg/600x600bb.jpg", listOf("Vas A Quedarte Aitana", "Formentera Aitana", "Las Babys Aitana")),
            createTile("curated_c_tangana", "C. Tangana", "Artist • C. Tangana", "C. Tangana", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/97/81/49/978149e7-578f-6fa1-f09d-cbcf57662cf4/21UMGIM81958.rgb.jpg/600x600bb.jpg", listOf("Tú Me Dejaste De Querer C. Tangana", "Demasiadas Mujeres C. Tangana", "Ateo C. Tangana Nathy Peluso")),
            createTile("curated_bad_bunny", "Bad Bunny", "Artist • Bad Bunny", "Bad Bunny", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/44/e9/87/44e98782-b7e1-88df-7598-a83d3ff0cb7e/22UMGIM45902.rgb.jpg/600x600bb.jpg", listOf("Tití Me Preguntó Bad Bunny", "Monaco Bad Bunny", "Ojitos Lindos Bad Bunny")),
            createTile("curated_rauw_alejandro", "Rauw Alejandro", "Artist • Rauw Alejandro", "Rauw Alejandro", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/c3/8c/d7/c38cd731-0498-8ec1-ea40-39efcbbd424b/23UM1IM48974.rgb.jpg/600x600bb.jpg", listOf("Todo De Ti Rauw Alejandro", "Punto 40 Rauw Alejandro", "Desesperados Rauw Alejandro")),
            createTile("curated_morat", "Morat", "Artist • Morat", "Morat", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/64/00/c7/6400c732-d81a-6df3-1fcf-8ee71be3fe46/0.jpg/600x600bb.jpg", listOf("Cómo Te Atreves Morat", "Besos En Guerra Morat", "No Se Va Morat"))
        )

        val shelves = listOf(
            CountryShelfConfig("🔥 Trending in Spain", "Top 50 España y los temas más virales del país", "top 50 espana spotify exitos 2024", "trending"),
            CountryShelfConfig("🌴 Reggaeton Latino & Urbano", "El ritmo urbano que suena en todas las discotecas y playas", "reggaeton urbano quevedo bad bunny rauw", "urbano"),
            CountryShelfConfig("💃 Flamenco Pop & Fusión", "Rumba, compás flamenco y modernidad española", "flamenco pop rosalia c tangana rumba", "flamenco"),
            CountryShelfConfig("✨ Pop Español", "Grandes baladas y pop en español con letras inolvidables", "pop espanol aitana morat pablo alboran", "pop"),
            CountryShelfConfig("🎸 Indie & Rock Español", "Guitarras, festivales y poesía de la escena indie nacional", "indie espanol vetusta morla love of lesbian", "rock")
        )

        val languages = listOf(
            LanguageInfo("es", "Spanish", "Español"),
            LanguageInfo("en", "English", "English")
        )

        val artists = listOf(
            ArtistRecommendationInfo("Rosalía", "Flamenco / Pop", "Spanish"),
            ArtistRecommendationInfo("Quevedo", "Urbano / Reggaeton", "Spanish"),
            ArtistRecommendationInfo("Aitana", "Pop", "Spanish"),
            ArtistRecommendationInfo("C. Tangana", "Fusión / Urbano", "Spanish"),
            ArtistRecommendationInfo("Bad Bunny", "Reggaeton / Trap", "Spanish"),
            ArtistRecommendationInfo("Rauw Alejandro", "Reggaeton", "Spanish"),
            ArtistRecommendationInfo("Morat", "Pop / Folk", "Spanish"),
            ArtistRecommendationInfo("Pablo Alborán", "Pop / Balada", "Spanish")
        )

        return CountryMusicProfile("ES", "Spain", "🇪🇸", "Spanish", languages, artists, hero, quickTiles, shelves)
    }

    private fun createTile(
        id: String,
        title: String,
        artist: String,
        primaryArtists: String,
        coverArtUrl: String,
        trackQueries: List<String>
    ): OnlineAlbumModel {
        return OnlineAlbumModel(
            id = id,
            title = title,
            year = "Artist",
            artist = artist,
            primaryArtists = primaryArtists,
            coverArtUrl = coverArtUrl,
            type = "Artist",
            source = "curated",
            searchQuery = "$title top songs",
            songCount = trackQueries.size,
            trackQueries = trackQueries
        )
    }
}
