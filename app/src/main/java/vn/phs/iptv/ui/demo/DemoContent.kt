package vn.phs.iptv.ui.demo

import androidx.compose.ui.graphics.Color

// ── Fake demo content (UI/UX pass — no backend) ─────────────────────────────────
// Real royalty-free photography (Unsplash CDN), all verified, so the Apple-tvOS
// screens can be evaluated end-to-end before the PMS/Retrofit layer is wired.

private fun img(id: String, w: Int = 900) = "https://images.unsplash.com/photo-$id?w=$w&q=80&auto=format&fit=crop"

object Img {
    // Core
    val hotelSuite = img("1611892440504-42a792e24d32", 1280)
    val cinema     = img("1489599849927-2ee91cede3ba")
    val studio     = img("1542204165-65bf26472b9b", 1280)
    val foodPlate  = img("1414235077428-338989a2e8c0")
    val spa        = img("1540555700478-4be289fbecef")
    val cityNight  = img("1480714378408-67cf0d13bc1b")
    val stadium    = img("1431324155629-1a6deb1dec8d")
    val nature     = img("1441974231531-c6227db76b6e")
    val coffee     = img("1495474472287-4d71bcdd2085")
    val pho        = img("1582878826629-29b7ad1cdc43")
    val dessert    = img("1488477181946-6428a0291777")
    val microphone = img("1516280440614-37939bbacd81")

    // Movies (cinematic mood)
    val mvNoir     = img("1440404653325-ab127d49abc1")
    val mvAlley    = img("1485846234645-a62644f84728")
    val mvDesert   = img("1517604931442-7e0c8ed2963c")
    val mvSpace    = img("1574267432553-4b4628081c31")
    val mvRomance  = img("1536440136628-849c177e76a1")
    val mvEpic     = img("1500530855697-b586d89ba3ee")
    val mvRoad     = img("1470790376778-a9fbc86d70e2")

    // Series / drama
    val srLone     = img("1493514789931-586cb221d7a7")
    val srCity     = img("1531259683007-016a7b628fc3")
    val srTension  = img("1509347528160-9a9e33742cdb")
    val srShadow   = img("1485095329183-d0797cdc5676")
    val srMystery  = img("1542038784456-1ea8e935640e")
    val srWindow   = img("1485178575877-1a13bf489dfe")

    // Sports
    val spFootball  = img("1574680096145-d05b474e2155")
    val spBasket    = img("1546519638-68e109498ffc")
    val spTennis    = img("1554068865-24cecd4e34b8")
    val spGolf      = img("1530549387789-4c1017266635")
    val spF1        = img("1552674605-db6ffd4facb5")
    val spSwim      = img("1571019613454-1cb2f99b2d8b")

    // Music & relax
    val mzConcert   = img("1518604666860-9ed391f76460")
    val mzPiano     = img("1493225457124-a3eb161ffa5f")
    val mzVinyl     = img("1511192336575-5a79af67a629")
    val mzCalm      = img("1470225620780-dba8ba36b745")
    val mzMeditate  = img("1506157786151-b8491531f063")
    val mzSunrise   = img("1465847899084-d164df4dedc6")

    // Documentary / nature
    val dcWild      = img("1547036967-23d11aacaee0")
    val dcOcean     = img("1505765050516-f72dcac9c60e")
    val dcForest    = img("1426604966848-d7adac402bff")
    val dcSpace     = img("1551632436-cbf8dd35adfa")
    val dcUnder     = img("1559827260-dc66d52bef19")

    // Hotel services
    val svConcierge = img("1582719478250-c89cae4dc85b")
    val svDining    = img("1517248135467-4c7edcad34c4")
    val svPool      = img("1571896349842-33c89424de2d")
    val svGym       = img("1534438327276-14e5300c3a48")
    val svLaundry   = img("1545173168-9f1947eebb7f")

    // Static map artwork for the Help screen
    val map         = img("1524661135-423995f22d0b", 1280)
}

data class HomeTile(
    val id: String, val title: String, val subtitle: String,
    val imageUrl: String, val accent: Color, val comingSoon: Boolean = false,
)

data class Channel(
    val number: String, val name: String, val nowPlaying: String,
    val imageUrl: String, val accent: Color,
)

/** A film/series/sport/music lockup for the content shelves. */
data class Media(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val badge: String? = null,
    val progress: Float? = null,
    val accent: Color = Color(0xFF1B1E27),
)

data class FolioRow(val date: String, val description: String, val qty: Int, val amount: Long)

data class MenuItem(
    val id: String, val name: String, val description: String, val price: Long,
    val imageUrl: String, val category: String,
)

