package com.audiovault.app.core.catalog

import com.audiovault.app.core.online.OnlineAlbumModel

/**
 * Curated catalog of pre-built internet music albums covering iconic eras
 * (90s Bollywood, 70s/80s Old Classics, Romantic Melodies, Retro Legends, Pop & Rock).
 * All artist albums contain strictly only that artist's songs, ordered with top trending hits first.
 * All artwork uses verified, clean, full HD (500x500) movie posters and authentic artist portraits.
 */
object CuratedAlbumCatalog {

    val curatedAlbums: List<OnlineAlbumModel> = listOf(
        // Signature Spotify TV Playlist: Bollywood Mush
        OnlineAlbumModel(
            id = "curated_bollywood_mush",
            title = "Bollywood Mush",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Arijit Singh, Mithoon, Tanishk Bagchi",
            coverArtUrl = "https://c.saavncdn.com/598/Saiyaara-Hindi-2025-20250703061754-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Bollywood Mush love songs",
            songCount = 12,
            trackQueries = listOf(
                "Darkhaast Mithoon Arijit Singh Sunidhi Chauhan",
                "Saiyaara (From \"Saiyaara\") Tanishk Bagchi Faheem Abdullah",
                "Baarish Shashaa Tirupati Ash King",
                "Agar Tum Saath Ho Alka Yagnik Arijit Singh",
                "Vaaroon Forever Shreya Ghoshal Anand Bhaskar",
                "Apna Bana Le Arijit Singh Sachin Jigar",
                "Kesariya Arijit Singh Pritam",
                "Raataan Lambiyan Jubin Nautiyal Asees Kaur",
                "Hawayein Arijit Singh Pritam",
                "Tum Hi Ho Arijit Singh Mithoon",
                "Dil Diyan Gallan Atif Aslam",
                "Zaalima Arijit Singh Harshdeep Kaur"
            )
        ),
        // Arijit Singh Curated Artist Album (Strictly Arijit Singh, Top Trending Hits First)
        OnlineAlbumModel(
            id = "curated_arijit_singh",
            title = "Arijit Singh",
            year = "Artist",
            artist = "Artist • Arijit Singh",
            primaryArtists = "Arijit Singh",
            coverArtUrl = "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Arijit Singh top hits",
            songCount = 12,
            trackQueries = listOf(
                "Tum Hi Ho Arijit Singh",
                "Kesariya Arijit Singh",
                "Channa Mereya Arijit Singh",
                "Apna Bana Le Arijit Singh",
                "Agar Tum Saath Ho Arijit Singh",
                "Hawayein Arijit Singh",
                "Ae Dil Hai Mushkil Arijit Singh",
                "Shayad Arijit Singh",
                "Gerua Arijit Singh Antara Mitra",
                "Zaalima Arijit Singh",
                "Kalank Title Track Arijit Singh",
                "Raabta Arijit Singh"
            )
        ),
        // Aditya Gadhvi Curated Artist Album (Strictly Aditya Gadhvi, Top Trending Hits First)
        OnlineAlbumModel(
            id = "curated_aditya_gadhvi",
            title = "Aditya Gadhvi",
            year = "Artist",
            artist = "Artist • Aditya Gadhvi",
            primaryArtists = "Aditya Gadhvi",
            coverArtUrl = "https://c.saavncdn.com/artists/Aditya_Gadhvi_004_20260902060949_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Aditya Gadhvi Gujarati songs",
            songCount = 8,
            trackQueries = listOf(
                "Khalasi Coke Studio Bharat Aditya Gadhvi Achint",
                "Mor Bani Thanghat Kare Aditya Gadhvi",
                "Vhalam Aavo Ne Aditya Gadhvi",
                "Navlaakhaal Loba Aditya Gadhvi",
                "Dakla Aditya Gadhvi",
                "Mahahetwali Aditya Gadhvi",
                "Aavjo Aditya Gadhvi",
                "Maha Hetvali Aditya Gadhvi"
            )
        ),
        // Pritam Curated Artist Album (Strictly Pritam compositions, Top Trending Hits First)
        OnlineAlbumModel(
            id = "curated_pritam",
            title = "Pritam",
            year = "Artist",
            artist = "Artist • Pritam",
            primaryArtists = "Pritam",
            coverArtUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Pritam hits songs",
            songCount = 12,
            trackQueries = listOf(
                "Kesariya Pritam Arijit Singh",
                "Channa Mereya Pritam Arijit Singh",
                "Tum Se Hi Pritam Mohit Chauhan",
                "Subhanallah Pritam Sreerama Chandra",
                "Badtameez Dil Pritam Benny Dayal",
                "Kabira Pritam Tochi Raina Rekha Bhardwaj",
                "Shayad Pritam Arijit Singh",
                "Illahi Pritam Arijit Singh",
                "Pee Loon Pritam Mohit Chauhan",
                "Tera Hone Laga Hoon Pritam Atif Aslam",
                "Mauja Hi Mauja Pritam Mika Singh",
                "Tum Mile Pritam Neeraj Shridhar"
            )
        ),
        // Kumar Sanu Curated Artist Album (Strictly Kumar Sanu, All-Time Greatest Hits First)
        OnlineAlbumModel(
            id = "curated_kumar_sanu_all_time_hits",
            title = "Kumar Sanu All Time Hits",
            year = "Artist",
            artist = "Artist • Kumar Sanu",
            primaryArtists = "Kumar Sanu",
            coverArtUrl = "https://c.saavncdn.com/886/Kumar-Sanu-Hits-2012-500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Kumar Sanu all time hits Hindi songs",
            songCount = 12,
            trackQueries = listOf(
                "Tujhe Dekha To Kumar Sanu",
                "Dheere Dheere Se Meri Zindagi Kumar Sanu",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik",
                "Mera Dil Bhi Kitna Pagal Hai Kumar Sanu",
                "Dil Hai Ki Manta Nahin Kumar Sanu",
                "Ek Ladki Ko Dekha Kumar Sanu",
                "Do Dil Mil Rahe Hain Kumar Sanu",
                "Baazigar O Baazigar Kumar Sanu",
                "Sambhala Hai Maine Kumar Sanu",
                "Yeh Kaali Kaali Aankhen Kumar Sanu",
                "Aankh Marey O Ladka Aankh Mare Kumar Sanu",
                "Ladki Badi Anjani Hai Kumar Sanu"
            )
        ),
        // Kishore Kumar Curated Artist Album (Strictly Kishore Kumar, Top Trending Hits First)
        OnlineAlbumModel(
            id = "curated_kishore_kumar",
            title = "Retro Kishore Kumar Golden Hits",
            year = "Artist",
            artist = "Artist • Kishore Kumar",
            primaryArtists = "Kishore Kumar",
            coverArtUrl = "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Kishore Kumar golden hits",
            songCount = 12,
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
                "Chala Jata Hoon Kishore Kumar",
                "Kora Kagaz Tha Yeh Man Mera Kishore Kumar",
                "Aane Wala Pal Jaane Wala Hai Kishore Kumar"
            )
        ),
        // Lata Mangeshkar Curated Artist Album (Strictly Lata Mangeshkar, Top Hits First)
        OnlineAlbumModel(
            id = "curated_lata_mangeshkar",
            title = "Evergreen Lata Mangeshkar Classics",
            year = "Artist",
            artist = "Artist • Lata Mangeshkar",
            primaryArtists = "Lata Mangeshkar",
            coverArtUrl = "https://c.saavncdn.com/artists/Lata_Mangeshkar_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Lata Mangeshkar classics hits",
            songCount = 12,
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
                "Didi Tera Devar Deewana Lata Mangeshkar",
                "Chura Liya Hai Tumne Jo Dil Ko Lata Mangeshkar",
                "Aap Ki Ankhon Mein Kuch Lata Mangeshkar"
            )
        ),
        // Mohammed Rafi Curated Artist Album (Strictly Mohammed Rafi, Top Hits First)
        OnlineAlbumModel(
            id = "curated_mohd_rafi",
            title = "Immortal Mohammed Rafi Hits",
            year = "Artist",
            artist = "Artist • Mohammed Rafi",
            primaryArtists = "Mohammed Rafi",
            coverArtUrl = "https://c.saavncdn.com/artists/Mohammed_Rafi_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Mohammed Rafi hits",
            songCount = 12,
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
                "Taarif Karoon Kya Uski Mohammed Rafi",
                "Ye Reshmi Zulfein Mohammed Rafi",
                "Deewana Hua Badal Mohammed Rafi"
            )
        ),
        // Shreya Ghoshal Curated Artist Album (Strictly Shreya Ghoshal, Top Hits First)
        OnlineAlbumModel(
            id = "curated_shreya_ghoshal",
            title = "Shreya Ghoshal",
            year = "Artist",
            artist = "Artist • Shreya Ghoshal",
            primaryArtists = "Shreya Ghoshal",
            coverArtUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
            type = "Artist",
            source = "curated",
            searchQuery = "Shreya Ghoshal top hits",
            songCount = 12,
            trackQueries = listOf(
                "Ghoomar Shreya Ghoshal Swaroop Khan",
                "Deewani Mastani Shreya Ghoshal",
                "Sunn Raha Hai Shreya Ghoshal",
                "Teri Ore Shreya Ghoshal Rahat Fateh Ali Khan",
                "Manwa Laage Shreya Ghoshal Arijit Singh",
                "Saibo Shreya Ghoshal Sachin Jigar",
                "Barso Re Shreya Ghoshal A R Rahman",
                "Piyu Bole Shreya Ghoshal Sonu Nigam",
                "Thodi Der Shreya Ghoshal Farhan Saeed",
                "Param Sundari Shreya Ghoshal",
                "Nagada Sang Dhol Shreya Ghoshal Osman Mir",
                "Dola Re Dola Shreya Ghoshal Kavita Krishnamurthy"
            )
        ),
        // Padmaavat EP
        OnlineAlbumModel(
            id = "curated_padmaavat",
            title = "Padmaavat",
            year = "EP",
            artist = "EP • Sanjay Leela Bhansali",
            primaryArtists = "Sanjay Leela Bhansali",
            coverArtUrl = "https://c.saavncdn.com/993/Padmaavat-Hindi-2018-20180122-500x500.jpg",
            type = "EP",
            source = "curated",
            searchQuery = "Padmaavat Sanjay Leela Bhansali songs",
            songCount = 6,
            trackQueries = listOf(
                "Ghoomar Shreya Ghoshal Swaroop Khan",
                "Khalibali Shivam Pathak",
                "Binte Dil Arijit Singh",
                "Ek Dil Ek Jaan Shivam Pathak",
                "Nainowale Ne Neeti Mohan",
                "Holi Padmaavat Richa Sharma"
            )
        ),
        // My songs Curated Playlist
        OnlineAlbumModel(
            id = "curated_my_songs",
            title = "My songs",
            year = "Playlist",
            artist = "Playlist • Jay",
            primaryArtists = "Jay",
            coverArtUrl = "https://c.saavncdn.com/440/Yeh-Jawaani-Hai-Deewani-2013-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Romantic Hindi trending hits",
            songCount = 12,
            trackQueries = listOf(
                "Tum Hi Ho Arijit Singh Mithoon",
                "Channa Mereya Arijit Singh Pritam",
                "Agar Tum Saath Ho Arijit Singh Alka Yagnik",
                "Kesariya Arijit Singh Pritam",
                "Apna Bana Le Arijit Singh Sachin Jigar",
                "Raataan Lambiyan Jubin Nautiyal Tanishk Bagchi",
                "Hawayein Arijit Singh Pritam",
                "Saiyaara Tanishk Bagchi Faheem Abdullah",
                "Dil Diyan Gallan Atif Aslam",
                "Ghungroo Arijit Singh Shilpa Rao Vishal Shekhar",
                "Subhanallah Yeh Jawaani Hai Deewani Sreerama Chandra",
                "Kabira Yeh Jawaani Hai Deewani Tochi Raina Rekha Bhardwaj"
            )
        ),
        // Sadi ke fall sa Playlist
        OnlineAlbumModel(
            id = "curated_sadi_ke_fall_sa",
            title = "Sadi ke fall sa",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Pritam, Nakash Aziz",
            coverArtUrl = "https://c.saavncdn.com/134/R-Rajkumar-Hindi-2025-20250930143440-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Saree Ke Fall Sa R Rajkumar songs",
            songCount = 6,
            trackQueries = listOf(
                "Saree Ke Fall Sa Nakash Aziz Antara Mitra Pritam",
                "Gandi Baat Mika Singh Kalpana Patowary Pritam",
                "Dhokha Dhadi Arijit Singh Palak Muchhal Pritam",
                "Mat Maari Kunal Ganjawala Sunidhi Chauhan Pritam",
                "Kaddu Katega Antara Mitra Pritam",
                "Gandi Baat Film Version Mika Singh Pritam"
            )
        ),
        // Love song 🥰💖 Playlist
        OnlineAlbumModel(
            id = "curated_love_songs_emoji",
            title = "Love song 🥰💖",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Bollywood Romantic",
            coverArtUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Love songs romantic Hindi hits",
            songCount = 12,
            trackQueries = listOf(
                "Saiyaara Tanishk Bagchi Faheem Abdullah",
                "Agar Tum Saath Ho Arijit Singh Alka Yagnik",
                "Apna Bana Le Arijit Singh Sachin Jigar",
                "Raataan Lambiyan Jubin Nautiyal Asees Kaur",
                "Tum Mile Neeraj Shridhar Pritam",
                "Pehli Nazar Mein Atif Aslam Pritam",
                "Tere Hawaale Arijit Singh Shilpa Rao Pritam",
                "Kesariya Arijit Singh Pritam",
                "Tum Hi Ho Arijit Singh Mithoon",
                "Hawayein Arijit Singh Pritam",
                "Raabta Agent Vinod Arijit Singh Pritam",
                "Subhanallah Yeh Jawaani Hai Deewani Sreerama Chandra"
            )
        ),
        OnlineAlbumModel(
            id = "curated_90s_bollywood",
            title = "Golden 90s Bollywood Hits",
            year = "1990s",
            artist = "Kumar Sanu, Alka Yagnik, Udit Narayan",
            primaryArtists = "Kumar Sanu, Alka Yagnik, Udit Narayan",
            coverArtUrl = "https://c.saavncdn.com/588/Dilwale-Dulhania-Le-Jayenge-Hindi-1995-20171114-500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/120/Sholay-Unknown-2008-20260914164528-500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/907/Kuch-Kuch-Hota-Hai-Original-Motion-Picture-Soundtrack-Hindi-1998-20240711192101-500x500.jpg",
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
            id = "curated_90s_pop_rock",
            title = "90s Pop & Rock Anthems",
            year = "1990 - 1999",
            artist = "Bryan Adams, Queen, Backstreet Boys",
            primaryArtists = "Bryan Adams, Queen, Backstreet Boys",
            coverArtUrl = "https://c.saavncdn.com/artists/Bryan_Adams_004_20190312090849_500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/artists/Jagjit_Singh_500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/046/Judwaa-Original-Motion-Picture-Soundtrack-Hindi-1996-20260314080148-500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/689/Sanam-Teri-Kasam-Hindi-2008-20260820195719-500x500.jpg",
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
            coverArtUrl = "https://c.saavncdn.com/588/Dilwale-Dulhania-Le-Jayenge-Hindi-1995-20171114-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "90s bollywood",
            songCount = 12,
            trackQueries = listOf(
                "Tujhe Dekha To Kumar Sanu DDLJ",
                "Pardesi Pardesi Udit Narayan Alka Yagnik Raja Hindustani",
                "Dheere Dheere Se Meri Zindagi Kumar Sanu Aashiqui",
                "Kuch Kuch Hota Hai Udit Narayan Alka Yagnik",
                "Tip Tip Barsa Paani Alka Yagnik Udit Narayan Mohra",
                "Chura Ke Dil Mera Kumar Sanu Alka Yagnik Main Khiladi Tu Anari",
                "Raja Ko Rani Se Pyar Ho Gaya Udit Narayan Alka Yagnik",
                "Baazigar O Baazigar Kumar Sanu Alka Yagnik",
                "Aaye Ho Meri Zindagi Mein Udit Narayan",
                "Mera Dil Bhi Kitna Pagal Hai Kumar Sanu Alka Yagnik Saajan",
                "Pehla Nasha Udit Narayan Sadhana Sargam Jo Jeeta Wohi Sikandar",
                "Bahon Ke Darmiyan Hariharan Alka Yagnik Khamoshi"
            )
        ),
        OnlineAlbumModel(
            id = "curated_old_is_gold_playlist",
            title = "Old is Gold",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Recordify",
            coverArtUrl = "https://c.saavncdn.com/120/Sholay-Unknown-2008-20260914164528-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Old is Gold Hindi hits",
            songCount = 12,
            trackQueries = listOf(
                "Likhe Jo Khat Tujhe Mohammed Rafi Kanyadaan",
                "Pal Pal Dil Ke Paas Kishore Kumar Blackmail",
                "Dekha Ek Khwab Lata Mangeshkar Kishore Kumar Silsila",
                "Chura Liya Hai Tumne Jo Dil Ko Asha Bhosle Mohammed Rafi Yaadon Ki Baaraat",
                "Yeh Sham Mastani Kishore Kumar Kati Patang",
                "Lag Ja Gale Se Phir Lata Mangeshkar Woh Kaun Thi",
                "Mere Samne Wali Khidki Mein Kishore Kumar Padosan",
                "Gulabi Aankhen Mohammed Rafi The Train",
                "Yeh Dosti Hum Nahi Todenge Kishore Kumar Manna Dey Sholay",
                "Badan Pe Sitare Lapete Huye Mohammed Rafi Prince",
                "Tere Bina Zindagi Se Lata Mangeshkar Kishore Kumar Aandhi",
                "O Mere Dil Ke Chain Kishore Kumar Mere Jeevan Saathi"
            )
        ),
        OnlineAlbumModel(
            id = "curated_arijit_sad_playlist",
            title = "Arijit Singh All Sad Songs 💔🥀",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Arijit Singh",
            coverArtUrl = "https://c.saavncdn.com/257/Ae-Dil-Hai-Mushkil-Hindi-2016-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Arijit Singh sad songs",
            songCount = 12,
            trackQueries = listOf(
                "Channa Mereya Arijit Singh Ae Dil Hai Mushkil",
                "Tum Hi Ho Arijit Singh Aashiqui 2",
                "Ae Dil Hai Mushkil Arijit Singh Pritam",
                "Hamari Adhuri Kahani Arijit Singh Jeet Gannguli",
                "Roke Na Ruke Naina Arijit Singh Badrinath Ki Dulhania",
                "Khairiyat Arijit Singh Chhichhore Pritam",
                "Tujhe Kitna Chahne Lage Arijit Singh Kabir Singh Mithoon",
                "Pachtaoge Arijit Singh Jaani B Praak",
                "Agar Tum Saath Ho Arijit Singh Alka Yagnik Tamasha",
                "Muskurane Arijit Singh Citylights Jeet Gannguli",
                "Tera Yaar Hoon Main Arijit Singh Sonu Ke Titu Ki Sweety",
                "Judaai Arijit Singh Rekha Bhardwaj Badlapur"
            )
        ),
        OnlineAlbumModel(
            id = "curated_sad_hindi_songs_playlist",
            title = "Sad Hindi Songs 🥺💔 (Mood Off 🥺)",
            year = "Playlist",
            artist = "Playlist • Recordify",
            primaryArtists = "Hindi Sad Hits",
            coverArtUrl = "https://c.saavncdn.com/807/Kabir-Singh-Hindi-2019-20240131131003-500x500.jpg",
            type = "Playlist",
            source = "curated",
            searchQuery = "Sad Hindi songs heartbreak",
            songCount = 12,
            trackQueries = listOf(
                "Thodi Der Farhan Saeed Shreya Ghoshal Half Girlfriend",
                "Tum Hi Aana Jubin Nautiyal Marjaavaan Payal Dev",
                "Jo Tum Mere Ho Anuv Jain",
                "Zaroorat Mustafa Zahid Ek Villain Mithoon",
                "Bewajah Sanam Teri Kasam Himesh Reshammiya",
                "Kaise Hua Vishal Mishra Kabir Singh",
                "Bekhayali Sachet Tandon Kabir Singh",
                "Tera Ban Jaunga Akhil Sachdeva Tulsi Kumar Kabir Singh",
                "Hasi Shreya Ghoshal Ami Mishra Hamari Adhuri Kahani",
                "Main Dhoondne Ko Zamaane Mein Arijit Singh Heartless",
                "Sunn Raha Hai Ankit Tiwari Aashiqui 2",
                "Channa Mereya Arijit Singh Ae Dil Hai Mushkil"
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
