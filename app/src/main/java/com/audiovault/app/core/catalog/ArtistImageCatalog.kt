package com.audiovault.app.core.catalog

/**
 * Curated high-resolution CDN images for popular domestic and global artists.
 * Used on Recordify's Home Screen ("Your Top Artists"), Search, and Artist Detail views.
 */
object ArtistImageCatalog {

    private val ARTIST_IMAGE_MAP: Map<String, String> = mapOf(
        // Indian & Regional Artists
        "arijit singh" to "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg",
        "shreya ghoshal" to "https://c.saavncdn.com/artists/Shreya_Ghoshal_004_20221104100347_500x500.jpg",
        "diljit dosanjh" to "https://c.saavncdn.com/artists/Diljit_Dosanjh_004_20221014075535_500x500.jpg",
        "pritam" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/3d/c7/43/3dc74387-e7f4-2342-397c-4cf2037c69a5/8902894623223_cover.jpg/500x500bb.jpg",
        "kk" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/a7/e9/f5/a7e9f51f-bd4a-55bc-4fc5-cd2b30bf91ea/8905574959759.jpg/500x500bb.jpg",
        "atif aslam" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/38/44/21/384421ca-69a0-83f2-0a34-88653d45db97/8901854001385.jpg/500x500bb.jpg",
        "sidhu moose wala" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/37/ca/50/37ca5028-a2dc-975b-6366-6427c2b81761/5064089945930_cover.jpg/500x500bb.jpg",
        "ar rahman" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/70/c2/f2/70c2f254-1eb5-2c77-f21c-55d953a65483/1200220383139.jpg/500x500bb.jpg",
        "a.r. rahman" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/70/c2/f2/70c2f254-1eb5-2c77-f21c-55d953a65483/1200220383139.jpg/500x500bb.jpg",
        "sachin jigar" to "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/2e/0b/c0/2e0bc070-112f-a827-6ad8-6bc64f7caaff/840214460180.png/500x500bb.jpg",
        "sachin-jigar" to "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/2e/0b/c0/2e0bc070-112f-a827-6ad8-6bc64f7caaff/840214460180.png/500x500bb.jpg",
        "darshan raval" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/be/59/d6/be59d661-7ea6-40cc-1081-b1fcfd6d2c89/1200220170838.jpg/500x500bb.jpg",
        "anirudh ravichander" to "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20231113063556_500x500.jpg",
        "anirudh" to "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20231113063556_500x500.jpg",
        "sonu nigam" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/7b/2c/2a/7b2c2ad4-4b98-56e6-118e-ed735885e9b2/8905574966177.jpg/500x500bb.jpg",
        "badshah" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/14/b8/58/14b85883-40a4-0a2e-de65-130f55726ee6/840780180390.png/500x500bb.jpg",
        "ap dhillon" to "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/5a/ac/00/5aac005f-9403-70e4-bce0-cf452017476e/197189606472.jpg/500x500bb.jpg",
        "kishore kumar" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/15/ff/ab/15ffab46-9dbb-8b61-ddd9-8e3d1f6ab192/8909024125442.png/500x500bb.jpg",
        "lata mangeshkar" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/38/44/21/384421ca-69a0-83f2-0a34-88653d45db97/8901854001385.jpg/500x500bb.jpg",
        "neha kakkar" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/87/78/db/8778db0e-988f-74d5-b217-3e898a51625d/886446897370.jpg/500x500bb.jpg",
        "amit trivedi" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/15/ff/ab/15ffab46-9dbb-8b61-ddd9-8e3d1f6ab192/8909024125442.png/500x500bb.jpg",
        "yo yo honey singh" to "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_002_20221208075308_500x500.jpg",
        "honey singh" to "https://c.saavncdn.com/artists/Yo_Yo_Honey_Singh_002_20221208075308_500x500.jpg",
        "aditya gadhvi" to "https://c.saavncdn.com/artists/Aditya_Gadhvi_000_20231019075727_500x500.jpg",
        "neeti mohan" to "https://c.saavncdn.com/artists/Neeti_Mohan_004_20220610091410_500x500.jpg",
        "vishal mishra" to "https://c.saavncdn.com/artists/Vishal_Mishra_004_20230626071415_500x500.jpg",
        "alka yagnik" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/38/44/21/384421ca-69a0-83f2-0a34-88653d45db97/8901854001385.jpg/500x500bb.jpg",
        "kumar sanu" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/38/44/21/384421ca-69a0-83f2-0a34-88653d45db97/8901854001385.jpg/500x500bb.jpg",
        "udit narayan" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/38/44/21/384421ca-69a0-83f2-0a34-88653d45db97/8901854001385.jpg/500x500bb.jpg",
        "sunidhi chauhan" to "https://c.saavncdn.com/artists/Sunidhi_Chauhan_003_20220407062402_500x500.jpg",
        "mohit chauhan" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/3d/c7/43/3dc74387-e7f4-2342-397c-4cf2037c69a5/8902894623223_cover.jpg/500x500bb.jpg",
        "jubin nautiyal" to "https://c.saavncdn.com/artists/Jubin_Nautiyal_005_20221124140026_500x500.jpg",
        "b praak" to "https://c.saavncdn.com/artists/B_Praak_002_20220606085601_500x500.jpg",
        "king" to "https://c.saavncdn.com/artists/King_006_20230818062547_500x500.jpg",
        "karan aujla" to "https://c.saavncdn.com/artists/Karan_Aujla_005_20230818062804_500x500.jpg",
        "guru randhawa" to "https://c.saavncdn.com/artists/Guru_Randhawa_003_20221104100650_500x500.jpg",

        // Global Pop, Hip-Hop & International Artists
        "taylor swift" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/a0/dd/fd/a0ddfd72-ee9e-f046-6466-a5dbefc696fa/26UM1IM21436.rgb.jpg/500x500bb.jpg",
        "drake" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/72/ec/cc/72eccc7b-8428-eff7-459d-ee2ddd442e11/808391286016.jpg/500x500bb.jpg",
        "the weeknd" to "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/a0/dd/fd/a0ddfd72-ee9e-f046-6466-a5dbefc696fa/26UM1IM21436.rgb.jpg/500x500bb.jpg",
        "billie eilish" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/92/9f/69/929f69f1-9977-3a44-d674-11f70c852d1b/24UMGIM36186.rgb.jpg/500x500bb.jpg",
        "ariana grande" to "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/56/f4/96/56f49612-02dd-83f4-44fe-d2118cc70707/26UMGIM51129.rgb.jpg/500x500bb.jpg",
        "ed sheeran" to "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/71/34/ec/7134ec98-7570-8b4d-176c-38fa910d65b7/5054197587842.jpg/500x500bb.jpg",
        "kendrick lamar" to "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/3b/b1/08/3bb10825-fef7-f472-a42e-13c545305fc5/22UMGIM46097.rgb.jpg/500x500bb.jpg",
        "post malone" to "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/d5/a2/2a/d5a22aa6-981f-4a0b-1934-297eb0dca191/23UMGIM70377.rgb.jpg/500x500bb.jpg",
        "bruno mars" to "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/bc/92/1f/bc921f00-3486-1d18-9104-e300109f3e4d/075679904297.jpg/500x500bb.jpg",
        "eminem" to "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/58/01/a5/5801a52e-c774-ec68-f99a-f4efb1eef750/22UMGIM69450.rgb.jpg/500x500bb.jpg",
        "justin bieber" to "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/30/e2/a9/30e2a906-81cf-ca5a-73c3-e847c2fe1f99/21UMGIM13467.rgb.jpg/500x500bb.jpg",
        "dua lipa" to "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/95/95/68/959568bc-3932-a5dc-51b6-a5db324f923b/5054197926290.jpg/500x500bb.jpg",
        "coldplay" to "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b4/0f/b6/b40fb68d-85f9-2b0e-f495-2aa3ef974b97/5054197143161.jpg/500x500bb.jpg",
        "adele" to "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/7b/72/7f/7b727f4f-4d0d-27b0-8f96-5f72cf9b1836/886449666430.jpg/500x500bb.jpg",
        "bts" to "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/f5/61/44/f5614486-318e-4a67-152e-2e0f4cbca7f3/196626601449.jpg/500x500bb.jpg",
        "blackpink" to "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/0e/e6/78/0ee678a9-4632-15be-6663-b184ca717f90/22UMGIM83870.rgb.jpg/500x500bb.jpg"
    )

    /**
     * Resolves high-resolution official CDN photo URL for the given artist name.
     * Returns empty string if unknown.
     */
    fun getImageUrl(artistName: String?): String {
        if (artistName.isNullOrBlank()) return ""
        val normalized = artistName.trim().lowercase()

        // 1. Direct exact key match
        ARTIST_IMAGE_MAP[normalized]?.let { return it }

        // 2. Substring or primary token match
        for ((key, url) in ARTIST_IMAGE_MAP) {
            if (normalized == key || normalized.startsWith("$key ") || normalized.contains(" $key ") || normalized.endsWith(" $key")) {
                return url
            }
        }

        // 3. Fallback: match without dots or hyphens
        val stripped = normalized.replace(".", "").replace("-", " ")
        ARTIST_IMAGE_MAP[stripped]?.let { return it }

        return ""
    }
}
