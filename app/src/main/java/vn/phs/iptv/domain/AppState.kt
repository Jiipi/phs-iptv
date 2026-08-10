package vn.phs.iptv.domain

// ── Navigation screens (PRD §12 state machine) ───────────────────────────────
sealed interface AppScreen {
    data object Loading      : AppScreen   // transient: reading DataStore on init
    data object Provisioning : AppScreen   // F0 — device not paired
    data object Idle         : AppScreen   // F0 — paired, no guest
    data object Welcome      : AppScreen   // F1 — guest checked in
    data object IntroVideo   : AppScreen   // F2 — autoplay video
    data object Language     : AppScreen   // guest picks language before entering the hub
    data object Home         : AppScreen   // hub after video
    data object LiveTv       : AppScreen   // live channel grid
    data object Folio        : AppScreen   // F4
    data object Voice        : AppScreen   // F3
    data object Order        : AppScreen   // F5
    data object Help         : AppScreen   // hotel info / address / map / wifi
    data object Services     : AppScreen   // hotel facilities (spa/pool/gym...)
    data object HotelIntro   : AppScreen   // replayable hotel-intro film (from Home hero)
}

// ── Guest-selectable language (PRD §i18n) ────────────────────────────────────
enum class AppLanguage(
    val code: String,      // ISO 639-1, matches res/values-<code>
    val endonym: String,   // language's own name
    val english: String,   // English label
    val flag: String,      // emoji flag (fallback only — Android TV fonts lack flag glyphs)
) {
    VI("vi", "Tiếng Việt", "Vietnamese", "🇻🇳"),
    EN("en", "English", "English", "🇬🇧"),
    RU("ru", "Русский", "Russian", "🇷🇺"),
}

// ── Domain models (mirror of PRD §5.3 DTO) ───────────────────────────────────
data class GuestProfile(
    val name: String,
    val title: String,
    val roomNo: String,
    val nationality: String,
    val isBirthday: Boolean,
)

data class ProvisioningConfig(
    val deviceId: String,    // permanent UUID identity — one-time generated first boot
    val branchId: String,
    val roomId: String,
    val roomNo: String,
)