/** Static hotel facts for the Help screen (UI-only demo — no PMS wiring). */
data class HotelInfo(
    val name: String,
    val address: String,
    val phone: String,
    val reception: String,
    val checkIn: String,
    val checkOut: String,
    val wifiSsid: String,
    val wifiPassword: String,
    val mapImageUrl: String,
)

/** A bookable/visitable hotel facility for the Services screen. */
data class HotelService(
    val id: String,
    val title: String,
    val description: String,
    val hours: String,
    val imageUrl: String,
)

object Demo {

    val homeTiles = listOf(
        HomeTile("tv",        "Live TV",        "84 channels",      Img.studio,     Color(0xFF0A84FF)),
        HomeTile("bill",      "My Bill",        "Folio · Room 302", Img.hotelSuite, Color(0xFF30D158)),
        HomeTile("service",   "Room Service",   "24-hour dining",   Img.svDining,   Color(0xFFFF9F0A)),
        HomeTile("voice",     "Assistant",      "Ask anything",     Img.microphone, Color(0xFFFF375F)),
        HomeTile("spa",       "Spa & Wellness", "Book a treatment", Img.spa,        Color(0xFF64D2FF)),
        HomeTile("pool",      "Pool & Beach",   "Open till 20:00",  Img.svPool,     Color(0xFF0A84FF)),
        HomeTile("gym",       "Fitness",        "24-hour access",   Img.svGym,      Color(0xFFBF5AF2)),
        HomeTile("concierge", "Concierge",      "We're here for you", Img.svConcierge, Color(0xFFFFD60A)),
    )

    val channels = listOf(
        Channel("01", "VTV1",      "Thời sự 19:00",       Img.studio,     Color(0xFFE53935)),
        Channel("03", "VTV3",      "Vietnam Idol",        Img.microphone, Color(0xFF8E24AA)),
        Channel("18", "HBO",       "Dune: Part Two",      Img.cinema,     Color(0xFF1E88E5)),
        Channel("21", "Disney",    "Inside Out 2",        Img.mvRomance,  Color(0xFF00897B)),
        Channel("30", "Nat Geo",   "Planet Earth III",    Img.dcWild,     Color(0xFF43A047)),
        Channel("44", "ESPN",      "Premier League Live", Img.spFootball, Color(0xFFF4511E)),
        Channel("52", "CNN",       "World News Today",    Img.cityNight,  Color(0xFF3949AB)),
        Channel("70", "Bloomberg", "Markets Asia",        Img.cityNight,  Color(0xFF00ACC1)),
    )

    val continueWatching = listOf(
        Media("cw1", "The Last Horizon",      "58 min left",      Img.mvEpic,    progress = 0.42f),
        Media("cw2", "Saigon Nights",         "1 of 8 episodes",  Img.cityNight, progress = 0.7f),
        Media("cw3", "Blue Planet II",        "Ep 3 · Coral Reefs", Img.dcOcean, progress = 0.25f),
        Media("cw4", "Live in Hanoi",         "21 min left",      Img.mzConcert, progress = 0.85f),
        Media("cw5", "Grand Prix Weekend",    "Qualifying",       Img.spF1,      progress = 0.15f),
    )

    val movies = listOf(
        Media("m1", "Midnight Drive", "Thriller · 2024", Img.mvRoad),
        Media("m2", "The Summit",     "Adventure",       Img.mvEpic),
        Media("m3", "Aurora",         "Sci-Fi",          Img.mvSpace),
        Media("m4", "Noir",           "Mystery",         Img.mvNoir),
        Media("m5", "Last Light",     "Romance",         Img.mvRomance),
        Media("m6", "Dust Road",      "Western",         Img.mvDesert),
        Media("m7", "Backstreets",    "Crime",           Img.mvAlley),
        Media("m8", "Encore",         "Music",           Img.mzConcert),
    )

    val series = listOf(
        Media("s1", "The Concierge", "Season 2",        Img.srWindow),
        Media("s2", "Backstage",     "Limited series",  Img.srShadow),
        Media("s3", "Wild Vietnam",  "Nature",          Img.dcForest),
        Media("s4", "Night Market",  "Drama",           Img.srCity),
        Media("s5", "Quiet Hours",   "Mystery",         Img.srMystery),
        Media("s6", "Solitude",      "Drama",           Img.srLone),
    )

    val sports = listOf(
        Media("sp1", "Premier League", "LIVE · 2nd half", Img.spFootball, badge = "LIVE"),
        Media("sp2", "NBA Tonight",    "02:00",           Img.spBasket),
        Media("sp3", "ATP Finals",     "LIVE",            Img.spTennis,   badge = "LIVE"),
        Media("sp4", "Formula 1",      "Race Sunday",     Img.spF1),
        Media("sp5", "The Masters",    "Round 3",         Img.spGolf),
        Media("sp6", "World Aquatics", "Highlights",      Img.spSwim),
    )

