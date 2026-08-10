package vn.phs.iptv.ui.theme

import androidx.compose.ui.graphics.Color

// ── Apple design tokens (design.md §Colors) ─────────────────────────────────────
// One interactive accent — Action Blue. No second brand color. No gradients.

// Brand & accent — 5-star hotel champagne gold (the single luxury accent).
val Gold           = Color(0xFFC9A45C)   // primary accent
val GoldBright     = Color(0xFFE6C57E)   // focus / highlight
val GoldDeep       = Color(0xFF9C7B2E)   // metallic shadow side
val OnGold         = Color(0xFF1A1407)   // near-black-brown text on gold fills
// Accent aliases — every former "Action Blue" surface now reads gold.
val ActionBlue     = Gold
val FocusBlue      = GoldBright
val SkyLinkBlue    = GoldBright

// Surfaces
val Canvas         = Color(0xFFFFFFFF)   // pure white
val Parchment      = Color(0xFFF5F5F7)   // signature Apple off-white
val SurfacePearl   = Color(0xFFFAFAFC)   // ghost-button fill
val SurfaceTile1   = Color(0xFF1B1E27)   // primary card surface — deep, faintly cool (not flat gray)
val SurfaceTile2   = Color(0xFF242838)   // focused row / raised
val SurfaceTile3   = Color(0xFF15171E)   // micro-step darker
val SurfaceBlack   = Color(0xFF000000)   // true void — nav, video, overlays
val ChipTranslucent = Color(0xFFD2D2D7)  // base of translucent control chips

// Ink / text
val Ink            = Color(0xFF1D1D1F)   // near-black — all text on light surfaces
val BodyOnDark     = Color(0xFFFFFFFF)   // text on dark tiles
val BodyMuted      = Color(0xFFCCCCCC)   // secondary copy on dark
val InkMuted80     = Color(0xFF333333)   // softened body on light
val InkMuted48     = Color(0xFF7A7A7A)   // disabled / fine print

// Hairlines
val DividerSoft    = Color(0xFFF0F0F0)
val Hairline       = Color(0xFFE0E0E0)

// ── Light-mode elevated surfaces (Apple System Colors) ──────────────────────────
// These replace the generic white-at-alpha approach with purpose-built fills that
// give cards depth and legibility on a bright canvas — matching real tvOS Light.
val LightCardFill    = Color(0xFFFBFBFD)   // warm near-white, NOT pure #FFF
val LightCardBorder  = Color(0xFFD1D1D6)   // Apple System Gray 4 — visible hairline
val LightNavRailBg   = Color(0xFFF2F2F7)   // Apple System Gray 6 — sidebar solid
val LightNavRailEdge = Color(0xFFE5E5EA)   // Apple System Gray 5 — sidebar right edge
val LightAccent      = Color(0xFF8B6914)   // Dark Gold — ≥ 4.5:1 on white / parchment

// ── Dark-tile companions tuned for a tvOS 10-foot canvas ────────────────────────
// The app lives mostly on dark tiles; these add the faint separations design.md
// builds from micro-step lightness changes between stacked dark surfaces.
val DarkCardRaised = Color(0xFF333336)   // a focusable card lifted off SurfaceTile1
val HairlineOnDark = Color(0x1FFFFFFF)   // 12% white — the only "line" on dark
val ScrimBlack     = Color(0xCC000000)   // 80% — overlays atop photography/video
