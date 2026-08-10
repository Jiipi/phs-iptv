package vn.phs.iptv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import vn.phs.iptv.R

// ── Inter as the SF substitute (design.md §"Note on Font Substitutes") ───────────
// SF is Apple-proprietary; Inter is its closest open-source equivalent.
// InterDisplay (optical display cut) carries large titles; Inter (text cut) the rest.

val InterText = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

val InterDisplay = FontFamily(
    Font(R.font.inter_display_medium,   FontWeight.Medium),
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold,     FontWeight.Bold),
)

// ── tvOS type scale (research §4) ────────────────────────────────────────────────
// Apple's official tvOS sizes are points where 1pt ≈ 1px on a 1080p @1x canvas.
// Our Android TV AVD runs at density 2.0 (320dpi, the Android-TV norm), so 1sp = 2px;
// to land the SAME physical size as tvOS we use sp ≈ tvOS_pt / 2.
// Weight floor is Medium — tvOS forbids thin/light at 10-foot (research §4, §8).
// tvOS pt → sp:  Title1 76→38 · Title2 57→28 · Title3 48→24 · Headline 38→19
//                Callout 31→16 · Body 29→15 · Caption1 25→13 · Caption2 23→12
internal val PhsTypography = Typography(
    // Title 1 (76pt) — hero wordmark / big numerals
    displayLarge = TextStyle(
        fontFamily = InterDisplay, fontSize = 38.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 46.sp, letterSpacing = (-0.8).sp,
    ),
    // Title 2 (57pt) — screen hero headline
    displayMedium = TextStyle(
        fontFamily = InterDisplay, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 36.sp, letterSpacing = (-0.6).sp,
    ),
    // Title 3 (48pt)
    displaySmall = TextStyle(
        fontFamily = InterDisplay, fontSize = 25.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    // Headline (38pt) — section headers, shelf titles
    headlineLarge = TextStyle(
        fontFamily = InterDisplay, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 27.sp, letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterDisplay, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp, letterSpacing = (-0.2).sp,
    ),
    // Callout (31pt) — lead subcopy
    titleLarge = TextStyle(
        fontFamily = InterText, fontSize = 16.sp, fontWeight = FontWeight.Medium,
        lineHeight = 21.sp, letterSpacing = 0.sp,
    ),
    // Body emphasized — lockup titles
    titleMedium = TextStyle(
        fontFamily = InterText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    // Captions under a glyph or a QR — the smallest style still in the title ladder, so it
    // keeps Medium weight rather than dropping to a body weight.
    titleSmall = TextStyle(
        fontFamily = InterText, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    // Body (29pt) — Medium floor for 10-foot legibility
    bodyLarge = TextStyle(
        fontFamily = InterText, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        lineHeight = 22.sp, letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    // Caption 2 (23pt)
    bodySmall = TextStyle(
        fontFamily = InterText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        lineHeight = 16.sp, letterSpacing = 0.1.sp,
    ),
    // Buttons / labels
    labelLarge = TextStyle(
        fontFamily = InterText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        lineHeight = 16.sp, letterSpacing = 0.2.sp,
    ),
)