    val relax = listOf(
        Media("r1", "Calm Piano",    "Focus & sleep",    Img.mzPiano),
        Media("r2", "Jazz Lounge",   "Evening set",      Img.mzVinyl),
        Media("r3", "Ocean Sounds",  "Nature ambience",  Img.dcUnder),
        Media("r4", "Spa Serenity",  "Wellness mix",     Img.mzCalm),
        Media("r5", "Meditation",    "10-minute reset",  Img.mzMeditate),
        Media("r6", "Sunrise",       "Morning acoustic", Img.mzSunrise),
    )

    val documentary = listOf(
        Media("d1", "Wild Earth",    "Wildlife",     Img.dcWild),
        Media("d2", "Blue Deep",     "Ocean",        Img.dcOcean),
        Media("d3", "High Country",  "Mountains",    Img.nature),
        Media("d4", "Ancient Woods", "Forest",       Img.dcForest),
        Media("d5", "Our Planet",    "From space",   Img.dcSpace),
        Media("d6", "Coral",         "Underwater",   Img.dcUnder),
    )

    val folio = listOf(
        FolioRow("Jun 28", "Room charge — Deluxe King", 1, 2_400_000),
        FolioRow("Jun 28", "Welcome minibar",           1,   180_000),
        FolioRow("Jun 29", "Room charge — Deluxe King", 1, 2_400_000),
        FolioRow("Jun 29", "Spa — Aromatherapy 60'",    1, 1_150_000),
        FolioRow("Jun 29", "Room service — Pho bò",     2,   320_000),
        FolioRow("Jun 30", "Breakfast — À la carte",    2,   460_000),
        FolioRow("Jun 30", "Laundry — Express",         3,   270_000),
    )
    val folioTotal: Long get() = folio.sumOf { it.amount }

    val menu = listOf(
        MenuItem("m1", "Phở bò Hà Nội",     "Slow-simmered beef broth, rice noodles", 160_000, Img.pho,     "Signature"),
        MenuItem("m2", "Gỏi cuốn tôm thịt", "Fresh spring rolls, peanut hoisin",      120_000, Img.svDining, "Starters"),
        MenuItem("m3", "Cơm tấm sườn",      "Grilled pork chop, broken rice",         180_000, Img.foodPlate, "Mains"),
        MenuItem("m4", "Club sandwich",      "Triple-decker, hand-cut fries",          210_000, Img.svDining, "Mains"),
        MenuItem("m5", "Cà phê sữa đá",     "Iced Vietnamese coffee",                  65_000, Img.coffee,   "Drinks"),
        MenuItem("m6", "Sinh tố xoài",      "Fresh mango smoothie",                    85_000, Img.coffee,   "Drinks"),
        MenuItem("m7", "Bánh flan caramel", "Crème caramel, sea-salt",                 75_000, Img.dessert,  "Desserts"),
    )

    val hotel = HotelInfo(
        name = "PHS Hotel · Hanoi",
        address = "12 Tràng Tiền, Hoàn Kiếm, Hà Nội, Việt Nam",
        phone = "+84 24 1234 5678",
        reception = "Dial 0 from your room",
        checkIn = "14:00",
        checkOut = "12:00",
        wifiSsid = "PHS_Guest",
        wifiPassword = "welcome2026",
        mapImageUrl = Img.map,
    )

    // id matches the home-tile / nav id so localized copy can be keyed consistently.
    val hotelServices = listOf(
        HotelService("spa", "Spa & Wellness", "Aromatherapy, hot-stone and couples' treatments in our top-floor spa.", "09:00 – 21:00", Img.spa),
        HotelService("pool", "Pool & Beach", "Heated outdoor infinity pool with towel and bar service.", "06:00 – 20:00", Img.svPool),
        HotelService("gym", "Fitness Centre", "Technogym cardio and free weights, open around the clock.", "24 hours", Img.svGym),
        HotelService("concierge", "Concierge", "Tours, transport and restaurant bookings — we arrange anything.", "24 hours", Img.svConcierge),
        HotelService("dining", "In-Room Dining", "All-day à la carte delivered to your room.", "24 hours", Img.svDining),
        HotelService("laundry", "Laundry & Pressing", "Same-day laundry, dry-cleaning and express pressing.", "08:00 – 18:00", Img.svLaundry),
    )

    val voiceSuggestions = listOf(
        "What time is breakfast?",
        "Order a pot of coffee to my room",
        "What's the Wi-Fi password?",
        "Late check-out tomorrow?",
    )
}

/** "1.150.000 ₫" — Vietnamese grouping for the demo. */
fun Long.vnd(): String {
    val s = this.toString().reversed().chunked(3).joinToString(".").reversed()
    return "$s ₫"
}
